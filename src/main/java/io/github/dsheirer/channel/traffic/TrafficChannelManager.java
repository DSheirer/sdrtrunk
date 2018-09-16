/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.channel.traffic;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventListener;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelEvent.Event;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.TrafficChannelEvent;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.event.CallEvent;
import io.github.dsheirer.module.decode.event.CallEvent.CallEventType;
import io.github.dsheirer.module.decode.event.ICallEventProvider;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficChannelManager extends Module implements ICallEventProvider, IDecoderStateEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(TrafficChannelManager.class);
    public static final String CHANNEL_START_REJECTED = "CHANNEL START REJECTED";
    public static final String NO_TUNER_AVAILABLE = "NO TUNER AVAILABLE";
    public static final String UNKNOWN_FREQUENCY = "UNKNOWN FREQUENCY";

    private int mTrafficChannelPoolMaximumSize = DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT;
    private List<Channel> mTrafficChannelPool = new ArrayList<Channel>();
    private Map<String,Channel> mTrafficChannelsInUse = new ConcurrentHashMap<String,Channel>();

    private DecoderStateEventListener mEventListener = new DecoderStateEventListener();
    private Listener<CallEvent> mCallEventListener;

    private ChannelModel mChannelModel;
    private DecodeConfiguration mDecodeConfiguration;
    private RecordConfiguration mRecordConfiguration;
    private String mSystem;
    private String mSite;
    private String mAliasListName;

    /**
     * Monitors call events and allocates traffic decoder channels in response
     * to traffic channel allocation call events.  Manages a pool of reusable
     * traffic channel allocations.
     *
     * @param channelModel containing channels currently in use
     * @param decodeConfiguration - decoder configuration to use for each
     * traffic channel allocation.
     * @param recordConfiguration - recording options for each traffic channel
     * @param system label to use to describe the system
     * @param site label to use to describe the site
     * @param aliasListName designated for the channel
     * @param trafficChannelPoolSize - maximum number of allocated traffic channels
     * in the pool
     */
    public TrafficChannelManager(ChannelModel channelModel,
                                 DecodeConfiguration decodeConfiguration,
                                 RecordConfiguration recordConfiguration,
                                 String system,
                                 String site,
                                 String aliasListName,
                                 int trafficChannelPoolSize)
    {
        mChannelModel = channelModel;
        mDecodeConfiguration = decodeConfiguration;
        mRecordConfiguration = recordConfiguration;
        mSystem = system;
        mSite = site;
        mAliasListName = aliasListName;
        mTrafficChannelPoolMaximumSize = trafficChannelPoolSize;
    }

    @Override
    public void dispose()
    {
        for(Channel trafficChannel : mTrafficChannelPool)
        {
            mChannelModel.broadcast(new ChannelEvent(trafficChannel, Event.REQUEST_DISABLE));
        }

        mTrafficChannelPool.clear();

        mTrafficChannelsInUse.clear();

        mCallEventListener = null;
        mDecodeConfiguration = null;
    }

    /**
     * Provides a channel by either reusing an existing channel or constructing
     * a new one, limited by the total number of constructed channels allowed.
     *
     * Note: you must enforce thread safety on the mTrafficChannelsInUse
     * external to this method.
     *
     * @param channelNumber
     * @param tunerChannel
     * @return
     */
    private Channel getChannel(String channelNumber, TunerChannel tunerChannel)
    {
        Channel channel = null;

        if(!mTrafficChannelsInUse.containsKey(channelNumber))
        {
            for(Channel configuredChannel : mTrafficChannelPool)
            {
                if(!configuredChannel.isProcessing())
                {
                    channel = configuredChannel;
                    break;
                }
            }

            if(channel == null && mTrafficChannelPool.size() < mTrafficChannelPoolMaximumSize)
            {
                channel = new Channel("Traffic", ChannelType.TRAFFIC);

                channel.setDecodeConfiguration(mDecodeConfiguration);

                channel.setRecordConfiguration(mRecordConfiguration);

                channel.setAliasListName(mAliasListName);

                mChannelModel.addChannel(channel);

                mTrafficChannelPool.add(channel);
            }

            /* If we have a configured channel, update metadata */
            if(channel != null)
            {
                channel.setSourceConfiguration(new SourceConfigTuner(tunerChannel));
                channel.setSystem(mSystem);
                channel.setSite(mSite);
                channel.setName(channelNumber);

                mChannelModel.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_CONFIGURATION_CHANGE));
            }
        }

        return channel;
    }

    /**
     * Processes the event and creates a traffic channel is resources are
     * available
     */
    private void process(TrafficChannelAllocationEvent event)
    {
        CallEvent callEvent = event.getCallEvent();

        /* Check for duplicate events and suppress */
        synchronized(mTrafficChannelsInUse)
        {
            if(mTrafficChannelsInUse.containsKey(callEvent.getChannel()))
            {
                return;
            }

            long frequency = callEvent.getFrequency();

            if(frequency > 0)
            {
                Channel channel = getChannel(callEvent.getChannel(), new TunerChannel(frequency,
                    mDecodeConfiguration.getChannelSpecification().getBandwidth()));

                if(channel != null)
                {
                    TrafficChannelEvent trafficChannelEvent =
                        new TrafficChannelEvent(this, channel, Event.REQUEST_ENABLE, callEvent);

                    //Request to enable the channel
                    mChannelModel.broadcast(trafficChannelEvent);

                    if(channel.isProcessing())
                    {
                        mTrafficChannelsInUse.put(callEvent.getChannel(), channel);
                    }
                    else
                    {
                        callEvent.setCallEventType(CallEventType.CALL_DETECT);

                        String details = callEvent.getDetails();

                        if(details == null || details.isEmpty())
                        {
                            callEvent.setDetails(CHANNEL_START_REJECTED);
                        }
                        else if(!details.contains(CHANNEL_START_REJECTED))
                        {
                            callEvent.setDetails(new StringBuilder(CHANNEL_START_REJECTED).append(" : ")
                                .append(callEvent.getDetails()).toString());
                        }
                    }
                }
                else
                {
                    callEvent.setCallEventType(CallEventType.CALL_DETECT);

                    String details = callEvent.getDetails();

                    if(details == null || details.isEmpty())
                    {
                        callEvent.setDetails(NO_TUNER_AVAILABLE);
                    }
                    else if(!details.contains(NO_TUNER_AVAILABLE))
                    {
                        callEvent.setDetails(new StringBuilder(NO_TUNER_AVAILABLE).append(" : ")
                            .append(callEvent.getDetails()).toString());
                    }
                }
            }
            else
            {
                callEvent.setCallEventType(CallEventType.CALL_DETECT);

                String details = callEvent.getDetails();

                if(details == null || details.isEmpty())
                {
                    callEvent.setDetails(UNKNOWN_FREQUENCY);
                }
                else if(!details.contains(UNKNOWN_FREQUENCY))
                {
                    callEvent.setDetails(new StringBuilder(UNKNOWN_FREQUENCY).append(" : ")
                        .append(callEvent.getDetails()).toString());
                }
            }

            final Listener<CallEvent> listener = mCallEventListener;

            if(listener != null)
            {
                listener.receive(callEvent);
            }
        }
    }

    /**
     * Compares the call type, channel and to fields for equivalence and the
     * from field for either both null, or equivalence.
     *
     * @param e1
     * @param e2
     * @return
     */
    public static boolean isSameCallEvent(CallEvent e1, CallEvent e2)
    {
        if(e1 == null || e2 == null)
        {
            return false;
        }

        if(e1.getCallEventType() != e2.getCallEventType())
        {
            return false;
        }

        if(e1.getChannel() == null || e2.getChannel() == null || !e1.getChannel().contentEquals(e2.getChannel()))
        {
            return false;
        }

        if(e1.getToID() == null || e2.getToID() == null || !e1.getToID().contentEquals(e2.getToID()))
        {
            return false;
        }

        if(e1.getFromID() == null || e2.getFromID() == null)
        {
            return (e1.getFromID() == null && e2.getFromID() == null);
        }
        else if(e1.getFromID().contentEquals(e2.getFromID()))
        {
            return true;
        }

        return false;
    }

    @Override
    public Listener<DecoderStateEvent> getDecoderStateListener()
    {
        return mEventListener;
    }

    @Override
    public void addCallEventListener(Listener<CallEvent> listener)
    {
        mCallEventListener = listener;
    }

    @Override
    public void removeCallEventListener(Listener<CallEvent> listener)
    {
        mCallEventListener = null;
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
        if(!mTrafficChannelsInUse.isEmpty())
        {
            List<String> channels = new ArrayList<>();

            /* Copy the keyset so we don't get concurrent modification of the map */
            channels.addAll(mTrafficChannelsInUse.keySet());

            for(String channel : channels)
            {
                callEnd(channel);
            }
        }
    }

    /**
     * Callback used by the TrafficChannelStatusListener class to signal the
     * end of a an allocated traffic channel call event
     *
     * @param channelNumber - channel number from the call event that signaled
     * the start of a traffic channel allocation
     */
    public void callEnd(String channelNumber)
    {
        synchronized(mTrafficChannelsInUse)
        {
            if(channelNumber != null && mTrafficChannelsInUse.containsKey(channelNumber))
            {
                Channel channel = mTrafficChannelsInUse.get(channelNumber);

                mChannelModel.broadcast(new ChannelEvent(channel, Event.REQUEST_DISABLE));

                mTrafficChannelsInUse.remove(channelNumber);
            }
        }
    }

    /**
     * Wrapper class for the decoder state event listener interface to catch
     * traffic channel allocation requests
     */
    public class DecoderStateEventListener implements Listener<DecoderStateEvent>
    {
        @Override
        public void receive(DecoderStateEvent event)
        {
            switch(event.getEvent())
            {
                case TRAFFIC_CHANNEL_ALLOCATION:
                    process((TrafficChannelAllocationEvent) event);
                    break;
                default:
                    break;
            }
        }
    }
}