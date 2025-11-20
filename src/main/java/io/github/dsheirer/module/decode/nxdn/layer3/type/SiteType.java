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
 * NXDN Type-D site type enumeration
 */
public enum SiteType
{
    RESERVED,
    WIDE,
    MIDDLE,
    NARROW;

    /**
     * Utility method to lookup the type from the value.
     * @param value transmitted
     * @return entry
     */
    public static SiteType fromValue(int value)
    {
        return switch (value)
        {
            case 1 -> WIDE;
            case 2 -> MIDDLE;
            case 3 -> NARROW;
            default -> RESERVED;
        };
    }
}
