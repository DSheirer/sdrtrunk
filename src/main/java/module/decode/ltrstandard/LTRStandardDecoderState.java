/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package module.decode.ltrstandard;

import alias.Alias;
import alias.AliasList;
import alias.id.AliasIDType;
import channel.metadata.AliasedStringAttributeMonitor;
import channel.metadata.Attribute;
import channel.metadata.AttributeChangeRequest;
import channel.state.DecoderState;
import channel.state.DecoderStateEvent;
import channel.state.DecoderStateEvent.Event;
import channel.state.State;
import message.Message;
import module.decode.DecoderType;
import module.decode.event.CallEvent.CallEventType;
import module.decode.ltrnet.LTRCallEvent;
import module.decode.ltrstandard.message.CallEndMessage;
import module.decode.ltrstandard.message.CallMessage;
import module.decode.ltrstandard.message.IdleMessage;
import module.decode.ltrstandard.message.LTRStandardMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

public class LTRStandardDecoderState extends DecoderState
{
    private final static Logger mLog =
        LoggerFactory.getLogger(LTRStandardDecoderState.class);

    private Map<Integer,LTRCallEvent> mActiveCalls = new HashMap<>();
    private Set<String> mTalkgroupsFirstHeard = new HashSet<>();
    private Set<String> mTalkgroups = new TreeSet<>();

    private AliasedStringAttributeMonitor mTalkgroupAttribute;
    private long mFrequency;
    private LCNTracker mLCNTracker = new LCNTracker();

