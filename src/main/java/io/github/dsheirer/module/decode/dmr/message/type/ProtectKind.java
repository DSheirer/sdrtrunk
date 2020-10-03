/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
 * Protect Kind enumeration
 *
 * Defined in: TS 102 361-4 7.2.21 Protect_Kind
 */
public enum ProtectKind
{
    DISABLE_PTT(0, "DISABLE PTT"),
    ENABLE_PTT(1, "ENABLE PTT"),
    ILLEGALLY_PARKED(2, "ILLEGALLY PARKED"),
    ENABLE_TARGET_ID_PTT(3, "ENABLE TARGET ID PTT ONLY"),
    //All other values reserved
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the item
     * @param label pretty string
     */
    ProtectKind(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Pretty version of the entry
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the entry from a value
     * @param value 0-3
     * @return entry or UNKNOWN
     */
    public static ProtectKind fromValue(int value)
    {
        if(0 <= value && value <= 3)
        {
            return ProtectKind.values()[value];
        }

        return UNKNOWN;
    }
}
