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

package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.mpt1327.channel.MPT1327Channel;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MPT1327TrafficChannelManager extends TrafficChannelManager implements IDecodeEventProvider,
    IChannelEventListener, IChannelEventProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327TrafficChannelManager.class);

    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";

    private Queue<Channel> mAvailableTrafficChannelQueue = new ConcurrentLinkedQueue<>();
    private List<Channel> mManagedTrafficChannels;
    private Map<MPT1327Channel,Channel> mAllocatedTrafficChannelMap = new ConcurrentHashMap<>();
    private Map<MPT1327Channel,MPT1327ChannelGrantEvent> mChannelGrantEventMap = new ConcurrentHashMap<>();
    private TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();
    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;
    private ChannelMap mChannelMap;

    /**
     * Constructs an MPT1327 traffic channel manage.
     *
     * @param parentChannel containing configuration items for traffic channels to inherit
     */
    public MPT1327TrafficChannelManager(Channel parentChannel, ChannelMap channelMap)
    {
        createTrafficChannels(parentChannel);
        mChannelMap = channelMap;
    }

    /**
     * Notification that the control frequency is updated.
     * @param previous frequency for the control channel (to remove from allocated channels)
     * @param current frequency for the control channel (to add to allocated channels)
     * @param channel for the current control channel
     */
    @Override
    protected void processControlFrequencyUpdate(long previous, long current, Channel channel)
    {
        MPT1327Channel toRemove = null;

        for(MPT1327Channel mpt: mAllocatedTrafficChannelMap.keySet())
        {
            if(mpt.getDownlinkFrequency() == current)
            {
                toRemove = mpt;
                break;
            }
        }

        if(toRemove != null)
        {
            broadcast(new ChannelEvent(mAllocatedTrafficChannelMap.get(toRemove), ChannelEvent.Event.REQUEST_DISABLE));
        }
    }

    /**
     * Processes channel grants to allocate traffic channels and track overall channel usage.  Generates
     * decode events for each new channel that is allocated.
     */
    public void processChannelGrant(MPT1327Message mpt1327Message, IdentifierCollection identifierCollection)
    {
        if(mpt1327Message.getMessageType() == MPT1327Message.MPTMessageType.GTC)
        {
            MPT1327Channel mpt1327Channel = MPT1327Channel.create(mpt1327Message.getChannel());
            mpt1327Channel.setChannelMap(mChannelMap);

            MPT1327ChannelGrantEvent event = mChannelGrantEventMap.get(mpt1327Channel);

            if(event != null)
            {
                if(isSameTalkgroup(identifierCollection, event.getIdentifierCollection()))
                {
                    //update the ending timestamp so that the duration value is correctly calculated
                    event.end(mpt1327Message.getTimestamp());
                    return;
                }
                else if(mAllocatedTrafficChannelMap.containsKey(mpt1327Channel))
                {
                    Channel trafficChannel = mAllocatedTrafficChannelMap.get(mpt1327Channel);
                    broadcast(new ChannelEvent(trafficChannel, ChannelEvent.Event.REQUEST_DISABLE));
                }
            }

            MPT1327ChannelGrantEvent channelGrantEvent = MPT1327ChannelGrantEvent
                .mpt1327Builder(DecodeEventType.CALL, mpt1327Message.getTimestamp())
                .channel(mpt1327Channel)
                .details("Traffic Channel Grant")
                .identifiers(identifierCollection)
                .build();

            mChannelGrantEventMap.put(mpt1327Channel, channelGrantEvent);

            if(mpt1327Channel.getDownlinkFrequency() == 0)
            {
                channelGrantEvent.setDetails("Invalid Channel Map - No Frequency For Channel " + mpt1327Channel.getChannelNumber());
            }
            else
            {
                Channel trafficChannel = mAvailableTrafficChannelQueue.poll();

                if(trafficChannel == null)
                {
                    channelGrantEvent.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED);
                    channelGrantEvent.setDetails("Detect:" + channelGrantEvent.getDetails());
                    return;
                }

                SourceConfigTuner sourceConfig = new SourceConfigTuner();
                sourceConfig.setFrequency(mpt1327Channel.getDownlinkFrequency());
                trafficChannel.setSourceConfiguration(sourceConfig);
                mAllocatedTrafficChannelMap.put(mpt1327Channel, trafficChannel);
                getInterModuleEventBus().post(new ChannelStartProcessingRequest(trafficChannel, mpt1327Channel,
                    identifierCollection));
            }

            broadcast(channelGrantEvent);
        }
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

        if(decodeConfiguration instanceof DecodeConfigMPT1327 decodeConfigMPT1327)
        {
            int maxTrafficChannels = decodeConfigMPT1327.getTrafficChannelPoolSize();

            for(int x = 0; x < maxTrafficChannels; x++)
            {
                Channel trafficChannel = new Channel("T-" + parentChannel.getName(), Channel.ChannelType.TRAFFIC);
                trafficChannel.setAliasListName(parentChannel.getAliasListName());
                trafficChannel.setSystem(parentChannel.getSystem());
                trafficChannel.setSite(parentChannel.getSite());
                trafficChannel.setDecodeConfiguration(decodeConfigMPT1327);
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
        mAvailableTrafficChannelQueue.clear();
        List<Channel> channels = new ArrayList<>(mAllocatedTrafficChannelMap.values());

        //Issue a disable request for each traffic channel
        for(Channel channel : channels)
        {
            mLog.debug("Stopping traffic channel: " + channel);
            broadcast(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_DISABLE));
        }
    }


    /**
     * Compares the TO role identifier(s) from each collection for equality
     *
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
     *
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
     * Monitors channel teardown events to detect when traffic channel processing has ended.  Reclaims the
     * channel instance for reuse by future traffic channel grants.
     */
    public class TrafficChannelTeardownMonitor implements Listener<ChannelEvent>
    {

        /**
         * Process channel events from the ChannelProcessingManager to account for owned child traffic channels.
         * Note: this method sees events for ALL channels and not just MPT1327 channels managed by this instance.
         *
         * @param channelEvent to process
         */
        @Override
        public synchronized void receive(ChannelEvent channelEvent)
        {
            Channel channel = channelEvent.getChannel();

            if(channel.isTrafficChannel() && mManagedTrafficChannels.contains(channel))
            {
                switch(channelEvent.getEvent())
                {
                    case NOTIFICATION_PROCESSING_STOP:
                        final var toRemove = mptChannelForChannel(channel);

                        if(toRemove != null)
                        {
                            mAllocatedTrafficChannelMap.remove(toRemove);
                            mAvailableTrafficChannelQueue.add(channel);

                            final var event = mChannelGrantEventMap.remove(toRemove);

                            if(event != null)
                            {
                                event.end(System.currentTimeMillis());
                                broadcast(event);
                            }
                        }
                        break;
                    case NOTIFICATION_PROCESSING_START_REJECTED:
                        final var rejected = mptChannelForChannel(channel);

                        if(rejected != null)
                        {
                            mAllocatedTrafficChannelMap.remove(rejected);
                            mAvailableTrafficChannelQueue.add(channel);

                            final var event = mChannelGrantEventMap.remove(rejected);

                            if(event != null)
                            {
                                if(channelEvent.getDescription() != null)
                                {
                                    event.setDetails(channelEvent.getDescription() + " - " + event.getDetails());
                                }
                                else
                                {
                                    event.setDetails(channelEvent.getDescription() + " - " + CHANNEL_START_REJECTED);
                                }
                                broadcast(event);
                            }
                        }
                        break;
                }
            }
        }

        private MPT1327Channel mptChannelForChannel(final Channel channel)
        {
            for (final var entry : mAllocatedTrafficChannelMap.entrySet())
            {
                if (entry.getValue() == channel)
                {
                    return entry.getKey();
                }
            }
            return null;
        }
    }
}
