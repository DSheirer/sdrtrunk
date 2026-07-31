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
 * Response CLASS/TYPE values from paragraph 6.5.22 Response Information
 */
public enum ResponseClassType
{
    ACK_RECEPTION_SUCCESSFUL("ACKNOWLEDGE SUCCESS"),
    ACK_SELECTIVE_RETRY("ACKNOWLEDGE SELECTIVE RETRY"),
    NACK_REQUEST_FULL_RETRY("NACK-REQUEST FULL RETRY"),
    NACK_MEMORY_FULL("NACK-MEMORY FULL"),
    NACK_ABORT("NACK-ABORT"),
    UNKNOWN("UNKNOWN");

    private final String mLabel;

    ResponseClassType(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to look up the entry from the value.
     * @param value to look up
     * @return matching entry or UNKNOWN
     */
    public static ResponseClassType fromValue(int value)
    {
        return switch(value)
        {
            case 1 -> ACK_RECEPTION_SUCCESSFUL;
            case 9 -> ACK_SELECTIVE_RETRY;
            case 25 -> NACK_REQUEST_FULL_RETRY;
            case 26 -> NACK_MEMORY_FULL;
            case 27 -> NACK_ABORT;
            default -> UNKNOWN;
        };
    }
}
