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

package io.github.dsheirer.module.decode.ip.icmp;

import java.util.Map;
import java.util.TreeMap;

/**
 * ICMP Type:Code enumeration
 */
public enum ICMPTypeCode
{
    ECHO_REPLY(0,0,"ECHO REPLY"),

    //Type: 3 Destination Unreachable
    DESTINATION_NETWORK_UNREACHABLE(3,0, "DESTINATION NETWORK UNREACHABLE"),
    DESTINATION_HOST_UNREACHABLE(3,1, "DESTINATION HOST UNREACHABLE"),
    DESTINATION_PROTOCOL_UNREACHABLE(3,2, "DESTINATION PROTOCOL UNREACHABLE"),
    DESTINATION_PORT_UNREACHABLE(3,3, "DESTINATION PORT UNREACHABLE"),
    FRAGMENTATION_REQUIRED(3,4, "FRAGMENTATION REQUIRED"),
    SOURCE_ROUTE_FAILED(3,5, "SOURCE ROUTE FAILED"),
    DESTINATION_NETWORK_UNKNOWN(3,6, "DESTINATION NETWORK UNKNOWN"),
    DESTINATION_HOST_UNKNOWN(3,7, "DESTINATION HOST UNKNOWN"),
    SOURCE_HOST_ISOLATED(3,8, "SOURCE HOST ISOLATED"),
    NETWORK_ADMINISTRATIVELY_PROHIBITED(3,9, "NETWORK ADMINISTRATIVELY PROHIBITED"),
    HOST_ADMINISTRATIVELY_PROHIBITED(3,10, "HOST ADMINISTRATIVELY PROHIBITED"),
    NETWORK_UNREACHABLE_FOR_TOS(3,11, "NETWORK UNREACHABLE FOR TOS"),
    HOST_UNREACHABLE_FOR_TOS(3,12, "HOST UNREACHABLE FOR TOS"),
    COMMUNICATION_ADMINISTRATIVELY_PROHIBITED(3,13, "COMMUNICATION ADMINISTRATIVELY PROHIBITED"),
    HOST_PRECEDENCE_VIOLATION(3,14, "HOST PRECEDENCE VIOLATION"),
    PRECEDENCE_CUTOFF_IN_EFFECT(3,15, "PRECEDENCE CUTOFF IN EFFECT"),

    //Type: 5 Redirect Message
    REDIRECT_DATAGRAM_FOR_THE_NETWORK(5,0, "REDIRECT DATAGRAM FOR THE NETWORK"),
    REDIRECT_DATAGRAM_FOR_THE_HOST(5,1, "REDIRECT DATAGRAM FOR THE HOST"),
    REDIRECT_DATAGRAM_FOR_THE_TOS_AND_NETWORK(5,2, "REDIRECT DATAGRAM FOR THE TOS & NETWORK"),
    REDIRECT_DATAGRAM_FOR_THE_TOS_AND_HOST(5,3, "REDIRECT DATAGRAM FOR THE TOS & HOST"),

    //Type: 8 Echo Request
    ECHO_REQUEST(8,0, "ECHO REQUEST"),

    //Type: 9 Router Advertisement
    ROUTER_ADVERTISEMENT(9, 0, "ROUTER ADVERTISEMENT"),

    //Type: 10 Router Solicitation
    ROUTER_SOLICITATION(10,0,"ROUTER SOLICITATION"),

    //Type: 11 Time Exceeded
    TTL_EXPIRED_IN_TRANSIT(11, 0, "TTL EXPIRED IN TRANSIT"),
    FRAGMENT_REASSEMBLY_TIME_EXCEEDED(11, 1, "FRAGMENT REASSEMBLY TIME EXCEEDED"),

    //Type: 12 Bad IP Header
    IP_HEADER_ERROR_POINTER(12, 0, "IP HEADER ERROR - POINTER INDICATES ERROR"),
    IP_HEADER_ERROR_MISSING_OPTION(12,1,"IP HEADER ERROR - MISSING REQUIRED OPTION"),
    IP_HEADER_ERROR_BAD_LENGTH(12,2,"IP HEADER ERROR - BAD LENGTH"),

    //Type: 13 Timestamp
    TIMESTAMP(13, 0, "TIMESTAMP"),

    //Type: 14 Timestamp Reply
    TIMESTAMP_REPLY(14, 0, "TIMESTAMP REPLY"),

    //Type: 42 Extended Echo Request
    EXTENDED_ECHO_REQUEST(42, 0, "EXTENDED ECHO REQUEST"),

    //Type: 43 Extended Echo Reply
    EXTENDED_ECHO_REPLY_NO_ERROR(43, 0, "EXTENDED ECHO - NO ERROR"),
    EXTENDED_ECHO_REPLY_MALFORMED_QUERY(43, 1, "EXTENDED ECHO - MALFORMED QUERY"),
    EXTENDED_ECHO_REPLY_NO_INTERFACE(43, 2, "EXTENDED ECHO - NO SUCH INTERFACE"),
    EXTENDED_ECHO_REPLY_NO_TABLE(43, 3, "EXTENDED ECHO - NO SUCH TABLE ENTRY"),
    EXTENDED_ECHO_REPLY_MULTIPLE_INTERFACES(43, 4, "EXTENDED ECHO - MULTIPLE INTERFACES"),

    UNKNOWN(-1, -1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    ICMPTypeCode(int type, int code, String label)
    {
        mValue = (type << 8) + code;
        mLabel = label;
    }

    private static final Map<Integer,ICMPTypeCode> LOOKUP_MAP = new TreeMap<>();

    static
    {
        for(ICMPTypeCode type: ICMPTypeCode.values())
        {
            LOOKUP_MAP.put(type.getValue(), type);
        }
    }

    /**
     * Utility method to lookup the ICMP type:code entry from the combined 16-bit value form the ICMP header
     * @param value to lookup
     * @return entry or UNKNOWN
     */
    public static ICMPTypeCode fromValue(int value)
    {
        if(LOOKUP_MAP.containsKey(value))
        {
            return LOOKUP_MAP.get(value);
        }

        return UNKNOWN;
    }

    /**
     * 16-bit value that is the type and code bytes combined.
     */
    public int getValue()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
