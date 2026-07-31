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

package io.github.dsheirer.module.decode.nxdn.layer2;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

import java.util.Arrays;

/**
 * NXDN Scrambler.
 *
 * Note: The first four bits of the scramble sequence are all zeros meaning that the first two symbols of the LICH do
 * not get scrambled.  This aligns with the ICD hinting that you could use the first two symbols of the LICH that
 * represent the RF channel type as part of the sync pattern for sync detection, once you concretely identify the
 * RF channel type.
 */
public class Scrambler
{
    private static final int INITIAL_FILL = 0x0E4;

    /**
     * Generates the scrambler sequence for the requested length.
     * @param length of scramble sequence
     * @return generated scramble sequence.
     */
    public static BinaryMessage generate(int length)
    {
        BinaryMessage sequence = new BinaryMessage(length);

        int register = INITIAL_FILL;
        boolean output;
        int x = 0;

        while(x < length)
        {
            output = (register & 1) == 1;

            if(output)
            {
                sequence.set(x);
            }

            x +=2;

            //XOR output with 5-4 tap for the feedback bit
            if(output ^ ((register & 0x10) == 0x10))
            {
                //Set the high order feedback bit before we right shift it into place.
                register |= 0x200;
            }

            //Unsigned right shift by 1 with no integer shifting wrap-around
            register >>>= 1;
        }

        return sequence;
    }
}
