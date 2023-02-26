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

package io.github.dsheirer.module.decode.ip.mototrbo.ars;

public enum ARSPDUType
{
    DEVICE_REGISTRATION(0x0),
    DEVICE_DEREGISTRATION(0x1),
    QUERY(0x4),
    USER_REGISTRATION(0x5),
    USER_DEREGISTRATION(0x6),
    USER_REGISTRATION_ACKNOWLEDGEMENT(0x7),
    REGISTRATION_ACKNOWLEDGEMENT(0xF),
    UNKNOWN(-1);

    private int mValue;

    ARSPDUType(int value)
    {
        mValue = value;
    }

    public static ARSPDUType fromValue(int value)
    {
        switch(value)
        {
            case 0x0:
                return DEVICE_REGISTRATION;
            case 0x1:
                return DEVICE_DEREGISTRATION;
            case 0x4:
                return QUERY;
            case 0x5:
                return USER_REGISTRATION;
            case 0x6:
                return USER_DEREGISTRATION;
            case 0x7:
                return USER_REGISTRATION_ACKNOWLEDGEMENT;
            case 0xF:
                return REGISTRATION_ACKNOWLEDGEMENT;
        }

        return UNKNOWN;
    }
}
