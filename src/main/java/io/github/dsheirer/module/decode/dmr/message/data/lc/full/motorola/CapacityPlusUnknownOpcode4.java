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
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.AbstractVoiceChannelUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Motola Capcity Plus - Unknown - FLCO:4
 */
public class CapacityPlusUnknownOpcode4 extends AbstractVoiceChannelUser
{
    private static final int[] GROUP_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] REST_REPEATER = new int[]{51, 52, 53, 54};
    private static final int[] REST_TIMESLOT = new int[]{55};
    private static final int[] SOURCE_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] UNKNOWN = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mRadio;
    private TalkgroupIdentifier mTalkgroup;
    private DMRChannel mRestChannel;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public CapacityPlusUnknownOpcode4(CorrectedBinaryMessage message, long timestamp, int timeslot)
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

        sb.append("FLC MOTOROLA CAP+ UNKNOWN OPCODE:4 FM:");
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
        sb.append(" UNK:").append(getUnknown());
        sb.append(" ").append(getServiceOptions());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Unknown 8-bit field
     */
    public int getUnknown()
    {
        return getMessage().getInt(UNKNOWN);
    }

    /**
     * Logical channel number (ie repeater number).
     */
    public DMRChannel getRestChannel()
    {
        if(mRestChannel == null && hasRestChannel())
        {
            mRestChannel = new DMRLogicalChannel(getRestRepeater(), getMessage().getInt(REST_TIMESLOT));
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
        return getMessage().getInt(REST_REPEATER);
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
}
