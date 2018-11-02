/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.reference;

public enum PDUPriorityMaximumType
{
    PRI_0("LOW-0"),
    PRI_1("MED-1"),
    PRI_2("MED-2"),
    PRI_3("MED-3"),
    PRI_4("MED-4"),
    PRI_5("MED-5"),
    PRI_6("MED-6"),
    PRI_7("MED-7"),
    PRI_8("MED-8"),
    PRI_9("MED-9"),
    PRI_10("MED-10"),
    PRI_11("MED-11"),
    PRI_12("MED-12"),
    PRI_13("MED-13"),
    PRI_14("HIGH-14"),
    PRI_15("EMERGENCY-15"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    PDUPriorityMaximumType(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static PDUPriorityMaximumType fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return PDUPriorityMaximumType.values()[value];
        }

        return UNKNOWN;
    }
}