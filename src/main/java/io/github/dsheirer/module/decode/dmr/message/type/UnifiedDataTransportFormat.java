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
 * Payload Formats for Unified Data Transport (UDT) packets
 */
public enum UnifiedDataTransportFormat
{
    BINARY("BINARY"),
    MOBILE_SUBSCRIBER_OR_TALKGROUP_ADDRESS("MS OR TG ADDRESS"),
    BCD_4_BITS("4-BIT BCD"),
    ASCII_7("ISO 7-BIT"),
    ASCII_8("ISO 8-BIT"),
    NMEA_GPS_LOCATION_CODED("NMEA GPS CODED"),
    IP_ADDRESS("IP ADDRESS"),
    UNICODE_16("16-BIT UNICODE"),
    VENDOR_PROPRIETARY_8("VENDOR SPECIFIC-8"),
    VENDOR_PROPRIETARY_9("VENDOR SPECIFIC-9"),
    MIXED_FORMAT("MIXED ADDRESS AND UNICODE"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    UnifiedDataTransportFormat(String label)
    {
        mLabel = label;
    }

    public static UnifiedDataTransportFormat fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return BINARY;
            case 1:
                return MOBILE_SUBSCRIBER_OR_TALKGROUP_ADDRESS;
            case 2:
                return BCD_4_BITS;
            case 3:
                return ASCII_7;
            case 4:
                return ASCII_8;
            case 5:
                return NMEA_GPS_LOCATION_CODED;
            case 6:
                return IP_ADDRESS;
            case 7:
                return UNICODE_16;
            case 8:
                return VENDOR_PROPRIETARY_8;
            case 9:
                return VENDOR_PROPRIETARY_9;
            case 10:
                return MIXED_FORMAT;
            default:
                return UNKNOWN;
        }
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
