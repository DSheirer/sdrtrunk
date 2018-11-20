/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventListener;
import io.github.dsheirer.channel.traffic.TrafficChannelAllocationEvent;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelEvent.Event;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.event.CallEvent;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.p25.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Monitors channel grant and channel grant update messages to allocate traffic channels to capture
 * traffic channel activity.
 *
 * Creates and reuses a limited set of Channel instances each with a TRAFFIC channel type.  Since each of
 * the traffic channels will be using the same decoder type and configuration options, we reuse each of the
 * channel instances to allow the ChannelProcessingManager to reuse a cached set of processing chains that
 * are created as each channel is activated.
 *
 * Each traffic channel is activated by sending a ChannelEvent via the channel model.  The channel processing
 * manager receives the activation request.  If successful, a processing chain is activated for the traffic
 * channel.  Otherwise, a channel event is broadcast indicating that the channel could not be activated.  On
 * teardown of an activated traffic channel, a channel event is broadcast to indicate the traffic channels
 * is no longer active.
 *
 * This manager monitors channel events watching for events related to managed traffic channels.
 */
public class P25TrafficChannelManager extends Module implements IDecodeEventProvider, IChannelEventListener,
    IDecoderStateEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25TrafficChannelManager.class);
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    public static final String NO_TUNER_AVAILABLE = "NO TUNER AVAILABLE";
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";
    public static final String UNKNOWN_FREQUENCY = "UNKNOWN FREQUENCY";

    private int mTrafficChannelPoolMaximumSize = DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private Queue<Channel> mAvailableTrafficChannelQueue = new ConcurrentLinkedQueue<>();
    private List<Channel> mManagedTrafficChannels;
    private Map<IChannelDescriptor,DecodeEvent> mChannelGrantEventMap = new ConcurrentHashMap<>();
    private Map<Channel,IChannelDescriptor> mAllocatedTrafficChannelMap = new ConcurrentHashMap<>();

    private ChannelModel mChannelModel;
    private Listener<IDecodeEvent> mDecodeEventListener;

    private TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();
    //TODO: why are we listening for these events?
    private DecoderStateEventListener mEventListener = new DecoderStateEventListener();

    /**
     * Monitors call events and allocates traffic decoder channels in response
     * to traffic channel allocation call events.  Manages a pool of reusable
     * traffic channel allocations.
     *
     * @param channelModel containing channels currently in use
     * @param parentChannel that owns this traffic channel manager
     * @param decodeEventListener to receive channel grant events
     */
    public P25TrafficChannelManager(ChannelModel channelModel, Channel parentChannel,
                                    Listener<IDecodeEvent> decodeEventListener)
    {
        mChannelModel = channelModel;
        mDecodeEventListener = decodeEventListener;
        createTrafficChannels(parentChannel);
    }

    /**
     * Creates up to the maximum number of traffic channels for use in allocating traffic channels.
     *
     * @param parentChannel for inheriting naming, alias list and traffic channel pool size properties
     */
    private void createTrafficChannels(Channel parentChannel)
    {
        DecodeConfiguration decodeConfiguration = parentChannel.getDecodeConfiguration();
        List<Channel> trafficChannelList = new ArrayList<>();

        if(decodeConfiguration instanceof DecodeConfigP25Phase1)
        {
            DecodeConfigP25Phase1 p25DecodeConfig = (DecodeConfigP25Phase1)decodeConfiguration;

            int maxTrafficChannels = p25DecodeConfig.getTrafficChannelPoolSize();

            for(int x = 0; x < maxTrafficChannels; x++)
            {
                Channel trafficChannel = new Channel("TRAFFIC", ChannelType.TRAFFIC);
                trafficChannel.setAliasListName(parentChannel.getAliasListName());
                trafficChannel.setSystem(parentChannel.getSystem());
                trafficChannel.setSite(parentChannel.getSite());
                trafficChannelList.add(trafficChannel);
            }
        }

        mAvailableTrafficChannelQueue.addAll(trafficChannelList);
        mManagedTrafficChannels = Collections.unmodifiableList(trafficChannelList);
    }

    /**
     * Broadcasts an initial or update decode event to any registered listener.
     */
    public void broadcast(DecodeEvent decodeEvent)
    {
        if(mDecodeEventListener != null)
        {
            mDecodeEventListener.receive(decodeEvent);
        }
    }

    /**
     * Processes channel grants to allocate traffic channels and track overall channel usage.  Generates
     * decode events for each new channel that is allocated.
     *
     * @param channelDescriptor for the traffic channel
     * @param serviceOptions for the traffic channel
     * @param identifierCollection associated with the channel grant
     * @param opcode to identify the call type for the event description
     */
    public void processChannelGrant(IChannelDescriptor channelDescriptor, ServiceOptions serviceOptions,
                                    IdentifierCollection identifierCollection, Opcode opcode, long timestamp)
    {
        if(isDuplicateChannelGrant(channelDescriptor, identifierCollection, timestamp))
        {
            return;
        }

        //Remove any existing channel grant for the channel descriptor
        mChannelGrantEventMap.remove(channelDescriptor);

        createChannel(channelDescriptor, serviceOptions, identifierCollection, opcode, timestamp);
    }

    /**
     * Creates a traffic channel
     *
     * @param channelDescriptor
     * @param serviceOptions
     * @param identifierCollection
     * @param opcode
     * @param timestamp
     */
    private void createChannel(IChannelDescriptor channelDescriptor, ServiceOptions serviceOptions,
                               IdentifierCollection identifierCollection, Opcode opcode, long timestamp)
    {
        //Create a new channel grant event
        DecodeEvent channelGrantEvent = P25ChannelGrantEvent.builder(timestamp)
            .channel(channelDescriptor)
            .description(serviceOptions.isEncrypted() ?
                DecodeEventType.CALL_GROUP_ENCRYPTED.toString() :
                DecodeEventType.CALL_GROUP.toString())
            .details("CHANNEL GRANT " + serviceOptions)
            .identifiers(identifierCollection)
            .build();

        mChannelGrantEventMap.put(channelDescriptor, channelGrantEvent);
        broadcast(channelGrantEvent);

        Channel trafficChannel = mAvailableTrafficChannelQueue.poll();

        if(trafficChannel == null)
        {
            channelGrantEvent.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED);
            channelGrantEvent.setEventDescription(DecodeEventType.CALL_DETECT.toString());
            return;
        }

        //TODO: create a traffic channel and enable it.  We have to update the traffic channel
        //allocation event to the new decode event format and update it to accept the channel
        //descriptor from the p25 event.
    }

    /**
     * Determines if a channel grant currently exists and the channel grant matches the new channel grant parameters.
     *
     * A channel grant matches when the TO talkgroup is the same and the channel grant start time is within
     * 500 milliseconds of this channel grant.
     *
     * @param channelDescriptor for the channel grant
     * @param identifierCollection for the channel grant
     * @param timestamp of the channel grant message
     * @return true if this is a duplicate channel grant
     */
    private boolean isDuplicateChannelGrant(IChannelDescriptor channelDescriptor,
                                            IdentifierCollection identifierCollection, long timestamp)
    {
        DecodeEvent existingChannelGrantEvent = mChannelGrantEventMap.get(channelDescriptor);

        if(existingChannelGrantEvent == null)
        {
            return false;
        }

        if(isSameTalkgroup(identifierCollection, existingChannelGrantEvent.getIdentifierCollection()) &&
            (Math.abs(timestamp - existingChannelGrantEvent.getTimeStart()) < 500))
        {
            return true;
        }

        return false;
    }

    /**
     * Channel event listener to receive notifications that a traffic channel has ended processing and we
     * can reclaim the traffic channel for reuse.
     *
     * @return listener for processing channel events.
     */
    @Override
    public Listener<ChannelEvent> getChannelEventListener()
    {
        return mTrafficChannelTeardownMonitor;
    }

