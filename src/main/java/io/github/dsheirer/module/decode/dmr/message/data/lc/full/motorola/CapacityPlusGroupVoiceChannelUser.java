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
 * Motorola Capacity Plus - Group Voice Channel User
 */
public class CapacityPlusGroupVoiceChannelUser extends AbstractVoiceChannelUser implements ITimeslotFrequencyReceiver
{
    private static final int[] GROUP_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] VOICE_CHANNEL_REPEATER = new int[]{51, 52, 53, 54};
    private static final int[] VOICE_CHANNEL_TIMESLOT = new int[]{55};
    private static final int[] SOURCE_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] UNKNOWN = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mRadio;
    private TalkgroupIdentifier mTalkgroup;
    private DMRLogicalChannel mVoiceChannel;
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

        sb.append("FLC MOTOROLA CAP+ GROUP VOICE CHANNEL USER FM:");
        sb.append(getRadio());
        sb.append(" TO:").append(getTalkgroup());
        sb.append(" ON CHANNEL:");
        if(hasVoiceChannel())
        {
            sb.append(getVoiceChannel());
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
    public String getUnknown()
    {
        return getMessage().getHex(UNKNOWN, 2);
    }

    /**
     * Logical channel number (ie repeater number).
     */
    public DMRLogicalChannel getVoiceChannel()
    {
        if(mVoiceChannel == null)
        {
            mVoiceChannel = new DMRLogicalChannel(getVoiceChannelRepeater(), getVoiceChannelTimeslot());
        }

        return mVoiceChannel;
    }

    /**
     * Rest repeater number
     */
    public int getVoiceChannelRepeater()
    {
        return getMessage().getInt(VOICE_CHANNEL_REPEATER) + 1;
    }

    /**
     * Rest timeslot
     * @return
     */
    public int getVoiceChannelTimeslot()
    {
        return getMessage().getInt(VOICE_CHANNEL_TIMESLOT) + 1;
    }

    /**
     * Indicates if this message has a voice channel indicated for the call
     */
    public boolean hasVoiceChannel()
    {
        return getVoiceChannelRepeater() != 0;
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

            if(hasVoiceChannel())
            {
                mIdentifiers.add(getVoiceChannel());
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
        return getVoiceChannel().getLSNArray();
    }

    /**
     * Applies the LSN to frequency map to the rest channel.
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
        {
            if(getVoiceChannel().getLogicalSlotNumber() == timeslotFrequency.getNumber())
            {
                getVoiceChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }
}
