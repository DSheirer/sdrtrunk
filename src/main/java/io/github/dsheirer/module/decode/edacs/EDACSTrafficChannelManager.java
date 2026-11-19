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

package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.traffic.TrafficChannelManager;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import io.github.dsheirer.source.heartbeat.IHeartbeatListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Follows EDACS Extended Addressing calls.  A group call assignment on the control channel starts a
 * voice (traffic) channel on the LCN's frequency - ProVoice or analog, as the assignment says - carrying the group and
 * the calling unit.  The control channel repeats the call (updates) every 0.1-0.6 s for as long as it lasts (MBTA
 * 2026-11-09), so a call ends when its updates stop for CALL_END_MS, and its voice channel is torn down then.
 */
public class EDACSTrafficChannelManager extends TrafficChannelManager implements IDecodeEventProvider,
    IChannelEventListener, IChannelEventProvider, IHeartbeatListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EDACSTrafficChannelManager.class);

    /** A call ends when the control channel has not announced it for this long */
    public static final long CALL_END_MS = 1500;
    private static final long HEARTBEAT_INTERVAL_MS = 250;

    public static final String MAX_TRAFFIC_CHANNELS_EXCEEDED = "MAX TRAFFIC CHANNELS EXCEEDED";
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";

    private final Queue<Channel> mAvailableTrafficChannels = new ArrayDeque<>();
    private final List<Channel> mManagedTrafficChannels;
    private final Map<Integer,Channel> mAllocatedTrafficChannels = new HashMap<>();
    private final Map<Integer,Call> mCalls = new HashMap<>();
    private final Listener<ChannelEvent> mTeardownMonitor = this::processChannelEvent;
    private final Listener<Heartbeat> mHeartbeatListener = heartbeat -> checkCallEnds();
    private Listener<ChannelEvent> mChannelEventListener;
    private Listener<IDecodeEvent> mDecodeEventListener;
    private LongSupplier mClock = System::currentTimeMillis;
    private long mLastCheck;
    private DecodeConfigEDACS mConfig;

    /**
     * One call on one voice channel
     */
    private static class Call
    {
        final DecodeEvent mEvent;
        long mLastUpdate;

        Call(DecodeEvent event, long now)
        {
            mEvent = event;
            mLastUpdate = now;
        }
    }

    public EDACSTrafficChannelManager(Channel parentChannel)
    {
        List<Channel> channels = new ArrayList<>();

        if(parentChannel.getDecodeConfiguration() instanceof DecodeConfigEDACS config)
        {
            for(int x = 0; x < config.getTrafficChannelPoolSize(); x++)
            {
                Channel trafficChannel = new Channel("T-" + parentChannel.getName(), Channel.ChannelType.TRAFFIC);
                trafficChannel.setAliasListName(parentChannel.getAliasListName());
                trafficChannel.setSystem(parentChannel.getSystem());
                trafficChannel.setSite(parentChannel.getSite());
                trafficChannel.setDecodeConfiguration(config.copyForTrafficChannel(DecodeConfigEDACS.VoiceMode.PROVOICE));
                mConfig = config;
                trafficChannel.setEventLogConfiguration(parentChannel.getEventLogConfiguration());
                trafficChannel.setRecordConfiguration(parentChannel.getRecordConfiguration());
                channels.add(trafficChannel);
            }
        }

        mAvailableTrafficChannels.addAll(channels);
        mManagedTrafficChannels = Collections.unmodifiableList(channels);
    }

    /**
     * Replaces the clock - for testing only
     */
    void setClock(LongSupplier clock)
    {
        mClock = clock;
    }

    /**
     * Processes a channel grant or a grant update from the control channel.
     *
     * @param channel granted
     * @param identifiers talkgroup (TO) and, for a new grant, the calling radio (FROM)
     * @param type of call for the event
     * @param details for the event, or null
     * @param follow false for a call that is not to be listened to (encrypted)
     * @param timestamp of the OSW
     */
    public synchronized void processGrant(EDACSChannel channel, IdentifierCollection identifiers,
                                          DecodeEventType type, String details, boolean follow,
                                          DecodeConfigEDACS.VoiceMode mode, long timestamp)
    {
        int key = channel.getLCN();
        long now = mClock.getAsLong();
        Call call = mCalls.get(key);

        if(call != null)
        {
            if(Objects.equals(getTo(call.mEvent.getIdentifierCollection()), getTo(identifiers)))
            {
                call.mLastUpdate = now;
                call.mEvent.end(timestamp);

                //A late-joined call (updates only) learns its calling radio from a later grant
                if(getFrom(call.mEvent.getIdentifierCollection()) == null && getFrom(identifiers) != null)
                {
                    call.mEvent.setIdentifierCollection(identifiers);
                    broadcast(call.mEvent);
                }
                return;
            }

            //Another talkgroup on this channel - the previous call is over
            endCall(key, call, "another talkgroup granted on the channel");
        }

        //A talkgroup is on one voice channel at a time: a new grant for it on another channel means its previous
        //transmission is over.  Close that voice channel now instead of 3 s later - while both were open, SDRTrunk's
        //duplicate call detection would suppress the new transmission (seen on Motorola Type II).
        Identifier to = getTo(identifiers);

        if(to != null)
        {
            for(Map.Entry<Integer,Call> entry : new ArrayList<>(mCalls.entrySet()))
            {
                if(entry.getKey() != key && to.equals(getTo(entry.getValue().mEvent.getIdentifierCollection())))
                {
                    endCall(entry.getKey(), entry.getValue(), "talkgroup granted on " + channel);
                }
            }
        }

        DecodeEvent event = DecodeEvent.builder(type, timestamp)
            .channel(channel)
            .identifiers(identifiers)
            .details(details)
            .protocol(Protocol.EDACS)
            .build();
        mCalls.put(key, new Call(event, now));

        if(!follow)
        {
            //Not followed
        }
        else if(channel.getDownlinkFrequency() <= 0)
        {
            event.setDetails(append(details, "NO FREQUENCY FOR LCN " + key + " IN THE LCN LIST"));
            LOGGER.info("EDACS call on LCN " + key + " not followed - no frequency for it in the LCN list");
        }
        else if(channel.getDownlinkFrequency() == getCurrentControlFrequency())
        {
            event.setDetails(append(details, "GRANT IS FOR THE CONTROL CHANNEL"));
        }
        else
        {
            Channel trafficChannel = mAvailableTrafficChannels.poll();

            if(trafficChannel == null)
            {
                event.setDetails(append(details, MAX_TRAFFIC_CHANNELS_EXCEEDED));
                LOGGER.info("EDACS call on " + channel + " not followed - all " +
                    mManagedTrafficChannels.size() + " voice channels in use");
            }
            else
            {
                SourceConfigTuner sourceConfig = new SourceConfigTuner();
                sourceConfig.setFrequency(channel.getDownlinkFrequency());
                trafficChannel.setSourceConfiguration(sourceConfig);
                if(mConfig != null)
                {
                    trafficChannel.setDecodeConfiguration(mConfig.copyForTrafficChannel(mode));
                }
                mAllocatedTrafficChannels.put(key, trafficChannel);
                getInterModuleEventBus().post(new ChannelStartProcessingRequest(trafficChannel, channel, identifiers));
            }
        }

        if(LOGGER.isDebugEnabled())
        {
            LOGGER.debug("EDACS call start " + channel + " TO:" + getTo(identifiers) + " FROM:" +
                getFrom(identifiers) + " " + type + (event.getDetails() != null ? " [" + event.getDetails() + "]" : "") +
                (mAllocatedTrafficChannels.containsKey(key) ? " - voice channel started" : " - not followed"));
        }

        broadcast(event);
    }

    /**
     * Ends calls whose updates have stopped.  Called on the control channel's heartbeat.
     */
    public synchronized void checkCallEnds()
    {
        long now = mClock.getAsLong();

        if(now - mLastCheck < HEARTBEAT_INTERVAL_MS)
        {
            return;
        }

        mLastCheck = now;
        Iterator<Map.Entry<Integer,Call>> it = mCalls.entrySet().iterator();

        while(it.hasNext())
        {
            Map.Entry<Integer,Call> entry = it.next();

            if(now - entry.getValue().mLastUpdate > CALL_END_MS)
            {
                it.remove();
                finishCall(entry.getKey(), entry.getValue(), "updates stopped");
            }
        }
    }

    private void endCall(int key, Call call, String reason)
    {
        mCalls.remove(key);
        finishCall(key, call, reason);
    }

    private void finishCall(int key, Call call, String reason)
    {
        if(LOGGER.isDebugEnabled())
        {
            LOGGER.debug("EDACS call end " + call.mEvent.getChannelDescriptor() + " TO:" +
                getTo(call.mEvent.getIdentifierCollection()) + " after " +
                (call.mEvent.getDuration() / 1000.0) + " s - " + reason +
                (mAllocatedTrafficChannels.containsKey(key) ? " - voice channel stopped" : ""));
        }

        broadcast(call.mEvent);
        Channel trafficChannel = mAllocatedTrafficChannels.remove(key);

        if(trafficChannel != null)
        {
            //Returned to the pool when it reports that processing stopped
            broadcast(new ChannelEvent(trafficChannel, ChannelEvent.Event.REQUEST_DISABLE));
        }
    }

    /**
     * Number of calls in progress - for testing
     */
    synchronized int getCallCount()
    {
        return mCalls.size();
    }

    /**
     * Number of voice channels in use - for testing
     */
    synchronized int getAllocatedCount()
    {
        return mAllocatedTrafficChannels.size();
    }

    /**
     * Sees channel events for every channel in the application, delivered by the channel processing manager while it
     * holds its own lock.  Events for other channels return before taking this manager's lock (as the P25 manager
     * does): taking it first deadlocked a P25 channel thread starting a P25 voice channel against the EDACS control
     * channel thread starting an EDACS voice channel (Cambridge, 2026-11-10 08:45).
     */
    private void processChannelEvent(ChannelEvent channelEvent)
    {
        Channel channel = channelEvent.getChannel();

        if(!channel.isTrafficChannel() || !mManagedTrafficChannels.contains(channel))
        {
            return;
        }

        processManagedChannelEvent(channelEvent);
    }

    private synchronized void processManagedChannelEvent(ChannelEvent channelEvent)
    {
        Channel channel = channelEvent.getChannel();

        switch(channelEvent.getEvent())
        {
            case NOTIFICATION_PROCESSING_STOP ->
            {
                Integer key = keyOf(channel);

                if(key != null)
                {
                    //Stopped by itself (call time limit or tuner) - the call stays listed until its updates stop, but
                    //is not restarted
                    mAllocatedTrafficChannels.remove(key);
                }

                release(channel);
            }
            case NOTIFICATION_PROCESSING_START_REJECTED ->
            {
                Integer key = keyOf(channel);

                if(key != null)
                {
                    mAllocatedTrafficChannels.remove(key);
                    Call call = mCalls.get(key);

                    if(call != null)
                    {
                        String reason = channelEvent.getDescription() != null ? channelEvent.getDescription() :
                            CHANNEL_START_REJECTED;
                        call.mEvent.setDetails(append(call.mEvent.getDetails(), reason));
                        LOGGER.info("EDACS voice channel " + call.mEvent.getChannelDescriptor() +
                            " could not start - " + reason);
                        broadcast(call.mEvent);
                    }
                }

                release(channel);
            }
            default ->
            {
            }
        }
    }

    private void release(Channel channel)
    {
        if(!mAvailableTrafficChannels.contains(channel))
        {
            mAvailableTrafficChannels.add(channel);
        }
    }

    private Integer keyOf(Channel channel)
    {
        for(Map.Entry<Integer,Channel> entry : mAllocatedTrafficChannels.entrySet())
        {
            if(entry.getValue() == channel)
            {
                return entry.getKey();
            }
        }

        return null;
    }

    private static String append(String details, String text)
    {
        return details == null || details.isEmpty() ? text : details + " - " + text;
    }

    private static Identifier getTo(IdentifierCollection collection)
    {
        List<Identifier> identifiers = collection.getIdentifiers(Role.TO);
        return identifiers.isEmpty() ? null : identifiers.get(0);
    }

    private static Identifier getFrom(IdentifierCollection collection)
    {
        List<Identifier> identifiers = collection.getIdentifiers(Role.FROM);
        return identifiers.isEmpty() ? null : identifiers.get(0);
    }

    @Override
    protected synchronized void processControlFrequencyUpdate(long previous, long current, Channel channel)
    {
        for(Map.Entry<Integer,Call> entry : new ArrayList<>(mCalls.entrySet()))
        {
            if(entry.getValue().mEvent.getChannelDescriptor() != null &&
                entry.getValue().mEvent.getChannelDescriptor().getDownlinkFrequency() == current)
            {
                endCall(entry.getKey(), entry.getValue(), "granted on the control channel frequency");
            }
        }
    }

    private void broadcast(DecodeEvent event)
    {
        if(mDecodeEventListener != null)
        {
            mDecodeEventListener.receive(event);
        }
    }

    private void broadcast(ChannelEvent channelEvent)
    {
        if(mChannelEventListener != null)
        {
            mChannelEventListener.receive(channelEvent);
        }
    }

    @Override
    public Listener<Heartbeat> getHeartbeatListener()
    {
        return mHeartbeatListener;
    }

    @Override
    public Listener<ChannelEvent> getChannelEventListener()
    {
        return mTeardownMonitor;
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
    public synchronized void stop()
    {
        for(Map.Entry<Integer,Call> entry : new ArrayList<>(mCalls.entrySet()))
        {
            endCall(entry.getKey(), entry.getValue(), "control channel stopped");
        }

        mAvailableTrafficChannels.clear();
    }
}
