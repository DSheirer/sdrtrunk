/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.edac;

public class ReedSolomon_63_35_29 extends BerlekempMassey_63
{
    /**
     * Reed-Solomon RS(63,35,29) decoder.  This can also be used for error detection and correction of the following
     * RS codes:
     *
     * RS(46,26,21) - max 10 errors = P25P2 IEMI
     * RS(45,26,20)  - max 9 errors = P25P2 SOEMI (FACCH)
     * RS(52,30,23) - max 11 errors = P25P2 IOEMI (SACCH)
     * RS(44,16,29) - max 14 errors = P25P2 ESS
     *
     * The maximum correctable errors is determined by (n-k)/2, or hamming distance divided by 2.
     */
    public ReedSolomon_63_35_29(int maximumCorrectableErrors)
    {
        super(maximumCorrectableErrors);
    }
}