    public LTRStandardDecoderState(AliasList aliasList)
    {
        super(aliasList);

        mTalkgroupAttribute = new AliasedStringAttributeMonitor(Attribute.PRIMARY_ADDRESS_TO,
            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.LTR_STANDARD;
    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop()
    {
    }

    @Override
    public void receive(Message message)
    {
        if(message.isValid() && message instanceof LTRStandardMessage)
        {
            switch(((LTRStandardMessage) message).getMessageType())
            {
                case CA_STRT:
                    CallMessage start = (CallMessage) message;

                    int channel = start.getChannel();

                    setChannelNumber(channel);

                    mLCNTracker.processFreeChannel(start.getFree());

                    //Only process calls on this LCN, or call detects for
                    //talkgroups that are homed on this LCN
                    if(mLCNTracker.isValidChannel(channel) &&
                        (mLCNTracker.isCurrentChannel(channel) ||
                            mLCNTracker.isCurrentChannel(start.getHomeRepeater())))
                    {
                        LTRCallEvent event = mActiveCalls.get(channel);

                        if(event == null || !event.isMatchingTalkgroup(start.getToID()))
                        {
                            //Check for different talkgroup
                            if(event != null)
                            {
                                event.end();
                                mActiveCalls.remove(channel);
                            }

                            boolean current = mLCNTracker.isCurrentChannel(channel);

                            event = new LTRCallEvent.Builder(
                                DecoderType.LTR_STANDARD,
                                current ? CallEventType.CALL : CallEventType.CALL_DETECT)
                                .to(start.getToID())
                                .aliasList(getAliasList())
                                .channel(start.getChannelFormatted())
                                .frequency(current ? mFrequency : 0)
                                .build();

                            mActiveCalls.put(channel, event);

                            broadcast(event);

                            if(mLCNTracker.isCurrentChannel(channel))
                            {
                                String talkgroup = start.getToID();
                                mTalkgroupAttribute.process(talkgroup);
                                processTalkgroup(talkgroup);
                            }
                        }

                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
                    }
                    break;
                case CA_ENDD:
                    CallEndMessage end = (CallEndMessage) message;

                    //Home channel is 31 for call end -- use the free channel
                    //as the call end channel
                    int repeater = end.getFree();

                    setChannelNumber(repeater);

                    if(mLCNTracker.isCurrentChannel(repeater))
                    {
                        String talkgroup = end.getToID();
                        mTalkgroupAttribute.process(talkgroup);
                        processTalkgroup(talkgroup);

                        LTRCallEvent event = mActiveCalls.remove(repeater);

                        if(event != null)
                        {
                            event.end();
                            broadcast(event);
                            broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
                        }
                    }
                    break;
                case SY_IDLE:
                    IdleMessage idle = (IdleMessage) message;

                    int lcn = idle.getChannel();

                    mLCNTracker.processCallChannel(lcn);

                    break;
                case UN_KNWN:
                default:
                    break;
            }
        }
    }

    private void processTalkgroup(String talkgroup)
    {
        if(mTalkgroupsFirstHeard.contains(talkgroup))
        {
            mTalkgroups.add(talkgroup);
        }
        else
        {
            mTalkgroupsFirstHeard.add(talkgroup);
        }
    }

    /**
     * Performs a full reset
     */
    public void reset()
    {
        mActiveCalls.clear();
        mTalkgroupsFirstHeard.clear();
        mTalkgroups.clear();
        mLCNTracker.reset();

        resetState();
    }

    /**
     * Performs a temporal reset following a call or other decode event
     */
    private void resetState()
    {
        for(Integer key : mActiveCalls.keySet())
        {
            LTRCallEvent event = mActiveCalls.get(key);

            if(event != null)
            {
                event.end();

                broadcast(event);
            }
        }

        mActiveCalls.clear();

        mTalkgroupAttribute.reset();
    }

    public boolean hasChannelNumber()
    {
        return mLCNTracker.getCurrentChannel() != 0;
    }

    public int getChannelNumber()
    {
        return mLCNTracker.getCurrentChannel();
    }

    private void setChannelNumber(int channel)
    {
        int original = mLCNTracker.getCurrentChannel();

        mLCNTracker.processCallChannel(channel);

        if(mLCNTracker.getCurrentChannel() != original)
        {
            broadcast(new AttributeChangeRequest<String>(Attribute.CHANNEL_FREQUENCY_LABEL,
                "LCN:" + mLCNTracker.getCurrentChannel()));
        }
    }

    @Override
    public void init()
    {
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case RESET:
                resetState();
                break;
            case SOURCE_FREQUENCY:
                mFrequency = event.getFrequency();
                break;
            default:
                break;
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary\n\n");

        sb.append("Decoder:\tLTR-Standard\n");

        sb.append("Monitored LCN: ");

        if(hasChannelNumber())
        {
            sb.append(getChannelNumber());
        }
        else
        {
            sb.append("*Insufficient Data*");
        }

        sb.append("\n");

        sb.append("Active LCNs:\t");

        List<Integer> lcns = mLCNTracker.getActiveLCNs();

        if(lcns.size() > 0)
        {
            sb.append(mLCNTracker.getActiveLCNs());
        }
        else
        {
            sb.append("*Insufficient Data*");
        }
        sb.append("\n\n");

        sb.append("Talkgroups\n");

        if(mTalkgroups.isEmpty())
        {
            sb.append("  None\n");
        }
        else
        {
            for(String talkgroup : mTalkgroups)
            {
                sb.append("  ");
                sb.append(talkgroup);
                sb.append(" ");

                if(hasAliasList())
                {
                    Alias alias = getAliasList().getTalkgroupAlias(talkgroup);

                    if(alias != null)
                    {
                        sb.append(alias.getName());
                    }
                }

                sb.append("\n");

            }
        }

        return sb.toString();
    }

    /**
     * Tracks the set of call and free channels for a system in order to
     * dynamically determine the current LCN and minimize false triggers from
     * bogus decoded LTR messages.  Tracks the number of occurances of each
     * LCN and uses a dynamic threshold to determine LCN validity.
     */
    public class LCNTracker
    {
        private static final int DEFAULT_COUNT = 10;
        private int[] mCallLCNCounts;
        private int[] mFreeLCNCounts;
        private int mCallHighestCount;
        private int mFreeHighestCount;
        private int mCurrentLCN;

        public LCNTracker()
        {
            reset();
        }

        public void logStatistics()
        {
            for(int x = 1; x <= 20; x++)
            {
                mLog.debug("Call " + x + ": " + mCallLCNCounts[x]);
            }
            mLog.debug("Call Highest Count: " + mCallHighestCount);
            for(int x = 1; x <= 20; x++)
            {
                mLog.debug("Free " + x + ": " + mFreeLCNCounts[x]);
            }
            mLog.debug("Free Highest Count: " + mFreeHighestCount);

            mLog.debug("Current LCN: " + mCurrentLCN);
        }

        public void reset()
        {
            //Dim channel arrays to 21 -- ignore the 0 index
            mCallLCNCounts = new int[21];
            mFreeLCNCounts = new int[21];
            mCallHighestCount = 3;
            mFreeHighestCount = DEFAULT_COUNT;
            mCurrentLCN = 0;
        }

        public boolean isCurrentChannel(int channel)
        {
            if(mCurrentLCN == 0)
            {
                return true;
            }
            else
            {
                return channel == mCurrentLCN;
            }
        }

        public int getCurrentChannel()
        {
            return mCurrentLCN;
        }

        /**
         * Indicates if the channel is valid based on the observed current and
         * free channels reported by the system.  Tracks the count of each
         * reported free channel the highest free channel count is used as a
         * threshold where a channel is valid when its count exceeds 20% of the
         * highest free channel count.  A channel is also valid when it is
         * the currently monitored channel.
         */
        public boolean isValidChannel(int channel)
        {
            if(isCurrentChannel(channel))
            {
                return true;
            }

            int count = mFreeLCNCounts[channel];

            int threshold = (int) ((double) mFreeHighestCount * 0.2);

            return count >= threshold;
        }

        public void processCallChannel(int channel)
        {
            if(1 <= channel && channel <= 20)
            {
                mCallLCNCounts[channel]++;

                if(mCallLCNCounts[channel] > mCallHighestCount)
                {
                    mCallHighestCount = mCallLCNCounts[channel];
                    mCurrentLCN = channel;
                }
            }
        }

        public void processFreeChannel(int channel)
        {
            if(1 <= channel && channel <= 20)
            {
                mFreeLCNCounts[channel]++;

                if(mFreeLCNCounts[channel] > mFreeHighestCount)
                {
                    mFreeHighestCount = mFreeLCNCounts[channel];
                }
            }
        }

        public List<Integer> getActiveLCNs()
        {
            List<Integer> active = new ArrayList<>();

            if(mFreeHighestCount > DEFAULT_COUNT)
            {
                for(int x = 1; x <= 20; x++)
                {
                    if(mCurrentLCN != 0 && isCurrentChannel(x))
                    {
                        active.add(x);
                    }
                    else if(isValidChannel(x))
                    {
                        active.add(x);
                    }
                }
            }

            return active;
        }
    }
}