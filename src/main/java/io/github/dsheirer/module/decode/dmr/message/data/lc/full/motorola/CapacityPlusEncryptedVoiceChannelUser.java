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
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus - Yet Another Voice Channel User Message - Why?  Is this one specific to Encrypted calls?
 */
public class CapacityPlusEncryptedVoiceChannelUser extends CapacityPlusVoiceChannelUser
{
    private static final int[] UNKNOWN_1 = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] TARGET_ADDRESS = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] UNKNOWN_2 = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] UNKNOWN_3 = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mRadio;
    private TalkgroupIdentifier mTalkgroup;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public CapacityPlusEncryptedVoiceChannelUser(CorrectedBinaryMessage message, long timestamp, int timeslot)
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

        sb.append("FLC MOTOROLA CAP+ ENCRYPTED VOICE CHANNEL USER");
        sb.append(" FM:").append(getRadio());
        sb.append(" TO:").append(getTalkgroup());
        sb.append(" ").append(getServiceOptions());
        sb.append(" UNK1:").append(getUnknown1());
        sb.append(" UNK2:").append(getUnknown2());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Unknown 8-bit fields
     */
    public String getUnknown1()
    {
        return getMessage().getHex(UNKNOWN_1, 2);
    }
    public String getUnknown2()
    {
        return getMessage().getHex(UNKNOWN_2, 2);
    }
    public String getUnknown3()
    {
        return getMessage().getHex(UNKNOWN_3, 2);
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
            mTalkgroup = DMRTalkgroup.create(getMessage().getInt(TARGET_ADDRESS));
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
