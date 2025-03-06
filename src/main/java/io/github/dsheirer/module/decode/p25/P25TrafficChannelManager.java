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
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelEvent.Event;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.alias.TalkerAliasManager;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupPreLoadDataContent;
import io.github.dsheirer.identifier.scramble.ScrambleParameterIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventDuplicateDetector;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25P2Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25P2ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.reference.DataServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors traffic channel state for a control channel and monitors individual traffic channel updates to track all
 * call events across the system.
 *
 * Call events originate on the control channel and receive continuing updates from the control channel.  Events
 * continue on the traffic channel.  The call event trackers manage the call event until call end where the call
 * duration is updated from the control messaging and once the traffic channel is allocated the tracker only accepts
 * event timing updates from the traffic messaging and at the end of the call the tracker marks the event as complete.
 * Further updates to an ended event will cause the event to be removed and replaced with the next tracked event.
 *
 * Creates and reuses a limited set of Channel instances each with a TRAFFIC channel type.  Since each of the traffic
 * channels will be using the same decoder type and configuration options, we reuse each of the channel instances to
 * allow the ChannelProcessingManager to reuse a cached set of processing chains that are created as each channel is
 * activated.
 *
 * Each traffic channel is activated by sending a ChannelEvent via the channel model.  The channel processing manager
 * receives the activation request.  If successful, a processing chain is activated for the traffic channel.  Otherwise,
 * a channel event is broadcast indicating that the channel could not be activated.  On teardown of an activated traffic
 * channel, a channel event is broadcast to indicate the traffic channels is no longer active.
 *
 * The ReentrantLock (mLock) protects threaded access to the mTSxChannelGrantEventMaps and to the allocated and
 * available traffic channel lists and queues.
 */
