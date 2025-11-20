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
 * SU radio status enumeration
 */
public enum Status
{
    NO_STATUS(0x00, "NO STATUS"),
    USER_DEFINED(0x01, "USER DEFINED"),
    PAGING_STATUS(0xD0, "PAGING STATUS"),
    RESERVED(0xDF, "RESERVED"),
    EMERGENCY(0xE0, "EMERGENCY"),
    EMERGENCY_MAN_DOWN(0xE1, "EMERGENCY MAN DOWN"),
    EMERGENCY_TERMINATION(0xE2, "EMERGENCY TERMINATION"),
    EMERGENCY_STATIONARY_DETECTION(0xE3, "EMERGENCY BY STATIONARY DETECTION"),
    EMERGENCY_MOTION_DETECTION(0xE4, "EMERGENCY BY MOTION DETECTION"),
    EMERGENCY_LONE_WORKER(0xE5, "EMERGENCY LONE WORKER"),
    PREDEFINED(0xE6, "PREDEFINED"),
    UNKNOWN(-1, "UNKNOWN");

    private final int mValue;
    private final String mLabel;

    /**
     * Constructs an instance
     *
     * @param value transmitted
     */
    Status(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public int getValue()
    {
        return mValue;
    }

    public static Status fromValue(int value)
    {
        if(0x01 <= value && value <= 0xCF)
        {
            return USER_DEFINED;
        }
        else if((0xD1 <= value && value <= 0xDF) || (0xE6 <= value && value <= 0xFF))
        {
            return RESERVED;
        }

        return switch(value)
        {
            case 0x00 -> NO_STATUS;
            case 0xD0 -> PAGING_STATUS;
            case 0xE0 -> EMERGENCY;
            case 0xE1 -> EMERGENCY_MAN_DOWN;
            case 0xE2 -> EMERGENCY_TERMINATION;
            case 0xE3 -> EMERGENCY_STATIONARY_DETECTION;
            case 0xE4 -> EMERGENCY_MOTION_DETECTION;
            case 0xE5 -> EMERGENCY_LONE_WORKER;
            default -> UNKNOWN;
        };
    }
}
