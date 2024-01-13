/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRTier3Channel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Max - Group Voice Channel Update (Opcode 33 / 0x21)
 *
 * Likely incomplete implementation.
 */
public class CapacityMaxGroupVoiceChannelUpdate extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] CHANNEL_NUMBER = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] TIMESLOT = new int[]{28}; //Logical guess - not seeing it set in the example Tier3 recording

    private static final int[] TALKGROUP = new int[]{64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private TalkgroupIdentifier mTalkgroup;
    private DMRTier3Channel mChannel;
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
    public CapacityMaxGroupVoiceChannelUpdate(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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

        sb.append(" CSBK CAP-MAX GROUP VOICE CHANNEL UPDATE TALKGROUP:").append(getTalkgroup());
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Talkgroup that is active on the channel.
     */
    public TalkgroupIdentifier getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = DMRTalkgroup.create(getMessage().getInt(TALKGROUP));
        }

        return mTalkgroup;
    }

    /**
     * Channel the talkgroup is using.
     * @return channel.
     */
    public DMRTier3Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = new DMRTier3Channel(getMessage().getInt(CHANNEL_NUMBER), getChannelTimeslot());
        }

        return mChannel;
    }

    /**
     * Timeslot
     */
    private int getChannelTimeslot()
    {
        return getMessage().getInt(TIMESLOT) + 1;
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        getChannel().apply(timeslotFrequencies);
    }

    @Override
    public int[] getLogicalChannelNumbers()
    {
        return getChannel().getLogicalChannelNumbers();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getChannel());
        }

        return mIdentifiers;
    }
}
