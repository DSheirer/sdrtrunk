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
package io.github.dsheirer.module.decode.dmr.message.data.lc.shorty;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCOpcode;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;

/**
 * Base short link control message
 */
public abstract class ShortLCMessage extends LCMessage implements IMessage
{
    protected static final int[] OPCODE = new int[]{0, 1, 2, 3};

    /**
     * Constructs an instance
     * @param message containing the short link control message bits
     */
    public ShortLCMessage(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    /**
     * Opcode for this message
     */
    @Override
    public LCOpcode getOpcode()
    {
        return getOpcode(getMessage());
    }

    /**
     * Opcode numeric value
     */
    protected int getOpcodeValue()
    {
        return getMessage().getInt(OPCODE);
    }

    /**
     * Vendor for this message
     */
    @Override
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Lookup the short link control opcode for the specified binary message
     */
    public static LCOpcode getOpcode(BinaryMessage binaryMessage)
    {
        return LCOpcode.fromValue(false, binaryMessage.getInt(OPCODE), getVendor(binaryMessage));
    }

    /**
     * Lookup the short link control vendor for the specified binary message
     */
    public static Vendor getVendor(BinaryMessage binaryMessage)
    {
        //TODO: determine when we have a non-standard vendor
        return Vendor.STANDARD;
    }
}

