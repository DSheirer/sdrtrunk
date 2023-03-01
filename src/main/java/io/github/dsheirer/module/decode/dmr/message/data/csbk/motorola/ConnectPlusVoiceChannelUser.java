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
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Connect Plus - Voice Channel User
 */
public class ConnectPlusVoiceChannelUser extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] SOURCE_ADDRESS = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] GROUP_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
        56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] TRAFFIC_CHANNEL_REPEATER = new int[]{64, 65, 66, 67};
    private static final int[] TRAFFIC_CHANNEL_TIMESLOT = new int[]{68};
    private static final int[] UNKNOWN_FIELD = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mRadio;
    private TalkgroupIdentifier mTalkgroup;
    private DMRLogicalChannel mDMRLogicalChannel;
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
    public ConnectPlusVoiceChannelUser(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CON+ VOICE CHANNEL USER FM:").append(getRadio());
        sb.append(" TO:").append(getTalkgroup());
        sb.append(" ").append(getChannel());
        sb.append(" UNK:").append(getUnknownField());
        return sb.toString();
    }

    /**
     * Source radio address
     */
    public RadioIdentifier getRadio()
    {
        if(mRadio == null)
        {
            mRadio = DMRRadio.createFrom(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mRadio;
    }

    /**
     * Talkgroup address
     */
    public TalkgroupIdentifier getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = DMRTalkgroup.create(getMessage().getInt(GROUP_ADDRESS));
        }

        return mTalkgroup;
    }

    /**
     * Unknown field
     */
    public int getUnknownField()
    {
        return getMessage().getInt(UNKNOWN_FIELD);
    }

    /**
     * Traffic channel repeater
     */
    public int getTrafficChannelRepeater()
    {
        return getMessage().getInt(TRAFFIC_CHANNEL_REPEATER);
    }

    /**
     * Traffic channel timeslot
     * @return 1 or 2
     */
    public int getTrafficChannelTimeslot()
    {
        return getMessage().getInt(TRAFFIC_CHANNEL_TIMESLOT) + 1;
    }

    /**
     * DMR Channel
     */
    public DMRLogicalChannel getChannel()
    {
        if(mDMRLogicalChannel == null)
        {
            mDMRLogicalChannel = new DMRLogicalChannel(getTrafficChannelRepeater(), getTrafficChannelTimeslot());
        }

        return mDMRLogicalChannel;
    }

    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getChannel().getLSNArray();
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
        {
            if(timeslotFrequency.getNumber() == getChannel().getLogicalSlotNumber())
            {
                getChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getRadio());
            mIdentifiers.add(getChannel());
        }

        return mIdentifiers;
    }
}
