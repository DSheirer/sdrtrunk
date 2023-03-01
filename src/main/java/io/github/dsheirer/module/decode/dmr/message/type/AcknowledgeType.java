/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * Acknowledge message type enumeration
 */
public enum AcknowledgeType
{
    ACKNOWLEDGE(0, "ACKNOWLEDGED"),
    NOT_ACKNOWLEDGE(1, "REJECTED/REFUSED/NOT ACKNOWLEDGED"),
    QUEUED(2, "QUEUED"),
    WAIT(3, "WAIT"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the entry
     * @param label for the entry
     */
    AcknowledgeType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the type from a value.
     * @param value of the acknowledge type
     * @return type or UNKNOWN
     */
    public static AcknowledgeType fromValue(int value)
    {
        if(0 <= value && value <= 3)
        {
            return AcknowledgeType.values()[value];
        }

        return UNKNOWN;
    }
}
