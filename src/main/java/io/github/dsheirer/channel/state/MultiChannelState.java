/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.channel.state;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelConfigurationChangeNotification;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.ChannelStateIdentifier;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multi-Channel state tracks the overall state of all processing modules and decoders configured for the channel
 * and provides squelch control and decoder state reset events.
 *
 * Uses a state enumeration that defines allowable channel state transitions in order to track a call or data decode
 * event from start to finish.  Uses a timer to monitor for inactivity and to provide a FADE period that indicates
 * to the user that the activity has stopped while continuing to provide details about the call, before the state is
 * reset to IDLE.
 *
 * State Descriptions:
 * IDLE:  Normal state. No voice or data call activity
 * CALL/DATA/ENCRYPTED/CONTROL:  Decoding states.
 * FADE:  The phase after a voice or data call when either an explicit call end has been received, or when no new
 * signalling updates have been received, and the fade timer has expired.  This phase allows for gui updates to
 * signal to the user that the call is ended, while continuing to display the call details for the user
 * TEARDOWN:  Indicates a traffic channel that will be torn down for reuse.
 */
public class MultiChannelState extends AbstractChannelState implements IDecoderStateEventListener, ISourceEventListener,
    IdentifierUpdateListener, IStateMachineListener
{
    private final static Logger mLog = LoggerFactory.getLogger(MultiChannelState.class);

    public static final long FADE_TIMEOUT_DELAY = 1200;
    public static final long RESET_TIMEOUT_DELAY = 2000;

    private IdentifierUpdateNotificationProxy mIdentifierUpdateNotificationProxy = new IdentifierUpdateNotificationProxy();
    private DecoderStateEventReceiver mDecoderStateEventReceiver = new DecoderStateEventReceiver();
    private SourceEventListener mInternalSourceEventListener;
    private Map<Integer,ChannelMetadata> mChannelMetadataMap = new TreeMap<>();
    private Map<Integer,MutableIdentifierCollection> mIdentifierCollectionMap = new TreeMap<>();
    private Map<Integer,StateMachine> mStateMachineMap = new TreeMap<>();
    private Map<Integer,StateMonitoringSquelchController> mSquelchControllerMap = new TreeMap<>();
    private int[] mTimeslots;
    private DecoderStateNotificationEventCache mStateNotificationCache = new DecoderStateNotificationEventCache();
    private Listener<IdentifierUpdateNotification> mIdentifierUpdateListener = new IdentifierUpdateListenerProxy();

    /**
     * Constructs an instance
     * @param channel configuration
     * @param aliasModel for channel metadata and identifiers
     * @param timeslots array of timeslot numbers to use
     */
    public MultiChannelState(Channel channel, AliasModel aliasModel, int[] timeslots)
    {
        super(channel);

        mTimeslots = timeslots;

        for(int timeslot: timeslots)
        {
            mChannelMetadataMap.put(timeslot, new ChannelMetadata(aliasModel, timeslot));
            MutableIdentifierCollection mutableIdentifierCollection = new MutableIdentifierCollection(timeslot);
            mIdentifierCollectionMap.put(timeslot, mutableIdentifierCollection);

            //Set the proxy as a listener so that echo'd updates are broadcast externally
            mutableIdentifierCollection.setIdentifierUpdateListener(mIdentifierUpdateNotificationProxy);

            StateMachine stateMachine = new StateMachine(timeslot, State.MULTI_CHANNEL_ACTIVE_STATES);
            mStateMachineMap.put(timeslot, stateMachine);
            stateMachine.addListener(this);

            StateMonitoringSquelchController squelchController = new StateMonitoringSquelchController(timeslot);
            mSquelchControllerMap.put(timeslot, squelchController);
            stateMachine.addListener(squelchController);

            stateMachine.setIdentifierUpdateListener(mutableIdentifierCollection);
            stateMachine.setEndTimeoutBufferMilliseconds(RESET_TIMEOUT_DELAY);
            stateMachine.setFadeTimeoutBufferMilliseconds(FADE_TIMEOUT_DELAY);
        }

        configureChannelType(channel);

        createConfigurationIdentifiers(channel);
    }

    /**
     * Configure items according to channel type
     * @param channel configuration
     */
    private void configureChannelType(Channel channel)
    {
        for(int timeslot: mTimeslots)
        {
            StateMachine stateMachine = mStateMachineMap.get(timeslot);
            stateMachine.setChannelType(channel.getChannelType());
        }
    }

    /**
     * Receive notification that the underlying channel configuration has changed.
     * @param notification
     */
    @Subscribe
    public void channelConfigurationChanged(ChannelConfigurationChangeNotification notification)
    {
        updateChannelConfiguration(notification.getChannel());
        configureChannelType(notification.getChannel());
        createConfigurationIdentifiers(notification.getChannel());
    }

    @Override
    public void stateChanged(State state, int timeslot)
    {
        ChannelStateIdentifier stateIdentifier = ChannelStateIdentifier.get(state);
        mIdentifierCollectionMap.get(timeslot).update(stateIdentifier);
        mChannelMetadataMap.get(timeslot).receive(new IdentifierUpdateNotification(stateIdentifier, IdentifierUpdateNotification.Operation.ADD, timeslot));

        switch(state)
        {
            case RESET:
                reset(timeslot);
                mStateMachineMap.get(timeslot).setState(State.IDLE);
                break;
            case TEARDOWN:
                mTeardownSequenceStarted = true;
                if(getChannel().isTrafficChannel())
                {
                    checkTeardown();
                }
                else
                {
                    mStateMachineMap.get(timeslot).setState(State.RESET);
                }
                break;
        }
    }

    /**
     * Checks the state of each timeslot and issues a teardown request if all timeslots are inactive
     */
    private void checkTeardown()
    {
        boolean teardown = false;
        boolean active = false;

        for(StateMachine stateMachine: mStateMachineMap.values())
        {
            State state = stateMachine.getState();

            //If we have an active state in either timeslot, don't teardown.  IDLE is a special state that is active
            //but doesn't prevent a teardown when the other timeslot is in TEARDOWN
            if(State.MULTI_CHANNEL_ACTIVE_STATES.contains(state) && state != State.IDLE)
            {
                active = true;
            }
            else if(state == State.TEARDOWN)
            {
                teardown = true;
            }
        }

        if(teardown && !active)
        {
            if(getChannel().isTrafficChannel())
            {
                try
                {
                    broadcast(new ChannelEvent(getChannel(), ChannelEvent.Event.REQUEST_DISABLE));
                }
                catch(Throwable t)
                {
                    mLog.error("Error broadcasting shutdown channel event", t);
                }

                mTeardownSequenceStarted = true;
            }
            else
            {
                for(StateMachine stateMachine: mStateMachineMap.values())
                {
                    stateMachine.setState(State.RESET);
                }
            }
        }
        //If one timeslot is teardown but the other is still active, reset the teardown timeslot to IDLE
        else if(teardown && active)
        {
            for(StateMachine stateMachine: mStateMachineMap.values())
            {
                if(stateMachine.getState() == State.TEARDOWN)
                {
                    stateMachine.setState(State.RESET);
                }
            }
        }
    }

    @Override
    protected void checkState()
    {
        for(StateMachine stateMachine: mStateMachineMap.values())
        {
            stateMachine.checkState();
        }
    }

    @Override
    public boolean isTeardownState()
    {
        for(StateMachine stateMachine: mStateMachineMap.values())
        {
            if(stateMachine.getState() == State.TEARDOWN)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates configuration identifiers for the channel name, system, site and alias list name.
     */
    private void createConfigurationIdentifiers(Channel channel)
    {
        for(int timeslot: mTimeslots)
        {
            MutableIdentifierCollection identifierCollection = mIdentifierCollectionMap.get(timeslot);

            identifierCollection.update(DecoderTypeConfigurationIdentifier.create(channel.getDecodeConfiguration().getDecoderType()));

            if(channel.hasSystem())
            {
                identifierCollection.update(SystemConfigurationIdentifier.create(channel.getSystem()));
            }
            if(channel.hasSite())
            {
                identifierCollection.update(SiteConfigurationIdentifier.create(channel.getSite()));
            }
            if(channel.getName() != null && !channel.getName().isEmpty())
            {
                identifierCollection.update(ChannelNameConfigurationIdentifier.create(channel.getName()));
            }
            if(channel.getAliasListName() != null && !channel.getAliasListName().isEmpty())
            {
                identifierCollection.update(AliasListConfigurationIdentifier.create(channel.getAliasListName()));
            }
            if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
            {
                long frequency = ((SourceConfigTuner)channel.getSourceConfiguration()).getFrequency();
                identifierCollection.update(FrequencyConfigurationIdentifier.create(frequency));
            }
            else if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER_MULTIPLE_FREQUENCIES)
            {
                List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).getFrequencies();

                if(frequencies.size() > 0)
                {
                    identifierCollection.update(FrequencyConfigurationIdentifier.create(frequencies.get(0)));
                }
            }
        }
    }

    /**
     * Interface to receive channel identifier updates from this channel state and from any
     * decoder states.
     */
    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return mIdentifierUpdateListener;
    }

    /**
     * Registers the external listener that will receive identifier update notifications produced by this channel state
     * @param listener
     */
    @Override
    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        mIdentifierUpdateNotificationProxy.setListener(listener);
    }

    /**
     * Unregisters the external listener from receiving identifier update notifications produced by this channel state
     */
    @Override
    public void removeIdentifierUpdateListener()
    {
        mIdentifierUpdateNotificationProxy.setListener(null);
    }

    /**
     * Updates the channel state identifier collection using the update notification.  This update will be reflected
     * in the internal channel state and will also be broadcast to any listeners, including the channel metadata for
     * this channel state.
     */
    @Override
    public void updateChannelStateIdentifiers(IdentifierUpdateNotification notification)
    {
        //Explicitly add or remove the identifier from the local identifier collection to allow it to be rebroadcast
        //to external listeners, which includes this state's channel metadata
        MutableIdentifierCollection identifierCollection = mIdentifierCollectionMap.get(notification.getTimeslot());

        if(identifierCollection != null)
        {
            if(notification.isAdd())
            {
                identifierCollection.update(notification.getIdentifier());
            }
            else if(notification.isSilentAdd())
            {
                identifierCollection.silentUpdate(notification.getIdentifier());
            }
            else if(notification.isRemove())
            {
                identifierCollection.remove(notification.getIdentifier());
            }
            else if(notification.isSilentRemove())
            {
                identifierCollection.silentRemove(notification.getIdentifier());
            }
        }
    }

    /**
     * Channel metadata for this channel.
     */
    public List<ChannelMetadata> getChannelMetadata()
    {
        return new ArrayList<>(mChannelMetadataMap.values());
    }

    /**
     * Resets this channel state and prepares it for reuse.
     */
    @Override
    public void reset()
    {
        for(int timeslot: mTimeslots)
        {
            reset(timeslot);
        }

        sourceOverflow(false);
    }

    private void reset(int timeslot)
    {
        mStateMachineMap.get(timeslot).setState(State.RESET);
        broadcast(new DecoderStateEvent(this, Event.REQUEST_RESET, State.IDLE, timeslot));
        MutableIdentifierCollection identifierCollection = mIdentifierCollectionMap.get(timeslot);
        identifierCollection.remove(IdentifierClass.USER);
    }

    @Override
    public void start()
    {
        for(int timeslot: mTimeslots)
        {
            mIdentifierCollectionMap.get(timeslot).broadcastIdentifiers();
            mStateMachineMap.get(timeslot).setState(State.RESET);
        }
    }

    @Override
    public void stop()
    {
        for(StateMonitoringSquelchController squelchController: mSquelchControllerMap.values())
        {
            squelchController.setSquelchLock(false);
        }
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        if(mInternalSourceEventListener == null)
        {
            mInternalSourceEventListener = new SourceEventListener();
        }

        return mInternalSourceEventListener;
    }


    @Override
    public void setSquelchStateListener(Listener<SquelchStateEvent> listener)
    {
        for(StateMonitoringSquelchController squelchController: mSquelchControllerMap.values())
        {
            squelchController.setSquelchStateListener(listener);
        }
    }

    @Override
    public void removeSquelchStateListener()
    {
        for(StateMonitoringSquelchController squelchController: mSquelchControllerMap.values())
        {
            squelchController.removeSquelchStateListener();
        }
    }

    /**
     * Broadcasts the source event to a registered external source event listener
     */
    protected void broadcast(SourceEvent sourceEvent)
    {
        if(mExternalSourceEventListener != null)
        {
            mExternalSourceEventListener.receive(sourceEvent);
        }
    }

    /**
     * Broadcasts the call event to the registered listener
     */
    protected void broadcast(IDecodeEvent event)
    {
        if(mDecodeEventListener != null)
        {
            mDecodeEventListener.receive(event);
        }
    }

    /**
     * Broadcasts the channel event to a registered listener
     */
    private void broadcast(ChannelEvent channelEvent)
    {
        if(mChannelEventListener != null)
        {
            mChannelEventListener.receive(channelEvent);
        }
    }

    /**
     * Broadcasts a channel state event to any registered listeners
     */
    protected void broadcast(DecoderStateEvent event)
    {
        if(mDecoderStateListener != null)
        {
            mDecoderStateListener.receive(event);
        }
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return mDecoderStateEventReceiver;
    }

    /**
     * Listener to receive source events.
     */
    public class SourceEventListener implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case NOTIFICATION_FREQUENCY_CHANGE:
                    //Rebroadcast source frequency change events for the decoder(s) to process
                    long frequency = sourceEvent.getValue().longValue();

                    for(int timeslot: mTimeslots)
                    {
                        broadcast(new DecoderStateEvent(this, Event.NOTIFICATION_SOURCE_FREQUENCY,
                            mStateMachineMap.get(timeslot).getState(), frequency));

                        //Create a new frequency configuration identifier so that downstream consumers receive the change
                        //via channel metadata and audio packet updates - this is a silent add that is sent as a notification
                        //to all identifier collections so that they don't rebroadcast the change and cause a feedback loop

                        mIdentifierUpdateNotificationProxy.receive(new IdentifierUpdateNotification(
                            FrequencyConfigurationIdentifier.create(frequency), IdentifierUpdateNotification.Operation.SILENT_ADD, timeslot));
                    }

                    break;
                case NOTIFICATION_MEASURED_FREQUENCY_ERROR:
                    //Rebroadcast frequency error measurements to external listener if we're currently
                    //in an active (ie sync locked) state.
                    for(int timeslot: mTimeslots)
                    {
                        if(State.MULTI_CHANNEL_ACTIVE_STATES.contains(mStateMachineMap.get(timeslot).getState()))
                        {
                            broadcast(SourceEvent.frequencyErrorMeasurementSyncLocked(sourceEvent.getValue().longValue(),
                                getChannel().getChannelType().name()));
                            return;
                        }
                    }
                    break;
            }
        }
    }

    /**
     * DecoderStateEvent receiver wrapper
     */
    public class DecoderStateEventReceiver implements Listener<DecoderStateEvent>
    {
        @Override
        public void receive(DecoderStateEvent event)
        {
            if(event.getSource() != this)
            {
                switch(event.getEvent())
                {
                    case REQUEST_ALWAYS_UNSQUELCH:
                        mSquelchControllerMap.get(event.getTimeslot()).setSquelchLock(true);
                        break;
                    case REQUEST_CHANGE_CALL_TIMEOUT:
                        if(event instanceof ChangeChannelTimeoutEvent)
                        {
                            ChangeChannelTimeoutEvent timeout = (ChangeChannelTimeoutEvent)event;
                            mStateMachineMap.get(event.getTimeslot()).setFadeTimeoutBufferMilliseconds(timeout.getCallTimeoutMilliseconds());
                        }
                        break;
                    case CONTINUATION:
                    case DECODE:
                    case START:
                        if(State.MULTI_CHANNEL_ACTIVE_STATES.contains(event.getState()))
                        {
                            mStateMachineMap.get(event.getTimeslot()).setState(event.getState());

                            //Broadcast current channel/timeslot state so that channel rotation monitor can track
                            broadcast(mStateNotificationCache.getStateNotificationEvent(event.getState(), event.getTimeslot()));
                        }
                        break;
                    case END:
                        mStateMachineMap.get(event.getTimeslot()).setState(event.getState());

                        //Broadcast current channel/timeslot state so that channel rotation monitor can track
                        broadcast(mStateNotificationCache.getStateNotificationEvent(event.getState(), event.getTimeslot()));
                        break;
                    case REQUEST_RESET:
                        /* Channel State does not respond to reset events */
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Receives and passes identifier update notifications to the correct channel metadata collections
     */
    public class IdentifierUpdateListenerProxy implements Listener<IdentifierUpdateNotification>
    {
        @Override
        public void receive(IdentifierUpdateNotification identifierUpdateNotification)
        {
            int timeslot = identifierUpdateNotification.getTimeslot();

            ChannelMetadata channelMetadata = mChannelMetadataMap.get(timeslot);

            if(channelMetadata != null)
            {
                channelMetadata.receive(identifierUpdateNotification);
            }
        }
    }

    /**
     * Proxy between the internal identifier collections and the external update notification listener.  This proxy
     * enables access to internal components to broadcast silent identifier update notifications externally.
     */
    public class IdentifierUpdateNotificationProxy implements Listener<IdentifierUpdateNotification>
    {
        private Listener<IdentifierUpdateNotification> mIdentifierUpdateNotificationListener;

        @Override
        public void receive(IdentifierUpdateNotification identifierUpdateNotification)
        {
            if(mIdentifierUpdateNotificationListener != null)
            {
                mIdentifierUpdateNotificationListener.receive(identifierUpdateNotification);
            }
        }

        public void setListener(Listener<IdentifierUpdateNotification> listener)
        {
            mIdentifierUpdateNotificationListener = listener;
        }

        public void removeListener()
        {
            mIdentifierUpdateNotificationListener = null;
        }
    }

}