public class P25TrafficChannelManager extends TrafficChannelManager implements IDecodeEventProvider, IChannelEventListener,
    IChannelEventProvider, IMessageListener
{
    private static final Logger mLog = LoggerFactory.getLogger(P25TrafficChannelManager.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(mLog);
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";

    private Queue<Channel> mAvailablePhase1TrafficChannelQueue = new LinkedTransferQueue<>();
    private Queue<Channel> mAvailablePhase2TrafficChannelQueue = new LinkedTransferQueue<>();
    private List<Channel> mManagedPhase1TrafficChannels;
    private List<Channel> mManagedPhase2TrafficChannels;
    private Map<Long,Channel> mAllocatedTrafficChannelMap = new HashMap<>();
    private Map<Long,P25TrafficChannelEventTracker> mTS1ChannelGrantEventMap = new HashMap<>();
    private Map<Long,P25TrafficChannelEventTracker> mTS2ChannelGrantEventMap = new HashMap<>();
    private ReentrantLock mLock = new ReentrantLock();
    private Map<Integer, IFrequencyBand> mFrequencyBandMap = new ConcurrentHashMap<>();
    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;
    private TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();
    private Channel mParentChannel;
    private ScrambleParameters mPhase2ScrambleParameters;
    private Listener<IMessage> mMessageListener;
    private boolean mIgnoreDataCalls;
    //Used only for data calls
    private DecodeEventDuplicateDetector mDuplicateDetector = new DecodeEventDuplicateDetector();
    private TalkerAliasManager mTalkerAliasManager = new TalkerAliasManager();

    /**
     * Constructs an instance.
     * @param parentChannel (ie control channel) that owns this traffic channel manager
     */
    public P25TrafficChannelManager(Channel parentChannel)
    {
        mParentChannel = parentChannel;

        if(parentChannel.getDecodeConfiguration() instanceof DecodeConfigP25Phase1 phase1)
        {
            mIgnoreDataCalls = phase1.getIgnoreDataCalls();
            createPhase1TrafficChannels(phase1.getTrafficChannelPoolSize(), phase1);
            createPhase2TrafficChannels(phase1.getTrafficChannelPoolSize(), new DecodeConfigP25Phase2());
        }
        else if(parentChannel.getDecodeConfiguration() instanceof DecodeConfigP25Phase2 phase2)
        {
            mIgnoreDataCalls = phase2.getIgnoreDataCalls();
            createPhase1TrafficChannels(phase2.getTrafficChannelPoolSize(), new DecodeConfigP25Phase1());
            createPhase2TrafficChannels(phase2.getTrafficChannelPoolSize(), phase2);
        }
    }

    /**
     * Talker alias manager
     * @return manager
     */
    public TalkerAliasManager getTalkerAliasManager()
    {
        return mTalkerAliasManager;
    }

    /**
     * Stores the frequency band (aka Identifier Update) to use for preload data in starting a new traffic channel.
     * @param frequencyBand to store
     */
    public void processFrequencyBand(IFrequencyBand frequencyBand)
    {
        mFrequencyBandMap.put(frequencyBand.getIdentifier(), frequencyBand);
    }

    /**
     * Notification that the control channel frequency is updated and removes any traffic channel that may be running
     * against the same frequency.
     * @param previous frequency (before this update)
     * @param current frequency
     * @param parentChannel configuration
     */
    @Override
    protected void processControlFrequencyUpdate(long previous, long current, Channel parentChannel)
    {
        if(previous == current)
        {
            return;
        }

        mLock.lock();

        try
        {
            //Shutdown all existing traffic channels and clear the maps.
            List<Channel> trafficChannelsToDisable = new ArrayList<>(mAllocatedTrafficChannelMap.values());

            for(Channel channelToDisable : trafficChannelsToDisable)
            {
                if(!parentChannel.equals(channelToDisable))
                {
                    broadcast(new ChannelEvent(channelToDisable, Event.REQUEST_DISABLE));
                }
            }

            mTS1ChannelGrantEventMap.clear();
            mTS2ChannelGrantEventMap.clear();

            //Remove the control channel from the previous frequency
            mAllocatedTrafficChannelMap.remove(previous);

            //Store the current control channel in the allocated channel map so that we don't allocate a traffic channel against it
            mAllocatedTrafficChannelMap.put(current, parentChannel);
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Creates up to the maximum number of traffic channels for use in allocating traffic channels.
     * @param trafficChannelPoolSize number of traffic channels to create
     * @param decodeConfigP25Phase1 to use for each traffic channel
     */
    private void createPhase1TrafficChannels(int trafficChannelPoolSize, DecodeConfigP25Phase1 decodeConfigP25Phase1)
    {
        if(mManagedPhase1TrafficChannels == null)
        {
            List<Channel> trafficChannelList = new ArrayList<>();

            if(trafficChannelPoolSize > 0)
            {
                for(int x = 0; x < trafficChannelPoolSize; x++)
                {
                    Channel trafficChannel = new Channel("T-" + mParentChannel.getName(), ChannelType.TRAFFIC);
                    trafficChannel.setAliasListName(mParentChannel.getAliasListName());
                    trafficChannel.setSystem(mParentChannel.getSystem());
                    trafficChannel.setSite(mParentChannel.getSite());
                    trafficChannel.setDecodeConfiguration(decodeConfigP25Phase1);
                    trafficChannel.setEventLogConfiguration(mParentChannel.getEventLogConfiguration());
                    trafficChannel.setRecordConfiguration(mParentChannel.getRecordConfiguration());
                    trafficChannelList.add(trafficChannel);
                }
            }

            mAvailablePhase1TrafficChannelQueue.addAll(trafficChannelList);
            mManagedPhase1TrafficChannels = Collections.unmodifiableList(trafficChannelList);
        }
    }

    /**
     * Creates up to the maximum number of traffic channels for use in allocating traffic channels.
     * @param trafficChannelPoolSize number of traffic channels to create
     * @param decodeConfiguration for the parent channel
     */
    private void createPhase2TrafficChannels(int trafficChannelPoolSize, DecodeConfiguration decodeConfiguration)
    {
        if(mManagedPhase2TrafficChannels == null)
        {
            List<Channel> trafficChannelList = new ArrayList<>();

            if(trafficChannelPoolSize > 0)
            {
                for(int x = 0; x < trafficChannelPoolSize; x++)
                {
                    Channel trafficChannel = new Channel("T-" + mParentChannel.getName(), ChannelType.TRAFFIC);
                    trafficChannel.setAliasListName(mParentChannel.getAliasListName());
                    trafficChannel.setSystem(mParentChannel.getSystem());
                    trafficChannel.setSite(mParentChannel.getSite());
                    trafficChannel.setDecodeConfiguration(decodeConfiguration);
                    trafficChannel.setEventLogConfiguration(mParentChannel.getEventLogConfiguration());
                    trafficChannel.setRecordConfiguration(mParentChannel.getRecordConfiguration());
                    trafficChannelList.add(trafficChannel);
                }
            }

            mAvailablePhase2TrafficChannelQueue.addAll(trafficChannelList);
            mManagedPhase2TrafficChannels = Collections.unmodifiableList(trafficChannelList);
        }
    }

    /**
     * Broadcasts an initial or update decode event to any registered listener.
     */
    public void broadcast(DecodeEvent decodeEvent)
    {
        if(mDecodeEventListener != null)
        {
            if(decodeEvent.getEventType() == DecodeEventType.DATA_CALL &&
                    mDuplicateDetector.isDuplicate(decodeEvent, System.currentTimeMillis()))
            {
                return;
            }

            mDecodeEventListener.receive(decodeEvent);
        }
    }

    /**
     * Broadcasts the decode event from the tracker.
     * @param tracker containing a decode event.
     */
    public void broadcast(P25TrafficChannelEventTracker tracker)
    {
        broadcast(tracker.getEvent());
    }

    /**
     * Retrieves the current event tracker for the specified channel and if the tracker is stale relative to the
     * timestamp, returns null, otherwise returns the current tracker.
     * @param channel to look up the tracker
     * @param timestamp to compare for staleness
     * @return tracker or null
     */
    private P25TrafficChannelEventTracker getTrackerRemoveIfStale(APCO25Channel channel, long timestamp)
    {
        return getTrackerRemoveIfStale(channel.getDownlinkFrequency(), channel.getTimeslot(), timestamp);
    }

    /**
     * Retrieves the current event tracker for the specified channel and if the tracker is stale relative to the
     * timestamp, returns null, otherwise returns the current tracker.
     * @param frequency to look up the tracker
     * @param timeslot to look up the tracker
     * @param timestamp to compare for staleness
     * @return tracker or null
     */
    private P25TrafficChannelEventTracker getTrackerRemoveIfStale(long frequency, int timeslot, long timestamp)
    {
        P25TrafficChannelEventTracker tracker = getTracker(frequency, timeslot);

        if(tracker != null && tracker.isStale(timestamp))
        {
            removeTracker(frequency, timeslot);
            tracker = null;
        }

        return tracker;
    }

    /**
     * Gets the tracker from the correct channel grant map.
     * @param frequency for the map lookup
     * @param timeslot to identify the correct map.
     */
    private P25TrafficChannelEventTracker getTracker(long frequency, int timeslot)
    {
        if(timeslot == P25P1Message.TIMESLOT_2)
        {
            return mTS2ChannelGrantEventMap.get(frequency);
        }
        else
        {
            return mTS1ChannelGrantEventMap.get(frequency);
        }
    }

    /**
     * Adds the tracker to the correct channel grant map.
     * @param tracker to add
     * @param frequency for the map lookup
     * @param timeslot to identify the correct map.
     */
    private void addTracker(P25TrafficChannelEventTracker tracker, long frequency, int timeslot)
    {
        if(timeslot == P25P1Message.TIMESLOT_2)
        {
            mTS2ChannelGrantEventMap.put(frequency, tracker);
        }
        else
        {
            mTS1ChannelGrantEventMap.put(frequency, tracker);
        }
    }

    /**
     * Removes the tracker from the correct channel grant map.
     * @param frequency for the map lookup
     * @param timeslot to identify the correct map.
     */
    private void removeTracker(long frequency, int timeslot)
    {
        if(timeslot == P25P1Message.TIMESLOT_2)
        {
            mTS2ChannelGrantEventMap.remove(frequency);
        }
        else
        {
            mTS1ChannelGrantEventMap.remove(frequency);
        }
    }

    /**
     * Processes a P25 Phase 2 channel update for any channel.  If the initial channel grant was not detected, invokes
     * the process channel grant method to auto-create the channel.
     *
     * For the update message, we normally only get the TO talkgroup value, so we'll do a comparison of the event using
     * just the TO identifier.
     *
     * @param channel where the activity is taking place.
     * @param serviceOptions for the call
     * @param ic identifier collection
     * @param macOpcode for the update message
     * @param timestamp of the message
     */
    public void processP2ChannelUpdate(APCO25Channel channel, ServiceOptions serviceOptions,
                                       IdentifierCollection ic, MacOpcode macOpcode, long timestamp, String context)
    {
        if(channel.getDownlinkFrequency() > 0)
        {
            mLock.lock();

            try
            {
                boolean processing = mAllocatedTrafficChannelMap.containsKey(channel.getDownlinkFrequency());

                if(!processing)
                {
                    processP2ChannelGrant(channel, serviceOptions, ic, macOpcode, timestamp, context);
                }
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * Closes the call event for the specified channel frequency and timeslot.
     * @param frequency for the channel
     * @param timeslot for the channel.
     * @param timestamp for the final duration update for the event
     * @return true if the current tracked call event was ended
     */
    public boolean processP2TrafficCallEnd(long frequency, int timeslot, long timestamp, String context)
    {
        boolean completed = false;

        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTracker(frequency, timeslot);

            //If we have a tracker that we can mark complete, broadcast the updated tracker/event.
            if(tracker != null && tracker.completeTraffic(timestamp))
            {
                completed = true;
                broadcast(tracker);
            }
        }
        finally
        {
            mLock.unlock();
        }

        return completed;
    }

    /**
     * Processes a traffic channel end push to talk event.  The PTT event is handled separately from the call end
     * method because there can be a timing issue between when the control channel issues the channel grant event and
     * when the traffic channel closes out traffic on the same channel, which can cause an event to be closed
     * prematurely.  This method checks to see if the call has been started before closing it out.
     * @param frequency for the channel
     * @param timeslot for the channel.
     * @param timestamp for the final duration update for the event
     * @return true if the current tracked call event was ended
     */
    public boolean processP2TrafficEndPushToTalk(long frequency, int timeslot, long timestamp, String context)
    {
        boolean completed = false;

        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTracker(frequency, timeslot);

            //If we have a tracker that is started that we can mark complete, broadcast the updated tracker/event.
            if(tracker != null && tracker.isStarted() && tracker.completeTraffic(timestamp))
            {
                completed = true;
                broadcast(tracker);
            }
        }
        finally
        {
            mLock.unlock();
        }

        return completed;
    }

    /**
     * Updates an identifier and call duration for an ongoing call event on the frequency and timeslot.
     *
     * Note: if this manager does not have an existing call event for the frequency and timeslot, the update is ignored
     * because we don't have enough detail to create a call event.
     *
     * This is used primarily to add encryption, GPS, talker alias, etc. but can be used for any identifier update
     * where the update infers that a call event is ongoing.
     *
     * @param frequency for the call event
     * @param timeslot for the call event
     * @param identifier to update within the event.
     * @param timestamp for the update
     */
    public void processP2TrafficCurrentUser(long frequency, int timeslot, Identifier identifier, long timestamp)
    {
        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, timeslot, timestamp);

            if(tracker != null && tracker.isComplete())
            {
                removeTracker(frequency, timeslot);
                tracker = null;
            }

            if(tracker != null)
            {
                tracker.addIdentifierIfMissing(identifier);
                tracker.updateDurationTraffic(timestamp);
                broadcast(tracker);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Process a TDMA data channel grant.
     * @param channel for data.
     * @param timestamp of the event
     */
    public void processP2DataChannel(APCO25Channel channel, long timestamp)
    {
        long frequency = channel != null ? channel.getDownlinkFrequency() : 0;

        if(frequency > 0)
        {
            mLock.lock();

            try
            {
                P25TrafficChannelEventTracker trackerTS1 = getTrackerRemoveIfStale(channel.getDownlinkFrequency(),
                        P25P1Message.TIMESLOT_1, timestamp);

                if(trackerTS1 != null && trackerTS1.exceedsMaxTDMADataDuration())
                {
                    removeTracker(frequency, P25P1Message.TIMESLOT_1);
                    trackerTS1 = null;
                }

                if(trackerTS1 == null)
                {
                    P25ChannelGrantEvent continuationGrantEvent = P25ChannelGrantEvent.builder(DecodeEventType.DATA_CALL,
                                    timestamp, new DataServiceOptions(0))
                            .channelDescriptor(channel)
                            .details("TDMA PHASE 2 DATA CHANNEL ACTIVE")
                            .identifiers(new IdentifierCollection())
                            .timeslot(P25P1Message.TIMESLOT_1)
                            .build();

                    trackerTS1 = new P25TrafficChannelEventTracker(continuationGrantEvent);
                    addTracker(trackerTS1, frequency, P25P1Message.TIMESLOT_1);
                }

                //update the ending timestamp so that the duration value is correctly calculated
                trackerTS1.updateDurationTraffic(timestamp);
                broadcast(trackerTS1);

                //Even though we have a tracked event, the initial channel grant may have been rejected.  Check to
                // see if there is a traffic channel allocated.  If not, allocate one and update the event description.
                if(!mAllocatedTrafficChannelMap.containsKey(frequency) && !mIgnoreDataCalls &&
                        (getCurrentControlFrequency() != frequency))
                {
                    Channel trafficChannel = mAvailablePhase2TrafficChannelQueue.poll();

                    if(trafficChannel != null)
                    {
                        requestTrafficChannelStart(trafficChannel, channel, new IdentifierCollection(), timestamp);
                    }
                    else
                    {
                        trackerTS1.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED);
                    }
                }

                P25TrafficChannelEventTracker trackerTS2 = getTrackerRemoveIfStale(channel.getDownlinkFrequency(),
                        P25P1Message.TIMESLOT_2, timestamp);

                if(trackerTS2 != null && trackerTS2.exceedsMaxTDMADataDuration())
                {
                    removeTracker(frequency, P25P1Message.TIMESLOT_2);
                    trackerTS2 = null;
                }

                if(trackerTS2 == null)
                {
                    P25ChannelGrantEvent continuationGrantEvent = P25ChannelGrantEvent.builder(DecodeEventType.DATA_CALL,
                                    timestamp, new DataServiceOptions(0))
                            .channelDescriptor(channel)
                            .details("TDMA PHASE 2 DATA CHANNEL ACTIVE")
                            .identifiers(new IdentifierCollection())
                            .timeslot(P25P1Message.TIMESLOT_2)
                            .build();

                    trackerTS2 = new P25TrafficChannelEventTracker(continuationGrantEvent);
                    addTracker(trackerTS2, frequency, P25P1Message.TIMESLOT_2);
                }

                //update the ending timestamp so that the duration value is correctly calculated
                trackerTS2.updateDurationTraffic(timestamp);
                broadcast(trackerTS1);
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * Starts a tracked event and updates the duration for a tracked event.
     *
     * @param frequency for the call event
     * @param timeslot for the call event
     * @param timestamp for the update
     */
    public void processP2TrafficVoice(long frequency, int timeslot, long timestamp)
    {
        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, timeslot, timestamp);

            if(tracker != null && tracker.isComplete())
            {
                removeTracker(frequency, timeslot);
                tracker = null;
            }

            if(tracker != null)
            {
                tracker.updateDurationTraffic(timestamp);
                broadcast(tracker);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes mac messaging that indicates a call on the current channel.  This update does not update the call
     * duration for the event but will create an event if one does not exist.  Updates to the call duration can only
     * occur
     * @param frequency of the current channel
     * @param timeslot of the current channel
     * @param serviceOptions for the call
     * @param ic for the call
     * @param timestamp for the message that is being processed
     * @return
     */
    public IChannelDescriptor processP2TrafficCurrentUser(long frequency, int timeslot, IChannelDescriptor channelDescriptor,
                                                          ServiceOptions serviceOptions, MacOpcode macOpcode,
                                                          IdentifierCollection ic, long timestamp, String additionalDetails, String context)
    {
        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, timeslot, timestamp);

            if(tracker != null && tracker.isComplete())
            {
                removeTracker(frequency, timeslot);
                tracker = null;
            }

            if(tracker != null && tracker.isSameCallCheckingToAndFrom(ic, timestamp))
            {
                for(Identifier identifier: ic.getIdentifiers())
                {
                    tracker.addIdentifierIfMissing(identifier);
                }

                tracker.addDetailsIfMissing(additionalDetails);
                tracker.addChannelDescriptorIfMissing(channelDescriptor);
                broadcast(tracker);
                return tracker.getEvent().getChannelDescriptor();
            }

            //Create a new event for the current call.
            DecodeEventType eventType = getEventType(macOpcode, serviceOptions, null);
            P25ChannelGrantEvent callEvent = P25ChannelGrantEvent.builder(eventType, timestamp, serviceOptions)
                    .channelDescriptor(channelDescriptor)
                    .details(additionalDetails != null ? additionalDetails : "PHASE 2 CALL " +
                            (serviceOptions != null ? serviceOptions : ""))
                    .identifiers(ic)
                    .build();

            tracker = new P25TrafficChannelEventTracker(callEvent);
            addTracker(tracker, frequency, timeslot);
            broadcast(tracker);
            return null;
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes phase 2 channel grants to allocate traffic channels and track overall channel usage.  Generates and
     * tracks decode events for each new channel that is allocated.
     *
     * @param apco25Channel for the traffic channel
     * @param serviceOptions for the traffic channel - optional can be null
     * @param ic associated with the channel grant
     * @param macOpcode to identify the call type for the event description
     */
    public void processP2ChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                      IdentifierCollection ic, MacOpcode macOpcode, long timestamp, String context)
    {
        mLock.lock();

        try
        {
            DecodeEventType decodeEventType = getEventType(macOpcode, serviceOptions, null);
            boolean isDataChannelGrant = macOpcode.isDataChannelGrant();

            if(apco25Channel.isTDMAChannel())
            {
                if(apco25Channel.getTimeslotCount() == 2)
                {
                    //Data channels may be granted as a phase 2 channel grant but are still phase 1 channels
                    if(macOpcode.isDataChannelGrant())
                    {
                        APCO25Channel phase1Channel = convertPhase2ToPhase1(apco25Channel);
                        processPhase1ControlChannelGrant(phase1Channel, serviceOptions, ic, decodeEventType,
                                isDataChannelGrant, timestamp, context);
                    }
                    else
                    {
                        processPhase2ChannelGrant(apco25Channel, serviceOptions, ic, decodeEventType,
                                isDataChannelGrant, timestamp);
                    }
                }
                else
                {
                    mLog.warn("Cannot process TDMA channel grant - unrecognized timeslot count: " +
                            apco25Channel.getTimeslotCount());
                }
            }
            else
            {
                processPhase1ControlChannelGrant(apco25Channel, serviceOptions, ic, decodeEventType, isDataChannelGrant,
                        timestamp, context);
            }

        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes phase 1 channel grants directed by the control channel to allocate traffic channels and track overall
     * channel usage.  Generates and tracks decode events for each new channel that is allocated.
     *
     * @param apco25Channel for the granted traffic channel
     * @param serviceOptions for the granted traffic channel - optional can be null
     * @param ic associated with the channel grant
     * @param opcode to identify the call type for the event description
     */
    public void processP1ControlDirectedChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                                     IdentifierCollection ic, Opcode opcode, long timestamp, String context)
    {
        mLock.lock();

        try
        {
            DecodeEventType decodeEventType = getEventType(opcode, serviceOptions, null);
            boolean isDataChannelGrant = opcode != null && opcode.isDataChannelGrant();

            if(apco25Channel.isTDMAChannel())
            {
                processPhase2ChannelGrant(apco25Channel, serviceOptions, ic, decodeEventType,
                        isDataChannelGrant, timestamp);
            }
            else
            {
                processPhase1ControlChannelGrant(apco25Channel, serviceOptions, ic, decodeEventType,
                        isDataChannelGrant, timestamp, context);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Process a Phase 1 HDU indicating a call start.
     *
     * @param frequency for the call event
     * @param talkgroup to update within the event.
     * @param eki for encryption settings.
     * @param timestamp for the update
     */
    public void processP1TrafficCallStart(long frequency, Identifier<?> talkgroup, Identifier<?> radio,
                                          EncryptionKeyIdentifier eki, ServiceOptions serviceOptions,
                                          IChannelDescriptor channelDescriptor, long timestamp)
    {
        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTracker(frequency, P25P1Message.TIMESLOT_1);

            //If the tracker is already started, it was for another call.  Close it and recreate the event.
            if(tracker != null && tracker.isStarted())
            {
                removeTracker(frequency, P25P1Message.TIMESLOT_1);
                tracker = null;
            }

            if(tracker != null)
            {
                tracker.addIdentifierIfMissing(talkgroup);
            }
            else
            {
                DecodeEventType decodeEventType = getDecodeEventType(talkgroup, eki);
                MutableIdentifierCollection mic = new MutableIdentifierCollection();
                mic.update(talkgroup);
                mic.update(radio);
                mic.update(eki);

                //Create a new event for the current call.
                P25ChannelGrantEvent callEvent = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                        .channelDescriptor(channelDescriptor)
                        .details("PHASE 1 CALL " + (serviceOptions != null ? serviceOptions : ""))
                        .identifiers(mic)
                        .build();

                tracker = new P25TrafficChannelEventTracker(callEvent);
                addTracker(tracker, frequency, P25P1Message.TIMESLOT_1);
            }

            broadcast(tracker);
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Determines the HDU message call decode event type from the talkgroup identifier and encryption key
     * @param talkgroup to inspect
     * @param eki to inspect
     * @return decode event type.
     */
    private static DecodeEventType getDecodeEventType(Identifier talkgroup, EncryptionKeyIdentifier eki)
    {
        DecodeEventType decodeEventType = null;

        if(talkgroup instanceof PatchGroupIdentifier)
        {
            decodeEventType = eki.isEncrypted() ? DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED : DecodeEventType.CALL_PATCH_GROUP;
        }
        else if(talkgroup instanceof TalkgroupIdentifier ti && ti.getValue() == 0) //Unit-to-Unit private call
        {
            decodeEventType = eki.isEncrypted() ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
        }
        else
        {
            decodeEventType = eki.isEncrypted() ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
        }
        return decodeEventType;
    }

    /**
     * Updates an identifier for an ongoing call event on the frequency and updates the event duration timestamp.
     *
     * Note: if this manager does not have an existing call event for the frequency, the update is ignored
     * because we don't have enough detail to create a call event.
     *
     * This is used primarily to add encryption, GPS, talker alias, etc. but can be used for any identifier update.
     *
     * @param frequency for the call event
     * @param identifier to update within the event.
     * @param timestamp for the update
     */
    public void processP1TrafficCurrentUser(long frequency, Identifier identifier, long timestamp, String context)
    {
        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTracker(frequency, P25P1Message.TIMESLOT_1);

            if(tracker != null && tracker.isComplete())
            {
                removeTracker(frequency, P25P1Message.TIMESLOT_1);
                tracker = null;
            }

            if(tracker != null)
            {
                tracker.addIdentifierIfMissing(identifier);

                //Add the encryption key to the call event details.
                if(identifier instanceof EncryptionKeyIdentifier eki && eki.isEncrypted())
                {
                    tracker.addDetailsIfMissing(eki.toString());
                }

                tracker.updateDurationTraffic(timestamp);
                broadcast(tracker);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Updates all current identifiers for an ongoing call event on the frequency and updates the event duration timestamp.
     *
     * Note: if this manager does not have an existing call event for the frequency, the update is ignored
     * because we don't have enough detail to create a call event.
     *
     * This is used primarily to add encryption, GPS, talker alias, etc. but can be used for any identifier update.
     *
     * @param frequency for the call event
     * @param identifiers to update within the event.
     * @param timestamp for the update
     */
    public void processP1TrafficLDU1(long frequency, List<Identifier> identifiers, long timestamp, String context)
    {
        mLock.lock();

        try
        {
            IChannelDescriptor channelDescriptor = null;

            P25TrafficChannelEventTracker tracker = getTracker(frequency, P25P1Message.TIMESLOT_1);

            if(tracker != null && tracker.isComplete())
            {
                channelDescriptor = tracker.getEvent().getChannelDescriptor();;
                removeTracker(frequency, P25P1Message.TIMESLOT_1);
                tracker = null;
            }

            if(tracker != null)
            {
                for(Identifier identifier : identifiers)
                {
                    tracker.addIdentifierIfMissing(identifier);

                    //Add the encryption key to the call event details.
                    if(identifier instanceof EncryptionKeyIdentifier eki && eki.isEncrypted())
                    {
                        tracker.addDetailsIfMissing(eki.toString());
                    }
                }

                tracker.updateDurationTraffic(timestamp);
                broadcast(tracker);
            }
            else
            {
                MutableIdentifierCollection mic = new MutableIdentifierCollection(identifiers);
                Identifier talkgroup = mic.getToIdentifier();
                Identifier encryption = mic.getEncryptionIdentifier();

                if(talkgroup != null && encryption instanceof EncryptionKeyIdentifier eki)
                {
                    DecodeEventType decodeEventType = getDecodeEventType(talkgroup, eki);
                    //Create a new event for the current call.
                    ServiceOptions serviceOptions = (eki.isEncrypted() ? VoiceServiceOptions.createEncrypted() :
                            VoiceServiceOptions.createUnencrypted());
                    P25ChannelGrantEvent callEvent = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                            .channelDescriptor(channelDescriptor)
                            .details("PHASE 1 CALL " + (eki.isEncrypted() ? eki.toString() : ""))
                            .identifiers(mic)
                            .build();

                    tracker = new P25TrafficChannelEventTracker(callEvent);
                    addTracker(tracker, frequency, P25P1Message.TIMESLOT_1);
                    broadcast(tracker);
                }
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes traffic channel announced current user information.
     * @param frequency of the traffic channel
     * @param channelDescriptor for the traffic channel
     * @param decodeEventType to use if the call event is not currently tracked
     * @param serviceOptions for the call
     * @param ic identifiers for the call
     * @param timestamp for the message that is being processed
     * @return channel descriptor for the event or null
     */
    public void processP1TrafficCurrentUser(long frequency, IChannelDescriptor channelDescriptor,
                                            DecodeEventType decodeEventType, ServiceOptions serviceOptions,
                                            IdentifierCollection ic, long timestamp, String additionalDetails, String context)
    {
        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTracker(frequency, P25P1Message.TIMESLOT_1);

            if(tracker != null && tracker.isComplete())
            {
                removeTracker(frequency, P25P1Message.TIMESLOT_1);
                tracker = null;
            }

            if(tracker != null && tracker.isSameCallCheckingToAndFrom(ic, timestamp))
            {
                for(Identifier identifier: ic.getIdentifiers())
                {
                    tracker.addIdentifierIfMissing(identifier);
                }

                tracker.updateDurationTraffic(timestamp);
                tracker.addDetailsIfMissing(additionalDetails);
                broadcast(tracker);
                return;
            }

            //Create a new event for the current call.
            P25ChannelGrantEvent callEvent = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                    .channelDescriptor(channelDescriptor)
                    .details(additionalDetails != null ? additionalDetails : "PHASE 1 CALL " +
                            (serviceOptions != null ? serviceOptions : ""))
                    .identifiers(ic)
                    .build();

            tracker = new P25TrafficChannelEventTracker(callEvent);
            addTracker(tracker, frequency, P25P1Message.TIMESLOT_1);
            broadcast(tracker);
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes a P25 Phase 1 control channel announced update to an allocated traffic channel.  If the initial traffic
     * channel grant was not detected, invokes the process channel grant method to auto-create the channel.
     *
     * From the update message we only get the TO talkgroup value, so we'll do a tracked event comparison using only
     * the TO identifier to determine if the update refers to the currently tracked event or if the update is for a
     * new and different event.
     *
     * @param channel where the activity is taking place.
     * @param serviceOptions for the call
     * @param ic identifier collection
     * @param opcode for the update message
     * @param timestamp of the message
     */
    public void processP1ControlAnnouncedTrafficUpdate(APCO25Channel channel, ServiceOptions serviceOptions,
                                                       IdentifierCollection ic, Opcode opcode, long timestamp, String context)
    {
        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTrackerRemoveIfStale(channel, timestamp);

            //If we have a tracked event, update it.  Otherwise, make sure we have the traffic channel allocated
            if(tracker != null && tracker.isSameCallCheckingToOnly(ic, timestamp))
            {
                //Only rebroadcast the tracked event if the timestamp was updated from this control channel timestamp
                //Once the traffic channel takes over updating the tracked event time/duration, further control channel
                // updates are ignored.
                if(tracker.updateDurationControl(timestamp))
                {
                    broadcast(tracker);
                }
            }
            else
            {
                //Remove the existing call tracker because it's a different call now
                removeTracker(channel.getDownlinkFrequency(), channel.getTimeslot());
                processP1ControlDirectedChannelGrant(channel, serviceOptions, ic, opcode, timestamp, context);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Marks the tracked traffic channel event as complete but does not remove the event from the tracker map so
     * that we can continue to track the event for the traffic channel.
     * @param frequency for the channel
     * @return true if the tracked call event was marked as complete.
     */
    public boolean processP1TrafficCallEnd(long frequency, long timestamp, String context)
    {
        boolean completed = false;

        mLock.lock();

        try
        {
            P25TrafficChannelEventTracker tracker = getTracker(frequency, P25P1Message.TIMESLOT_1);

            //If we have a tracker that we can mark complete, broadcast the updated tracker/event.
            if(tracker != null && tracker.isStarted() && tracker.completeTraffic(timestamp))
            {
                completed = true;
                broadcast(tracker);
            }
        }
        finally
        {
            mLock.unlock();
        }

        return completed;
    }

    /**
     * Sends a channel start request to the ChannelProcessingManager.
     *
     * Note: this method is not thread safe and the calling method must protect access using mLock.
     *
     * @param trafficChannel to use for the traffic channel
     * @param apco25Channel that describes the traffic channel downlink frequency
     * @param identifierCollection containing identifiers for the call
     * @param timestamp of the request event
     */
    private void requestTrafficChannelStart(Channel trafficChannel, APCO25Channel apco25Channel,
                                            IdentifierCollection identifierCollection, long timestamp)
    {
        if(apco25Channel != null && apco25Channel.getDownlinkFrequency() > 0 && getInterModuleEventBus() != null)
        {
            SourceConfigTuner sourceConfig = new SourceConfigTuner();
            sourceConfig.setFrequency(apco25Channel.getDownlinkFrequency());
            if(mParentChannel.getSourceConfiguration() instanceof SourceConfigTuner parentConfigTuner)
            {
                sourceConfig.setPreferredTuner(parentConfigTuner.getPreferredTuner());
            }
            trafficChannel.setSourceConfiguration(sourceConfig);

            //If this is a phase2 request and we have scramble/randomizer parameters, set them
            if(mPhase2ScrambleParameters != null && trafficChannel.getDecodeConfiguration() instanceof DecodeConfigP25Phase2 p2)
            {
                p2.setScrambleParameters(mPhase2ScrambleParameters.copy());
            }

            mAllocatedTrafficChannelMap.put(apco25Channel.getDownlinkFrequency(), trafficChannel);

            ChannelStartProcessingRequest startChannelRequest = new ChannelStartProcessingRequest(trafficChannel,
                    apco25Channel, identifierCollection, this);
            startChannelRequest.addPreloadDataContent(new PatchGroupPreLoadDataContent(identifierCollection, timestamp));
            startChannelRequest.addPreloadDataContent(new P25FrequencyBandPreloadDataContent(mFrequencyBandMap.values()));
            getInterModuleEventBus().post(startChannelRequest);
        }
        else
        {
            //Return the channel to the traffic channel pool since we didn't start it.
            if(mManagedPhase1TrafficChannels.contains(trafficChannel))
            {
                mAvailablePhase1TrafficChannelQueue.add(trafficChannel);
            }
            else if(mManagedPhase2TrafficChannels.contains(trafficChannel))
            {
                mAvailablePhase2TrafficChannelQueue.add(trafficChannel);
            }
        }
    }

    /**
     * Processes Phase 1 control-only channel grants to allocate traffic channels and track overall channel usage.
     * Generates a tracked decode event for each new channel that is allocated.
     *
     * Note: this method is not thread safe and the calling method must protect access using mLock.
     *
     * @param apco25Channel for the traffic channel
     * @param serviceOptions for the traffic channel - optional can be null
     * @param ic associated with the channel grant
     * @param decodeEventType to use
     * @param isDataChannelGrant indicator if this is a data channel grant
     * @param timestamp for the grant event.
     */
    private void processPhase1ControlChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                                  IdentifierCollection ic, DecodeEventType decodeEventType,
                                                  boolean isDataChannelGrant, long timestamp, String context)
    {
        long frequency = apco25Channel.getDownlinkFrequency();

        P25TrafficChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, P25P1Message.TIMESLOT_1, timestamp);

        if(tracker != null && tracker.isSameCallCheckingToOnly(ic, timestamp))
        {
            Identifier from = ic.getFromIdentifier();

            if(from != null && tracker.isDifferentTalker(from))
            {
                P25ChannelGrantEvent event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                    .channelDescriptor(apco25Channel)
                    .details("CONTINUE - PHASE 1 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""))
                    .identifiers(ic)
                    .build();

                tracker = new P25TrafficChannelEventTracker(event);
                addTracker(tracker, frequency, P25P1Message.TIMESLOT_1);
            }

            //The tracked event can have an empty FROM identifier at start of call ... update here
            tracker.addIdentifierIfMissing(from);

            //update the ending timestamp so that the duration value is correctly calculated
            tracker.updateDurationControl(timestamp);

            broadcast(tracker);

            //Even though we have a tracked event, the initial channel grant may have been rejected.  Check to see if there
            //is a traffic channel allocated.  If not, allocate one and update the event description.
            if(!mAllocatedTrafficChannelMap.containsKey(frequency) && !(mIgnoreDataCalls && isDataChannelGrant))
            {
                Channel trafficChannel = mAvailablePhase1TrafficChannelQueue.poll();

                if(trafficChannel != null)
                {
                    if(isDataChannelGrant)
                    {
                        tracker.setDetails("PHASE 1 DATA CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""));
                    }
                    else
                    {
                        tracker.setDetails("PHASE 1 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""));
                    }
                    tracker.addChannelDescriptorIfMissing(apco25Channel);
                    broadcast(tracker);

                    requestTrafficChannelStart(trafficChannel, apco25Channel, ic, timestamp);
                }
                else
                {
                    tracker.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED);
                }
            }

            return;
        }

        if(mIgnoreDataCalls && isDataChannelGrant)
        {
            if(tracker == null)
            {
                P25ChannelGrantEvent event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                        .channelDescriptor(apco25Channel)
                        .details("IGNORED: PHASE 1 DATA CALL " + (serviceOptions != null ? serviceOptions : ""))
                        .identifiers(ic)
                        .build();
                tracker = new P25TrafficChannelEventTracker(event);
                addTracker(tracker, frequency, P25P1Message.TIMESLOT_1);
                broadcast(tracker);
            }

            return;
        }

        String details = isDataChannelGrant ? "PHASE 1 DATA CHANNEL GRANT " : "PHASE 1 CHANNEL GRANT " +
                (serviceOptions != null ? serviceOptions : "");

        P25ChannelGrantEvent event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
            .channelDescriptor(apco25Channel)
            .details(details)
            .identifiers(ic)
            .build();
        tracker = new P25TrafficChannelEventTracker(event);
        addTracker(tracker, frequency, P25P1Message.TIMESLOT_1);

        //Allocate a traffic channel for the downlink frequency if one isn't already allocated
        if(!mAllocatedTrafficChannelMap.containsKey(frequency))
        {
            Channel trafficChannel = mAvailablePhase1TrafficChannelQueue.poll();

            if(trafficChannel == null)
            {
                tracker.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " - " + (event.getDetails() != null ? event.getDetails() : ""));
                return;
            }

            requestTrafficChannelStart(trafficChannel, apco25Channel, ic, timestamp);
        }

        broadcast(tracker);
    }

    /**
     * Processes Phase 2 channel grants from both the control channel and from traffic channels to allocate traffic
     * channels and track overall channel usage.  Generates decode events for each new channel that is allocated.
     *
     * Note: this method is not thread safe and the calling method must protect access using mLock.
     *
     * @param apco25Channel for the traffic channel
     * @param serviceOptions for the traffic channel - optional can be null
     * @param ic associated with the channel grant
     * @param decodeEventType to use for the event.
     * @param isDataChannelGrant indicator if this is a data channel grant
     * @param timestamp for the event
     */
    private void processPhase2ChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                           IdentifierCollection ic, DecodeEventType decodeEventType,
                                           boolean isDataChannelGrant, long timestamp)
    {
        if(mPhase2ScrambleParameters != null && ic instanceof MutableIdentifierCollection)
        {
            ((MutableIdentifierCollection)ic).silentUpdate(ScrambleParameterIdentifier.create(mPhase2ScrambleParameters));
        }

        int timeslot = apco25Channel.getTimeslot();
        long frequency = apco25Channel.getDownlinkFrequency();
        ic.setTimeslot(timeslot);

        P25TrafficChannelEventTracker tracker = getTrackerRemoveIfStale(apco25Channel, timestamp);

        if(tracker != null && tracker.isSameCallCheckingToOnly(ic, timestamp))
        {
            Identifier from = ic.getFromIdentifier();

            if(from != null && tracker.isDifferentTalker(from))
            {
                P25ChannelGrantEvent continuationGrantEvent = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                    .channelDescriptor(apco25Channel)
                    .details("CONTINUE - PHASE 2 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""))
                    .identifiers(ic)
                    .timeslot(apco25Channel.getTimeslot())
                    .build();

                tracker = new P25TrafficChannelEventTracker(continuationGrantEvent);
                addTracker(tracker, frequency, timeslot);
                broadcast(tracker);
            }

            //update the ending timestamp so that the duration value is correctly calculated
            tracker.updateDurationControl(timestamp);
            broadcast(tracker);

            //Even though we have a tracked event, the initial channel grant may have been rejected.  Check to see if there
            //is a traffic channel allocated.  If not, allocate one and update the event description.
            if(!mAllocatedTrafficChannelMap.containsKey(frequency) && !(mIgnoreDataCalls && isDataChannelGrant) &&
                (getCurrentControlFrequency() != frequency))
            {
                Channel trafficChannel = mAvailablePhase2TrafficChannelQueue.poll();

                if(trafficChannel != null)
                {
                    tracker.setDetails("PHASE 2 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""));
                    tracker.addChannelDescriptorIfMissing(apco25Channel);
                    broadcast(tracker);
                    requestTrafficChannelStart(trafficChannel, apco25Channel, ic, timestamp);
                }
                else
                {
                    tracker.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED);
                }
            }

            return;
        }

        if(mIgnoreDataCalls && isDataChannelGrant && tracker == null)
        {
            P25ChannelGrantEvent event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                .channelDescriptor(apco25Channel)
                .details("PHASE 2 DATA CALL IGNORED: " + (serviceOptions != null ? serviceOptions : ""))
                .identifiers(ic)
                .timeslot(apco25Channel.getTimeslot())
                .build();

            tracker = new P25TrafficChannelEventTracker(event);
            addTracker(tracker, frequency, P25P1Message.TIMESLOT_1);
            broadcast(tracker);
            return;
        }

        P25ChannelGrantEvent event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
            .channelDescriptor(apco25Channel)
            .details("PHASE 2 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""))
            .identifiers(ic)
            .timeslot(apco25Channel.getTimeslot())
            .build();

        tracker = new P25TrafficChannelEventTracker(event);
        addTracker(tracker, frequency, timeslot);

        //Allocate a traffic channel for the downlink frequency if one isn't already allocated
        if(!mAllocatedTrafficChannelMap.containsKey(frequency) && frequency != getCurrentControlFrequency())
        {
            Channel trafficChannel = mAvailablePhase2TrafficChannelQueue.poll();

            if(trafficChannel == null)
            {
                tracker.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " - IGNORED");
            }
            else
            {
                requestTrafficChannelStart(trafficChannel, apco25Channel, ic, timestamp);
            }
        }

        broadcast(tracker);
    }

    /**
     * Creates a Phase 2 call event type description for the specified opcode and service options
     * @param macOpcode to evaluate
     * @param serviceOptions for the call
     * @param current decode event type (optional null).
     * @return event type for the mac opcode.
     */
    private DecodeEventType getEventType(MacOpcode macOpcode, ServiceOptions serviceOptions, DecodeEventType current)
    {
        boolean encrypted = serviceOptions != null && serviceOptions.isEncrypted();

        DecodeEventType type = null;

        switch(macOpcode)
        {
            case PUSH_TO_TALK:
                if(current != null)
                {
                    type = current;
                }
                else
                {
                    type = encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
                }
                break;
            case TDMA_01_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
            case TDMA_05_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_IMPLICIT:
            case TDMA_21_GROUP_VOICE_CHANNEL_USER_EXTENDED:
            case TDMA_25_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
            case PHASE1_40_GROUP_VOICE_CHANNEL_GRANT_IMPLICIT:
            case PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
            case PHASE1_C0_GROUP_VOICE_CHANNEL_GRANT_EXPLICIT:
            case PHASE1_C3_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                type = encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
                break;

            case MOTOROLA_80_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
            case MOTOROLA_83_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
            case PHASE1_90_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
            case MOTOROLA_A0_GROUP_REGROUP_VOICE_CHANNEL_USER_EXTENDED:
            case MOTOROLA_A3_GROUP_REGROUP_CHANNEL_GRANT_IMPLICIT:
            case MOTOROLA_A4_GROUP_REGROUP_CHANNEL_GRANT_EXPLICIT:
            case MOTOROLA_A5_GROUP_REGROUP_CHANNEL_GRANT_UPDATE:
            case L3HARRIS_B0_GROUP_REGROUP_EXPLICIT_ENCRYPTION_COMMAND:
                type = encrypted ? DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED : DecodeEventType.CALL_PATCH_GROUP;
                break;

            case TDMA_02_UNIT_TO_UNIT_VOICE_CHANNEL_USER_ABBREVIATED:
            case TDMA_22_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
            case PHASE1_44_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_ABBREVIATED:
            case PHASE1_46_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
            case PHASE1_48_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_IMPLICIT:
            case PHASE1_C4_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_VCH:
            case PHASE1_C6_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_VCH:
            case PHASE1_CF_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_LCCH:
                type = encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
                break;

            case TDMA_03_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
            case PHASE1_C8_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_EXPLICIT:
                type = encrypted ? DecodeEventType.CALL_INTERCONNECT_ENCRYPTED : DecodeEventType.CALL_INTERCONNECT;
                break;

            case PHASE1_54_SNDCP_DATA_CHANNEL_GRANT:
            case L3HARRIS_A0_PRIVATE_DATA_CHANNEL_GRANT:
            case L3HARRIS_AC_UNIT_TO_UNIT_DATA_CHANNEL_GRANT:
                type = encrypted ? DecodeEventType.DATA_CALL_ENCRYPTED : DecodeEventType.DATA_CALL;
                break;
        }

        if(type == null)
        {
            LOGGING_SUPPRESSOR.error(macOpcode.name(), 2, "Unrecognized MAC opcode for determining " +
                    "decode event type: " + macOpcode.name());
            type = current;
        }

        if(type == null)
        {
            type = DecodeEventType.CALL;
        }

        return type;
    }

    /**
     * Creates a call event type description for the specified opcode and service options
     */
    private DecodeEventType getEventType(Opcode opcode, ServiceOptions serviceOptions, DecodeEventType current)
    {
        boolean encrypted = serviceOptions != null && serviceOptions.isEncrypted();

        DecodeEventType type = null;

        if(opcode != null)
        {
            type = switch(opcode)
            {
                case OSP_GROUP_VOICE_CHANNEL_GRANT, OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE,
                     OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT ->
                        encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT, OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE ->
                        encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT,
                     OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE ->
                        encrypted ? DecodeEventType.CALL_INTERCONNECT_ENCRYPTED : DecodeEventType.CALL_INTERCONNECT;
                case OSP_SNDCP_DATA_CHANNEL_GRANT, OSP_GROUP_DATA_CHANNEL_GRANT, OSP_INDIVIDUAL_DATA_CHANNEL_GRANT ->
                        encrypted ? DecodeEventType.DATA_CALL_ENCRYPTED : DecodeEventType.DATA_CALL;
                case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT, MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_UPDATE ->
                        encrypted ? DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED : DecodeEventType.CALL_PATCH_GROUP;
                default -> type;
            };
        }

        if(type == null)
        {
            type = current;

            if(opcode != null)
            {
                LOGGING_SUPPRESSOR.error(opcode.name(), 2, "Unrecognized opcode for determining decode " +
                        "event type: " + opcode.name());
            }
        }

        if(type == null)
        {
            type = encrypted ? DecodeEventType.CALL_ENCRYPTED : DecodeEventType.CALL;
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
            mLog.info("Stopping traffic channel: " + channel);
            broadcast(new ChannelEvent(channel, Event.REQUEST_DISABLE));
        }

        mAvailablePhase1TrafficChannelQueue.clear();
        mAvailablePhase2TrafficChannelQueue.clear();
        mTS1ChannelGrantEventMap.clear();
        mTS2ChannelGrantEventMap.clear();
    }

    /**
     * Processes the decoded message stream and captures P25 Phase II randomizer (scramble) parameters from the TSBK
     * network status broadcast message so that we can pre-load any Phase2 channels with the correct descrambler
     * sequence.
     *
     * @return listener to process the message stream.
     */
    @Override
    public Listener<IMessage> getMessageListener()
    {
        if(mMessageListener == null)
        {
            mMessageListener = message -> {
                if(mPhase2ScrambleParameters == null && message.isValid())
                {
                    if(message instanceof NetworkStatusBroadcast nsb)
                    {
                        mPhase2ScrambleParameters = nsb.getScrambleParameters();
                    }
                    else if(message instanceof AMBTCNetworkStatusBroadcast nsb)
                    {
                        mPhase2ScrambleParameters = nsb.getScrambleParameters();
                    }
                }
            };
        }

        return mMessageListener;
    }

    /**
     * Converts a phase 2 channel to a phase 1 channel
     * @param channel to convert
     * @return channel converted to phase 1 or the original channel if no conversion is necessary
     */
    private static APCO25Channel convertPhase2ToPhase1(APCO25Channel channel)
    {
        P25Channel toConvert = channel.getValue();

        if(toConvert instanceof P25P2ExplicitChannel)
        {
            P25P2ExplicitChannel phase2 = (P25P2ExplicitChannel)toConvert;
            return APCO25ExplicitChannel.create(phase2.getDownlinkBandIdentifier(),
                phase2.getDownlinkChannelNumber(), phase2.getUplinkBandIdentifier(),
                phase2.getUplinkChannelNumber());
        }
        else if(toConvert instanceof P25P2Channel)
        {
            P25P2Channel phase2 = (P25P2Channel)toConvert;
            return APCO25Channel.create(phase2.getDownlinkBandIdentifier(), phase2.getDownlinkChannelNumber());
        }

        return channel;
    }

    /**
     * Monitors channel teardown events to detect when traffic channel processing has ended or channel start has been
     * rejected.  Reclaims the traffic channel instance for reuse by future traffic channel grants.
     */
    public class TrafficChannelTeardownMonitor implements Listener<ChannelEvent>
    {
        /**
         * Process channel events from the ChannelProcessingManager to account for owned child traffic channels.
         * Note: this method sees events for ALL channels and not just P25 channels managed by this instance.
         *
         * @param channelEvent to process
         */
        @Override
        public void receive(ChannelEvent channelEvent)
        {
            Channel channel = channelEvent.getChannel();

            if(mManagedPhase1TrafficChannels.contains(channel))
            {
                mLock.lock();

                try
                {
                    switch(channelEvent.getEvent())
                    {
                        case NOTIFICATION_PROCESSING_STOP:
                            mAllocatedTrafficChannelMap.entrySet()
                                    .stream()
                                    .filter(entry -> entry.getValue() == channel)
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .ifPresent(frequency -> {
                                        mAllocatedTrafficChannelMap.remove(frequency);
                                        removeTracker(frequency, P25P1Message.TIMESLOT_1);
                                        mAvailablePhase1TrafficChannelQueue.add(channel);
                                    });
                            break;
                        case NOTIFICATION_PROCESSING_START_REJECTED:
                            mAllocatedTrafficChannelMap.entrySet().stream()
                                    .filter(entry -> entry.getValue() == channel)
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .ifPresent(rejectedFrequency -> {
                                        mAllocatedTrafficChannelMap.remove(rejectedFrequency);
                                        mAvailablePhase1TrafficChannelQueue.add(channel);

                                        //Leave the event in the map so that it doesn't get recreated.  The channel
                                        //processing manager set the 'tuner not available' in the details already
                                        P25TrafficChannelEventTracker tracker = getTracker(rejectedFrequency, P25P1Message.TIMESLOT_1);

                                        if(tracker != null && !tracker.getEvent().getDetails().contains(CHANNEL_START_REJECTED))
                                        {
                                            tracker.setDetails(CHANNEL_START_REJECTED + " " + channelEvent.getDescription() +
                                                    (tracker.getEvent().getDetails() != null ? " - " + tracker.getEvent().getDetails() : ""));
                                        }
                                        broadcast(tracker);
                                    });
                            break;
                    }
                }
                finally
                {
                    mLock.unlock();
                }
            }
            else if(mManagedPhase2TrafficChannels.contains(channel))
            {
                mLock.lock();

                try
                {
                    switch(channelEvent.getEvent())
                    {
                        case NOTIFICATION_PROCESSING_STOP:
                            mAllocatedTrafficChannelMap.entrySet()
                                    .stream()
                                    .filter(entry -> entry.getValue() == channel)
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .ifPresent(frequency -> {
                                        mAllocatedTrafficChannelMap.remove(frequency);
                                        mAvailablePhase2TrafficChannelQueue.add(channel);
                                        //Remove tracker from both timeslots
                                        removeTracker(frequency, P25P1Message.TIMESLOT_1);
                                        removeTracker(frequency, P25P1Message.TIMESLOT_2);
                                    });
                            break;
                        case NOTIFICATION_PROCESSING_START_REJECTED:
                            mAllocatedTrafficChannelMap.entrySet().stream()
                                    .filter(entry -> entry.getValue() == channel)
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .ifPresent(rejectedFrequency -> {
                                        mAllocatedTrafficChannelMap.remove(rejectedFrequency);
                                        mAvailablePhase2TrafficChannelQueue.add(channel);

                                        //Leave the tracked event in the map so that it doesn't get recreated.  The channel
                                        //processing manager set the 'tuner not available' in the details already
                                        P25TrafficChannelEventTracker tracker = getTracker(rejectedFrequency, P25P1Message.TIMESLOT_1);
                                        if (tracker != null)
                                        {
                                            broadcast(tracker);
                                        }

                                        //Leave the tracked event in the map so that it doesn't get recreated.  The channel
                                        //processing manager set the 'tuner not available' in the details already
                                        P25TrafficChannelEventTracker tracker2 = getTracker(rejectedFrequency, P25P1Message.TIMESLOT_2);
                                        if (tracker2 != null)
                                        {
                                            broadcast(tracker2);
                                        }
                                    });
                            break;
                    }
                }
                finally
                {
                    mLock.unlock();
                }
            }
        }
    }
}
