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
import io.github.dsheirer.identifier.alias.TalkerAliasManager;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageHistoryPreloadData;
import io.github.dsheirer.message.MessageHistoryRequest;
import io.github.dsheirer.message.MessageHistoryResponse;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.event.DMRDecodeEvent;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DMRTrafficChannelManager extends TrafficChannelManager implements IChannelEventListener,
    IChannelEventProvider, IDecodeEventProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRTrafficChannelManager.class);
    public static final String CHANNEL_START_REJECTED = " REJECTED - NO TUNER";
    public static final String DATA_CALL_IGNORED = "DATA CALL IGNORED";
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";
    public static final String NO_FREQUENCY = "NO FREQUENCY - CHECK PLAYLIST CHANNEL CONFIG LSN CHANNEL MAP";
    public static final long EVENT_TIME_STALE_THRESHOLD = 5000; //5 seconds

    private Queue<Channel> mAvailableTrafficChannels = new ConcurrentLinkedQueue<>();
    private List<Channel> mAllocatedTrafficChannels;
    private Map<Long,Channel> mAllocatedChannelFrequencyMap = new ConcurrentHashMap<>();
    private ReentrantLock mLock = new ReentrantLock();
    private Map<Long,IDecodeEvent> mCallEventsTS1 = new ConcurrentHashMap<>();
    private Map<Long,IDecodeEvent> mCallEventsTS2 = new ConcurrentHashMap<>();
    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;
    private TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();
    private TalkerAliasManager mTalkerAliasManager = new TalkerAliasManager();
    private Channel mParentChannel;
    private boolean mIgnoreDataCalls;

    //Used as temporary storage for message and decode event history during Cap+ REST channel rotation
    private DecodeEventHistory mTransientDecodeEventHistory;
    private List<IMessage> mTransientMessageHistory;

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
     * Talker alias manager
     */
    public TalkerAliasManager getTalkerAliasManager()
    {
        return mTalkerAliasManager;
    }

    /**
     * Sets the current parent control channel frequency so that channel grants for the current frequency do not
     * produce an additional traffic channel allocation.
     * @param previous for the current control channel (to remove from allocated channels)
     * @param current for current control channel (to add to allocated channels)
     * @param channel for the current control channel
     */
    public void processControlFrequencyUpdate(long previous, long current, Channel channel)
    {
        if(previous == current)
        {
            return;
        }

        mLock.lock();

        try
        {
            Channel existing = mAllocatedChannelFrequencyMap.get(previous);

            //Only remove the channel if it is non-null and it matches the current control channel.
            if(channel.equals(existing))
            {
                //Unlock the frequency in the channel rotation monitor
                getInterModuleEventBus().post(FrequencyLockChangeRequest.unlock(previous));
                mAllocatedChannelFrequencyMap.remove(previous);
            }

            mAllocatedChannelFrequencyMap.put(current, channel);
            getInterModuleEventBus().post(FrequencyLockChangeRequest.lock(current));
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Creates up to the maximum number of traffic channels for use in allocating traffic channels.
     *
     * Note: this method uses lazy initialization and will only create the channels once.  Subsequent calls will be ignored.
     */
    private void createTrafficChannels()
    {
        if(mAllocatedTrafficChannels == null)
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
                        Channel trafficChannel = new Channel("T-" + mParentChannel.getName(), ChannelType.TRAFFIC);
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

            mAvailableTrafficChannels.addAll(trafficChannelList);

            //Keep track of the complete list so that we can check channel events to determine if the channel event
            //is for a traffic channel owned by this traffic channel manager.
            mAllocatedTrafficChannels = Collections.unmodifiableList(trafficChannelList);
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
        mLock.lock();

        try
        {
            long rest = restChannel.getDownlinkFrequency();

            //Only do the conversion of the original channel has multiple frequencies defined and the rest channel is
            //one of those frequencies
            if(rest > 0 && !mAllocatedChannelFrequencyMap.containsKey(rest) &&
                    channel.getSourceConfiguration().getSourceType() == SourceType.TUNER_MULTIPLE_FREQUENCIES)
            {
                SourceConfigTunerMultipleFrequency originalSourceConfig = (SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration();

                //Add the rest channel to the list of frequencies in the source configuration
                if(!originalSourceConfig.getFrequencies().contains(restChannel.getDownlinkFrequency()))
                {
                    originalSourceConfig.addFrequency(restChannel.getDownlinkFrequency());
                }

                //Convert current channel to a traffic channel if one is available
                Channel trafficChannel = mAvailableTrafficChannels.poll();

                if(trafficChannel != null)
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

                    mAllocatedChannelFrequencyMap.put(currentFrequency, trafficChannel);

                    //Set the preferred frequency to use when restarting the original channel
                    originalSourceConfig.setPreferredFrequency(rest);

                    //Dispatch request to persistently start the original channel with the rest channel frequency and reuse
                    //this traffic channel manager in the new processing chain.
                    ChannelStartProcessingRequest request = new ChannelStartProcessingRequest(channel, restChannel,
                            null, this);
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
        finally
        {
            mLock.unlock();
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
    public void broadcast(IDecodeEvent decodeEvent)
    {
        if(mDecodeEventListener != null)
        {
            mDecodeEventListener.receive(decodeEvent);
        }
    }

    /**
     * Receive decode events/updates from traffic channels to capture in the parent channel's decode event history.  We
     * do this by rebroadcasting the event to the parent control channel's processing chain so that the event history
     * can receive the event or update.
     * @param trafficChannelEvent to receive and rebroadcast.
     */
    public void receiveTrafficChannelEvent(IDecodeEvent trafficChannelEvent)
    {
        broadcast(trafficChannelEvent);
    }

    /**
     * Processes channel grants to allocate traffic channels and track overall channel usage.  Generates
     * decode events for each new channel that is allocated.
     */
    public void processChannelGrant(DMRChannel channel, IdentifierCollection identifierCollection,
                                    Opcode opcode, long timestamp, boolean encrypted)
    {
        mLock.lock();

        try
        {
            boolean allocated = mAllocatedChannelFrequencyMap.containsKey(channel.getDownlinkFrequency());

            if(allocated)
            {
                //Note: if traffic channel is allocated then do nothing & let the channel maintain its own event state
            }
            else
            {
                long frequency = channel.getDownlinkFrequency();

                //If we don't have a frequency value for the channel, use the channel number as a place holder.  We won't
                //allocate a traffic channel for it, but we can track the related channel grant event.
                if(frequency == 0)
                {
                    frequency = channel.getChannelNumber();
                }

                Map<Long,IDecodeEvent> eventMap = channel.getTimeslot() == 1 ? mCallEventsTS1 : mCallEventsTS2;
                IDecodeEvent temp = eventMap.get(frequency);
                DecodeEvent event = (temp != null) ? (DecodeEvent)temp : null;

                if(isStale(event, timestamp, identifierCollection)) //Create new event
                {
                    DecodeEventType decodeEventType = getEventType(opcode, identifierCollection, encrypted);
                    event = DMRDecodeEvent.builder(decodeEventType, timestamp)
                            .channel(channel)
                            .details("CHANNEL GRANT" + (encrypted ? " ENCRYPTED" : ""))
                            .identifiers(identifierCollection)
                            .timeslot(channel.getTimeslot())
                            .build();
                    eventMap.put(frequency, event);
                }
                else if(event != null) //Update current event
                {
                    Identifier from = getIdentifier(identifierCollection, Role.FROM);

                    if(from != null)
                    {
                        Identifier currentFrom = getIdentifier(event.getIdentifierCollection(), Role.FROM);
                        if(currentFrom != null && !Objects.equals(from, currentFrom))
                        {
                            event.end(timestamp);

                            DecodeEventType decodeEventType = getEventType(opcode, identifierCollection, encrypted);
                            event = DMRDecodeEvent.builder(decodeEventType, timestamp)
                                    .channel(channel)
                                    .details("CONTINUE - CHANNEL GRANT" + (encrypted ? " ENCRYPTED" : ""))
                                    .identifiers(identifierCollection)
                                    .timeslot(channel.getTimeslot())
                                    .build();

                            eventMap.put(frequency, event);
                        }
                    }

                    //update the ending timestamp so that the duration value is correctly calculated
                    event.update(timestamp);
                }

                //If our channel doesn't have a frequency value, update the event to reflect this.
                if(channel.getDownlinkFrequency() == 0)
                {
                    if(event.getDetails() == null)
                    {
                        event.setDetails(NO_FREQUENCY);
                    }
                    else if(!event.getDetails().endsWith(NO_FREQUENCY))
                    {
                        event.setDetails(event.getDetails() + " - " + NO_FREQUENCY);
                    }

                    broadcast(event);
                    return;
                }

                if(mIgnoreDataCalls && opcode.isDataChannelGrantOpcode())
                {
                    if(event.getDetails() == null)
                    {
                        event.setDetails(DATA_CALL_IGNORED);
                    }
                    else if(!event.getDetails().endsWith(DATA_CALL_IGNORED))
                    {
                        event.setDetails(event.getDetails() + " - " + DATA_CALL_IGNORED);
                    }

                    broadcast(event);
                    return;
                }

                Channel trafficChannel = mAvailableTrafficChannels.poll();

                if(trafficChannel != null)
                {
                    SourceConfigTuner sourceConfig = new SourceConfigTuner();
                    sourceConfig.setFrequency(frequency);
                    trafficChannel.setSourceConfiguration(sourceConfig);
                    mAllocatedChannelFrequencyMap.put(frequency, trafficChannel);
                    //Preload the channel grant event for the traffic channel to use/maintain
                    if(trafficChannel.getDecodeConfiguration() instanceof DecodeConfigDMR config)
                    {
                        config.setChannelGrantEvent(event);
                    }

                    getInterModuleEventBus().post(new ChannelStartProcessingRequest(trafficChannel, channel,
                            identifierCollection, this));
                }
                else
                {
                    if(event.getDetails() == null)
                    {
                        event.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED);
                    }
                    else if(!event.getDetails().endsWith(MAX_TRAFFIC_CHANNELS_EXCEEDED))
                    {
                        event.setDetails(event.getDetails() + " - " + MAX_TRAFFIC_CHANNELS_EXCEEDED);
                    }
                }

                broadcast(event);
            }
        }
        catch(Exception e)
        {
            //This shouldn't happen, but we'll log if it ever does.
            mLog.error("Error while processing channel grant event - traffic channel lock is released", e);
        }
        finally
        {
            mLock.unlock();
        }
    }


    /**
     * Creates a call event type description for the specified opcode and service options
     */
    private DecodeEventType getEventType(Opcode opcode, IdentifierCollection identifierCollection, boolean encrypted)
    {
        DecodeEventType type = null;

        switch(opcode)
        {
            case STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT:
            case STANDARD_BROADCAST_TALKGROUP_VOICE_CHANNEL_GRANT:
            case MOTOROLA_CAPMAX_CHANNEL_UPDATE_OPEN_MODE:
            case MOTOROLA_CAPMAX_CHANNEL_UPDATE_ADVANTAGE_MODE:
                type = encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
                break;

            case MOTOROLA_CONPLUS_VOICE_CHANNEL_USER:
                Identifier to = identifierCollection.getToIdentifier();

                if(to instanceof DMRRadio)
                {
                    type = encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
                }
                else
                {
                    type = encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
                }
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
     * Indicates if the event is a stale event, meaning that the event is null, or the event start exceeds the max
     * valid call duration threshold, or if the event identifiers don't match the current identifiers.
     *
     * @param event to check for staleness
     * @param timestamp to check the event against
     * @param currentIdentifiers to compare against the event
     * @return true if the event is stale.
     */
    private boolean isStale(IDecodeEvent event, long timestamp, IdentifierCollection currentIdentifiers)
    {
        if(event == null)
        {
            return true;
        }

        if(timestamp - event.getTimeEnd() > EVENT_TIME_STALE_THRESHOLD)
        {
            return true;
        }

        return !isSameCall(event.getIdentifierCollection(), currentIdentifiers);
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
        mLock.lock();

        try
        {
            for(Long frequency: mAllocatedChannelFrequencyMap.keySet())
            {
                getInterModuleEventBus().post(FrequencyLockChangeRequest.lock(frequency));
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    @Override
    public void stop()
    {
        mLock.lock();

        try
        {
            mAvailableTrafficChannels.clear();

            List<Channel> channels = new ArrayList<>(mAllocatedChannelFrequencyMap.values());

            //Issue a disable request for each traffic channel
            for(Channel channel: channels)
            {
                broadcast(new ChannelEvent(channel, Event.REQUEST_DISABLE));
            }
        }
        finally
        {
            mLock.unlock();
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
         */
        private void removeCallEvents(long frequency)
        {
            mLock.lock();

            try
            {
                mCallEventsTS1.remove(frequency);
                mCallEventsTS2.remove(frequency);
            }
            finally
            {
                mLock.unlock();
            }
        }

        /**
         * Updates any call events related to the frequency to indicate the channel start was rejected.
         * @param frequency for the channel.
         */
        private void setChannelStartRejected(long frequency)
        {
            mLock.lock();

            try
            {
                IDecodeEvent event = mCallEventsTS1.get(frequency);

                if(event instanceof DecodeEvent decodeEvent1)
                {
                    if(decodeEvent1.getDetails() == null)
                    {
                        decodeEvent1.setDetails(CHANNEL_START_REJECTED);
                    }
                    else if(!decodeEvent1.getDetails().endsWith(CHANNEL_START_REJECTED))
                    {
                        decodeEvent1.setDetails(event.getDetails() + "-" + CHANNEL_START_REJECTED);
                    }

                    broadcast(decodeEvent1);
                }

                event = mCallEventsTS2.get(frequency);

                if(event instanceof DecodeEvent decodeEvent2)
                {
                    if(decodeEvent2.getDetails() == null)
                    {
                        decodeEvent2.setDetails(CHANNEL_START_REJECTED);
                    }
                    else if(!decodeEvent2.getDetails().endsWith(CHANNEL_START_REJECTED))
                    {
                        decodeEvent2.setDetails(decodeEvent2.getDetails() + "-" + CHANNEL_START_REJECTED);
                    }

                    broadcast(decodeEvent2);
                }
            }
            finally
            {
                mLock.unlock();
            }
        }

        /**
         /**
         * Process channel events from the ChannelProcessingManager to account for owned child traffic channels.
         * Note: this method sees events for ALL channels and not just DMR channels managed by this instance.
         *
         * @param channelEvent to process
         */
        @Override
        public void receive(ChannelEvent channelEvent)
        {
            Channel channel = channelEvent.getChannel();

            //Only process the event if it's one of the traffic channels managed by this instance.
            if(mAllocatedTrafficChannels.contains(channel))
            {
                switch(channelEvent.getEvent())
                {
                    case NOTIFICATION_PROCESSING_STOP:
                        long frequencyToRemove = 0;

                        mLock.lock();

                        try
                        {
                            for(Map.Entry<Long,Channel> entry: mAllocatedChannelFrequencyMap.entrySet())
                            {
                                if(channel == entry.getValue() && entry.getKey() != null)
                                {
                                    frequencyToRemove = entry.getKey();
                                    removeCallEvents(entry.getKey());
                                    break;
                                }
                            }

                            if(frequencyToRemove > 0)
                            {
                                mAllocatedChannelFrequencyMap.remove(frequencyToRemove);

                                //Unlock the frequency in the channel rotation monitor
                                getInterModuleEventBus().post(FrequencyLockChangeRequest.unlock(frequencyToRemove));
                            }

                            //Add the traffic channel back to the queue to be reused
                            if(!mAvailableTrafficChannels.contains(channel))
                            {
                                mAvailableTrafficChannels.add(channel);
                            }
                        }
                        finally
                        {
                            mLock.unlock();
                        }
                        break;
                    case NOTIFICATION_PROCESSING_START_REJECTED:
                        mLock.lock();
                        try
                        {
                            long frequencyToUpdate = 0;

                            for(Map.Entry<Long,Channel> entry: mAllocatedChannelFrequencyMap.entrySet())
                            {
                                if(channel == entry.getValue() && entry.getKey() != null)
                                {
                                    frequencyToUpdate = entry.getKey();
                                    setChannelStartRejected(entry.getKey());
                                    break;
                                }
                            }

                            if(frequencyToUpdate > 0)
                            {
                                mAllocatedChannelFrequencyMap.remove(frequencyToUpdate);

                                //Unlock the frequency in the channel rotation monitor
                                getInterModuleEventBus().post(FrequencyLockChangeRequest.unlock(frequencyToUpdate));
                            }

                            //Add the traffic channel back to the queue to be reused
                            if(!mAvailableTrafficChannels.contains(channel))
                            {
                                mAvailableTrafficChannels.add(channel);
                            }
                        }
                        finally
                        {
                            mLock.unlock();
                        }
                        break;
                }
            }
        }
    }
}