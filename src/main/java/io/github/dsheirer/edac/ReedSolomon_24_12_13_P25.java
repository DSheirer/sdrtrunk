/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.edac;

/**
 * Reed Solomon RS(24,12,13) decoder over GF(6)
 */
public class ReedSolomon_24_12_13_P25 extends ReedSolomon_63_P25
{
    /**
     * Constructs an instance.
     *
     * Note: this is a shortened form of RS(63,51,13)
     */
    public ReedSolomon_24_12_13_P25()
    {
        super(63,51);
    }
}
