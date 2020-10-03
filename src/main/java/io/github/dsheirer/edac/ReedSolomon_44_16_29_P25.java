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

/**
 * Reed Solomon Decoder for P25 RS(44,16,29) protected messages.
 */
public class ReedSolomon_44_16_29_P25 extends ReedSolomon_63_P25
{
    /**
     * Constructs an instance
     */
    public ReedSolomon_44_16_29_P25()
    {
        //The RS(44,16,29) is shortened from RS(63,35,29)
        super(63,35);
    }
}
