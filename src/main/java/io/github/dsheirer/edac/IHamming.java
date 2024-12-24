/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

/**
 * Interface for Hamming error index calculator.
 */
public interface IHamming
{
    int NO_ERRORS = -1;
    int MULTIPLE_ERRORS = 1000;

    /**
     * Calculate the error index for the Hamming protected binary sequence
     * @param message containing the binary sequence
     * @param offset to the start of the binary sequence
     * @return -1 if no errors, single-bit error index, or 1000 if there is more than a single-bit error detected.
     */
    int getErrorIndex(BinaryMessage message, int offset);
}