//TODO: review/remove the code below

    /**
     * Compares the TO role identifier(s) from each collection for equality
     * @param collection1 containing a TO identifier
     * @param collection2 containing a TO identifier
     * @return true if both collections contain a TO identifier and the TO identifiers are the same value
     */
    private boolean isSameTalkgroup(IdentifierCollection collection1, IdentifierCollection collection2)
    {
        Identifier toIdentifier1 = getToIdentifier(collection1);
        Identifier toIdentifier2 = getToIdentifier(collection2);

        return Objects.equals(toIdentifier1, toIdentifier2);
    }

    /**
     * Retrieves the first identifier with a TO role.
     * @param collection containing a TO identifier
     * @return TO identifier or null
     */
    private Identifier getToIdentifier(IdentifierCollection collection)
    {
        List<Identifier> identifiers = collection.getIdentifiers(Role.TO);

        if(identifiers.size() >= 1)
        {
            return identifiers.get(0);
        }

        return null;
    }

    @Override
    public void dispose()
    {
        for(Channel trafficChannel : mAvailableTrafficChannelQueue)
        {
            mChannelModel.broadcast(new ChannelEvent(trafficChannel, Event.REQUEST_DISABLE));
        }

        mAvailableTrafficChannelQueue.clear();

//        mTrafficChannelsInUse.clear();

        mDecodeEventListener = null;
    }

    /**
     * Provides a channel by either reusing an existing channel or constructing
     * a new one, limited by the total number of constructed channels allowed.
     *
     * Note: you must enforce thread safety on the mTrafficChannelsInUse
     * external to this method.
     *
     * @param channelNumber
     * @param tunerChannel
     * @return
     */
    private Channel getChannel(String channelNumber, TunerChannel tunerChannel)
    {
        Channel channel = null;

//        if(!mTrafficChannelsInUse.containsKey(channelNumber))
//        {
//            for(Channel configuredChannel : mAvailableTrafficChannelQueue)
//            {
//                if(!configuredChannel.isProcessing())
//                {
//                    channel = configuredChannel;
//                    break;
//                }
//            }
//
//            if(channel == null && mAvailableTrafficChannelQueue.size() < mTrafficChannelPoolMaximumSize)
//            {
//                channel = new Channel("Traffic", ChannelType.TRAFFIC);
//
//                channel.setDecodeConfiguration(mDecodeConfiguration);
//
//                channel.setRecordConfiguration(mRecordConfiguration);
//
//                channel.setAliasListName(mAliasListName);
//
//                mChannelModel.addChannel(channel);
//
//                mAvailableTrafficChannelQueue.add(channel);
//            }
//
//            /* If we have a configured channel, update metadata */
//            if(channel != null)
//            {
//                channel.setSourceConfiguration(new SourceConfigTuner(tunerChannel));
//                channel.setSystem(mSystem);
//                channel.setSite(mSite);
//                channel.setName(channelNumber);
//
//                mChannelModel.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_CONFIGURATION_CHANGE));
//            }
//        }

        return channel;
    }

    /**
     * Processes the event and creates a traffic channel is resources are
     * available
     */
    private void process(TrafficChannelAllocationEvent event)
    {

//        CallEvent callEvent = event.getCallEvent();
//
//        /* Check for duplicate events and suppress */
//        synchronized(mTrafficChannelsInUse)
//        {
//            if(mTrafficChannelsInUse.containsKey(callEvent.getChannel()))
//            {
//                return;
//            }
//
//            long frequency = callEvent.getFrequency();
//
//            if(frequency > 0)
//            {
//                Channel channel = getChannel(callEvent.getChannel(), new TunerChannel(frequency,
//                    mDecodeConfiguration.getChannelSpecification().getBandwidth()));
//
//                if(channel != null)
//                {
//                    TrafficChannelEvent trafficChannelEvent =
//                        new TrafficChannelEvent(this, channel, Event.REQUEST_ENABLE, callEvent);
//
//                    //Request to enable the channel
//                    mChannelModel.broadcast(trafficChannelEvent);
//
//                    if(channel.isProcessing())
//                    {
//                        mTrafficChannelsInUse.put(callEvent.getChannel(), channel);
//                    }
//                    else
//                    {
//                        callEvent.setCallEventType(CallEventType.CALL_DETECT);
//
//                        String details = callEvent.getDetails();
//
//                        if(details == null || details.isEmpty())
//                        {
//                            callEvent.setDetails(CHANNEL_START_REJECTED);
//                        }
//                        else if(!details.contains(CHANNEL_START_REJECTED))
//                        {
//                            callEvent.setDetails(new StringBuilder(CHANNEL_START_REJECTED).append(" : ")
//                                .append(callEvent.getDetails()).toString());
//                        }
//                    }
//                }
//                else
//                {
//                    callEvent.setCallEventType(CallEventType.CALL_DETECT);
//
//                    String details = callEvent.getDetails();
//
//                    if(details == null || details.isEmpty())
//                    {
//                        callEvent.setDetails(NO_TUNER_AVAILABLE);
//                    }
//                    else if(!details.contains(NO_TUNER_AVAILABLE))
//                    {
//                        callEvent.setDetails(new StringBuilder(NO_TUNER_AVAILABLE).append(" : ")
//                            .append(callEvent.getDetails()).toString());
//                    }
//                }
//            }
//            else
//            {
//                callEvent.setCallEventType(CallEventType.CALL_DETECT);
//
//                String details = callEvent.getDetails();
//
//                if(details == null || details.isEmpty())
//                {
//                    callEvent.setDetails(UNKNOWN_FREQUENCY);
//                }
//                else if(!details.contains(UNKNOWN_FREQUENCY))
//                {
//                    callEvent.setDetails(new StringBuilder(UNKNOWN_FREQUENCY).append(" : ")
//                        .append(callEvent.getDetails()).toString());
//                }
//            }
//
//            final Listener<IDecodeEvent> listener = mDecodeEventListener;
//
//            if(listener != null)
//            {
//                listener.receive(callEvent);
//            }
//        }
    }

    /**
     * Compares the call type, channel and to fields for equivalence and the
     * from field for either both null, or equivalence.
     *
     * @param e1
     * @param e2
     * @return
     */
    public static boolean isSameCallEvent(CallEvent e1, CallEvent e2)
    {
        if(e1 == null || e2 == null)
        {
            return false;
        }

        if(e1.getCallEventType() != e2.getCallEventType())
        {
            return false;
        }

        if(e1.getChannel() == null || e2.getChannel() == null || !e1.getChannel().contentEquals(e2.getChannel()))
        {
            return false;
        }

        if(e1.getToID() == null || e2.getToID() == null || !e1.getToID().contentEquals(e2.getToID()))
        {
            return false;
        }

        if(e1.getFromID() == null || e2.getFromID() == null)
        {
            return (e1.getFromID() == null && e2.getFromID() == null);
        }
        else if(e1.getFromID().contentEquals(e2.getFromID()))
        {
            return true;
        }

        return false;
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return mEventListener;
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

    @Override
    public void reset()
    {
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
//        if(!mTrafficChannelsInUse.isEmpty())
//        {
//            List<String> channels = new ArrayList<>();
//
//            /* Copy the keyset so we don't get concurrent modification of the map */
//            channels.addAll(mTrafficChannelsInUse.keySet());
//
//            for(String channel : channels)
//            {
//                callEnd(channel);
//            }
//        }
    }

    /**
     * Callback used by the TrafficChannelStatusListener class to signal the
     * end of a an allocated traffic channel call event
     *
     * @param channelNumber - channel number from the call event that signaled
     * the start of a traffic channel allocation
     */
    public void callEnd(String channelNumber)
    {
//        synchronized(mTrafficChannelsInUse)
//        {
//            if(channelNumber != null && mTrafficChannelsInUse.containsKey(channelNumber))
//            {
//                Channel channel = mTrafficChannelsInUse.get(channelNumber);
//
//                mChannelModel.broadcast(new ChannelEvent(channel, Event.REQUEST_DISABLE));
//
//                mTrafficChannelsInUse.remove(channelNumber);
//            }
//        }
    }

    /**
     * Wrapper class for the decoder state event listener interface to catch
     * traffic channel allocation requests
     */
    public class DecoderStateEventListener implements Listener<DecoderStateEvent>
    {
        @Override
        public void receive(DecoderStateEvent event)
        {
            switch(event.getEvent())
            {
                case TRAFFIC_CHANNEL_ALLOCATION:
                    process((TrafficChannelAllocationEvent) event);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Monitors channel teardown events to detect when traffic channel processing has ended.  Reclaims the
     * channel instance for reuse by future traffic channel grants.
     */
    public class TrafficChannelTeardownMonitor implements Listener<ChannelEvent>
    {
        @Override
        public void receive(ChannelEvent channelEvent)
        {
            if(channelEvent.getEvent() == Event.NOTIFICATION_PROCESSING_STOP &&
                mManagedTrafficChannels.contains(channelEvent.getChannel()))
            {
                mAvailableTrafficChannelQueue.add(channelEvent.getChannel());
            }
        }
    }
}