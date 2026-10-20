/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.BinaryMessage;

/**
 * LRRP variable length integer (varint) reader.
 *
 * LRRP encodes integer token values as one or more bytes where each byte carries seven value bits, most significant
 * group first, and the high bit of a byte indicates that another byte follows.  For example 0x84 0x58 decodes to
 * (0x04 << 7) | 0x58 = 600, 0x9C 0x10 decodes to (0x1C << 7) | 0x10 = 3600 and a value smaller than 128 occupies a
 * single byte (0x64 = 100).
 *
 * References:
 *  - Moto.Net, LRRPPacket.cs: "looks like this stuff is using VLQ ... for most of the fields"
 *  - node-dmr-lib, src/Protocols/LRRP.js: parseLRRPInt() and toLRRPInt()
 */
public final class VarInt
{
    /**
     * Maximum number of bytes consumed for a single value.  Five 7-bit groups hold 35 bits, which covers any 32-bit
     * value, and the cap keeps a corrupted run of continuation bits from consuming the rest of a packet.
     */
    public static final int MAX_BYTES = 5;

    private static final int[] BYTE_VALUE = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int CONTINUATION_MASK = 0x80;
    private static final int VALUE_MASK = 0x7F;

    private VarInt()
    {
        //Static utility class
    }

    /**
     * Number of bytes occupied by the varint that starts at the bit offset.
     *
     * @param message containing the value
     * @param offset bit offset to the first byte of the value
     * @param limit maximum number of bytes available for the value (ie bytes remaining in the packet payload)
     * @return byte count from 0 to the lesser of limit and MAX_BYTES.  Zero indicates that no value is present.
     */
    public static int length(BinaryMessage message, int offset, int limit)
    {
        int maximum = Math.min(limit, MAX_BYTES);
        int count = 0;

        while(count < maximum)
        {
            int b = message.getInt(BYTE_VALUE, offset + (count * 8));
            count++;

            if((b & CONTINUATION_MASK) == 0)
            {
                break;
            }
        }

        return count;
    }

    /**
     * Decodes the varint that starts at the bit offset.
     *
     * @param message containing the value
     * @param offset bit offset to the first byte of the value
     * @param limit maximum number of bytes available for the value (ie bytes remaining in the packet payload)
     * @return decoded value, or zero when no bytes are available.
     */
    public static int value(BinaryMessage message, int offset, int limit)
    {
        int maximum = Math.min(limit, MAX_BYTES);
        int value = 0;

        for(int count = 0; count < maximum; count++)
        {
            int b = message.getInt(BYTE_VALUE, offset + (count * 8));
            value = (value << 7) | (b & VALUE_MASK);

            if((b & CONTINUATION_MASK) == 0)
            {
                break;
            }
        }

        return value;
    }
}
