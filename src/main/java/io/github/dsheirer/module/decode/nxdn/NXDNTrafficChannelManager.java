/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.alias.TalkerAliasManager;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventDuplicateDetector;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannel;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignmentDuplicateTraffic;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallTimer;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.DataCallOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Duplex;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.module.decode.nxdn.layer3.type.VoiceCallOption;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages trunked system traffic channel activations
 */
public class NXDNTrafficChannelManager extends TrafficChannelManager implements IDecodeEventProvider,
        IChannelEventListener, IChannelEventProvider, IMessageListener
{
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";

    private Lock mLock = new ReentrantLock();
    private Channel mParentChannel;
    private boolean mIgnoreDataCalls;
    private final Queue<Channel> mAvailableTrafficChannelQueue = new LinkedTransferQueue<>();
    private final Map<Long, Channel> mAllocatedTrafficChannelMap = new HashMap<>();
    private final Map<Long, NXDNChannelEventTracker> mChannelEventTrackerMap = new HashMap<>();
    private List<Channel> mManagedTrafficChannels;
    private ChannelAccessInformation mChannelAccessInformation;
    private final List<ChannelFrequency> mChannelFrequencies = new ArrayList<>();
    private final DecodeEventDuplicateDetector mDuplicateDetector = new DecodeEventDuplicateDetector();
    private final TalkerAliasManager mTalkerAliasManager = new TalkerAliasManager();
    private Listener<IDecodeEvent> mDecodeEventListener;
    private final TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();

    /**
     * Constructs an instance
     *
     * @param parentChannel configuration for the parent channel
     */
    public NXDNTrafficChannelManager(Channel parentChannel)
    {
        mParentChannel = parentChannel;

        if(parentChannel.getDecodeConfiguration() instanceof DecodeConfigNXDN configNXDN)
        {
            mIgnoreDataCalls = configNXDN.isIgnoreDataCalls();
            mChannelFrequencies.addAll(configNXDN.getChannelMap());
            createTrafficChannels(configNXDN);
        }
    }

    /**
     * Sets the channel access info from the control channel.
     *
     * @param info from the control channel
     */
    public void setChannelAccessInformation(ChannelAccessInformation info)
    {
        if(mChannelAccessInformation == null)
        {
            mChannelAccessInformation = info;
        }
    }

    /**
     * Creates up to the maximum number of traffic channels for use in allocating traffic channels.
     *
     * @param decodeConfig to use for each traffic channel
     */
    private void createTrafficChannels(DecodeConfigNXDN decodeConfig)
    {
        if(mManagedTrafficChannels == null)
        {
            int trafficChannelPoolSize = decodeConfig.getTrafficChannelPoolSize();
            List<Channel> trafficChannelList = new ArrayList<>();

            if(trafficChannelPoolSize > 0)
            {
                for(int x = 0; x < trafficChannelPoolSize; x++)
                {
                    Channel trafficChannel = new Channel("T-" + mParentChannel.getName(), Channel.ChannelType.TRAFFIC);
                    trafficChannel.setAliasListName(mParentChannel.getAliasListName());
                    trafficChannel.setSystem(mParentChannel.getSystem());
                    trafficChannel.setSite(mParentChannel.getSite());
                    trafficChannel.setDecodeConfiguration(decodeConfig);
                    trafficChannel.setEventLogConfiguration(mParentChannel.getEventLogConfiguration());
                    trafficChannel.setRecordConfiguration(mParentChannel.getRecordConfiguration());
                    trafficChannelList.add(trafficChannel);
                }
            }

            mAvailableTrafficChannelQueue.addAll(trafficChannelList);
            mManagedTrafficChannels = Collections.unmodifiableList(trafficChannelList);
        }
    }

    /**
     * Retrieves the current event tracker for the specified channel and if the tracker is stale relative to the
     * timestamp, returns null, otherwise returns the current tracker.
     *
     * @param frequency to look up the tracker
     * @param timestamp to compare for staleness
     * @return tracker or null
     */
    private NXDNChannelEventTracker getTrackerRemoveIfStale(long frequency, long timestamp)
    {
        NXDNChannelEventTracker tracker = mChannelEventTrackerMap.get(frequency);

        if(tracker != null && tracker.isStale(timestamp))
        {
            mChannelEventTrackerMap.remove(frequency);
            tracker = null;
        }

        return tracker;
    }

    /**
     * Broadcasts the decode event from the tracker.
     *
     * @param tracker containing a decode event.
     */
    public void broadcast(NXDNChannelEventTracker tracker)
    {
        broadcast(tracker.getEvent());
    }

    /**
     * Broadcasts an initial or update decode event to any registered listener.
     */
    public void broadcast(DecodeEvent decodeEvent)
    {
        if(mDecodeEventListener != null)
        {
            if(decodeEvent.getEventType() == DecodeEventType.DATA_CALL && mDuplicateDetector.isDuplicate(decodeEvent,
                    System.currentTimeMillis()))
            {
                return;
            }

            mDecodeEventListener.receive(decodeEvent);
        }
        else
        {
            System.out.println("Decode event listener is null");
        }
    }

    /**
     * Selects the decode event type based on the call type and encryption.
     *
     * @param callType for the call
     * @param encryption for the call
     * @return decode event type
     */
    private DecodeEventType getType(CallType callType, EncryptionKeyIdentifier encryption)
    {
        boolean encrypted = encryption.isEncrypted();
        return switch(callType)
        {
            case GROUP_BROADCAST, GROUP_CONFERENCE ->
                    encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
            case INDIVIDUAL ->
                    encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
            case INTERCONNECT, SPEED_DIAL ->
                    encrypted ? DecodeEventType.CALL_INTERCONNECT_ENCRYPTED : DecodeEventType.CALL_INTERCONNECT;
            default -> encrypted ? DecodeEventType.CALL_ENCRYPTED : DecodeEventType.CALL;
        };
    }

    /**
     * Creates a traffic channel event tracker
     *
     * @param eventType for the decode event
     * @param ic collection of from/to identifiers
     * @param channel to track
     * @param timestamp for the event
     * @return tracker
     */
    private NXDNChannelEventTracker createTracker(DecodeEventType eventType, IdentifierCollection ic, NXDNChannel channel,
                                                  long timestamp)
    {
        DecodeEvent event = NXDNDecodeEvent.builder(eventType, timestamp)
                .timeslot(0)
                .channel(channel)
                .identifiers(ic)
                .build();
        NXDNChannelEventTracker tracker = new NXDNChannelEventTracker(event);
        mChannelEventTrackerMap.put(channel.getDownlinkFrequency(), tracker);
        broadcast(tracker);
        return tracker;
    }

    /**
     * Process a data call assignment
     *
     * @param dca to process
     */
    public void processDataCallAssignment(DataCallAssignment dca)
    {
        if(dca.hasChannel() && dca.getChannel().getDownlinkFrequency() > 0)
        {
            DecodeEventType eventType = dca.getEncryptionKeyIdentifier().isEncrypted() ?
                    DecodeEventType.DATA_CALL_ENCRYPTED : DecodeEventType.DATA_CALL;
            processDataCall(dca.getIdentifiers(), dca.getChannel(), eventType, dca.getTimestamp(), dca.getCallOption(),
                    dca.getCallTimer());
        }
    }

    /**
     * Process a voice call assignment or notification
     * @param identifiers for the call
     * @param channel for the call
     */
    private void processDataCall(List<Identifier> identifiers, NXDNChannel channel, DecodeEventType eventType,
                                 long timestamp, DataCallOption dco, CallTimer callTimer)
    {
        if(channel.getDownlinkFrequency() > 0)
        {
            mLock.lock();

            try
            {
                NXDNChannelEventTracker tracker = getTrackerRemoveIfStale(channel.getDownlinkFrequency(), timestamp);
                MutableIdentifierCollection ic = new MutableIdentifierCollection(identifiers);
                if(tracker != null)
                {
                    if(tracker.isSameCallCheckingToAndFrom(ic, timestamp))
                    {
                        tracker.updateDurationControl(timestamp);
                        broadcast(tracker);
                    }
                    else
                    {
                        mChannelEventTrackerMap.remove(channel.getDownlinkFrequency());
                        tracker = null;
                    }
                }

                if(tracker == null)
                {
                    tracker = createTracker(eventType, ic, channel, timestamp);
                    Duplex duplex = dco.getDuplex();
                    TransmissionMode mode = dco.getTransmissionMode();
                    tracker.setDetails("TIMER:" + callTimer + " " + duplex + " " + mode);
                }

                if(!mIgnoreDataCalls)
                {
                    if(!mAllocatedTrafficChannelMap.containsKey(channel.getDownlinkFrequency()))
                    {
                        //Retrieve a channel from the traffic channel queue
                        Channel traffic = mAvailableTrafficChannelQueue.poll();

                        if(traffic != null)
                        {
                            requestTrafficChannelStart(traffic, channel, ic);
                        }
                        else
                        {
                            tracker.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " " + tracker.getEvent().getDetails());
                        }
                    }
                }
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * Process a voice call assignment
     *
     * @param vca to process
     */
    public void processVoiceCallAssignment(VoiceCallAssignment vca)
    {
        if(vca.hasChannel() && vca.getChannel().getDownlinkFrequency() > 0)
        {
            DecodeEventType eventType = getType(vca.getCallType(), vca.getEncryptionKeyIdentifier());
            processVoiceCall(vca.getIdentifiers(), vca.getChannel(), eventType, vca.getTimestamp(), vca.getCallOption(),
                    vca.getCallTimer());
        }
    }

    /**
     * Process a voice call assignment duplicate notification from a traffic channel
     *
     * @param vca to process
     */
    public void processVoiceCallAssignment(VoiceCallAssignmentDuplicateTraffic vca)
    {
        if(vca.hasChannel() && vca.getChannel().getDownlinkFrequency() > 0)
        {
            DecodeEventType eventType = getType(vca.getCallType(), vca.getEncryptionKeyIdentifier());
            processVoiceCall(vca.getIdentifiers(), vca.getChannel(), eventType, vca.getTimestamp(), vca.getCallOption(),
                    vca.getCallTimer());
        }
    }

    /**
     * Process a voice call assignment or notification
     * @param identifiers for the call
     * @param channel for the call
     */
    private void processVoiceCall(List<Identifier> identifiers, NXDNChannel channel, DecodeEventType eventType,
                                  long timestamp, VoiceCallOption vco, CallTimer callTimer)
    {
        if(channel.getDownlinkFrequency() > 0)
        {
            mLock.lock();

            try
            {
                NXDNChannelEventTracker tracker = getTrackerRemoveIfStale(channel.getDownlinkFrequency(), timestamp);
                MutableIdentifierCollection ic = new MutableIdentifierCollection(identifiers);
                if(tracker != null)
                {
                    if(tracker.isSameCallCheckingToAndFrom(ic, timestamp))
                    {
                        tracker.updateDurationControl(timestamp);
                        broadcast(tracker);
                    }
                    else
                    {
                        mChannelEventTrackerMap.remove(channel.getDownlinkFrequency());
                        tracker = null;
                    }
                }

                if(tracker == null)
                {
                    tracker = createTracker(eventType, ic, channel, timestamp);
                    AudioCodec audioCodec = vco.getCodec();
                    TransmissionMode mode = vco.getTransmissionMode();
                    tracker.setDetails("TIMER:" + callTimer + " " + audioCodec + " " + mode);
                }

                if(!mAllocatedTrafficChannelMap.containsKey(channel.getDownlinkFrequency()))
                {
                    //Retrieve a channel from the traffic channel queue
                    Channel traffic = mAvailableTrafficChannelQueue.poll();

                    if(traffic != null)
                    {
                        requestTrafficChannelStart(traffic, channel, ic);
                    }
                    else
                    {
                        tracker.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " " + tracker.getEvent().getDetails());
                    }
                }
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * Sends a channel start request to the ChannelProcessingManager.
     * Note: this method is not thread safe and the calling method must protect access using mLock.
     *
     * @param trafficChannel to use for the traffic channel
     * @param nxdnChannel that describes the traffic channel downlink frequency
     * @param identifierCollection containing identifiers for the call
     */
    private void requestTrafficChannelStart(Channel trafficChannel, NXDNChannel nxdnChannel,
                                            IdentifierCollection identifierCollection)
    {
        if(nxdnChannel != null && nxdnChannel.getDownlinkFrequency() > 0 && getInterModuleEventBus() != null)
        {
            SourceConfigTuner sourceConfig = new SourceConfigTuner();
            sourceConfig.setFrequency(nxdnChannel.getDownlinkFrequency());
            if(mParentChannel.getSourceConfiguration() instanceof SourceConfigTuner parentConfigTuner)
            {
                sourceConfig.setPreferredTuner(parentConfigTuner.getPreferredTuner());
            }
            trafficChannel.setSourceConfiguration(sourceConfig);
            mAllocatedTrafficChannelMap.put(nxdnChannel.getDownlinkFrequency(), trafficChannel);
            ChannelStartProcessingRequest startChannelRequest = new ChannelStartProcessingRequest(trafficChannel,
                    nxdnChannel, identifierCollection, this);
            startChannelRequest.addPreloadDataContent(new NXDNChannelInfoPreloadData(mChannelAccessInformation, mChannelFrequencies));
            getInterModuleEventBus().post(startChannelRequest);
        }
        else
        {
            //Return the channel to the traffic channel pool if we didn't start it.
            mAvailableTrafficChannelQueue.add(trafficChannel);
        }
    }

    @Override
    public Listener<ChannelEvent> getChannelEventListener()
    {
        return mTrafficChannelTeardownMonitor;
    }

    @Override
    public void setChannelEventListener(Listener<ChannelEvent> listener)
    {
        //TODO: configure this
    }

    @Override
    public void removeChannelEventListener()
    {

    }

    @Override
    public Listener<IMessage> getMessageListener()
    {
        return null;
    }

    @Override
    protected void processControlFrequencyUpdate(long previous, long current, Channel channel)
    {

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

    private void removeTracker(long frequency)
    {
        //TODO: implement this
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

            if(mManagedTrafficChannels.contains(channel))
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
                                        removeTracker(frequency);
                                        mAvailableTrafficChannelQueue.add(channel);
                                    });
                            break;
                        case NOTIFICATION_PROCESSING_START_REJECTED:
                            mAllocatedTrafficChannelMap.entrySet().stream()
                                    .filter(entry -> entry.getValue() == channel)
                                    .map(Map.Entry::getKey)
                                    .findFirst()
                                    .ifPresent(rejectedFrequency -> {
                                        mAllocatedTrafficChannelMap.remove(rejectedFrequency);
                                        mAvailableTrafficChannelQueue.add(channel);

                                        //Leave the event in the map so that it doesn't get recreated.  The channel
                                        //processing manager set the 'tuner not available' in the details already
                                        NXDNChannelEventTracker tracker = mChannelEventTrackerMap.get(rejectedFrequency);

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
        }
    }

}
