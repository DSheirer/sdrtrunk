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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Data write options field.
 */
public class DataWriteOption extends Option
{
    private static final int MASK_EMERGENCY = 0x80;

    /**
     * Constructs an instance
     *
     * @param value for the field
     */
    public DataWriteOption(int value)
    {
        super(value);
    }

    /**
     * Indicates if this is emergency
     */
    public boolean isEmergency()
    {
        return isSet(MASK_EMERGENCY);
    }
}
