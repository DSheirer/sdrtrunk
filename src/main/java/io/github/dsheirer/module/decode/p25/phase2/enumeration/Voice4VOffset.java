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

package io.github.dsheirer.module.decode.p25.phase2.enumeration;

/**
 * Indicates the offset from a current MAC message to the next start of a 4V voice frame sequence
 */
public enum Voice4VOffset
{
    SLOTS_1("NEXT NON-SACCH SLOT"),
    SLOTS_2("2 NON-SACCH SLOTS"),
    SLOTS_3("3 NON-SACCH SLOTS"),
    SLOTS_4("4 NON-SACCH SLOTS"),
    SLOTS_5("5 NON-SACCH SLOTS"),
    SLOTS_6("6 NON-SACCH SLOTS"),
    INBOUND("INBOUND"),
    NO_VOICE("NO VOICE FRAMING"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    Voice4VOffset(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static Voice4VOffset fromValue(int value)
    {
        if(0 <= value && value <= 7)
        {
            return Voice4VOffset.values()[value];
        }

        return UNKNOWN;
    }
}
