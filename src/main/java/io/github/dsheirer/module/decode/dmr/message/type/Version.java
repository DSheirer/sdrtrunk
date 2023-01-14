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
 * DMR Tier III ETSI 102 361-4 ICD Versions
 */
public enum Version
{
    VERSION_0("1.0.0-1.5.1"),
    VERSION_1("1.6.1"),
    VERSION_2("1.7.1-1.9.1"),
    VERSION_3("RESERVED 3"),
    VERSION_4("RESERVED 4"),
    VERSION_5("RESERVED 5"),
    VERSION_6("RESERVED 6"),
    VERSION_7("RESERVED 7"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    Version(String label)
    {
        mLabel = label;
    }

    /**
     * Utility method to lookup the model type from the integer value
     * @param value 0-7
     * @return entry or UNKNOWN
     */
    public static Version fromValue(int value)
    {
        if(0 <= value && value <= 7)
        {
            return Version.values()[value];
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
