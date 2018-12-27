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
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
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
public class P25TrafficChannelManager extends Module implements IDecodeEventProvider, IChannelEventListener,
    IChannelEventProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(P25TrafficChannelManager.class);
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";

    private Queue<Channel> mAvailableTrafficChannelQueue = new ConcurrentLinkedQueue<>();
    private List<Channel> mManagedTrafficChannels;
    private Map<APCO25Channel,Channel> mAllocatedTrafficChannelMap = new ConcurrentHashMap<>();
    private Map<APCO25Channel,P25ChannelGrantEvent> mChannelGrantEventMap = new ConcurrentHashMap<>();

    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;

    private TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();

    private boolean mIgnoreDataCalls = false;

    /**
     * Monitors call events and allocates traffic decoder channels in response
     * to traffic channel allocation call events.  Manages a pool of reusable
     * traffic channel allocations.
     *
     * @param parentChannel that owns this traffic channel manager
     */
    public P25TrafficChannelManager(Channel parentChannel)
    {
        createTrafficChannels(parentChannel);
        DecodeConfigP25Phase1 p25Config = (DecodeConfigP25Phase1)parentChannel.getDecodeConfiguration();
        mIgnoreDataCalls = p25Config.getIgnoreDataCalls();
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
                trafficChannel.setDecodeConfiguration(p25DecodeConfig);
                trafficChannel.setEventLogConfiguration(parentChannel.getEventLogConfiguration());
                trafficChannel.setRecordConfiguration(parentChannel.getRecordConfiguration());
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
     * @param apco25Channel for the traffic channel
     * @param serviceOptions for the traffic channel - optional can be null
     * @param identifierCollection associated with the channel grant
     * @param opcode to identify the call type for the event description
     */
    public void processChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                    IdentifierCollection identifierCollection, Opcode opcode, long timestamp)
    {
        P25ChannelGrantEvent event = mChannelGrantEventMap.get(apco25Channel);

        if(event != null && isSameCall(identifierCollection, event.getIdentifierCollection()))
        {
            Identifier from = getIdentifier(identifierCollection, Role.FROM);

            if(from != null)
            {
                Identifier currentFrom = getIdentifier(event.getIdentifierCollection(), Role.FROM);
                if(currentFrom != null && !Objects.equals(from, currentFrom))
                {
                    event.end(timestamp);

                    P25ChannelGrantEvent continuationGrantEvent = P25ChannelGrantEvent.builder(timestamp, serviceOptions)
                        .channel(apco25Channel)
                        .eventDescription(getEventType(opcode, serviceOptions).toString() + " - Continue")
                        .details("CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : "UNKNOWN SERVICE OPTIONS"))
                        .identifiers(identifierCollection)
                        .build();

                    mChannelGrantEventMap.put(apco25Channel, continuationGrantEvent);
                    broadcast(continuationGrantEvent);
                }
            }

            //update the ending timestamp so that the duration value is correctly calculated
            event.update(timestamp);
            broadcast(event);

            //Even though we have an event, the initial channel grant may have been rejected.  Check to see if there
            //is a traffic channel allocated.  If not, allocate one and update the event description.
            if(!mAllocatedTrafficChannelMap.containsKey(apco25Channel) &&
               !(mIgnoreDataCalls && opcode == Opcode.OSP_SNDCP_DATA_CHANNEL_GRANT))
            {
                Channel trafficChannel = mAvailableTrafficChannelQueue.poll();

                if(trafficChannel != null)
                {
                    event.setEventDescription(getEventType(opcode, serviceOptions).toString());
                    event.setDetails("CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : "UNKNOWN SERVICE OPTIONS"));
                    event.setChannelDescriptor(apco25Channel);
                    broadcast(event);
                    SourceConfigTuner sourceConfig = new SourceConfigTuner();
                    sourceConfig.setFrequency(apco25Channel.getDownlinkFrequency());
                    trafficChannel.setSourceConfiguration(sourceConfig);
                    mAllocatedTrafficChannelMap.put(apco25Channel, trafficChannel);
                    broadcast(new ChannelGrantEvent(trafficChannel, Event.REQUEST_ENABLE, apco25Channel, identifierCollection));
                }
            }

            return;
        }

        if(mIgnoreDataCalls && opcode == Opcode.OSP_SNDCP_DATA_CHANNEL_GRANT)
        {
            P25ChannelGrantEvent channelGrantEvent = P25ChannelGrantEvent.builder(timestamp, serviceOptions)
                .channel(apco25Channel)
                .eventDescription(getEventType(opcode, serviceOptions).toString() + " - Ignored")
                .details("DATA CALL IGNORED: " + (serviceOptions != null ? serviceOptions : "UNKNOWN SERVICE OPTIONS"))
                .identifiers(identifierCollection)
                .build();

            mChannelGrantEventMap.put(apco25Channel, channelGrantEvent);

            broadcast(channelGrantEvent);
            return;
        }

        P25ChannelGrantEvent channelGrantEvent = P25ChannelGrantEvent.builder(timestamp, serviceOptions)
            .channel(apco25Channel)
            .eventDescription(getEventType(opcode, serviceOptions).toString())
            .details("CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : "UNKNOWN SERVICE OPTIONS"))
            .identifiers(identifierCollection)
            .build();

        mChannelGrantEventMap.put(apco25Channel, channelGrantEvent);

        //Allocate a traffic channel for the downlink frequency if one isn't already allocated
        //NOTE: we could also allocate a traffic channel for the uplink frequency here, in the future
        if(!mAllocatedTrafficChannelMap.containsKey(apco25Channel))
        {
            Channel trafficChannel = mAvailableTrafficChannelQueue.poll();

            if(trafficChannel == null)
            {
                channelGrantEvent.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED);
                channelGrantEvent.setEventDescription(channelGrantEvent.getEventDescription() + " - Ignored");
                return;
            }

            SourceConfigTuner sourceConfig = new SourceConfigTuner();
            sourceConfig.setFrequency(apco25Channel.getDownlinkFrequency());
            trafficChannel.setSourceConfiguration(sourceConfig);
            mAllocatedTrafficChannelMap.put(apco25Channel, trafficChannel);
            broadcast(new ChannelGrantEvent(trafficChannel, Event.REQUEST_ENABLE, apco25Channel, identifierCollection));
        }

        broadcast(channelGrantEvent);
    }

    /**
     * Creates a call event type description for the specified opcode and service options
     */
    private DecodeEventType getEventType(Opcode opcode, ServiceOptions serviceOptions)
    {
        boolean encrypted = serviceOptions != null ? serviceOptions.isEncrypted() : false;

        DecodeEventType type = null;

        switch(opcode)
        {
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                type = encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
                break;

            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                type = encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
                break;

            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                type = encrypted ? DecodeEventType.CALL_INTERCONNECT_ENCRYPTED : DecodeEventType.CALL_INTERCONNECT;
                break;

            case OSP_SNDCP_DATA_CHANNEL_GRANT:
            case OSP_GROUP_DATA_CHANNEL_GRANT:
            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                type = encrypted ? DecodeEventType.DATA_CALL_ENCRYPTED : DecodeEventType.DATA_CALL;
                break;

            case MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT:
            case MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT_UPDATE:
                type = encrypted ? DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED : DecodeEventType.CALL_PATCH_GROUP;
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

        mAvailableTrafficChannelQueue.clear();

//        mTrafficChannelsInUse.clear();
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
        List<Channel> channels = new ArrayList<>(mAllocatedTrafficChannelMap.values());

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
        @Override
        public synchronized void receive(ChannelEvent channelEvent)
        {
            Channel channel = channelEvent.getChannel();

            if(channel.isTrafficChannel() && mManagedTrafficChannels.contains(channel))
            {
                switch(channelEvent.getEvent())
                {
                    case NOTIFICATION_PROCESSING_STOP:
                        APCO25Channel toRemove = null;

                        for(Map.Entry<APCO25Channel,Channel> entry: mAllocatedTrafficChannelMap.entrySet())
                        {
                            if(entry.getValue() == channel)
                            {
                                toRemove = entry.getKey();
                                continue;
                            }
                        }

                        if(toRemove != null)
                        {
                            mAllocatedTrafficChannelMap.remove(toRemove);
                            mAvailableTrafficChannelQueue.add(channel);
                            P25ChannelGrantEvent event = mChannelGrantEventMap.remove(toRemove);

                            if(event != null)
                            {
                                event.end(System.currentTimeMillis());
                                broadcast(event);
                            }
                        }
                        break;
                    case NOTIFICATION_PROCESSING_START_REJECTED:
                        APCO25Channel rejected = null;

                        for(Map.Entry<APCO25Channel,Channel> entry: mAllocatedTrafficChannelMap.entrySet())
                        {
                            if(entry.getValue() == channel)
                            {
                                rejected = entry.getKey();
                                continue;
                            }
                        }

                        if(rejected != null)
                        {
                            mAllocatedTrafficChannelMap.remove(rejected);
                            mAvailableTrafficChannelQueue.add(channel);
                            P25ChannelGrantEvent event = mChannelGrantEventMap.get(rejected);

                            if(event != null)
                            {
                                event.setEventDescription(event.getEventDescription() + " - Rejected");

                                if(channelEvent.getDescription() != null)
                                {
                                    event.setDetails(channelEvent.getDescription() + " - " + event.getDetails());
                                }
                                else
                                {
                                    event.setDetails(CHANNEL_START_REJECTED + " - " + event.getDetails());
                                }

                                broadcast(event);
                            }
                        }
                        break;
                }
            }
        }
    }
}