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
package io.github.dsheirer.module.decode.ltrstandard;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.identifier.talkgroup.LTRTalkgroup;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.ltrstandard.channel.LtrChannel;
import io.github.dsheirer.module.decode.ltrstandard.message.Call;
import io.github.dsheirer.module.decode.ltrstandard.message.CallEnd;
import io.github.dsheirer.module.decode.ltrstandard.message.Idle;
import io.github.dsheirer.module.decode.ltrstandard.message.LTRMessage;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LTRStandardDecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(LTRStandardDecoderState.class);

    private Map<Integer,DecodeEvent> mActiveCalls = new HashMap<>();
    private Set<LTRTalkgroup> mTalkgroupsFirstHeard = new HashSet<>();
    private Set<LTRTalkgroup> mTalkgroups = new TreeSet<>();
    private LCNTracker mLCNTracker = new LCNTracker();
    private DecodeEvent mCurrentCallEvent;
    private LTRTalkgroup mCurrentTalkgroup;

    public LTRStandardDecoderState()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.LTR;
    }

    @Override
    public void receive(IMessage message)
    {
        if(message.isValid() && message instanceof LTRMessage)
        {
            switch(((LTRMessage)message).getMessageType())
            {
                case CALL:
                    if(message instanceof Call start)
                    {
                        int channel = start.getChannel();
                        setChannelNumber(channel);
                        mLCNTracker.processFreeChannel(start.getFree());

                        //Only process calls or call detects for valid channels
                        if(mLCNTracker.isValidChannel(channel) && mLCNTracker.isCurrentChannel(channel))
                        {
                            if(mCurrentTalkgroup == null || !mCurrentTalkgroup.equals(start.getTalkgroup()))
                            {
                                mCurrentTalkgroup = start.getTalkgroup();
                                getIdentifierCollection().remove(IdentifierClass.USER);
                                getIdentifierCollection().update(start.getTalkgroup());
                                mCurrentCallEvent = LTRStandardDecodeEvent.builder(DecodeEventType.CALL, start.getTimestamp())
                                    .identifiers(getIdentifierCollection().copyOf())
                                    .channel(getCurrentChannel())
                                    .build();
                            }
                            else
                            {
                                mCurrentCallEvent.update(start.getTimestamp());
                            }
                            broadcast(mCurrentCallEvent);
                            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
                        }
                    }
                    break;
                case CALL_END:
                    if(message instanceof CallEnd end)
                    {
                        mCurrentTalkgroup = null;

                        //Home channel is 31 for call end -- use the free channel as the call end channel
                        int repeater = end.getFree();
                        setChannelNumber(repeater);
                        if(mLCNTracker.isCurrentChannel(repeater))
                        {
                            if(mCurrentCallEvent != null)
                            {
                                mCurrentCallEvent.end(end.getTimestamp());
                            }

                            broadcast(new DecoderStateEvent(this, Event.END, State.IDLE));
                        }
                    }
                    break;
                case IDLE:
                    if(message instanceof Idle idle)
                    {
                        mCurrentTalkgroup = null;
                        mLCNTracker.processCallChannel(idle.getChannel());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Performs a full reset
     */
    public void reset()
    {
        super.reset();
        mActiveCalls.clear();
        mTalkgroupsFirstHeard.clear();
        mTalkgroups.clear();
        mLCNTracker.reset();

        resetState();
    }

    @Override
    public void init() {}

    /**
     * Performs a temporal reset following a call or other decode event
     */
    protected void resetState()
    {
        super.resetState();
        mActiveCalls.clear();
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
            getIdentifierCollection().update(DecoderLogicalChannelNameIdentifier
                .create(String.valueOf(mLCNTracker.getCurrentChannel()), Protocol.LTR));

            LtrChannel ltrChannel = new LtrChannel(mLCNTracker.getCurrentChannel());

            Identifier identifier = getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION,
                Form.CHANNEL_FREQUENCY, Role.ANY);

            if(identifier instanceof FrequencyConfigurationIdentifier)
            {
                ltrChannel.setDownlink(((FrequencyConfigurationIdentifier)identifier).getValue());
            }

            setCurrentChannel(ltrChannel);
        }
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET:
                resetState();
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
            for(LTRTalkgroup talkgroup : mTalkgroups)
            {
                sb.append("  ");
                sb.append(talkgroup.formatted());
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

            int threshold = (int)((double)mFreeHighestCount * 0.2);

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