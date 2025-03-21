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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the 5-bit Checksum error detection algorithm specified in TS 102 361-1 paragraph B.3.11
 */
public class Checksum_5_DMR
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Checksum_5_DMR.class);

    private static final int[] LC_0 = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] LC_1 = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] LC_2 = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] LC_3 = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] LC_4 = {32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] LC_5 = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] LC_6 = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] LC_7 = {56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] LC_8 = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] CHECKSUM = {72, 73, 74, 75, 76};

    /**
     * Indicates if the 77-bit Full Link Control message is valid.
     * @param message to check
     * @return residual value leftover from xor of the calculated and transmitted checksums.
     */
    public static int isValid(CorrectedBinaryMessage message)
    {
        int accumulator = message.getInt(LC_0);
        accumulator += message.getInt(LC_1);
        accumulator += message.getInt(LC_2);
        accumulator += message.getInt(LC_3);
        accumulator += message.getInt(LC_4);
        accumulator += message.getInt(LC_5);
        accumulator += message.getInt(LC_6);
        accumulator += message.getInt(LC_7);
        accumulator += message.getInt(LC_8);
        accumulator = accumulator % 31;
        int checksum = message.getInt(CHECKSUM);
        return accumulator ^ checksum;
    }
}
