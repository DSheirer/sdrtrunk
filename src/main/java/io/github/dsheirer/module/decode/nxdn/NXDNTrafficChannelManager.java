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

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.state.SingleChannelState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.health.CallOutcome;
import io.github.dsheirer.health.CallTally;
import io.github.dsheirer.health.HealthRegistry;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.alias.TalkerAliasManager;
import io.github.dsheirer.identifier.alias.TalkerAliasTracker;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventDuplicateDetector;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannel;
import io.github.dsheirer.module.decode.nxdn.channel.NXDNChannelFake;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkerAliasIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
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
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages trunked system traffic channel activations
 */
public class NXDNTrafficChannelManager extends TrafficChannelManager implements IDecodeEventProvider,
        IChannelEventListener, IChannelEventProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NXDNTrafficChannelManager.class);
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    private final Channel mParentChannel;
    private final DecodeEventDuplicateDetector mDuplicateDetector = new DecodeEventDuplicateDetector();
    private final List<ChannelFrequency> mChannelFrequencies = new ArrayList<>();
    private final Lock mLock = new ReentrantLock();
    private final Map<Long, Channel> mAllocatedTrafficChannelMap = new HashMap<>();
    private final Map<Long, NXDNChannelEventTracker> mEventTrackerMap = new HashMap<>();
    private final Queue<Channel> mAvailableTrafficChannelQueue = new LinkedTransferQueue<>();
    private final TalkerAliasManager mTalkerAliasManager = new TalkerAliasManager();
    private final TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();
    private List<Channel> mManagedTrafficChannels;
    private Listener<IDecodeEvent> mDecodeEventListener;
    private Listener<ChannelEvent> mChannelEventListener;
    private ChannelAccessInformation mChannelAccessInformation;
    private boolean mIgnoreDataCalls;
    private boolean mIgnoreEncryptedCalls;
    private List<Identifier<?>> mChannelIdentifiers;

    /**
     * Constructs an instance
     *
     * @param parentChannel configuration for the parent channel
     */
    private CallTally mCallTally;

    public NXDNTrafficChannelManager(Channel parentChannel)
    {
        mParentChannel = parentChannel;

        if(parentChannel.getDecodeConfiguration() instanceof DecodeConfigNXDN configNXDN)
        {
            mIgnoreDataCalls = configNXDN.isIgnoreDataCalls();
            mIgnoreEncryptedCalls = configNXDN.isIgnoreEncryptedCalls();
            mChannelFrequencies.addAll(configNXDN.getChannelMap());
            createTrafficChannels(configNXDN);
        }

        mChannelIdentifiers = SingleChannelState.createChannelIdentifiers(parentChannel);
    }

    /**
     * Creates a new list of identifiers containing the channel metadata identifiers and the provided identifiers.
     * @param identifiers to merge with the channel identifiers
     * @return new list of identifiers.
     */
    private List<Identifier> getMergedIdentifiers(List<Identifier> identifiers)
    {
        List<Identifier> merged = new ArrayList<>();
        merged.addAll(identifiers);
        merged.addAll(mChannelIdentifiers);
        return merged;
    }

    /**
     * Creates a new mutable identifier collection with the identifiers and the channel identifiers
     * @param identifiers for initial fill of the collection
     * @return mutable identifier collection
     */
    private MutableIdentifierCollection getMergedIdentifierCollection(List<Identifier> identifiers)
    {
        List<Identifier> merged = getMergedIdentifiers(identifiers);
        MutableIdentifierCollection mic = new MutableIdentifierCollection();
        mic.update(merged);
        return new MutableIdentifierCollection(merged);
    }

    /**
     * Talker alias manager
     */
    @Override
    public TalkerAliasManager getTalkerAliasManager()
    {
        return mTalkerAliasManager;
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
                    trafficChannelList.add(createTrafficChannel(decodeConfig));
                }
            }

            mAvailableTrafficChannelQueue.addAll(trafficChannelList);
            mManagedTrafficChannels = Collections.unmodifiableList(trafficChannelList);
        }
    }

    /**
     * Creates a child traffic channel that share's the parent decode configuration
     * @param decodeConfig from the parent
     * @return traffic channel.
     */
    private @NonNull Channel createTrafficChannel(DecodeConfigNXDN decodeConfig)
    {
        Channel trafficChannel = new Channel("T-" + mParentChannel.getName(), Channel.ChannelType.TRAFFIC);
        trafficChannel.setAliasListName(mParentChannel.getAliasListName());
        trafficChannel.setSystem(mParentChannel.getSystem());
        trafficChannel.setSite(mParentChannel.getSite());
        trafficChannel.setDecodeConfiguration(decodeConfig);
        trafficChannel.setEventLogConfiguration(mParentChannel.getEventLogConfiguration());
        trafficChannel.setRecordConfiguration(mParentChannel.getRecordConfiguration());
        return trafficChannel;
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
        NXDNChannelEventTracker tracker = mEventTrackerMap.get(frequency);

        if(tracker != null && tracker.isStale(timestamp))
        {
            removeTracker(frequency);
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
    private NXDNChannelEventTracker createTracker(DecodeEventType eventType, IdentifierCollection ic,
                                                  NXDNChannel channel, long timestamp)
    {
        long frequency = channel != null ? channel.getDownlinkFrequency() : 0;

        DecodeEvent event = NXDNDecodeEvent.builder(eventType, timestamp)
                .timeslot(0)
                .channel(channel)
                .identifiers(ic)
                .build();
        NXDNChannelEventTracker tracker = new NXDNChannelEventTracker(event);
        mEventTrackerMap.put(frequency, tracker);
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
                MutableIdentifierCollection ic = getMergedIdentifierCollection(identifiers);
                if(tracker != null)
                {
                    if(tracker.isSameCallCheckingToAndFrom(ic, timestamp))
                    {
                        tracker.updateDurationControl(timestamp);
                        broadcast(tracker);
                    }
                    else
                    {
                        removeTracker(channel.getDownlinkFrequency());
                        tracker = null;
                    }
                }

                if(tracker == null)
                {
                    tracker = createTracker(eventType, ic, channel, timestamp);
                    Duplex duplex = dco.getDuplex();
                    TransmissionMode mode = dco.getTransmissionMode();
                    tracker.addDetails("TIMER:" + callTimer + " " + duplex + " " + mode);
                }

                if(mIgnoreDataCalls)
                {
                    tracker.prefixDetails("IGNORED DATA");
                    tallyCall(true, tracker, CallOutcome.DATA);
                }
                else
                {
                    if(!tracker.isTrafficChannelAllocated() && channel.isValid() &&
                            channel.getDownlinkFrequency() != getCurrentControlFrequency() &&
                            !mAllocatedTrafficChannelMap.containsKey(channel.getDownlinkFrequency()))
                    {
                        //Retrieve a channel from the traffic channel queue
                        Channel traffic = mAvailableTrafficChannelQueue.poll();

                        if(traffic != null)
                        {
                            startAndTally(true, traffic, channel, ic, tracker, timestamp);
                        }
                        else
                        {
                            tracker.addDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " " + tracker.getEvent().getDetails());
                            tallyCall(true, tracker, CallOutcome.NO_RECORDER);
                        }
                    }
                    else
                    {
                        tallyCall(true, tracker, channel.isValid() ? CallOutcome.RECORDED : CallOutcome.NO_FREQUENCY);
                    }
                }

                broadcast(tracker);
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * Process a voice call assignment indicating there is a call on another channel.
     *
     * @param vca to process
     */
    public void processVoiceCallAssignment(VoiceCallAssignment vca)
    {
        if(vca.hasChannel() && vca.getChannel().getDownlinkFrequency() > 0)
        {
            processVoiceCall(vca.getIdentifiers(), vca.getChannel(), vca.getCallType(), vca.getEncryptionKeyIdentifier(),
                    vca.getTimestamp(), vca.getCallOption(), vca.getCallTimer());
        }
    }

    /**
     * Process a voice call assignment duplicate notification from a traffic channel
     *
     * @param vca to process
     */
    public void processVoiceCallAssignment(VoiceCallAssignmentDuplicateTraffic vca)
    {
        NXDNChannel channel = vca.getChannel();

        if(channel == null || channel.getDownlinkFrequency() == 0)
        {
            //If the channel is null or not configured, use the talkgroup value so we can track the call event
            channel = new NXDNChannelFake(vca.getDestination().getValue());
        }

        processVoiceCall(vca.getIdentifiers(), channel, vca.getCallType(), vca.getEncryptionKeyIdentifier(),
                vca.getTimestamp(), vca.getCallOption(), vca.getCallTimer());
    }

    /**
     * Process a traffic channel voice call message
     * @param voiceCall message
     * @param channel for the call, can be null
     */
    public void processVoiceCall(VoiceCall voiceCall, IChannelDescriptor channel)
    {
        NXDNChannel nxdn = null;
        if(channel instanceof NXDNChannel nxdnChannel)
        {
            nxdn = nxdnChannel;
        }

        processVoiceCall(voiceCall.getIdentifiers(), nxdn, voiceCall.getCallType(), voiceCall.getEncryptionKeyIdentifier(),
                voiceCall.getTimestamp(), voiceCall.getCallOption(), CallTimer.UNSPECIFIED);
    }

    /**
     * Adds or updates the talker alias for the tracked call event, when the event is for the talker, or when the
     * event's talker isn't identified yet.
     * @param channel for the call
     * @param talkerAlias to add or update
     * @param radio that the alias belongs to, or null when the talker isn't identified yet
     * @param timestamp for the talker alias message
     */
    public void processTalkerAlias(IChannelDescriptor channel, NXDNTalkerAliasIdentifier talkerAlias,
                                   RadioIdentifier radio, long timestamp)
    {
        mLock.lock();

        try
        {
            long frequency = channel != null ? channel.getDownlinkFrequency() : 0;
            NXDNChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, timestamp);

            if(tracker != null)
            {
                Identifier eventRadio = tracker.getEvent().getIdentifierCollection()
                        .getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

                if(radio == null || eventRadio == null || TalkerAliasTracker.isSameRadio(radio, eventRadio))
                {
                    tracker.addIdentifierIfMissing(talkerAlias);
                    broadcast(tracker);
                }
            }

            if(radio != null)
            {
                getTalkerAliasManager().update(radio, talkerAlias);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes call audio: updates the end time for an ongoing call to reflect progress, or creates a call event when
     * there isn't one, since a conventional transmission can send audio (and the talker alias) before the first call
     * message that identifies the call.  The first call message then updates this event.
     * @param channel descriptor for the traffic channel, can be null
     * @param talkerAlias for the talker of this transmission, can be null
     * @param encrypted true when the call is known to be encrypted
     * @param canStartCall true when the audio is reliable enough to create a call event when there isn't one
     * @param timestamp of the audio
     */
    public void processAudio(IChannelDescriptor channel, NXDNTalkerAliasIdentifier talkerAlias, boolean encrypted,
                             boolean canStartCall, long timestamp)
    {
        mLock.lock();
        try
        {
            long frequency = channel != null ? channel.getDownlinkFrequency() : 0;
            NXDNChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, timestamp);

            if(tracker == null)
            {
                if(!canStartCall)
                {
                    return;
                }

                //Only the talker alias: TO/FROM left in the decoder state from the previous call may not be this call's
                List<Identifier> identifiers = new ArrayList<>();

                if(talkerAlias != null)
                {
                    identifiers.add(talkerAlias);
                }

                tracker = createTracker(encrypted ? DecodeEventType.CALL_ENCRYPTED : DecodeEventType.CALL,
                        getMergedIdentifierCollection(identifiers), channel instanceof NXDNChannel nxdn ? nxdn : null,
                        timestamp);
                tracker.setStartedFromAudio(true);
            }
            else if(tracker.isStartedFromAudio())
            {
                tracker.addIdentifierIfMissing(talkerAlias);
            }

            tracker.updateDurationTraffic(timestamp);
            broadcast(tracker);
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Ends the current call and removes the event tracker
     * @param channel for the call
     * @param timestamp for the end call
     */
    public void processEndCall(IChannelDescriptor channel, long timestamp)
    {
        mLock.lock();

        try
        {
            long frequency = channel != null ? channel.getDownlinkFrequency() : 0;
            NXDNChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, timestamp);

            if(tracker != null)
            {
                tracker.completeTraffic(timestamp);
                broadcast(tracker);
                removeTracker(frequency);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Process a voice call assignment or notification
     * @param identifiers for the call
     * @param channel for the call
     */
    void processVoiceCall(List<Identifier> identifiers, NXDNChannel channel, CallType callType,
                          EncryptionKeyIdentifier encryption, long timestamp, VoiceCallOption vco,
                          CallTimer callTimer)
    {
        mLock.lock();

        long frequency = channel != null ? channel.getDownlinkFrequency() : 0;

        try
        {
            NXDNChannelEventTracker tracker = getTrackerRemoveIfStale(frequency, timestamp);
            MutableIdentifierCollection ic = getMergedIdentifierCollection(identifiers);
            //Apply the stored talker alias for the radio (enrich() returns a copy, so update the collection in place)
            mTalkerAliasManager.enrichMutable(ic);
            boolean identified = false;

            if(tracker != null)
            {
                if(tracker.isStartedFromAudio())
                {
                    //The event was created from the call audio.  This first call message identifies the call.
                    Identifier eventAlias = tracker.getEvent().getIdentifierCollection()
                            .getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);

                    if(eventAlias != null && ic.getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM) == null)
                    {
                        ic.update(eventAlias);
                    }

                    tracker.getEvent().setIdentifierCollection(ic);
                    tracker.getEvent().setDecodeEventType(getType(callType, encryption));
                    tracker.setStartedFromAudio(false);
                    identified = true;
                }
                else if(tracker.isSameCallCheckingToAndFrom(ic, timestamp))
                {
                    tracker.updateDurationControl(timestamp);
                }
                else
                {
                    removeTracker(frequency);
                    tracker = null;
                }
            }

            if(tracker == null || identified)
            {
                if(tracker == null)
                {
                    DecodeEventType eventType = getType(callType, encryption);
                    tracker = createTracker(eventType, ic, channel, timestamp);
                }

                AudioCodec audioCodec = vco.getCodec();
                TransmissionMode mode = vco.getTransmissionMode();

                if(encryption.isEncrypted())
                {
                    tracker.addDetails(encryption + " TIMER:" + callTimer + " " + audioCodec + " " + mode);
                }
                else
                {
                    tracker.addDetails("TIMER:" + callTimer + " " + audioCodec + " " + mode);
                }
            }

            boolean ignoreEncrypted = mIgnoreEncryptedCalls && encryption.isEncrypted();
            //A placeholder channel (duplicate traffic) counts only toward a call already counted.
            boolean counted = !(channel instanceof NXDNChannelFake) || tracker.getCallOutcome() != null;

            if(ignoreEncrypted)
            {
                tracker.prefixDetails("IGNORED ENCRYPTED");
                tallyCall(counted, tracker, CallOutcome.ENCRYPTED);
            }
            else if(!tracker.isTrafficChannelAllocated() && channel != null && channel.isValid() &&
                    channel.getDownlinkFrequency() != getCurrentControlFrequency() &&
                    !mAllocatedTrafficChannelMap.containsKey(frequency))
            {
                //Retrieve a channel from the traffic channel queue
                Channel traffic = mAvailableTrafficChannelQueue.poll();

                if(traffic != null)
                {
                    startAndTally(counted, traffic, channel, ic, tracker, timestamp);
                }
                else
                {
                    tracker.addDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " " + tracker.getEvent().getDetails());
                    tallyCall(counted, tracker, CallOutcome.NO_RECORDER);
                }
            }
            else
            {
                //Followed already (a traffic channel runs on it, or it's the control channel's own), or a channel the
                //channel map doesn't give a frequency for.
                tallyCall(counted, tracker, channel != null && channel.isValid() && frequency > 0 ?
                    CallOutcome.RECORDED : CallOutcome.NO_FREQUENCY);
            }

            broadcast(tracker);
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Sends a channel start request to the ChannelProcessingManager.
     * Note: this method is not thread safe and the calling method must protect access using mLock.
     *
     * @param trafficChannel to use for the traffic channel
     * @param nxdnChannel that describes the traffic channel downlink frequency
     * @param ic containing identifiers for the call
     * @param tracker to update with channel allocation flag
     */
    private boolean requestTrafficChannelStart(Channel trafficChannel, NXDNChannel nxdnChannel,
                                               IdentifierCollection ic, NXDNChannelEventTracker tracker,
                                               long timestamp)
    {
        if(nxdnChannel != null && nxdnChannel.getDownlinkFrequency() > 0 && getInterModuleEventBus() != null)
        {
            SourceConfigTuner sourceConfig = new SourceConfigTuner();
            sourceConfig.setFrequency(nxdnChannel.getDownlinkFrequency());
            sourceConfig.setPreferredTuner(getPreferredTuner(mParentChannel));
            trafficChannel.setSourceConfiguration(sourceConfig);
            mAllocatedTrafficChannelMap.put(nxdnChannel.getDownlinkFrequency(), trafficChannel);
            ChannelStartProcessingRequest startChannelRequest = new ChannelStartProcessingRequest(trafficChannel,
                    nxdnChannel, ic, this);
            startChannelRequest.setStartTimestamp(timestamp);
            startChannelRequest.addPreloadDataContent(new NXDNChannelInfoPreloadData(mChannelAccessInformation, mChannelFrequencies));
            //Allocated before the request: a request no tuner can take is rejected while it's posted, and the
            //rejection clears this.
            tracker.setTrafficChannelAllocated(true);
            getInterModuleEventBus().post(startChannelRequest);
            return true;
        }
        else
        {
            //Return the channel to the traffic channel pool if we didn't start it.
            mAvailableTrafficChannelQueue.add(trafficChannel);
            return false;
        }
    }

    /**
     * Calls this control channel announced and what became of each, for node health reports.  Asked for at the first
     * call: an NXDN channel that never announces one (conventional) has no calls counted, and is reported as a channel.
     */
    private CallTally calls()
    {
        if(mCallTally == null)
        {
            mCallTally = HealthRegistry.getInstance().calls(mParentChannel);
        }

        return mCallTally;
    }

    /**
     * Counts a call by what has become of it so far, for node health reports.
     * @param counted false for a call heard only on a placeholder channel, not counted
     */
    private void tallyCall(boolean counted, NXDNChannelEventTracker tracker, CallOutcome outcome)
    {
        if(counted && tracker != null)
        {
            tracker.tally(calls(), outcome);
        }
    }

    /**
     * Starts a traffic channel for a call and counts the call as recorded.  It's counted before the request, because a
     * request no tuner can take is rejected while it's made (the rejection moves the call to noSource).  A request
     * that can't be made (no frequency) says so.
     */
    private void startAndTally(boolean counted, Channel trafficChannel, NXDNChannel nxdnChannel,
                               IdentifierCollection ic, NXDNChannelEventTracker tracker, long timestamp)
    {
        tallyCall(counted, tracker, CallOutcome.RECORDED);

        if(!requestTrafficChannelStart(trafficChannel, nxdnChannel, ic, tracker, timestamp))
        {
            tallyCall(counted, tracker, CallOutcome.NO_FREQUENCY);
        }
    }

    @Override
    public Listener<ChannelEvent> getChannelEventListener()
    {
        return mTrafficChannelTeardownMonitor;
    }

    /**
     * Broadcasts the channel event ot an optionally registered listener
     * @param event to broadcast
     */
    private void broadcast(ChannelEvent event)
    {
        if(mChannelEventListener != null)
        {
            mChannelEventListener.receive(event);
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

    /**
     * Process the current control frequency to ensure we don't allocated traffic channels for the same frequency.
     * @param previous frequency for the control channel (to remove from allocated channels)
     * @param current frequency for the control channel (to add to allocated channels)
     * @param channel for the current control channel
     */
    @Override
    protected void processControlFrequencyUpdate(long previous, long current, Channel channel)
    {
        mLock.lock();

        try
        {
            //Clear any traffic channel that is already allocated on the new/current control frequency
            if(mAllocatedTrafficChannelMap.containsKey(current))
            {
                broadcast(new ChannelEvent(mAllocatedTrafficChannelMap.get(current), ChannelEvent.Event.REQUEST_DISABLE));
            }
        }
        finally
        {
            mLock.unlock();
        }

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
        mLock.lock();

        try
        {
            mAvailableTrafficChannelQueue.clear();

            List<Channel> channels = new ArrayList<>(mAllocatedTrafficChannelMap.values());

            //Issue a disable request for each traffic channel
            for(Channel channel: channels)
            {
                LOGGER.info("Stopping NXDN traffic channel: " + channel);
                broadcast(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_DISABLE));
            }
        }
        finally
        {
            mLock.unlock();
        }
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
     * Remotes the event tracker from the tracker map
     * @param frequency for the tracker
     */
    private void removeTracker(long frequency)
    {
        mLock.lock();

        try
        {
            mEventTrackerMap.remove(frequency);
        }
        finally
        {
            mLock.unlock();
        }
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
                                        //Don't remove the tracker.  There's a chance the control channel can still
                                        //reference the now terminating traffic channel and cause a residual event
                                        //creation and channel allocation and we'll use the tracker to keep that from
                                        //happening.
                                        //removeTracker(frequency);
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
                                        NXDNChannelEventTracker tracker = mEventTrackerMap.get(rejectedFrequency);

                                        if(tracker != null)
                                        {
                                            if(tracker.getCallOutcome() == CallOutcome.RECORDED)
                                            {
                                                tracker.tally(calls(), CallOutcome.NO_SOURCE);
                                            }

                                            if(!tracker.getEvent().getDetails().contains(CHANNEL_START_REJECTED))
                                            {
                                                tracker.addDetails(CHANNEL_START_REJECTED + " " + channelEvent.getDescription() +
                                                        (tracker.getEvent().getDetails() != null ? " - " + tracker.getEvent().getDetails() : ""));
                                            }
                                            tracker.setTrafficChannelAllocated(false);
                                            broadcast(tracker);
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
