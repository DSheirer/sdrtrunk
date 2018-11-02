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

/**
 * SNDCP Network Address Type
 */
public enum NetworkAddressType
{
    IPV4_STATIC_ADDRESS("IPV4 STATIC ADDRESS"),
    IPV4_DYNAMIC_ADDRESS("IPV4 DYNAMIC ADDRESS"),
    RESERVED("RESERVED"),
    NO_ADDRESS("NO ADDRESS"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    NetworkAddressType(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static NetworkAddressType fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return IPV4_STATIC_ADDRESS;
            case 1:
                return IPV4_DYNAMIC_ADDRESS;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
                return RESERVED;
            case 15:
                return NO_ADDRESS;
            default:
                return UNKNOWN;
        }
    }
}