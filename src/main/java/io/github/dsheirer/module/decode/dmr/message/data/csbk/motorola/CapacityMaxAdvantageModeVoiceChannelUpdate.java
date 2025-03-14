/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.dmr.channel.DMRTier3Channel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Max Advantage Mode - Voice Channel Update (Opcode 34 / 0x22)
 * Note: it appears that Capacity Max Talkgroups in advantage mode are only 10-bits long in range of 0 - 1023.
 */
public class CapacityMaxAdvantageModeVoiceChannelUpdate extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] CHANNEL_1 = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] TALKGROUP_CH1_TS1 = new int[]{28, 29, 30, 31, 32, 33, 34, 35, 36, 37};
    private static final int[] TALKGROUP_CH1_TS2 = new int[]{38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] CHANNEL_2 = new int[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59};
    private static final int[] TALKGROUP_CH2_TS1 = new int[]{60, 61, 62, 63, 64, 65, 66, 67, 68, 69};
    private static final int[] TALKGROUP_CH2_TS2 = new int[]{70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private TalkgroupIdentifier mTalkgroupCH1TS1;
    private TalkgroupIdentifier mTalkgroupCH1TS2;
    private TalkgroupIdentifier mTalkgroupCH2TS1;
    private TalkgroupIdentifier mTalkgroupCH2TS2;
    private DMRTier3Channel mChannel1TS1;
    private DMRTier3Channel mChannel1TS2;
    private DMRTier3Channel mChannel2TS1;
    private DMRTier3Channel mChannel2TS2;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public CapacityMaxAdvantageModeVoiceChannelUpdate(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        if(hasRAS())
        {
            sb.append(" RAS:").append(getBPTCReservedBits());
        }

        sb.append(" CSBK CAP-MAX MODE:ADVANTAGE CHANNEL UPDATE");

        if(hasChannel1Timeslot1())
        {
            sb.append(" TG1:").append(getTalkgroupCH1TS1());
            sb.append(" ON").append(getChannel1TS1());
        }
        if(hasChannel1Timeslot2())
        {
            sb.append(" TG2:").append(getTalkgroupCH1TS2());
            sb.append(" ON").append(getChannel1TS2());
        }

        if(hasChannel2Timeslot1())
        {
            sb.append(" TG3:").append(getTalkgroupCH1TS1());
            sb.append(" ON").append(getChannel2TS1());
        }
        if(hasChannel2Timeslot2())
        {
            sb.append(" TG4:").append(getTalkgroupCH1TS2());
            sb.append(" ON").append(getChannel2TS2());
        }

        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Indicates if this message contains activity on channel 1 timeslot 1.
     */
    public boolean hasChannel1Timeslot1()
    {
        return getMessage().getInt(TALKGROUP_CH1_TS1) > 0;
    }

    /**
     * Indicates if this message contains activity on channel 1 timeslot 2.
     */
    public boolean hasChannel1Timeslot2()
    {
        return getMessage().getInt(TALKGROUP_CH1_TS2) > 0;
    }

    /**
     * Indicates if this message contains activity on channel 2 timeslot 1.
     */
    public boolean hasChannel2Timeslot1()
    {
        return getMessage().getInt(TALKGROUP_CH2_TS1) > 0;
    }

    /**
     * Indicates if this message contains activity on channel 2 timeslot 2.
     */
    public boolean hasChannel2Timeslot2()
    {
        return getMessage().getInt(TALKGROUP_CH2_TS2) > 0;
    }

    /**
     * Talkgroup that is active on channel 1 timeslot 1.
     */
    public TalkgroupIdentifier getTalkgroupCH1TS1()
    {
        if(mTalkgroupCH1TS1 == null)
        {
            mTalkgroupCH1TS1 = DMRTalkgroup.create(getMessage().getInt(TALKGROUP_CH1_TS1));
        }

        return mTalkgroupCH1TS1;
    }

    /**
     * Talkgroup that is active on channel 1 timeslot 2.
     */
    public TalkgroupIdentifier getTalkgroupCH1TS2()
    {
        if(mTalkgroupCH1TS2 == null)
        {
            mTalkgroupCH1TS2 = DMRTalkgroup.create(getMessage().getInt(TALKGROUP_CH1_TS2));
        }

        return mTalkgroupCH1TS2;
    }

    /**
     * Talkgroup that is active on channel 2 timeslot 1.
     */
    public TalkgroupIdentifier getTalkgroupCH2TS1()
    {
        if(mTalkgroupCH2TS1 == null)
        {
            mTalkgroupCH2TS1 = DMRTalkgroup.create(getMessage().getInt(TALKGROUP_CH2_TS1));
        }

        return mTalkgroupCH2TS1;
    }

    /**
     * Talkgroup that is active on channel 2 timeslot 2.
     */
    public TalkgroupIdentifier getTalkgroupCH2TS2()
    {
        if(mTalkgroupCH2TS2 == null)
        {
            mTalkgroupCH2TS2 = DMRTalkgroup.create(getMessage().getInt(TALKGROUP_CH2_TS2));
        }

        return mTalkgroupCH2TS2;
    }

    /**
     * Channel 1 Timeslot 1.
     */
    public DMRTier3Channel getChannel1TS1()
    {
        if(mChannel1TS1 == null)
        {
            mChannel1TS1 = new DMRTier3Channel(getMessage().getInt(CHANNEL_1), 1);
        }

        return mChannel1TS1;
    }

    /**
     * Channel 1 Timeslot 2.
     */
    public DMRTier3Channel getChannel1TS2()
    {
        if(mChannel1TS2 == null)
        {
            mChannel1TS2 = new DMRTier3Channel(getMessage().getInt(CHANNEL_1), 2);
        }

        return mChannel1TS2;
    }

    /**
     * Channel 2 Timeslot 1.
     */
    public DMRTier3Channel getChannel2TS1()
    {
        if(mChannel2TS1 == null)
        {
            mChannel2TS1 = new DMRTier3Channel(getMessage().getInt(CHANNEL_2), 1);
        }

        return mChannel2TS1;
    }

    /**
     * Channel 2 Timeslot 2.
     */
    public DMRTier3Channel getChannel2TS2()
    {
        if(mChannel2TS2 == null)
        {
            mChannel2TS2 = new DMRTier3Channel(getMessage().getInt(CHANNEL_2), 2);
        }

        return mChannel2TS2;
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        getChannel1TS1().apply(timeslotFrequencies);
        getChannel1TS2().apply(timeslotFrequencies);
        getChannel2TS1().apply(timeslotFrequencies);
        getChannel2TS2().apply(timeslotFrequencies);
    }

    @Override
    public int[] getLogicalChannelNumbers()
    {
        return new int[]{getChannel1TS1().getChannelNumber(),
                         getChannel1TS2().getChannelNumber(),
                         getChannel2TS1().getChannelNumber(),
                         getChannel2TS2().getChannelNumber()};
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasChannel1Timeslot1())
            {
                mIdentifiers.add(getTalkgroupCH1TS1());
                mIdentifiers.add(getChannel1TS1());
            }

            if(hasChannel1Timeslot2())
            {
                mIdentifiers.add(getTalkgroupCH1TS2());
                mIdentifiers.add(getChannel1TS2());
            }

            if(hasChannel2Timeslot1())
            {
                mIdentifiers.add(getTalkgroupCH2TS1());
                mIdentifiers.add(getChannel2TS1());
            }

            if(hasChannel2Timeslot2())
            {
                mIdentifiers.add(getTalkgroupCH2TS2());
                mIdentifiers.add(getChannel2TS2());
            }
        }

        return mIdentifiers;
    }
}
