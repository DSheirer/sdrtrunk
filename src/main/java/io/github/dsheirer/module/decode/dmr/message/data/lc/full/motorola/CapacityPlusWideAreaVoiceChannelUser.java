/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.AbstractVoiceChannelUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus - Wide Area (Multi-Site) Voice Channel User
 */
public class CapacityPlusWideAreaVoiceChannelUser extends AbstractVoiceChannelUser implements ITimeslotFrequencyReceiver
{
    private static final int[] UNKNOWN_1 = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] GROUP_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] REST_REPEATER = new int[]{51, 52, 53, 54};
    private static final int[] REST_TIMESLOT = new int[]{55};
    private static final int[] SOURCE_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] UNKNOWN_2 = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mRadio;
    private TalkgroupIdentifier mTalkgroup;
    private DMRLogicalChannel mRestChannel;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public CapacityPlusWideAreaVoiceChannelUser(CorrectedBinaryMessage message, long timestamp, int timeslot)
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

        sb.append("FLC MOTOROLA CAP+ WIDE-AREA VOICE CHANNEL USER FM:");
        sb.append(getRadio());
        sb.append(" TO:").append(getTalkgroup());
        sb.append(" REST:");
        if(hasRestChannel())
        {
            sb.append(getRestChannel());
        }
        else
        {
            sb.append("--");
        }
        sb.append(" UNK1:").append(getUnknown1());
        sb.append(" UNK2:").append(getUnknown2());
        sb.append(" ").append(getServiceOptions());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Unknown1 8-bit field
     */
    public int getUnknown1()
    {
        return getMessage().getInt(UNKNOWN_1);
    }

    /**
     * Unknown2 8-bit field
     */
    public int getUnknown2()
    {
        return getMessage().getInt(UNKNOWN_2);
    }

    /**
     * Logical channel number (ie repeater number).
     */
    public DMRLogicalChannel getRestChannel()
    {
        if(mRestChannel == null)
        {
            mRestChannel = new DMRLogicalChannel(getRestRepeater(), getRestTimeslot());
        }

        return mRestChannel;
    }

    /**
     * Rest channel timeslot
     */
    public int getRestTimeslot()
    {
        return getMessage().getInt(REST_TIMESLOT) + 1;
    }

    /**
     * Rest channel repeater number
     */
    public int getRestRepeater()
    {
        return getMessage().getInt(REST_REPEATER) + 1;
    }

    /**
     * Indicates if this message has a reset channel defined.
     */
    public boolean hasRestChannel()
    {
        return getRestRepeater() != 0;
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

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getRadio());
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
