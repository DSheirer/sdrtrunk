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
 * Motorola MOTOTRBO Network Interface Service application types
 *
 * See: https://cwh050.blogspot.com/2019/08/what-does-mnis-do.html
 *
 * Note: this is a work in progress enumeration
 * Missing: Telemetry, External Data, OTAP, XNA, XCMP, ...
 */
public enum ApplicationType
{
    MNIS_LRRP(0x01, "MNIS LRRP"),
    LOCATION_REQUEST_RESPONSE_PROTOCOL(0x11, "LRRP"),
    AUTOMATIC_REGISTRATION_SERVICE(0x33, "ARS"),
    EXTENSIBLE_COMMAND_MESSAGE_PROTOCOL(0x88, "XCMP"),
    UNKNOWN(0, "UNKNOWN");

    private int mValue;
    private String mLabel;

    ApplicationType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to lookup the application type from a value.
     * @param value to lookup
     * @return application type or UNKNOWN.
     */
    public static ApplicationType fromValue(int value)
    {
        switch(value)
        {
            case 0x01:
                return MNIS_LRRP;
            case 0x11:
                return LOCATION_REQUEST_RESPONSE_PROTOCOL;
            case 0x33:
                return AUTOMATIC_REGISTRATION_SERVICE;
            case 0x88:
                return EXTENSIBLE_COMMAND_MESSAGE_PROTOCOL;
            default:
                return UNKNOWN;
        }
    }
}
