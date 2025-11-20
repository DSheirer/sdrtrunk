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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Qualifier for a location ID value.
 */
public enum LocationIDOption
{
    COMMON,
    DESTINATION,
    SOURCE,
    RESERVED;

    /**
     * Indicates if the location field applies to the source ID
     */
    public boolean isSource()
    {
        return this == SOURCE || this == COMMON;
    }

    /**
     * Indicates if the location field applies to the destination ID
     */
    public boolean isDestination()
    {
        return this == DESTINATION || this == COMMON;
    }

    /**
     * Utility method to lookup the entry from a transmitted value.
     * @param value that was transmitted as a five bit field
     * @return matching entry or RESERVED.
     */
    public static LocationIDOption fromValue(int value)
    {
        return switch(value)
        {
            //Note: value includes the 3x spare bits as the least significant bits
            case 0x00 -> COMMON;
            case 0x08 -> DESTINATION;
            case 0x10 -> SOURCE;
            default -> RESERVED;
        };
    }
}
