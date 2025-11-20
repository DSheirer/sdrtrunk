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
 * Location ID categories that define the length of the system and site fields, in bits.
 */
public enum LocationCategory
{
    GLOBAL(10, 12, "GLOBAL"),
    REGIONAL(14, 8, "REGIONAL"),
    LOCAL(17, 5, "LOCAL"),
    RESERVED(1, 1, "RESERVED");

    private final int mSystemFieldLength;
    private final int mSiteFieldLength;
    private final String mLabel;

    /**
     * Constructs an instance
     * @param systemFieldLength in bits
     * @param siteFieldLength in bits
     * @param mLabel to display
     */
    LocationCategory(int systemFieldLength, int siteFieldLength, String label)
    {
        mSystemFieldLength = systemFieldLength;
        mSiteFieldLength = siteFieldLength;
        mLabel = label;
    }

    /**
     * Bit length of the system field
     */
    public int getSystemFieldLength()
    {
        return mSystemFieldLength;
    }

    /**
     * Bit length of the site field
     */
    public int getSiteFieldLength()
    {
        return mSiteFieldLength;
    }

    /**
     * Utility method to lookup the category from the transmitted value.
     * @param value transmitted
     * @return matching entry or RESERVED
     */
    public static LocationCategory fromValue(int value)
    {
        return switch (value)
        {
            case 0 -> GLOBAL;
            case 1 -> LOCAL;
            case 2 -> REGIONAL;
            default -> RESERVED;
        };
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
