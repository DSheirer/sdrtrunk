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
 * Motorola Capacity Max Open System Mode - Voice Channel Update (Opcode 33 / 0x21)
 *
 * Opcode 33 and 34 are very similar.  However, Opcode 33 appears to support full 24-bit group identifiers whereas
 * Opcode 34 only supports 10-bit group identifiers.
 * <p>
 * Likely incomplete implementation.
 */
public class CapacityMaxOpenModeVoiceChannelUpdate extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] CHANNEL_NUMBER = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] TALKGROUP_TS1 = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] TALKGROUP_TS2 = new int[]{56, 57, 58, 59, 60, 61, 63, 64, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private TalkgroupIdentifier mTalkgroupTS1;
    private TalkgroupIdentifier mTalkgroupTS2;
    private DMRTier3Channel mChannelTS1;
    private DMRTier3Channel mChannelTS2;
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
    public CapacityMaxOpenModeVoiceChannelUpdate(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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

        sb.append(" CSBK CAP-MAX MODE:OPEN-SYSTEM CHANNEL UPDATE");

        if(hasTimeslot1())
        {
            sb.append(" TG1:").append(getTalkgroupTS1());
            sb.append(" ON").append(getChannelTS1());
        }
        if(hasTimeslot2())
        {
            sb.append(" TG2:").append(getTalkgroupTS2());
            sb.append(" ON").append(getChannelTS2());
        }

        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Indicates if this message contains channel update for timeslot 1.
     */
    public boolean hasTimeslot1()
    {
        return getMessage().getInt(TALKGROUP_TS1) > 0;
    }

    /**
     * Indicates if this message contains channel update for timeslot 2.
     */
    public boolean hasTimeslot2()
    {
        return getMessage().getInt(TALKGROUP_TS2) > 0;
    }

    /**
     * Talkgroup that is active on the channel for timeslot 1.
     */
    public TalkgroupIdentifier getTalkgroupTS1()
    {
        if(mTalkgroupTS1 == null)
        {
            mTalkgroupTS1 = DMRTalkgroup.create(getMessage().getInt(TALKGROUP_TS1));
        }

        return mTalkgroupTS1;
    }

    /**
     * Talkgroup that is active on the channel for timeslot 2.
     */
    public TalkgroupIdentifier getTalkgroupTS2()
    {
        if(mTalkgroupTS2 == null)
        {
            mTalkgroupTS2 = DMRTalkgroup.create(getMessage().getInt(TALKGROUP_TS2));
        }

        return mTalkgroupTS2;
    }

    /**
     * Channel for timeslot 1
     */
    public DMRTier3Channel getChannelTS1()
    {
        if(mChannelTS1 == null)
        {
            mChannelTS1 = new DMRTier3Channel(getMessage().getInt(CHANNEL_NUMBER), 1);
        }

        return mChannelTS1;
    }

    /**
     * Channel for timeslot 2
     */
    public DMRTier3Channel getChannelTS2()
    {
        if(mChannelTS2 == null)
        {
            mChannelTS2 = new DMRTier3Channel(getMessage().getInt(CHANNEL_NUMBER), 2);
        }

        return mChannelTS2;
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        getChannelTS1().apply(timeslotFrequencies);
        getChannelTS2().apply(timeslotFrequencies);
    }

    @Override
    public int[] getLogicalChannelNumbers()
    {
        return new int[]{getChannelTS1().getChannelNumber(), getChannelTS2().getChannelNumber()};
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasTimeslot1())
            {
                mIdentifiers.add(getChannelTS1());
                mIdentifiers.add(getTalkgroupTS1());
            }
            if(hasTimeslot2())
            {
                mIdentifiers.add(getChannelTS2());
                mIdentifiers.add(getTalkgroupTS2());
            }
        }

        return mIdentifiers;
    }
}
