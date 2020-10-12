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

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelConversionRequest;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelEvent.Event;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageHistoryPreloadData;
import io.github.dsheirer.message.MessageHistoryRequest;
import io.github.dsheirer.message.MessageHistoryResponse;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.event.DMRChannelGrantEvent;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventHistory;
import io.github.dsheirer.module.decode.event.DecodeEventHistoryPreloadData;
import io.github.dsheirer.module.decode.event.DecodeEventHistoryRequest;
import io.github.dsheirer.module.decode.event.DecodeEventHistoryResponse;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.channel.rotation.DisableChannelRotationMonitorRequest;
import io.github.dsheirer.source.tuner.channel.rotation.FrequencyLockChangeRequest;
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
public class DMRTrafficChannelManager extends TrafficChannelManager implements IDecodeEventProvider, IChannelEventListener,
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

    //Used as temporary storage for message and decode event history during Cap+ REST channel rotation
    private DecodeEventHistory mTransientDecodeEventHistory;
    private List<IMessage> mTransientMessageHistory;

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
     * Used with Capacity Plus systems to convert the existing standard channel to a traffic channel and then recreate
     * the original standard channel with the frequency specified for the new rest channel.
     * @param channel
     * @param currentFrequency of the standard channel
     * @param restChannel to identify the new channel frequency to start
     */
    public void convertToTrafficChannel(Channel channel, long currentFrequency, IChannelDescriptor restChannel,
                                        DMRNetworkConfigurationMonitor networkConfigurationMonitor)
    {
        //Only do the conversion of the original channel has multiple frequencies defined and the rest channel is
        //one of those frequencies
        if(restChannel.getDownlinkFrequency() > 0 &&
           !mAllocatedTrafficChannelFrequencyMap.containsKey(restChannel.getDownlinkFrequency()) &&
           channel.getSourceConfiguration().getSourceType() == SourceType.TUNER_MULTIPLE_FREQUENCIES)
        {
            SourceConfigTunerMultipleFrequency originalSourceConfig = (SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration();

            //Add the rest channel to the list of frequencies in the source configuration
            if(!originalSourceConfig.getFrequencies().contains(restChannel.getDownlinkFrequency()))
            {
                originalSourceConfig.addFrequency(restChannel.getDownlinkFrequency());

                //Note: the channel configuration modification is not persisted from here, to avoid unnecessary coupling
                //to the channel model.  The configuration will simply be adjusted at runtime.
            }

            //Convert current channel to a traffic channel if one is available
            Channel trafficChannel = mAvailableTrafficChannelQueue.poll();

            if(trafficChannel != null && restChannel.getDownlinkFrequency() > 0)
            {
                //Disable the channel rotation manager when we have multiple frequencies defined
                getInterModuleEventBus().post(new DisableChannelRotationMonitorRequest());

                //Set the frequency for the traffic channel configuration
                SourceConfigTuner trafficSourceConfig = new SourceConfigTuner();
                trafficSourceConfig.setFrequency(currentFrequency);
                trafficChannel.setSourceConfiguration(trafficSourceConfig);

                //Post a request for message and decode event history to transfer to the new REST channel.  This has
                // to be posted before the channel conversion request
                getInterModuleEventBus().post(new DecodeEventHistoryRequest());
                getInterModuleEventBus().post(new MessageHistoryRequest());

                //Dispatch a request to convert this processing chain to the traffic channel.  This will cause the
                //processing chain to convert to a traffic channel and notify all of the modules, which will in-turn
                //cause the decoder states (2 timeslots) to dereference this manager so that the existing channel can
                //no longer allocate traffic channels.
                getInterModuleEventBus().post(new ChannelConversionRequest(channel, trafficChannel));

                mAllocatedTrafficChannelFrequencyMap.put(currentFrequency, trafficChannel);

                //Set the preferred frequency to use when restarting the original channel
                originalSourceConfig.setPreferredFrequency(restChannel.getDownlinkFrequency());

                //Dispatch request to persistently start the original channel with the rest channel frequency and reuse
                //this traffic channel manager in the new processing chain.
                ChannelStartProcessingRequest request = new ChannelStartProcessingRequest(channel, this);
                request.setPersistentAttempt(true);
                request.setChildDecodeEventHistory(mTransientDecodeEventHistory);

                //If we received an event history response, add it to the request as preload data content
                if(mTransientDecodeEventHistory != null)
                {
                    DecodeEventHistoryPreloadData eventHistory =
                        new DecodeEventHistoryPreloadData(mTransientDecodeEventHistory.getItems());
                    request.addPreloadDataContent(eventHistory);

                    mTransientDecodeEventHistory = null;
                }

                //If we received a message history response, add it to the request as preload data content
                if(mTransientMessageHistory != null)
                {
                    MessageHistoryPreloadData messageHistory = new MessageHistoryPreloadData(mTransientMessageHistory);
                    request.addPreloadDataContent(messageHistory);
                    mTransientMessageHistory = null;
                }

                //Add the DMR network configuration monitor as preload data
                if(networkConfigurationMonitor != null)
                {
                    request.addPreloadDataContent(new DMRNetworkConfigurationPreloadData(networkConfigurationMonitor));
                }

                getInterModuleEventBus().post(request);
            }
        }
    }

    /**
     * Processes a decode event history response and temporarily stores the event history.
     *
     * Note: this is used for Cap+ REST channel rotation.
     *
     * @param response containing the current decode event history.
     */
    @Subscribe
    public void process(DecodeEventHistoryResponse response)
    {
        mTransientDecodeEventHistory = response.getDecodeEventHistory();
    }

    /**
     * Processes a message history response and temporarily stores the history.
     *
     * Note: this is used for Cap+ REST channel rotation.
     *
     * @param response containing the current message history.
     */
    @Subscribe
    public void process(MessageHistoryResponse response)
    {
        mTransientMessageHistory = response.getMessages();
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
                    getInterModuleEventBus().post(new ChannelStartProcessingRequest(trafficChannel, channel, identifierCollection));
                }
                else
                {
                    String description = (event.getEventDescription() != null ? event.getEventDescription() : "") +
                        " - MAX TRAFFIC CHANNELS EXCEEDED";
                    event.setEventDescription(description);
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
            }
            else
            {
                SourceConfigTuner sourceConfig = new SourceConfigTuner();
                sourceConfig.setFrequency(frequency);
                trafficChannel.setSourceConfiguration(sourceConfig);
                mAllocatedTrafficChannelFrequencyMap.put(frequency, trafficChannel);
                broadcast(ChannelEvent.requestEnable(trafficChannel));
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

    /**
     * Starts this traffic channel manager.
     *
     * Note: for Capacity+ systems, this traffic channel manager will be reused when the current channel is in use and
     * a new rest channel is nominated.  This traffic channel manager instance will be transferred to the new standard
     * channel created to monitor the new rest channel.  As such, this manager will have a list of currently allocated
     * traffic channels.  Broadcast frequency lock requests for each allocated traffic channel frequency so that the
     * new rest channel rotation manager doesn't rotate onto frequencies already being monitored as traffic channels.
     */
    @Override
    public void start()
    {
        for(Long frequency: mAllocatedTrafficChannelFrequencyMap.keySet())
        {
            getInterModuleEventBus().post(FrequencyLockChangeRequest.lock(frequency));
        }
    }

    @Override
    public void stop()
    {
        mAvailableTrafficChannelQueue.clear();

        List<Channel> channels = new ArrayList<>(mAllocatedTrafficChannelFrequencyMap.values());

        //Issue a disable request for each traffic channel
        for(Channel channel: channels)
        {
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
                String optionalDescription = (channelEvent.getEvent() == Event.NOTIFICATION_PROCESSING_START_REJECTED ?
                    "Rejected" : null);

                long frequencyToRemove = 0;

                for(Map.Entry<Long,Channel> entry: mAllocatedTrafficChannelFrequencyMap.entrySet())
                {
                    if(channel == entry.getValue() && entry.getKey() != null)
                    {
                        frequencyToRemove = entry.getKey();
                        cleanupCallEvents(entry.getKey(), optionalDescription);
                        break;
                    }
                }

                if(frequencyToRemove > 0)
                {
                    mAllocatedTrafficChannelFrequencyMap.remove(frequencyToRemove);

                    //Unlock the frequency in the channel rotation monitor
                    getInterModuleEventBus().post(FrequencyLockChangeRequest.unlock(frequencyToRemove));
                }

                //Add the traffic channel back to the queue to be reused
                if(!mAvailableTrafficChannelQueue.contains(channel))
                {
                    mAvailableTrafficChannelQueue.add(channel);
                }
            }
        }
    }
}