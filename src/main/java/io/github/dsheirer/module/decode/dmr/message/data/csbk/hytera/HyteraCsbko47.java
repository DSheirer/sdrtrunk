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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera DMR Tier III - CSBKO 47 -
 *
 * Analysis: ... this was transmitted on the traffic channel while the mobile was at the end of a call, in the terminator
 * sequence.  Since this comes from the SMS gateway 16776906, I wonder if this is a traffic channel message waiting notification?
 */
public class HyteraCsbko47 extends CSBKMessage
{
    private static final int[] UNKNOWN_1 = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] UNKNOWN_2 = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] DESTINATION_RADIO = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_RADIO = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
            73, 74, 75, 76, 77, 78, 79};

    private List<Identifier> mIdentifiers;
    private RadioIdentifier mDestinationRadio;
    private RadioIdentifier mSourceRadio;

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
    public HyteraCsbko47(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" HYTERA CSBKO=47  #########  UNKNOWN");
        sb.append(" FM:").append(getSourceRadio());
        sb.append(" TO:").append(getDestinationRadio());
        sb.append(" UNK1:").append(getUnknown1());
        sb.append(" UNK2:").append(getUnknown2());

        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     * Addressed (ie TO) radio identifier
     * @return radio identifier
     */
    public RadioIdentifier getDestinationRadio()
    {
        if(mDestinationRadio == null)
        {
            mDestinationRadio = DmrTier3Radio.createTo(getMessage().getInt(DESTINATION_RADIO));
        }

        return mDestinationRadio;
    }

    public String getUnknown1()
    {
        return getMessage().getHex(UNKNOWN_1, 2);
    }

    public String getUnknown2()
    {
        return getMessage().getHex(UNKNOWN_2, 2);
    }

    /**
     * Source radio.  Should be 16,777,906 the Hytera SMS gateway.
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DmrTier3Radio.createFrom(getMessage().getInt(SOURCE_RADIO));
        }

        return mSourceRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getDestinationRadio());
            mIdentifiers.add(getSourceRadio());
        }

        return mIdentifiers;
    }
}
