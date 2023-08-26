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
package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCOpcode;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;

/**
 * Full Link Control message
 */
public abstract class FullLCMessage extends LCMessage
{
    private static final int ENCRYPTION_FLAG = 0;
    private static final int RESERVED = 1;
    protected static final int[] OPCODE = new int[]{2, 3, 4, 5, 6, 7};
    private static final int[] VENDOR = new int[]{8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs an instance
     * @param message for link control payload
     */
    public FullLCMessage(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    /**
     * Link control opcode for this message
     */
    @Override
    public LCOpcode getOpcode()
    {
        return getOpcode(getMessage());
    }

    /**
     * Opcode numeric value
     */
    public int getOpcodeValue()
    {
        return getMessage().getInt(OPCODE);
    }

    /**
     * Lookup the opcode for a full link control message
     */
    public static LCOpcode getOpcode(CorrectedBinaryMessage message)
    {
        return LCOpcode.fromValue(true, message.getInt(OPCODE), getVendor(message));
    }

    /**
     * Vendor or Feature Identifier for this link control message
     */
    @Override
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Lookup the vendor for a full link control message
     */
    public static Vendor getVendor(CorrectedBinaryMessage message)
    {
        return Vendor.fromValue(message.getInt(VENDOR));
    }

    /**
     * Value for the vendor
     */
    public int getVendorValue()
    {
        return getMessage().getInt(VENDOR);
    }

    /**
     * Indicates if the contents of this link control message are encrypted
     */
    public boolean isEncrypted()
    {
        return getMessage().get(ENCRYPTION_FLAG);
    }

    /**
     * Indicates if the reserved bit is set.
     * @return true if set
     */
    public boolean isReservedBitSet()
    {
        return getMessage().get(RESERVED);
    }
}
