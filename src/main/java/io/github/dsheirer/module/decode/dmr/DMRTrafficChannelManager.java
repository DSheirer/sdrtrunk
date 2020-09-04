/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelEvent.Event;
import io.github.dsheirer.controller.channel.ChannelGrantEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.event.DMRChannelGrantEvent;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
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
public class DMRTrafficChannelManager extends Module implements IDecodeEventProvider, IChannelEventListener,
    IChannelEventProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRTrafficChannelManager.class);
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";

    private Queue<Channel> mAvailableTrafficChannelQueue = new ConcurrentLinkedQueue<>();
    private List<Channel> mManagedTrafficChannels;

    private Map<Long,Channel> mAllocatedTrafficChannelFrequencyMap = new ConcurrentHashMap<>();
    private Map<Integer,DMRChannelGrantEvent> mLSNGrantEventMap = new ConcurrentHashMap<>();

    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;

    private TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();
    private Channel mParentChannel;

    private boolean mIgnoreDataCalls;
    private long mCurrentControlFrequency;

    /**
     * Monitors call events and allocates traffic decoder channels in response
     * to traffic channel allocation call events.  Manages a pool of reusable
     * traffic channel allocations.
     *
     * @param parentChannel that owns this traffic channel manager
     */
    public DMRTrafficChannelManager(Channel parentChannel)
    {
        mParentChannel = parentChannel;

        if(parentChannel.getDecodeConfiguration() instanceof DecodeConfigDMR)
        {
            mIgnoreDataCalls = ((DecodeConfigDMR)parentChannel.getDecodeConfiguration()).getIgnoreDataCalls();
        }

        createTrafficChannels();
    }

    /**
     * Sets the current parent control channel frequency so that channel grants for the current frequency do not
     * produce an additional traffic channel allocation.
     * @param currentControlFrequency for current control channel.
     */
    public void setCurrentControlFrequency(long currentControlFrequency)
    {
        mCurrentControlFrequency = currentControlFrequency;
    }

    /**
     * Creates up to the maximum number of traffic channels for use in allocating traffic channels.
     *
     * Note: this method uses lazy initialization and will only create the channels once.  Subsequent calls will be ignored.
     */
    private void createTrafficChannels()
    {
        if(mManagedTrafficChannels == null)
        {
            DecodeConfiguration decodeConfiguration = mParentChannel.getDecodeConfiguration();
            List<Channel> trafficChannelList = new ArrayList<>();

            if(decodeConfiguration instanceof DecodeConfigDMR)
            {
                DecodeConfigDMR decodeConfig = (DecodeConfigDMR)decodeConfiguration;

                int maxTrafficChannels = decodeConfig.getTrafficChannelPoolSize();

                mLog.debug("Creating [" + maxTrafficChannels + "] traffic channels");
                if(maxTrafficChannels > 0)
                {
                    for(int x = 0; x < maxTrafficChannels; x++)
                    {
                        Channel trafficChannel = new Channel("TRAFFIC", ChannelType.TRAFFIC);
                        trafficChannel.setAliasListName(mParentChannel.getAliasListName());
                        trafficChannel.setSystem(mParentChannel.getSystem());
                        trafficChannel.setSite(mParentChannel.getSite());
                        trafficChannel.setDecodeConfiguration(decodeConfig);
                        trafficChannel.setEventLogConfiguration(mParentChannel.getEventLogConfiguration());
                        trafficChannel.setRecordConfiguration(mParentChannel.getRecordConfiguration());
                        trafficChannelList.add(trafficChannel);
                    }
                }
            }

            mAvailableTrafficChannelQueue.addAll(trafficChannelList);
            mManagedTrafficChannels = Collections.unmodifiableList(trafficChannelList);
        }
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

    public void processEndChannelGrant()
    {
        //TODO: process the CSBK opcode that indicates traffic channel teardown
    }

    /**
     * Processes channel grants to allocate traffic channels and track overall channel usage.  Generates
     * decode events for each new channel that is allocated.
     *
     */
    public void processChannelGrant(DMRChannel channel, IdentifierCollection identifierCollection,
                                    Opcode opcode, long timestamp, boolean encrypted)
    {
        int lsn = channel.getLogicalSlotNumber();

        DMRChannelGrantEvent event = mLSNGrantEventMap.get(lsn);

        if(event != null && isSameCall(identifierCollection, event.getIdentifierCollection()))
        {
            Identifier from = getIdentifier(identifierCollection, Role.FROM);

            if(from != null)
            {
                Identifier currentFrom = getIdentifier(event.getIdentifierCollection(), Role.FROM);
                if(currentFrom != null && !Objects.equals(from, currentFrom))
                {
                    event.end(timestamp);

                    DMRChannelGrantEvent continuationGrantEvent = DMRChannelGrantEvent.channelGrantBuilder(timestamp)
                        .channel(channel)
                        .eventDescription(getEventType(opcode, event.isEncrypted()).toString() + " - Continue")
                        .details("CHANNEL GRANT" + (encrypted ? " ENCRYPTED" : ""))
                        .identifiers(identifierCollection)
                        .build();

                    mLSNGrantEventMap.put(lsn, continuationGrantEvent);
                    broadcast(continuationGrantEvent);
                }
            }

            //update the ending timestamp so that the duration value is correctly calculated
            event.update(timestamp);
            broadcast(event);

            //Even though we have an event, the initial channel grant may have been rejected.  Check to see if there
            //is a traffic channel allocated.  If not, allocate one and update the event description.
            long frequency = channel.getDownlinkFrequency();

            if(frequency > 0 && frequency != mCurrentControlFrequency &&
                !mAllocatedTrafficChannelFrequencyMap.containsKey(channel.getDownlinkFrequency()) &&
                !(mIgnoreDataCalls && opcode.isDataChannelGrantOpcode()))
            {
                Channel trafficChannel = mAvailableTrafficChannelQueue.poll();

                if(trafficChannel != null)
                {
                    event.setEventDescription(getEventType(opcode, event.isEncrypted()).toString());
                    event.setDetails("CHANNEL GRANT " + (encrypted ? " ENCRYPTED" : ""));
                    event.setChannelDescriptor(channel);
                    broadcast(event);

                    SourceConfigTuner sourceConfig = new SourceConfigTuner();
                    sourceConfig.setFrequency(frequency);
                    trafficChannel.setSourceConfiguration(sourceConfig);
                    mAllocatedTrafficChannelFrequencyMap.put(frequency, trafficChannel);
                    broadcast(new ChannelGrantEvent(trafficChannel, Event.REQUEST_ENABLE, channel, identifierCollection));
                }
                else
                {
                    mLog.error("No more traffic channels available");
                }
            }

            return;
        }

        if(mIgnoreDataCalls && opcode.isDataChannelGrantOpcode())
        {
            DMRChannelGrantEvent channelGrantEvent = DMRChannelGrantEvent.channelGrantBuilder(timestamp)
                .encrypted(encrypted)
                .channel(channel)
                .eventDescription(getEventType(opcode, encrypted).toString() + " - Ignored")
                .details("DATA CALL IGNORED")
                .identifiers(identifierCollection)
                .build();

            mLSNGrantEventMap.put(channel.getLogicalSlotNumber(), channelGrantEvent);
            broadcast(channelGrantEvent);
            return;
        }

        DMRChannelGrantEvent channelGrantEvent = DMRChannelGrantEvent.channelGrantBuilder(timestamp)
            .encrypted(encrypted)
            .channel(channel)
            .eventDescription(getEventType(opcode, encrypted).toString())
            .details("CHANNEL GRANT")
            .identifiers(identifierCollection)
            .build();

        mLSNGrantEventMap.put(channel.getLogicalSlotNumber(), channelGrantEvent);

        //Allocate a traffic channel for the downlink frequency if one isn't already allocated
        //NOTE: we could also allocate a traffic channel for the uplink frequency here, in the future
        long frequency = channel.getDownlinkFrequency();

        mLog.debug("Requested frequency is:" + frequency);

        if(frequency > 0 && frequency != mCurrentControlFrequency &&
            !mAllocatedTrafficChannelFrequencyMap.containsKey(frequency))
        {
            Channel trafficChannel = mAvailableTrafficChannelQueue.poll();

            if(trafficChannel == null)
            {
                String details = (channelGrantEvent.getDetails() != null ? channelGrantEvent.getDetails() : "") +
                    " " + MAX_TRAFFIC_CHANNELS_EXCEEDED;
                channelGrantEvent.setDetails(details);
                channelGrantEvent.setEventDescription(channelGrantEvent.getEventDescription() + " - Ignored");
                mLog.error("No more traffic channels available - normal allocation");
            }
            else
            {
                mLog.debug("Allocating traffic channel for frequency " + frequency);
                SourceConfigTuner sourceConfig = new SourceConfigTuner();
                sourceConfig.setFrequency(frequency);
                trafficChannel.setSourceConfiguration(sourceConfig);
                mAllocatedTrafficChannelFrequencyMap.put(frequency, trafficChannel);
                broadcast(new ChannelGrantEvent(trafficChannel, Event.REQUEST_ENABLE, channel, identifierCollection));
            }
        }

        broadcast(channelGrantEvent);
    }


    /**
     * Creates a call event type description for the specified opcode and service options
     */
    private DecodeEventType getEventType(Opcode opcode, boolean encrypted)
    {
        DecodeEventType type = null;

        switch(opcode)
        {
            case STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT:
            case STANDARD_BROADCAST_TALKGROUP_VOICE_CHANNEL_GRANT:
            case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                type = encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
                break;

            case STANDARD_PRIVATE_VOICE_CHANNEL_GRANT:
            case STANDARD_DUPLEX_PRIVATE_VOICE_CHANNEL_GRANT:
                type = encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
                break;

            case STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM:
            case STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM:
            case STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT:
            case STANDARD_PRIVATE_DATA_CHANNEL_GRANT_MULTI_ITEM:
            case STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_MULTI_ITEM:
            case MOTOROLA_CONPLUS_DATA_CHANNEL_GRANT:
                type = encrypted ? DecodeEventType.DATA_CALL_ENCRYPTED : DecodeEventType.DATA_CALL;
                break;
        }

        if(type == null)
        {
            mLog.debug("Unrecognized opcode for determining decode event type: " + opcode.name());
            type = DecodeEventType.CALL;
        }

        return type;
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

    /**
     * Broadcasts a channel event to a registered external listener (for action).
     */
    private void broadcast(ChannelEvent channelEvent)
    {
        if(mChannelEventListener != null)
        {
            mChannelEventListener.receive(channelEvent);
        }
        else
        {
            mLog.debug("Channel Event Listener is Null");
        }
    }

    /**
     * Sets the external channel event listener to receive channel events from this traffic channel manager
     */
    @Override
    public void setChannelEventListener(Listener<ChannelEvent> listener)
    {
        mChannelEventListener = listener;
    }

    /**
     * Removes the channel event listener
     */
    @Override
    public void removeChannelEventListener()
    {
        mChannelEventListener = null;
    }

    /**
     * Compares the TO role identifier(s) from each collection for equality
     *
     * @param collection1 containing a TO identifier
     * @param collection2 containing a TO identifier
     * @return true if both collections contain a TO identifier and the TO identifiers are the same value
     */
    private boolean isSameCall(IdentifierCollection collection1, IdentifierCollection collection2)
    {
        Identifier toIdentifier1 = getIdentifier(collection1, Role.TO);
        Identifier toIdentifier2 = getIdentifier(collection2, Role.TO);
        return Objects.equals(toIdentifier1, toIdentifier2);
    }

    /**
     * Retrieves the first identifier with a TO role.
     *
     * @param collection containing a TO identifier
     * @return TO identifier or null
     */
    private Identifier getIdentifier(IdentifierCollection collection, Role role)
    {
        List<Identifier> identifiers = collection.getIdentifiers(role);

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
            broadcast(new ChannelEvent(trafficChannel, Event.REQUEST_DISABLE));
        }
    }

    /**
     * Implements the IDecodeEventProvider interface to provide channel events to an external listener.
     */
    @Override
    public void addDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventListener = listener;
    }

    /**
     * Removes the external decode event listener
     */
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
        List<Channel> channels = new ArrayList<>(mAllocatedTrafficChannelFrequencyMap.values());

        //Issue a disable request for each traffic channel
        for(Channel channel: channels)
        {
            mLog.debug("Stopping traffic channel: " + channel);
            broadcast(new ChannelEvent(channel, Event.REQUEST_DISABLE));
        }
    }

    /**
     * Monitors channel teardown events to detect when traffic channel processing has ended.  Reclaims the
     * channel instance for reuse by future traffic channel grants.
     */
    public class TrafficChannelTeardownMonitor implements Listener<ChannelEvent>
    {
        /**
         * Removes any call events that are associated with the specified frequency.
         * @param frequency that was removed
         * @param description to optionally append to the details for the event
         */
        private void cleanupCallEvents(long frequency, String description)
        {
            List<Integer> lsnsToRemove = new ArrayList<>();

            for(Map.Entry<Integer,DMRChannelGrantEvent> entry: mLSNGrantEventMap.entrySet())
            {
                if(entry.getValue().getChannelDescriptor().getDownlinkFrequency() == frequency)
                {
                    lsnsToRemove.add(entry.getKey());
                }
            }

            mLog.debug("Removing events for the following LSNS:" + lsnsToRemove);

            for(Integer lsn: lsnsToRemove)
            {
                DMRChannelGrantEvent event = mLSNGrantEventMap.remove(lsn);

                if(event != null)
                {
                    if(description != null)
                    {
                        if(event.getEventDescription() != null)
                        {
                            event.setEventDescription(event.getEventDescription() + " - " + description);
                        }
                        else
                        {
                            event.setEventDescription(description);
                        }
                    }

                    broadcast(event);
                }
            }
        }

        @Override
        public synchronized void receive(ChannelEvent channelEvent)
        {
            Channel channel = channelEvent.getChannel();

            if(channel.isTrafficChannel() && (channelEvent.getEvent() == Event.NOTIFICATION_PROCESSING_STOP ||
                channelEvent.getEvent() == Event.NOTIFICATION_PROCESSING_START_REJECTED))
            {
                mLog.debug("Received Channel Stop Event: " + channel.toString() + " Event:" + channelEvent.getEvent());
                String optionalDescription = (channelEvent.getEvent() == Event.NOTIFICATION_PROCESSING_START_REJECTED ?
                    "Rejected" : null);

                for(Map.Entry<Long,Channel> entry: mAllocatedTrafficChannelFrequencyMap.entrySet())
                {
                    if(channel == entry.getValue() && entry.getKey() != null)
                    {
                        mLog.debug("Cleaning up channel frequency map for stopped traffic channel for entry: " + entry.getKey() + " and channel " + entry.getValue());
                        cleanupCallEvents(entry.getKey(), optionalDescription);
                    }
                }

                //Add the traffic channel back to the queue to be reused
                mLog.debug("Adding traffic channel back to queue: " + channel);

                if(!mAvailableTrafficChannelQueue.contains(channel))
                {
                    mAvailableTrafficChannelQueue.add(channel);
                }
            }
        }
    }
}