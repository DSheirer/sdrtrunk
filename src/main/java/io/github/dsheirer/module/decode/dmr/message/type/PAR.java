/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * DMR Tier III PAR subfield.  Identifies which mobile subscribers can access a site and indicates if the site
 * has more than one control channel such that the mobile subscribers are partitioned into two groups, A and B.
 */
public enum PAR
{
    RESERVED("RESERVED"),
    CATEGORY_A("MULTIPLE CONTROL CHANNELS - CAT A SUBSCRIBERS"),
    CATEGORY_B("MULTIPLE CONTROL CHANNELS - CAT B SUBSCRIBERS"),
    CATEGORY_A_AND_B("SINGLE CONTROL CHANNEL - CAT A & B SUBSCRIBERS"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    PAR(String label)
    {
        mLabel = label;
    }

    /**
     * Utility method to lookup the model type from the integer value
     * @param value 0-3
     * @return entry or UNKNOWN
     */
    public static PAR fromValue(int value)
    {
        if(0 <= value && value <= 3)
        {
            return PAR.values()[value];
        }

        return UNKNOWN;
    }

    /**
     * Indicates if the site is setup for multiple control channels.
     */
    public boolean isMultipleControlChannels()
    {
        return this == CATEGORY_A || this == CATEGORY_B;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
