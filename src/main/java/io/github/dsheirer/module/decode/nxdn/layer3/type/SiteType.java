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
    RESERVED("RESERVED"),
    WIDE("LARGE COVERAGE AREA"),
    MIDDLE("MEDIUM COVERAGE AREA"),
    NARROW("SMALL COVERAGE AREA");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label to display to user
     */
    SiteType(String label)
    {
        mLabel = label;
    }



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

    @Override
    public String toString()
    {
        return mLabel;
    }
}
