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
 * Enumeration of service access point (SAP)
 */
public enum ServiceAccessPoint
{
    UNIFIED_DATA_TRANSPORT("UNIFIED DATA TRANSPORT"),
    SAP_1("RESERVED 1"),
    TCP_IP_COMPRESSED_HEADER("TCP/IP HEADER COMPRESSION"),
    UDP_IP_COMPRESSED_HEADER("UDP/IP HEADER COMPRESSION"),
    IP_PACKET_DATA("IP PACKET DATA"),
    ARP("ARP"),
    SAP_6("RESERVED 6"),
    SAP_7("RESERVED 7"),
    SAP_8("RESERVED 8"),
    PROPRIETARY_DATA("PROPRIETARY DATA"),
    SHORT_DATA("SHORT DATA"),
    SAP_11("RESERVED 11"),
    SAP_12("RESERVED 12"),
    SAP_13("RESERVED 13"),
    SAP_14("RESERVED 14"),
    SAP_15("RESERVED 15"),
    UNKNOWN("UNKNOWN");

    private String mLabel;

    ServiceAccessPoint(String label)
    {
        mLabel = label;
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
    public static ServiceAccessPoint fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return UNIFIED_DATA_TRANSPORT;
            case 1:
                return SAP_1;
            case 2:
                return TCP_IP_COMPRESSED_HEADER;
            case 3:
                return UDP_IP_COMPRESSED_HEADER;
            case 4:
                return IP_PACKET_DATA;
            case 5:
                return ARP;
            case 6:
                return SAP_6;
            case 7:
                return SAP_7;
            case 8:
                return SAP_8;
            case 9:
                return PROPRIETARY_DATA;
            case 10:
                return SHORT_DATA;
            case 11:
                return SAP_11;
            case 12:
                return SAP_12;
            case 13:
                return SAP_13;
            case 14:
                return SAP_14;
            case 15:
                return SAP_15;
            default:
                return UNKNOWN;
        }
    }
}
