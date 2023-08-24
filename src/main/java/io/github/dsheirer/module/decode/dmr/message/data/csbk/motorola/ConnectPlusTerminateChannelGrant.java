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
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Connect Plus - Terminate Channel Grant
 */
public class ConnectPlusTerminateChannelGrant extends CSBKMessage
{
    private static final int[] TARGET_ADDRESS = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39};

    //Analysis: this field correlates to UNKNOWN_FIELD(bits: 48-55) in ConnectPlusDataChannelGrant.
    private static final int[] UNKNOWN_FIELD_1 = new int[]{40, 41, 42, 43, 44, 45, 46, 47};

    private static final int[] UNKNOWN_FIELD_2 = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] UNKNOWN_FIELD_3 = new int[]{56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] UNKNOWN_FIELD_4 = new int[]{64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] UNKNOWN_FIELD_5 = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mTargetRadio;
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
    public ConnectPlusTerminateChannelGrant(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK ").append(getVendor());
        sb.append(" TERMINATE CHANNEL GRANT TO:").append(getTargetRadio());
        sb.append(" U1:").append(getUnknownField1());
        sb.append(" U2:").append(getUnknownField2());
        sb.append(" U3:").append(getUnknownField3());
        sb.append(" U4:").append(getUnknownField4());
        sb.append(" U5:").append(getUnknownField5());
        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     * Target radio address
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = DMRRadio.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetRadio;
    }

    /**
     * Unknown field
     */
    public int getUnknownField1()
    {
        return getMessage().getInt(UNKNOWN_FIELD_1);
    }

    /**
     * Unknown field
     */
    public int getUnknownField2()
    {
        return getMessage().getInt(UNKNOWN_FIELD_2);
    }

    /**
     * Unknown field
     */
    public int getUnknownField3()
    {
        return getMessage().getInt(UNKNOWN_FIELD_3);
    }

    /**
     * Unknown field
     */
    public int getUnknownField4()
    {
        return getMessage().getInt(UNKNOWN_FIELD_4);
    }

    /**
     * Unknown field
     */
    public int getUnknownField5()
    {
        return getMessage().getInt(UNKNOWN_FIELD_5);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetRadio());
        }

        return mIdentifiers;
    }
}
