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

public enum CallType
{
    GROUP_BROADCAST(0, "GROUP BROADCAST"),
    GROUP_CONFERENCE(1, "GROUP CONFERENCE"),
    UNSPECIFIED(2, "UNSPECIFIED"),
    SESSION_CALL(3, "SESSION CALL"), //On Type-D systems, per dsd-fme
    INDIVIDUAL(4, "INDIVIDUAL"),
    RESERVED_5(5, "RESERVED 5"),
    INTERCONNECT(6, "INTERCONNECT"),
    SPEED_DIAL(7, "SPEED DIAL"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    CallType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    public int getValue()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the call type from the transmitted value.
     *
     * @param value to lookup
     * @return matching entry or UNKNOWN
     */
    public static CallType fromValue(int value)
    {
        return switch(value)
        {
            case 0 -> GROUP_BROADCAST;
            case 1 -> GROUP_CONFERENCE;
            case 2 -> UNSPECIFIED;
            case 3 -> SESSION_CALL;
            case 4 -> INDIVIDUAL;
            case 5 -> RESERVED_5;
            case 6 -> INTERCONNECT;
            case 7 -> SPEED_DIAL;
            default -> UNKNOWN;
        };
    }
}
