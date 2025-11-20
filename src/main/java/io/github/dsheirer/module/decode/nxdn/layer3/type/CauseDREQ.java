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
 * Cause values for disconnect request (DREQ)
 */
public enum CauseDREQ
{
    DISCONNECTED_BY_USER(0x10, "USER DISCONNECT"),
    DISCONNECTED_BY_TIMER(0x14, "TIMER DISCONNECT"),
    OTHER_DISCONNECT(0x1F, "OTHER DISCONNECT"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    /**
     * Constructs an instance
     *
     * @param value for the entry
     * @param label to display
     */
    CauseDREQ(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Transmitted value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Display label.
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the matching entry.
     *
     * @param value to look up
     * @return matching entry or UNKNOWN
     */
    public static CauseDREQ fromValue(int value)
    {
        for(CauseDREQ cause : CauseDREQ.values())
        {
            if(cause.getValue() == value)
            {
                return cause;
            }
        }

        return UNKNOWN;
    }
}
