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
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.alias.TalkerAliasManager;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupPreLoadDataContent;
import io.github.dsheirer.identifier.scramble.ScrambleParameterIdentifier;
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
import io.github.dsheirer.module.decode.p25.identifier.channel.StandardChannel;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
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
public class P25TrafficChannelManager extends TrafficChannelManager implements IDecodeEventProvider, IChannelEventListener,
    IChannelEventProvider, IMessageListener
{
    private static final Logger mLog = LoggerFactory.getLogger(P25TrafficChannelManager.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(mLog);
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";
    private static final long STALE_EVENT_THRESHOLD_MS = 2000;

    private Queue<Channel> mAvailablePhase1TrafficChannelQueue = new LinkedTransferQueue<>();
    private List<Channel> mManagedPhase1TrafficChannels;
    private Queue<Channel> mAvailablePhase2TrafficChannelQueue = new LinkedTransferQueue<>();
    private List<Channel> mManagedPhase2TrafficChannels;
    private Map<Long,Channel> mAllocatedTrafficChannelMap = new HashMap<>();
    private Map<Long,P25ChannelGrantEvent> mTS1ChannelGrantEventMap = new HashMap<>();
    private Map<Long,P25ChannelGrantEvent> mTS2ChannelGrantEventMap = new HashMap<>();
    private Map<Integer, IFrequencyBand> mFrequencyBandMap = new ConcurrentHashMap<>();
    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;
    private TrafficChannelTeardownMonitor mTrafficChannelTeardownMonitor = new TrafficChannelTeardownMonitor();
    private Channel mParentChannel;
    private ScrambleParameters mPhase2ScrambleParameters;
    private Listener<IMessage> mMessageListener;
    private boolean mIgnoreDataCalls;
    private boolean mIdleNullProtect = false;
    //Used only for data calls
    private DecodeEventDuplicateDetector mDuplicateDetector = new DecodeEventDuplicateDetector();
    private ReentrantLock mLock = new ReentrantLock();
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
            mAllocatedTrafficChannelMap.clear();

            for(Channel channel : trafficChannelsToDisable)
            {
                if(!parentChannel.equals(channel))
                {
                    broadcast(new ChannelEvent(channel, Event.REQUEST_DISABLE));
                }
            }

            mTS1ChannelGrantEventMap.clear();
            mTS2ChannelGrantEventMap.clear();

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
                                       IdentifierCollection ic, MacOpcode macOpcode, long timestamp)
    {
        if(channel.getDownlinkFrequency() > 0)
        {
            mLock.lock();

            try
            {
                boolean processing = mAllocatedTrafficChannelMap.containsKey(channel.getDownlinkFrequency());

                if(!processing)
                {
                    processP2ChannelGrant(channel, serviceOptions, ic, macOpcode, timestamp);
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
     * @param isIdleNull to indicate if this close event is trigged by an IDLE/NULL message
     */
    public void closeP2CallEvent(long frequency, int timeslot, boolean isIdleNull)
    {
        /**
         * Hack: L3Harris systems can issue a channel grant on control/TS1 which creates an event for TS2 and then the
         * next message for TS2 is an IDLE/NULL which causes the event that was just created to be closed.  So, we set
         * a protect flag on the channel grant and then ignore the first IDLE/NULL triggered close event call, to
         * protect the just created event.
         */
        if(isIdleNull && mIdleNullProtect)
        {
            mIdleNullProtect = false;
            return;
        }

        mLock.lock();

        try
        {
            if(timeslot == P25P1Message.TIMESLOT_0 || timeslot == P25P1Message.TIMESLOT_1)
            {
                mTS1ChannelGrantEventMap.remove(frequency);
            }
            else
            {
                mTS2ChannelGrantEventMap.remove(frequency);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Updates an identifier for an ongoing call event on the frequency and timeslot.
     *
     * Note: if this manager does not have an existing call event for the frequency and timeslot, the update is ignored
     * because we don't have enough detail to create a call event.
     *
     * This is used primarily to add encryption, GPS, or a talker alias, but can be used for any identifier update.
     *
     * @param frequency for the call event
     * @param timeslot for the call event
     * @param identifier to update within the event.
     * @param timestamp for the update
     */
    public void processP2CurrentUser(long frequency, int timeslot, Identifier identifier, long timestamp)
    {
        mLock.lock();

        try
        {
            DecodeEvent event = null;

            if(timeslot == P25P1Message.TIMESLOT_1)
            {
                event = mTS1ChannelGrantEventMap.get(frequency);

                if(isStaleEvent(event, timestamp))
                {
                    event = null;
                    mTS1ChannelGrantEventMap.remove(frequency);
                }
            }
            else if(timeslot == P25P1Message.TIMESLOT_2)
            {
                event = mTS2ChannelGrantEventMap.get(frequency);

                if(isStaleEvent(event, timestamp))
                {
                    event = null;
                    mTS1ChannelGrantEventMap.remove(frequency);
                }
            }

            if(event != null)
            {
                if(!event.getIdentifierCollection().hasIdentifier(identifier))
                {
                    MutableIdentifierCollection mic = new MutableIdentifierCollection(event.getIdentifierCollection()
                            .getIdentifiers());
                    mic.update(identifier);
                    event.setIdentifierCollection(mic);
                }

                event.update(timestamp);
                broadcast(event);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }



    /**
     * Processes a call on the current channel
     * @param frequency of the current channel
     * @param timeslot of the current channel
     * @param serviceOptions for the call
     * @param ic for the call
     * @param timestamp for the message that is being processed
     * @return
     */
    public IChannelDescriptor processP2CurrentUser(long frequency, int timeslot, IChannelDescriptor channelDescriptor,
                                                   ServiceOptions serviceOptions, MacOpcode macOpcode,
                                                   IdentifierCollection ic, long timestamp, String additionalDetails)
    {
        mLock.lock();

        try
        {
            DecodeEvent event = null;

            if(timeslot == P25P1Message.TIMESLOT_1)
            {
                event = mTS1ChannelGrantEventMap.get(frequency);

                if(isStaleEvent(event, timestamp))
                {
                    event = null;
                    mTS1ChannelGrantEventMap.remove(frequency);
                }
            }
            else if(timeslot == P25P1Message.TIMESLOT_2)
            {
                event = mTS2ChannelGrantEventMap.get(frequency);

                if(isStaleEvent(event, timestamp))
                {
                    event = null;
                    mTS2ChannelGrantEventMap.remove(frequency);
                }
            }

            if(event != null)
            {
                if(isSameCallFull(event.getIdentifierCollection(), ic))
                {
                    boolean updateRequired = false;

                    for(Identifier identifier: ic.getIdentifiers())
                    {
                        if(!event.getIdentifierCollection().hasIdentifier(identifier))
                        {
                            updateRequired = true;
                            break;
                        }
                    }

                    if(updateRequired)
                    {
                        MutableIdentifierCollection mic = new MutableIdentifierCollection(event.getIdentifierCollection().getIdentifiers());
                        for(Identifier identifier: ic.getIdentifiers())
                        {
                            mic.update(identifier);
                        }

                        event.setIdentifierCollection(mic);
                    }

                    event.update(timestamp);
                    event.setDecodeEventType(getEventType(macOpcode, serviceOptions, event.getEventType()));

                    if(additionalDetails != null)
                    {
                        if(event.getDetails() == null)
                        {
                            event.setDetails(additionalDetails);
                        }
                        else if(!event.getDetails().endsWith(additionalDetails))
                        {
                            event.setDetails(event.getDetails() + " " + additionalDetails);
                        }
                    }

                    if(event.getChannelDescriptor() == null)
                    {
                        event.setChannelDescriptor(channelDescriptor);
                    }

                    broadcast(event);
                    return event.getChannelDescriptor();
                }
            }

            //Create a new event for the current call.
            DecodeEventType eventType = getEventType(macOpcode, serviceOptions, null);
            P25ChannelGrantEvent callEvent = P25ChannelGrantEvent.builder(eventType, timestamp, serviceOptions)
                    .channelDescriptor(channelDescriptor)
                    .details(additionalDetails != null ? additionalDetails : "PHASE 2 CALL " +
                            (serviceOptions != null ? serviceOptions : ""))
                    .identifiers(ic)
                    .build();

            if(timeslot == P25P1Message.TIMESLOT_0 || timeslot == P25P1Message.TIMESLOT_1)
            {
                mTS1ChannelGrantEventMap.put(frequency, callEvent);
            }
            else
            {
                mTS2ChannelGrantEventMap.put(frequency, callEvent);
            }

            broadcast(callEvent);
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
                                      IdentifierCollection ic, MacOpcode macOpcode, long timestamp)
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
                        processPhase1ChannelGrant(phase1Channel, serviceOptions, ic, decodeEventType,
                                isDataChannelGrant, timestamp);
                    }
                    else
                    {
                        processPhase2ChannelGrant(apco25Channel, serviceOptions, ic, decodeEventType,
                                isDataChannelGrant, timestamp);

                        //Hack: set the IDLE/NULL protect flag so that this event doesn't get immediately closed on
                        //L3Harris control channels.
                        mIdleNullProtect = true;
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
                processPhase1ChannelGrant(apco25Channel, serviceOptions, ic, decodeEventType, isDataChannelGrant,
                        timestamp);
            }

        }
        finally
        {
            mLock.unlock();
        }
    }

        /**
         * Processes phase 1 channel grants to allocate traffic channels and track overall channel usage.  Generates and
         * tracks decode events for each new channel that is allocated.
         *
         * @param apco25Channel for the traffic channel
         * @param serviceOptions for the traffic channel - optional can be null
         * @param identifierCollection associated with the channel grant
         * @param opcode to identify the call type for the event description
         */
    public void processP1ChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                      IdentifierCollection identifierCollection, Opcode opcode, long timestamp)
    {
        mLock.lock();

        try
        {
            DecodeEventType decodeEventType = getEventType(opcode, serviceOptions, null);
            boolean isDataChannelGrant = opcode != null && opcode.isDataChannelGrant();

            if(apco25Channel.isTDMAChannel())
            {
                processPhase2ChannelGrant(apco25Channel, serviceOptions, identifierCollection, decodeEventType,
                        isDataChannelGrant, timestamp);
            }
            else
            {
                processPhase1ChannelGrant(apco25Channel, serviceOptions, identifierCollection, decodeEventType,
                        isDataChannelGrant, timestamp);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Updates an identifier for an ongoing call event on the frequency.
     *
     * Note: if this manager does not have an existing call event for the frequency, the update is ignored
     * because we don't have enough detail to create a call event.
     *
     * This is used primarily to add encryption, GPS, or a talker alias, but can be used for any identifier update.
     *
     * @param frequency for the call event
     * @param identifier to update within the event.
     * @param timestamp for the update
     */
    public void processP1CurrentUser(long frequency, Identifier identifier, long timestamp)
    {
        mLock.lock();

        try
        {
            DecodeEvent event = mTS1ChannelGrantEventMap.get(frequency);

            if(event != null)
            {
                if(!event.getIdentifierCollection().hasIdentifier(identifier))
                {
                    MutableIdentifierCollection mic = new MutableIdentifierCollection(event.getIdentifierCollection()
                            .getIdentifiers());
                    mic.update(identifier);
                    event.setIdentifierCollection(mic);
                }

                //Add the encryption key to the call event details.
                if(identifier instanceof EncryptionKeyIdentifier eki && eki.isEncrypted())
                {
                    if(event.getDetails() == null)
                    {
                        event.setDetails(eki.toString());
                    }
                    else if(!event.getDetails().contains(eki.toString()))
                    {
                        event.setDetails(event.getDetails() + " " + eki);
                    }
                }

                event.update(timestamp);
                broadcast(event);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes a call on the current channel
     * @param frequency of the current channel
     * @param channelDescriptor for the current channel
     * @param decodeEventType to use for the event if it doesn't already exist
     * @param serviceOptions for the call
     * @param ic identifiers for the call
     * @param timestamp for the message that is being processed
     * @return channel descriptor for the event or null
     */
    public IChannelDescriptor processP1CurrentUser(long frequency, IChannelDescriptor channelDescriptor,
                                                   DecodeEventType decodeEventType, ServiceOptions serviceOptions,
                                                   IdentifierCollection ic, long timestamp, String additionalDetails)
    {
        mLock.lock();

        try
        {
            DecodeEvent event = mTS1ChannelGrantEventMap.get(frequency);

            if(event != null)
            {
                if(isSameCallFull(event.getIdentifierCollection(), ic))
                {
                    boolean updateRequired = false;

                    for(Identifier identifier: ic.getIdentifiers())
                    {
                        if(!event.getIdentifierCollection().hasIdentifier(identifier))
                        {
                            updateRequired = true;
                            break;
                        }
                    }

                    if(updateRequired)
                    {
                        MutableIdentifierCollection mic = new MutableIdentifierCollection(event.getIdentifierCollection().getIdentifiers());
                        for(Identifier identifier: ic.getIdentifiers())
                        {
                            mic.update(identifier);
                        }

                        event.setIdentifierCollection(mic);
                    }

                    event.update(timestamp);

                    if(additionalDetails != null)
                    {
                        if(event.getDetails() == null)
                        {
                            event.setDetails(additionalDetails);
                        }
                        else if(!event.getDetails().endsWith(additionalDetails))
                        {
                            event.setDetails(event.getDetails() + " " + additionalDetails);
                        }
                    }

                    broadcast(event);
                    return event.getChannelDescriptor();
                }
            }

            if(channelDescriptor == null && frequency > 0)
            {
                channelDescriptor = new StandardChannel(frequency);
            }

            //Create a new event for the current call.
            P25ChannelGrantEvent callEvent = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                    .channelDescriptor(channelDescriptor)
                    .details(additionalDetails != null ? additionalDetails : "PHASE 1 CALL " +
                            (serviceOptions != null ? serviceOptions : ""))
                    .identifiers(ic)
                    .build();

            mTS1ChannelGrantEventMap.put(frequency, callEvent);
            broadcast(callEvent);
            return null;
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Processes a P25 Phase 1 channel update for any channel.  If the initial channel grant was not detected, invokes
     * the process channel grant method to auto-create the channel.
     *
     * For the update message, we normally only get the TO talkgroup value, so we'll do a comparison of the event using
     * just the TO identifier.
     *
     * @param channel where the activity is taking place.
     * @param serviceOptions for the call
     * @param ic identifier collection
     * @param opcode for the update message
     * @param timestamp of the message
     */
    public void processP1ChannelUpdate(APCO25Channel channel, ServiceOptions serviceOptions,
                                       IdentifierCollection ic, Opcode opcode, long timestamp)
    {

        mLock.lock();

        try
        {
            DecodeEvent event = null;

            if(channel.isTDMAChannel() && channel.getTimeslot() == P25P1Message.TIMESLOT_2)
            {
                event = mTS2ChannelGrantEventMap.get(channel.getDownlinkFrequency());
                // If the event is stale, remove it and allow a new event to be created.
                if(isStaleEvent(event, timestamp))
                {
                    event = null;
                    mTS2ChannelGrantEventMap.remove(channel.getDownlinkFrequency());
                }
            }
            else
            {
                event = mTS1ChannelGrantEventMap.get(channel.getDownlinkFrequency());
                // If the event is stale, remove it and allow a new event to be created.
                if(isStaleEvent(event, timestamp))
                {
                    event = null;
                    mTS1ChannelGrantEventMap.remove(channel.getDownlinkFrequency());
                }
            }

            //If we have an event, update it.  Otherwise, make sure we have the traffic channel allocated
            if(event != null && isSameCallUpdate(event.getIdentifierCollection(), ic))
            {
                event.update(timestamp);
                broadcast(event);
            }
            else
            {
                processP1ChannelGrant(channel, serviceOptions, ic, opcode, timestamp);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Closes the call event for the specified channel frequency.
     * @param frequency for the channel
     */
    public void closeP1CallEvent(long frequency, long timestamp)
    {
        mLock.lock();

        try
        {
            DecodeEvent event = mTS1ChannelGrantEventMap.remove(frequency);

            if(event != null)
            {
                event.end(timestamp);
                broadcast(event);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Indicates if the event is stale and should be replaced with a new decode event.  Compares the event down time
     * to the reference timestamp argument and indicates if that staleness exceeds a staleness threshold (2 seconds).
     *
     * Note: some decode events can remain in the channel grant event map if a traffic channel is not allocated for the
     * event (ie tuner unavailable).  The control channel only tells us that the traffic channel is active, but doesn't
     * tell us when the traffic channel is torn down.  So, we use this staleness evaluation to remove stale events that
     * are left over from a previous traffic channel allocation.
     *
     * @param decodeEvent to evaluate
     * @param timestamp for reference of event staleness.
     * @return true if the event is stale.
     */
    private static boolean isStaleEvent(DecodeEvent decodeEvent, long timestamp)
    {
        return decodeEvent != null && (timestamp - decodeEvent.getTimeEnd() > STALE_EVENT_THRESHOLD_MS);
    }

    /**
     * Processes Phase 1 channel grants to allocate traffic channels and track overall channel usage.  Generates
     * decode events for each new channel that is allocated.
     *
     * Note: protected thread access to this method is controlled by the calling method.
     *
     * @param apco25Channel for the traffic channel
     * @param serviceOptions for the traffic channel - optional can be null
     * @param identifierCollection associated with the channel grant
     * @param decodeEventType to use
     * @param isDataChannelGrant indicator if this is a data channel grant
     * @param timestamp for the grant event.
     */
    private void processPhase1ChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                    IdentifierCollection identifierCollection, DecodeEventType decodeEventType,
                                    boolean isDataChannelGrant, long timestamp)
    {
        long frequency = apco25Channel.getDownlinkFrequency();

        P25ChannelGrantEvent event = mTS1ChannelGrantEventMap.get(frequency);

        // If the event is stale, remove it and allow a new event to be created.
        if(isStaleEvent(event, timestamp))
        {
            event = null;
            mTS1ChannelGrantEventMap.remove(frequency);
        }

        if(event != null && isSameCallUpdate(identifierCollection, event.getIdentifierCollection()))
        {
            Identifier from = identifierCollection.getFromIdentifier();

            if(from != null)
            {
                Identifier currentFrom = event.getIdentifierCollection().getFromIdentifier();
                if(currentFrom != null && !from.equals(currentFrom))
                {
                    event.end(timestamp);
                    broadcast(event);

                    event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                        .channelDescriptor(apco25Channel)
                        .details("CONTINUE - PHASE 1 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""))
                        .identifiers(identifierCollection)
                        .build();

                    mTS1ChannelGrantEventMap.put(frequency, event);
                }
            }

            //update the ending timestamp so that the duration value is correctly calculated
            event.update(timestamp);
            broadcast(event);

            //Even though we have an event, the initial channel grant may have been rejected.  Check to see if there
            //is a traffic channel allocated.  If not, allocate one and update the event description.
            if(!mAllocatedTrafficChannelMap.containsKey(frequency) && !(mIgnoreDataCalls && isDataChannelGrant))
            {
                Channel trafficChannel = mAvailablePhase1TrafficChannelQueue.poll();

                if(trafficChannel != null)
                {
                    if(isDataChannelGrant)
                    {
                        event.setDetails("PHASE 1 DATA CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""));
                    }
                    else
                    {
                        event.setDetails("PHASE 1 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""));
                    }
                    event.setChannelDescriptor(apco25Channel);
                    broadcast(event);
                    SourceConfigTuner sourceConfig = new SourceConfigTuner();
                    sourceConfig.setFrequency(frequency);
                    if(mParentChannel.getSourceConfiguration() instanceof  SourceConfigTuner parentConfigTuner)
                    {
                        sourceConfig.setPreferredTuner(parentConfigTuner.getPreferredTuner());
                    }
                    trafficChannel.setSourceConfiguration(sourceConfig);
                    mAllocatedTrafficChannelMap.put(frequency, trafficChannel);

                    ChannelStartProcessingRequest startChannelRequest = new ChannelStartProcessingRequest(trafficChannel,
                            apco25Channel, identifierCollection, this);
                    startChannelRequest.addPreloadDataContent(new PatchGroupPreLoadDataContent(identifierCollection, timestamp));
                    startChannelRequest.addPreloadDataContent(new P25FrequencyBandPreloadDataContent(mFrequencyBandMap.values()));
                    getInterModuleEventBus().post(startChannelRequest);
                }
            }

            return;
        }

        if(mIgnoreDataCalls && isDataChannelGrant && event == null)
        {
            event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                .channelDescriptor(apco25Channel)
                .details("IGNORED: PHASE 1 DATA CALL " + (serviceOptions != null ? serviceOptions : ""))
                .identifiers(identifierCollection)
                .build();
            mTS1ChannelGrantEventMap.put(frequency, event);
            broadcast(event);
            return;
        }

        event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
            .channelDescriptor(apco25Channel)
            .details("PHASE 1 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""))
            .identifiers(identifierCollection)
            .build();

        if(isDataChannelGrant)
        {
            event.setDetails("PHASE 1 DATA CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""));
        }

        mTS1ChannelGrantEventMap.put(frequency, event);

        //Allocate a traffic channel for the downlink frequency if one isn't already allocated
        if(!mAllocatedTrafficChannelMap.containsKey(frequency))
        {
            Channel trafficChannel = mAvailablePhase1TrafficChannelQueue.poll();

            if(trafficChannel == null)
            {
                event.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " - " + (event.getDetails() != null ? event.getDetails() : ""));
                return;
            }

            SourceConfigTuner sourceConfig = new SourceConfigTuner();
            sourceConfig.setFrequency(frequency);
            if(mParentChannel.getSourceConfiguration() instanceof  SourceConfigTuner parentConfigTuner)
            {
                sourceConfig.setPreferredTuner(parentConfigTuner.getPreferredTuner());
            }
            trafficChannel.setSourceConfiguration(sourceConfig);
            mAllocatedTrafficChannelMap.put(frequency, trafficChannel);

            ChannelStartProcessingRequest startChannelRequest =
                    new ChannelStartProcessingRequest(trafficChannel, apco25Channel, identifierCollection, this);
            startChannelRequest.addPreloadDataContent(new PatchGroupPreLoadDataContent(identifierCollection, timestamp));
            startChannelRequest.addPreloadDataContent(new P25FrequencyBandPreloadDataContent(mFrequencyBandMap.values()));

            if(getInterModuleEventBus() != null)
            {
                getInterModuleEventBus().post(startChannelRequest);
            }
        }

        broadcast(event);
    }

    /**
     * Processes Phase 2 channel grants to allocate traffic channels and track overall channel usage.  Generates
     * decode events for each new channel that is allocated.
     *
     * Note: protected thread access to this method is controlled by the calling method.
     *
     * @param apco25Channel for the traffic channel
     * @param serviceOptions for the traffic channel - optional can be null
     * @param identifierCollection associated with the channel grant
     * @param decodeEventType to use for the event.
     * @param isDataChannelGrant indicator if this is a data channel grant
     * @param timestamp for the event
     */
    private DecodeEvent processPhase2ChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                           IdentifierCollection identifierCollection, DecodeEventType decodeEventType,
                                           boolean isDataChannelGrant, long timestamp)
    {
        if(mPhase2ScrambleParameters != null && identifierCollection instanceof MutableIdentifierCollection)
        {
            ((MutableIdentifierCollection)identifierCollection).silentUpdate(ScrambleParameterIdentifier.create(mPhase2ScrambleParameters));
        }

        int timeslot = apco25Channel.getTimeslot();
        long frequency = apco25Channel.getDownlinkFrequency();

        P25ChannelGrantEvent event = null;

        if(timeslot == P25P1Message.TIMESLOT_0 || timeslot == P25P1Message.TIMESLOT_1)
        {
            event = mTS1ChannelGrantEventMap.get(frequency);
            // If the event is stale, remove it and allow a new event to be created.
            if(isStaleEvent(event, timestamp))
            {
                event = null;
                mTS1ChannelGrantEventMap.remove(frequency);
            }
        }
        else if(timeslot == P25P1Message.TIMESLOT_2)
        {
            event = mTS2ChannelGrantEventMap.get(frequency);
            // If the event is stale, remove it and allow a new event to be created.
            if(isStaleEvent(event, timestamp))
            {
                event = null;
                mTS2ChannelGrantEventMap.remove(frequency);
            }
        }
        else
        {
            mLog.error("Ignoring: Invalid timeslot [" + timeslot + "] detected for P25 Phase 2 Channel Grant.");
            return event;
        }

        identifierCollection.setTimeslot(timeslot);

        if(event != null && isSameCallUpdate(identifierCollection, event.getIdentifierCollection()))
        {
            Identifier from = identifierCollection.getFromIdentifier();

            if(from != null)
            {
                Identifier currentFrom = event.getIdentifierCollection().getFromIdentifier();
                if(currentFrom != null && !from.equals(currentFrom))
                {
                    event.end(timestamp);

                    P25ChannelGrantEvent continuationGrantEvent = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                        .channelDescriptor(apco25Channel)
                        .details("CONTINUE - PHASE 2 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""))
                        .identifiers(identifierCollection)
                        .build();

                    if(timeslot == P25P1Message.TIMESLOT_0 || timeslot == P25P1Message.TIMESLOT_1)
                    {
                        mTS1ChannelGrantEventMap.put(frequency, continuationGrantEvent);
                    }
                    else
                    {
                        mTS2ChannelGrantEventMap.put(frequency, continuationGrantEvent);
                    }

                    broadcast(continuationGrantEvent);
                }
            }

            //update the ending timestamp so that the duration value is correctly calculated
            event.update(timestamp);

            //Update the event type in case this they change from unencrypted to encrypted.
            event.setDecodeEventType(decodeEventType);
            broadcast(event);

            //Even though we have an event, the initial channel grant may have been rejected.  Check to see if there
            //is a traffic channel allocated.  If not, allocate one and update the event description.
            if(!mAllocatedTrafficChannelMap.containsKey(frequency) && !(mIgnoreDataCalls && isDataChannelGrant) &&
                (getCurrentControlFrequency() != frequency))
            {
                Channel trafficChannel = mAvailablePhase2TrafficChannelQueue.poll();

                if(trafficChannel != null)
                {
                    event.setDetails("PHASE 2 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""));
                    event.setChannelDescriptor(apco25Channel);
                    broadcast(event);
                    SourceConfigTuner sourceConfig = new SourceConfigTuner();
                    sourceConfig.setFrequency(frequency);
                    if(mParentChannel.getSourceConfiguration() instanceof  SourceConfigTuner parentConfigTuner)
                    {
                        sourceConfig.setPreferredTuner(parentConfigTuner.getPreferredTuner());
                    }
                    trafficChannel.setSourceConfiguration(sourceConfig);
                    mAllocatedTrafficChannelMap.put(frequency, trafficChannel);

                    //If we have valid scramble/randomizer parameters, set them in the decode config
                    if(mPhase2ScrambleParameters != null)
                    {
                        DecodeConfigP25Phase2 decodeConfig = (DecodeConfigP25Phase2)trafficChannel.getDecodeConfiguration();
                        decodeConfig.setScrambleParameters(mPhase2ScrambleParameters.copy());
                    }

                    ChannelStartProcessingRequest startChannelRequest =
                            new ChannelStartProcessingRequest(trafficChannel, apco25Channel, identifierCollection, this);
                    startChannelRequest.addPreloadDataContent(new PatchGroupPreLoadDataContent(identifierCollection, timestamp));
                    startChannelRequest.addPreloadDataContent(new P25FrequencyBandPreloadDataContent(mFrequencyBandMap.values()));
                    getInterModuleEventBus().post(startChannelRequest);
                }
            }

            return event;
        }

        if(mIgnoreDataCalls && isDataChannelGrant && event == null)
        {
           event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
                .channelDescriptor(apco25Channel)
                .details("PHASE 2 DATA CALL IGNORED: " + (serviceOptions != null ? serviceOptions : ""))
                .identifiers(identifierCollection)
                .build();

            //Phase 1 events get stored in TS1 only
           mTS1ChannelGrantEventMap.put(frequency, event);
           broadcast(event);
           return event;
        }

        event = P25ChannelGrantEvent.builder(decodeEventType, timestamp, serviceOptions)
            .channelDescriptor(apco25Channel)
            .details("PHASE 2 CHANNEL GRANT " + (serviceOptions != null ? serviceOptions : ""))
            .identifiers(identifierCollection)
            .build();

        if(timeslot == P25P1Message.TIMESLOT_0 || timeslot == P25P1Message.TIMESLOT_1)
        {
            mTS1ChannelGrantEventMap.put(frequency, event);
        }
        else
        {
            mTS2ChannelGrantEventMap.put(frequency, event);
        }

        //Allocate a traffic channel for the downlink frequency if one isn't already allocated
        if(!mAllocatedTrafficChannelMap.containsKey(frequency) && frequency != getCurrentControlFrequency())
        {
            Channel trafficChannel = mAvailablePhase2TrafficChannelQueue.poll();

            if(trafficChannel == null)
            {
                event.setDetails(MAX_TRAFFIC_CHANNELS_EXCEEDED + " - IGNORED");
                return event;
            }

            SourceConfigTuner sourceConfig = new SourceConfigTuner();
            sourceConfig.setFrequency(frequency);
            if(mParentChannel.getSourceConfiguration() instanceof  SourceConfigTuner parentConfigTuner)
            {
                sourceConfig.setPreferredTuner(parentConfigTuner.getPreferredTuner());
            }
            trafficChannel.setSourceConfiguration(sourceConfig);
            mAllocatedTrafficChannelMap.put(frequency, trafficChannel);

            ChannelStartProcessingRequest startChannelRequest =
                    new ChannelStartProcessingRequest(trafficChannel, apco25Channel, identifierCollection, this);
            startChannelRequest.addPreloadDataContent(new PatchGroupPreLoadDataContent(identifierCollection, timestamp));
            startChannelRequest.addPreloadDataContent(new P25FrequencyBandPreloadDataContent(mFrequencyBandMap.values()));

            if(getInterModuleEventBus() == null)
            {
                return event;
            }

            getInterModuleEventBus().post(startChannelRequest);
        }

        broadcast(event);
        return event;
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
        boolean encrypted = serviceOptions != null ? serviceOptions.isEncrypted() : false;

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
        boolean encrypted = serviceOptions != null ? serviceOptions.isEncrypted() : false;

        DecodeEventType type = null;

        if(opcode != null)
        {
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

                case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT:
                case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_UPDATE:
                    type = encrypted ? DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED : DecodeEventType.CALL_PATCH_GROUP;
                    break;
            }
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
     * Compares the TO role identifier(s) from each collection for equality.  This is normally used to compare a call
     * update where we only compare the TO role.
     *
     * @param existingIC containing a TO identifier
     * @param nextIC containing a TO identifier
     * @return true if both collections contain a TO identifier and the TO identifiers are the same value
     */
    private boolean isSameCallUpdate(IdentifierCollection existingIC, IdentifierCollection nextIC)
    {
        Identifier existingTO = existingIC.getToIdentifier();
        Identifier nextTO = nextIC.getToIdentifier();
        return existingTO != null && existingTO.equals(nextTO);
    }

    /**
     * Compares the TO role identifier(s) from each collection for equality.  This is normally used to compare a call
     * update where we only compare the TO role.
     *
     * @param existingIC for the existing/previous event containing a TO identifier
     * @param nextIC for the next event containing a TO identifier
     * @return true if both collections contain a TO identifier and the TO identifiers are the same value
     */
    private boolean isSameCallFull(IdentifierCollection existingIC, IdentifierCollection nextIC)
    {
        Identifier existingTO = existingIC.getToIdentifier();
        Identifier nextTO = nextIC.getToIdentifier();

        if(existingTO != null && existingTO.equals(nextTO))
        {
            Identifier existingFROM = existingIC.getFromIdentifier();

            //If the FROM identifier hasn't yet been established, then this is the same call.  We also ignore the
            //talker alias as a call identifier since on L3Harris systems they can transmit the talker alias before
            //they transmit the radio ID.
            if(existingFROM == null || existingFROM.getForm() == Form.TALKER_ALIAS)
            {
                return true;
            }

            Identifier nextFROM = nextIC.getFromIdentifier();

            if(nextFROM != null && nextFROM.getForm() == Form.TALKER_ALIAS)
            {
                return true;
            }

            return existingFROM.equals(nextFROM);
        }

        return false;
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
                    if(message instanceof NetworkStatusBroadcast)
                    {
                        mPhase2ScrambleParameters = ((NetworkStatusBroadcast)message).getScrambleParameters();
                    }
                    else if(message instanceof AMBTCNetworkStatusBroadcast)
                    {
                        mPhase2ScrambleParameters = ((AMBTCNetworkStatusBroadcast)message).getScrambleParameters();
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
                                        mTS1ChannelGrantEventMap.remove(frequency);
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
                                        P25ChannelGrantEvent event = mTS1ChannelGrantEventMap.get(rejectedFrequency);

                                        if(event != null && !event.getDetails().contains(CHANNEL_START_REJECTED))
                                        {
                                            event.setDetails(CHANNEL_START_REJECTED + " " + channelEvent.getDescription() +
                                                    (event.getDetails() != null ? " - " + event.getDetails() : ""));
                                        }
                                        broadcast(event);
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
                                        mTS1ChannelGrantEventMap.remove(frequency);
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
                                        P25ChannelGrantEvent event1 = mTS1ChannelGrantEventMap.get(rejectedFrequency);
                                        if (event1 != null)
                                        {
                                            broadcast(event1);
                                        }

                                        //Leave the event in the map so that it doesn't get recreated.  The channel
                                        //processing manager set the 'tuner not available' in the details already
                                        P25ChannelGrantEvent event2 = mTS2ChannelGrantEventMap.get(rejectedFrequency);
                                        if (event2 != null)
                                        {
                                            broadcast(event2);
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