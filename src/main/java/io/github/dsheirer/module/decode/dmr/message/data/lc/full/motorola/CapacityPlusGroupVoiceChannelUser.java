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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus - Group Voice Channel User
 */
public class CapacityPlusGroupVoiceChannelUser extends CapacityPlusVoiceChannelUser implements ITimeslotFrequencyReceiver
{
    private static final int[] CAPACITY_PLUS_GROUP_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] CONVENTIONAL_GROUP_ADDRESS = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] REST_CHANNEL = new int[]{51, 52, 53, 54};
    private static final int[] REST_CHANNEL_TIMESLOT = new int[]{55};
    private static final int[] CAPACITY_PLUS_SOURCE_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] CONVENTIONAL_SOURCE_ADDRESS = new int[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
            60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] UNKNOWN = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mRadio;
    private TalkgroupIdentifier mTalkgroup;
    private DMRLogicalChannel mRestChannel;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public CapacityPlusGroupVoiceChannelUser(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        if(isReservedBitSet())
        {
            sb.append(" RESERVED-BIT");
        }

        if(getServiceOptions().isCapacityPlus())
        {
            sb.append("FLC MOTOROLA CAP+ GROUP VOICE CHANNEL USER");
        }
        else
        {
            sb.append("FLC MOTOROLA CONV/IP SITE GROUP VOICE CHANNEL USER");
        }
        sb.append(" FM:").append(getRadio());
        sb.append(" TO:").append(getTalkgroup());

        if(hasRestChannel())
        {
            sb.append(" REST:");
            sb.append(getRestChannel());
        }

        sb.append(" ").append(getServiceOptions());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Unknown 8-bit field
     */
    public String getUnknown()
    {
        return getMessage().getHex(UNKNOWN, 2);
    }

    /**
     * Logical channel number (ie repeater number).
     */
    public DMRLogicalChannel getRestChannel()
    {
        if(mRestChannel == null)
        {
            mRestChannel = new DMRLogicalChannel(getRestChannelRepeater(), getRestChannelTimeslot());
        }

        return mRestChannel;
    }

    /**
     * Rest repeater number
     */
    public int getRestChannelRepeater()
    {
        return getMessage().getInt(REST_CHANNEL) + 1;
    }

    /**
     * Rest timeslot
     *
     * @return
     */
    public int getRestChannelTimeslot()
    {
        return getMessage().getInt(REST_CHANNEL_TIMESLOT) + 1;
    }

    /**
     * Indicates if this message has a rest channel indicated for the call
     */
    public boolean hasRestChannel()
    {
        return getServiceOptions().isCapacityPlus() && getRestChannelRepeater() != 0;
    }

    /**
     * Source radio address
     */
    public RadioIdentifier getRadio()
    {
        if(mRadio == null)
        {
            if(getServiceOptions().isCapacityPlus())
            {
                mRadio = DMRRadio.createFrom(getMessage().getInt(CAPACITY_PLUS_SOURCE_ADDRESS));
            }
            else
            {
                mRadio = DMRRadio.createFrom(getMessage().getInt(CONVENTIONAL_SOURCE_ADDRESS));
            }
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
            if(getServiceOptions().isCapacityPlus())
            {
                mTalkgroup = DMRTalkgroup.create(getMessage().getInt(CAPACITY_PLUS_GROUP_ADDRESS));
            }
            else
            {
                mTalkgroup = DMRTalkgroup.create(getMessage().getInt(CONVENTIONAL_GROUP_ADDRESS));
            }
        }

        return mTalkgroup;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getRadio());

            if(hasRestChannel())
            {
                mIdentifiers.add(getRestChannel());
            }
        }

        return mIdentifiers;
    }

    /**
     * Exposes the rest channel logical slot number so that a LSN to frequency map can be applied to this message.
     */
    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getRestChannel().getLSNArray();
    }

    /**
     * Applies the LSN to frequency map to the rest channel.
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency : timeslotFrequencies)
        {
            if(getRestChannel().getLogicalSlotNumber() == timeslotFrequency.getNumber())
            {
                getRestChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }
}
