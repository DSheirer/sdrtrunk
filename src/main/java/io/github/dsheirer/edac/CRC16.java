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

package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.IntField;

/**
 * Utility for calculating the CRC checksum for CRC-16 using polynomial 0x1021 and Initial Fill/Residual of 0xFFFF
 */
public class CRC16
{
    /**
     * Calculates the 16-bit CRC checksum for the message using polynomial 0x1021 and residual 0xFFFF
     * @param message with transmitted 16-bit checksum at the end.
     * @return true if the check is correct or false if it fails the CRC check.
     */
    public static boolean check(BinaryMessage message)
    {
        BinaryMessage copy = message.copy();
        BinaryMessage polynomial = BinaryMessage.loadHex("11021");
        polynomial.rotateLeft(3, 0, 20);
        polynomial.setSize(message.size());

        int previousX = 0;
        for(int x = copy.nextSetBit(0); x >= 0 && x < copy.size() - 16; x = copy.nextSetBit(x + 1))
        {
            polynomial.rotateRight(x - previousX, previousX, 17 + x);
            previousX = x;
            copy.xor(polynomial);
        }

        IntField crc = IntField.length16(copy.size() - 16);
        return copy.getInt(crc) == 0xFFFF;
    }
}
