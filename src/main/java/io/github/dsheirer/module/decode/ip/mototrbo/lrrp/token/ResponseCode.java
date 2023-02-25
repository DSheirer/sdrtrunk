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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

/**
 * LRRP Response codes.
 *
 * From: https://github.com/pboyd04/Moto.Net/blob/master/Moto.Net/Mototrbo/LRRP/LRRPResponseCodes.cs
 */
public enum ResponseCode
{
    SUCCESS(0x0, "SUCCESS"),
    INVALID_COMMAND(0x0A, "INVALID COMMAND"),
    UNKNOWN_ERROR_F(0x0F, "UNRECOGNIZED ERROR [0F]"),
    NO_GPS(0x10, "NO GPS"),
    DUPLICATE_REQUEST(0x16, "DUPLICATE REQUEST"),
    GPS_INITIALIZING(0x200, "GPS INITIALIZING"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value of the code
     * @param label for pretty printing
     */
    ResponseCode(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Numeric value of the response code
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Lookup a response code from a value
     * @param value to lookup
     * @return matching response code or UNKNOWN
     */
    public static ResponseCode fromValue(int value)
    {
        for(ResponseCode responseCode: ResponseCode.values())
        {
            if(responseCode.getValue() == value)
            {
                return responseCode;
            }
        }

        return UNKNOWN;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
