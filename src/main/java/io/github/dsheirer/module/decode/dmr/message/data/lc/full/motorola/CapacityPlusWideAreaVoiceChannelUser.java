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
import io.github.dsheirer.module.decode.dmr.channel.DmrRestLsn;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus - Wide Area (Linked or Multi-Site) Voice Channel User
 *
 * Note: in Linked Capacity Plus talkgroups range 1-255 and radio IDs range 1-65535
 */
public class CapacityPlusWideAreaVoiceChannelUser extends CapacityPlusVoiceChannelUser implements ITimeslotFrequencyReceiver
{
    //Bits 16-23: Service Options
    private static final int[] UNKNOWN_1 = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] GROUP_ID = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] UNUSED = new int[]{48, 49, 50};
    private static final int[] REST_LSN = new int[]{51, 52, 53, 54, 55};
    private static final int[] RADIO_ID = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    //Reed Solomon FEC: 72-95

    private RadioIdentifier mRadio;
    private TalkgroupIdentifier mTalkgroup;
    private DmrRestLsn mRestChannel;
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

        if(isEncrypted())
        {
            sb.append(" *ENCRYPTED*");
        }

        if(isReservedBitSet())
        {
            sb.append(" *RESERVED-BIT*");
        }

        sb.append("FLC MOTOROLA CAP+ WIDE-AREA VOICE CHANNEL USER FM:");
        sb.append(getRadio());
        sb.append(" TO:").append(getTalkgroup());
        if(hasRestChannel())
        {
            sb.append(" ").append(getRestChannel());
        }
        sb.append(" ").append(getServiceOptions());
        sb.append(" UNK1:").append(getUnknown1());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Unknown1 8-bit field
     */
    public String getUnknown1()
    {
        return getMessage().getHex(UNKNOWN_1, 4);
    }

    /**
     * Logical channel number (ie repeater number).
     */
    public DmrRestLsn getRestChannel()
    {
        if(mRestChannel == null)
        {
            mRestChannel = new DmrRestLsn(getRestLSN());
        }

        return mRestChannel;
    }

    /**
     * Rest LSN
     * @return Logical Slot Number 1-16
     */
    public int getRestLSN()
    {
        return getMessage().getInt(REST_LSN);
    }

    /**
     * Indicates if this message has a reset channel defined.
     */
    public boolean hasRestChannel()
    {
        return getRestLSN() != 0;
    }

    /**
     * Source radio address
     */
    public RadioIdentifier getRadio()
    {
        if(mRadio == null)
        {
            mRadio = DMRRadio.createFrom(getMessage().getInt(RADIO_ID));
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
            mTalkgroup = DMRTalkgroup.create(getMessage().getInt(GROUP_ID));
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
    public int[] getLogicalChannelNumbers()
    {
        return getRestChannel().getLogicalChannelNumbers();
    }

    /**
     * Applies the LSN to frequency map to the rest channel.
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        getRestChannel().apply(timeslotFrequencies);
    }
}
