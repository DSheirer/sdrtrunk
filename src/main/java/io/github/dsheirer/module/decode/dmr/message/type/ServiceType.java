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
 * Service Type enumeration for Unified Single Block Data (USBD) messages
 *
 * See: TS 102 361-1 and TS 102 361.4
 */
public enum ServiceType
{
    LIP_SHORT_LOCATION_REQUEST(0, "LIP SHORT LOCATION REQUEST"),
    RESERVED_1(1, "RESERVED 1"),
    RESERVED_2(2, "RESERVED 2"),
    RESERVED_3(3, "RESERVED 3"),
    RESERVED_4(4, "RESERVED 4"),
    RESERVED_5(5, "RESERVED 5"),
    RESERVED_6(6, "RESERVED 6"),
    RESERVED_7(7, "RESERVED 7"),
    VENDOR_SERVICE_1(8, "VENDOR SERVICE 1"),
    VENDOR_SERVICE_2(9, "VENDOR SERVICE 2"),
    VENDOR_SERVICE_3(10, "VENDOR SERVICE 3"),
    VENDOR_SERVICE_4(11, "VENDOR SERVICE 4"),
    VENDOR_SERVICE_5(12, "VENDOR SERVICE 5"),
    VENDOR_SERVICE_6(13, "VENDOR SERVICE 6"),
    VENDOR_SERVICE_7(14, "VENDOR SERVICE 7"),
    VENDOR_SERVICE_8(15, "VENDOR SERVICE 8"),

    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the entry
     * @param label for the entry
     */
    ServiceType(int value, String label)
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
     * @param value of the service type
     * @return type or UNKNOWN
     */
    public static ServiceType fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return ServiceType.values()[value];
        }

        return UNKNOWN;
    }
}
