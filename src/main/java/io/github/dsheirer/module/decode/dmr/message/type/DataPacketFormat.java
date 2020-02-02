/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
 * Enumeration of data packet formats
 */
public enum DataPacketFormat
{
    UNIFIED_DATA_TRANSPORT(0, "UNIFIED DATA TRANSPORT"),
    RESPONSE_PACKET(1, "RESPONSE PACKET"),
    UNCONFIRMED_DATA_PACKET(2, "UNCONFIRMED DATA PACKET"),
    CONFIRMED_DATA_PACKET(3, "CONFIRMED DATA PACKET"),
    DEFINED_SHORT_DATA(13, "DEFINED SHORT DATA"),
    RAW_OR_STATUS_SHORT_DATA(14, "RAW OR STATUS SHORT DATA"),
    PROPRIETARY_DATA_PACKET(15, "PROPRIETARY DATA PACKET"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    DataPacketFormat(int value, String label)
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
     * Utility method to lookup the entry from a value.
     * @param value for the format
     * @return entry or UNKNOWN
     */
    public static DataPacketFormat fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return UNIFIED_DATA_TRANSPORT;
            case 1:
                return RESPONSE_PACKET;
            case 2:
                return UNCONFIRMED_DATA_PACKET;
            case 3:
                return CONFIRMED_DATA_PACKET;
            case 13:
                return DEFINED_SHORT_DATA;
            case 14:
                return RAW_OR_STATUS_SHORT_DATA;
            case 15:
                return PROPRIETARY_DATA_PACKET;
            default:
                return UNKNOWN;
        }
    }
}
