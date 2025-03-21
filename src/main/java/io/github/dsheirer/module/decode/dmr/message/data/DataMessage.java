/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRBurst;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;

/**
 * Base DMR Data Message
 */
public abstract class DataMessage extends DMRBurst
{
    //R2, R1, R0 extracted from the BPTC extraction process. Note: message length remains at 96, even though these 3x bits
    //are set at the end of the message.
    private static final int[] BPTC_RESERVED_BITS = new int[]{96, 97, 98};
    private SlotType mSlotType;

    /**
     * Constructs an instance.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing extracted 196-bit payload.
     * @param cach for the DMR burst
     * @param slotType for this data message
     * @param timestamp message was received
     * @param timeslot for the DMR burst
     */
    public DataMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach,  SlotType slotType,
                       long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, timestamp, timeslot);
        mSlotType = slotType;
    }

    /**
     * Slot Type identifies the color code and data type for this data message
     */
    public SlotType getSlotType()
    {
        return mSlotType;
    }

    /**
     * 3x reserved bits that are left-over from the BPTC encode/decode process that can be used to hold values like
     * Moto RAS indicator.  The message length from the BPTC decoder is set to 96 and these 3x bits are appended to
     * the end as overage.  However, to keep the message.toString() correct, we specify the length as 96.
     * @return reserved value.
     */
    public int getBPTCReservedBits()
    {
        return getMessage().getInt(BPTC_RESERVED_BITS);
    }

    /**
     * Value of the reserved bits.  Normally this is 2 for RAS enabled systems.
     */
    public int getRAS()
    {
        return getBPTCReservedBits();
    }

    /**
     * Indicates if the BPTC reserved bits value is anything other than 0.
     * @return true if non-zero.
     */
    public boolean hasRAS()
    {
        return getBPTCReservedBits() != 0;
    }
}
