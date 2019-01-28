/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.channel.state;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.squelch.ISquelchStateProvider;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.ChannelStateIdentifier;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import io.github.dsheirer.source.heartbeat.IHeartbeatListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChannelState extends Module implements IChannelEventProvider, IDecodeEventProvider,
    IDecoderStateEventListener, IDecoderStateEventProvider, ISourceEventListener, ISourceEventProvider,
    IHeartbeatListener, ISquelchStateProvider, IOverflowListener, IdentifierUpdateListener, IdentifierUpdateProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelState.class);

    public static final long FADE_TIMEOUT_DELAY = 1200;
    public static final long RESET_TIMEOUT_DELAY = 2000;

    private State mState = State.IDLE;
    private MutableIdentifierCollection mIdentifierCollection = new MutableIdentifierCollection();
    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;
    private Listener<DecoderStateEvent> mDecoderStateListener;
    private Listener<SquelchState> mSquelchStateListener;
    private Listener<SourceEvent> mExternalSourceEventListener;
    private DecoderStateEventReceiver mDecoderStateEventReceiver = new DecoderStateEventReceiver();
    private HeartbeatReceiver mHeartbeatReceiver = new HeartbeatReceiver();
    private Channel mChannel;
    private SourceEventListener mInternalSourceEventListener;
    private ChannelMetadata mChannelMetadata;
    private IdentifierUpdateNotificationProxy mIdentifierUpdateNotificationProxy = new IdentifierUpdateNotificationProxy();

    private boolean mSquelchLocked = false;
    private boolean mSelected = false;
    private boolean mSourceOverflow = false;

    private long mStandardChannelFadeTimeout = FADE_TIMEOUT_DELAY;
    private long mTrafficChannelFadeTimeout = DecodeConfiguration.DEFAULT_CALL_TIMEOUT_SECONDS * 1000;
    private long mFadeTimeout;
    private long mEndTimeout;


    /**
     * Channel state tracks the overall state of all processing modules and decoders configured for the channel and
     * provides squelch control and decoder state reset events.
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
    public ChannelState(Channel channel, AliasModel aliasModel)
    {
        mChannel = channel;
        mChannelMetadata = new ChannelMetadata(aliasModel);
        mIdentifierCollection.setIdentifierUpdateListener(mIdentifierUpdateNotificationProxy);
        createConfigurationIdentifiers(channel);
    }

    /**
     * Creates configuration identifiers for the channel name, system, site and alias list name.
     */
    private void createConfigurationIdentifiers(Channel channel)
    {
        mIdentifierCollection.update(DecoderTypeConfigurationIdentifier.create(channel.getDecodeConfiguration().getDecoderType()));

        if(channel.hasSystem())
        {
            mIdentifierCollection.update(SystemConfigurationIdentifier.create(channel.getSystem()));
        }
        if(channel.hasSite())
        {
            mIdentifierCollection.update(SiteConfigurationIdentifier.create(channel.getSite()));
        }
        if(channel.getName() != null && !channel.getName().isEmpty())
        {
            mIdentifierCollection.update(ChannelNameConfigurationIdentifier.create(channel.getName()));
        }
        if(channel.getAliasListName() != null && !channel.getAliasListName().isEmpty())
        {
            mIdentifierCollection.update(AliasListConfigurationIdentifier.create(channel.getAliasListName()));
        }
        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            long frequency = ((SourceConfigTuner)channel.getSourceConfiguration()).getFrequency();
            mIdentifierCollection.update(FrequencyConfigurationIdentifier.create(frequency));
        }
        else if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER_MULTIPLE_FREQUENCIES)
        {
            List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).getFrequencies();

            if(frequencies.size() > 0)
            {
                mIdentifierCollection.update(FrequencyConfigurationIdentifier.create(frequencies.get(0)));
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
        return mChannelMetadata;
    }

    /**
     * Updates the channel state identifier collection using the update notification.  This update will be reflected
     * in the internal channel state and will also be broadcast to any listeners, including the channel metadata for
     * this channel state.
     */
    public void updateChannelStateIdentifiers(IdentifierUpdateNotification notification)
    {
        mIdentifierCollection.receive(notification);
    }

    @Override
    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        mIdentifierUpdateNotificationProxy.setListener(listener);
    }

    @Override
    public void removeIdentifierUpdateListener()
    {
        mIdentifierUpdateNotificationProxy.removeListener();
    }

    /**
     * Channel metadata for this channel.
     */
    public ChannelMetadata getChannelMetadata()
    {
        return mChannelMetadata;
    }

    /**
     * Resets this channel state and prepares it for reuse.
     */
    @Override
    public void reset()
    {
        mState = State.IDLE;
        broadcast(new DecoderStateEvent(this, Event.RESET, State.IDLE));
        mIdentifierCollection.remove(IdentifierClass.USER);
        mIdentifierCollection.update(ChannelStateIdentifier.IDLE);
        mSourceOverflow = false;
    }

    @Override
    public void start()
    {
        mIdentifierCollection.broadcastIdentifiers();

        if(mChannel.getChannelType() == ChannelType.TRAFFIC)
        {
            setState(State.CALL);
        }
    }

    @Override
    public void stop()
    {
        processTeardownState();
        mSquelchLocked = false;
    }

    public void dispose()
    {
        mDecodeEventListener = null;
        mDecoderStateListener = null;
        mSquelchStateListener = null;
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

    public void setStandardChannelTimeout(long milliseconds)
    {
        mStandardChannelFadeTimeout = milliseconds;

        if(mChannel.isStandardChannel())
        {
            mFadeTimeout = mStandardChannelFadeTimeout;
        }
    }

    public void setTrafficChannelTimeout(long milliseconds)
    {
        mTrafficChannelFadeTimeout = milliseconds;

        if(mChannel.isTrafficChannel())
        {
            mFadeTimeout = System.currentTimeMillis() + mTrafficChannelFadeTimeout;
        }
    }

    public void setSelected(boolean selected)
    {
        mSelected = selected;
    }

    public boolean isSelected()
    {
        return mSelected;
    }

    public State getState()
    {
        return mState;
    }

    /**
     * Updates the fade timeout threshold to the current time plus delay
     */
    private void updateFadeTimeout()
    {
        if(mChannel.isTrafficChannel())
        {
            mFadeTimeout = System.currentTimeMillis() + mTrafficChannelFadeTimeout;
        }
        else
        {
            mFadeTimeout = System.currentTimeMillis() + mStandardChannelFadeTimeout;
        }

    }

    /**
     * Updates the reset timeout threshold to the current time plus delay
     */
    private void updateResetTimeout()
    {
        if(mChannel.isTrafficChannel())
        {
            mEndTimeout = System.currentTimeMillis();
        }
        else
        {
            mEndTimeout = System.currentTimeMillis() + RESET_TIMEOUT_DELAY;
        }
    }

    /**
     * Broadcasts the squelch state to the registered listener
     */
    protected void broadcast(SquelchState state)
    {
        if(mSquelchStateListener != null && !mSquelchLocked)
        {
            mSquelchStateListener.receive(state);
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
     * Sets the squelch state listener
     */
    @Override
    public void setSquelchStateListener(Listener<SquelchState> listener)
    {
        mSquelchStateListener = listener;
    }

    /**
     * Removes the squelch state listener
     */
    @Override
    public void removeSquelchStateListener()
    {
        mSquelchStateListener = null;
    }

    /**
     * Sets the channel state to the specified state, or updates the timeout values so that the state monitor will not
     * change state.  Broadcasts a squelch event when the state changes and the audio squelch state should change.  Also
     * broadcasts changed attribute and decoder state events so that external processes can maintain sync with this
     * channel state.
     */
    protected void setState(State state)
    {
        if(state == mState)
        {
            if(State.CALL_STATES.contains(state))
            {
                updateFadeTimeout();
            }
        }
        else if(mState.canChangeTo(state))
        {
            switch(state)
            {
                case ACTIVE:
                    broadcast(SquelchState.SQUELCH);
                    updateFadeTimeout();
                    mState = state;
                    mIdentifierCollection.update(ChannelStateIdentifier.ACTIVE);
                    break;
                case CONTROL:
                    //Don't allow traffic channels to be control channels, otherwise they can't transition to teardown
                    if(mChannel.isStandardChannel())
                    {
                        broadcast(SquelchState.SQUELCH);
                        updateFadeTimeout();
                        mState = state;
                        mIdentifierCollection.update(ChannelStateIdentifier.CONTROL);
                    }
                    break;
                case DATA:
                    broadcast(SquelchState.SQUELCH);
                    updateFadeTimeout();
                    mState = state;
                    mIdentifierCollection.update(ChannelStateIdentifier.DATA);
                    break;
                case ENCRYPTED:
                    broadcast(SquelchState.SQUELCH);
                    updateFadeTimeout();
                    mState = state;
                    mIdentifierCollection.update(ChannelStateIdentifier.ENCRYPTED);
                    break;
                case CALL:
                    broadcast(SquelchState.UNSQUELCH);
                    updateFadeTimeout();
                    mState = state;
                    mIdentifierCollection.update(ChannelStateIdentifier.CALL);
                    break;
                case FADE:
                    processFadeState();
                    mIdentifierCollection.update(ChannelStateIdentifier.FADE);
                    break;
                case IDLE:
                    processIdleState();
                    mIdentifierCollection.update(ChannelStateIdentifier.IDLE);
                    break;
                case TEARDOWN:
                    processTeardownState();
                    break;
                case RESET:
                    mState = State.IDLE;
                    mIdentifierCollection.update(ChannelStateIdentifier.IDLE);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Receiver inner class that implements the IHeartbeatListener interface to receive heartbeat messages.
     */
    @Override
    public Listener<Heartbeat> getHeartbeatListener()
    {
        return mHeartbeatReceiver;
    }

    /**
     * This method is invoked if the source buffer provider goes into overflow state.  Since this is an external state,
     * we use the mSourceOverflow variable to override the internal state reported to external listeners.
     *
     * @param overflow true to indicate an overflow state
     */
    @Override
    public void sourceOverflow(boolean overflow)
    {
        mSourceOverflow = overflow;
    }

    /**
     * Indicates if this channel's sample buffer is in overflow state, meaning that the inbound sample
     * stream is not being processed fast enough and samples are being thrown away until the processing can
     * catch up.
     *
     * @return true if the channel is in overflow state.
     */
    public boolean isOverflow()
    {
        return mSourceOverflow;
    }

    /**
     * Sets the state and processes related actions
     */
    private void processFadeState()
    {
        updateResetTimeout();
        mState = State.FADE;
        mIdentifierCollection.update(ChannelStateIdentifier.FADE);

        broadcast(SquelchState.SQUELCH);
    }

    private void processIdleState()
    {
        broadcast(SquelchState.SQUELCH);

        if(mState == State.FADE)
        {
            broadcast(new DecoderStateEvent(this, Event.RESET, State.IDLE));
        }

        mState = State.IDLE;
        mIdentifierCollection.update(ChannelStateIdentifier.IDLE);
    }

    private void processTeardownState()
    {
        broadcast(SquelchState.SQUELCH);

        mState = State.TEARDOWN;
        mIdentifierCollection.update(ChannelStateIdentifier.TEARDOWN);

        if(mChannel.isTrafficChannel())
        {
            broadcast(new ChannelEvent(mChannel, ChannelEvent.Event.REQUEST_DISABLE));
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

    @Override
    public void setChannelEventListener(Listener<ChannelEvent> listener)
    {
        mChannelEventListener = listener;
    }

    @Override
    public void removeChannelEventListener()
    {
        mChannelEventListener = null;
    }

    @Override
    public void addDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventListener = listener;
    }

    @Override
    public void removeDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventListener = null;
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

    /**
     * Adds a decoder state event listener
     */
    @Override
    public void setDecoderStateListener(Listener<DecoderStateEvent> listener)
    {
        mDecoderStateListener = listener;
    }

    /**
     * Removes the decoder state event listener
     */
    @Override
    public void removeDecoderStateListener()
    {
        mDecoderStateListener = null;
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return mDecoderStateEventReceiver;
    }

    /**
     * Registers the listener to receive source events from the channel state
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mExternalSourceEventListener = listener;
    }

    /**
     * De-Registers a listener from receiving source events from the channel state
     */
    @Override
    public void removeSourceEventListener()
    {
        mExternalSourceEventListener = null;
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
                    broadcast(new DecoderStateEvent(this, Event.SOURCE_FREQUENCY, getState(), frequency));

                    //Create a new frequency configuration identifier so that downstream consumers receive the change
                    //via channel metadata and audio packet updates - this is a silent add that is sent as a notification
                    //to all identifier collections so that they don't rebroadcast the change and cause a feedback loop
                    mIdentifierUpdateNotificationProxy.receive(new IdentifierUpdateNotification(
                        FrequencyConfigurationIdentifier.create(frequency), IdentifierUpdateNotification.Operation.SILENT_ADD));
                    break;
                case NOTIFICATION_MEASURED_FREQUENCY_ERROR:
                    //Rebroadcast frequency error measurements to external listener if we're currently
                    //in an active (ie sync locked) state.
                    if(getState().isActiveState())
                    {
                        broadcast(SourceEvent.frequencyErrorMeasurementSyncLocked(sourceEvent.getValue().longValue(),
                            mChannel.getChannelType().name()));
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
                    case ALWAYS_UNSQUELCH:
                        broadcast(SquelchState.UNSQUELCH);
                        mSquelchLocked = true;
                        break;
                    case CHANGE_CALL_TIMEOUT:
                        if(event instanceof ChangeChannelTimeoutEvent)
                        {
                            ChangeChannelTimeoutEvent timeout = (ChangeChannelTimeoutEvent)event;

                            if(timeout.getChannelType() == ChannelType.STANDARD)
                            {
                                setStandardChannelTimeout(timeout.getCallTimeout());
                            }
                            else
                            {
                                setTrafficChannelTimeout(timeout.getCallTimeout());
                            }
                        }
                    case CONTINUATION:
                    case DECODE:
                    case START:
                        if(State.CALL_STATES.contains(event.getState()))
                        {
                            setState(event.getState());
                        }
                        break;
                    case END:
                        if(mChannel.isTrafficChannel())
                        {
                            setState(State.TEARDOWN);
                        }
                        else
                        {
                            setState(State.FADE);
                        }
                        break;
                    case RESET:
                        /* Channel State does not respond to reset events */
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Processes periodic heartbeats received from the processing chain to perform state monitoring and cleanup
     * functions.
     *
     * Monitors decoder state events to automatically transition the channel state to IDLE (standard channel) or to
     * TEARDOWN (traffic channel) when decoding stops or the monitored channel returns to a no signal state.
     *
     * Provides a FADE transition state to allow for momentary decoding dropouts and to allow the user access to call
     * details for a fade period upon call end.
     */
    public class HeartbeatReceiver implements Listener<Heartbeat>
    {
        @Override
        public void receive(Heartbeat heartbeat)
        {
            try
            {
                if(State.CALL_STATES.contains(mState) && mFadeTimeout <= System.currentTimeMillis())
                {
                    processFadeState();
                }
                else if(mState == State.FADE && mEndTimeout <= System.currentTimeMillis())
                {
                    if(mChannel.isTrafficChannel())
                    {
                        processTeardownState();
                    }
                    else
                    {
                        processIdleState();
                    }
                }
            }
            catch(Throwable e)
            {
                mLog.error("An error occurred while state monitor was running " +
                    "- state [" + getState() +
                    "] current [" + System.currentTimeMillis() +
                    "] mResetTimeout [" + mEndTimeout +
                    "] mFadeTimeout [" + mFadeTimeout +
                    "]", e);
            }
        }
    }

    /**
     * Proxy between the identifier collection and the external update notification listener.  This proxy enables
     * access to internal components to broadcast silent identifier update notifications externally.
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
