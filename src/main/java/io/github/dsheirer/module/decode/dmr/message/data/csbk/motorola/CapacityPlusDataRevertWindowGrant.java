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
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity+ Enhanced Data Revert Channel - Window Grant
 *
 * This appears to be a grant to an individual user to transmit data in a specified super frame and window.
 */
public class CapacityPlusDataRevertWindowGrant extends CSBKMessage
{
    //Note: bits 16-23 may be reserved for 24-bit target address, however Cap+ radio IDs are limited to 16-bit values.
    private static final int[] TARGET_ADDRESS = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] WINDOW = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SUPER_FRAME = new int[]{48, 49, 50, 51, 52, 53, 54, 55};

    private RadioIdentifier mTargetAddress;
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
    public CapacityPlusDataRevertWindowGrant(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CAP+ ENHANCED DATA REVERT GRANT TO:").append(getTargetAddress());
        sb.append(" TRANSMIT IN WINDOW:").append(getSuperFrame()).append(".").append(getWindow());
        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    public RadioIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = DMRRadio.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * Super frame (window) in which the target mobile should transmit their data
     */
    public int getSuperFrame()
    {
        return getMessage().getInt(SUPER_FRAME);
    }

    /**
     * Data frame window within the super frame in which the target mobile should transmit their data
     */
    public int getWindow()
    {
        return getMessage().getInt(WINDOW);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
