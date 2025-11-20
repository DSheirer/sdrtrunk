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

package io.github.dsheirer.module.decode.nxdn.layer3;

public enum NXDNMessageType
{
    TC_VOICE_CALL(1, "VOICE CALL"),
    TC_VOICE_CALL_INITIALIZATION_VECTOR(3, "VOICE CALL INITIALIZATION VECTOR"),
    TC_TRANSMISSION_RELEASE_EXTENSION(7, "TRANSMISSION RELEASE EXTENSION"),
    TC_TRANSMISSION_RELEASE(8, "TRANSMISSION RELEASE"),
    TC_DATA_CALL_HEADER(9, "DATA CALL HEADER"),
    TC_DATA_CALL_BLOCK(11, "DATA CALL USER DATA"),
    TC_DATA_CALL_ACKNOWLEDGE(12, "DATA CALL ACKNOWLEDGE"),
    TC_HEADER_DELAY(15, "HEADER DELAY"),
    TC_STATUS_INQUIRY_REQUEST(48, "STATUS INQUIRY REQUEST"),
    TC_STATUS_INQUIRY_RESPONSE(49, "STATUS INQUIRY RESPONSE"),
    TC_STATUS_REQUEST(50, "STATUS REQUEST"),
    TC_STATUS_RESPONSE(51, "STATUS RESPONSE"),
    TC_REMOTE_CONTROL_REQUEST(52, "REMOTE CONTROL REQUEST"),
    TC_REMOTE_CONTROL_RESPONSE(53, "REMOTE CONTROL RESPONSE"),
    TC_REMOTE_CONTROL_REQUEST_WITH_ESN(54, "REMOTE CONTROL REQUEST WITH ESN"),
    TC_REMOTE_CONTROL_RESPONSE_WITH_ESN(55, "REMOTE CONTROL RESPONSE WITH ESN"),
    TC_SHORT_DATA_CALL_REQUEST_HEADER(56, "SHORT DATA CALL REQUEST HEADER"),
    TC_SHORT_DATA_CALL_BLOCK(57, "SHORT DATA CALL REQUEST USER"),
    TC_SHORT_DATA_CALL_INITIALIZATION_VECTOR(58, "SHORT DATA CALL INITIALIZATION VECTOR"),
    TC_SHORT_DATA_CALL_RESPONSE(59, "SHORT DATA CALL RESPONSE"),

    CC_VOICE_CALL_REQUEST(1, "VOICE CALL REQUEST"),
    CC_VOICE_CALL_RESPONSE(1, "VOICE CALL RESPONSE"),
    CC_VOICE_CALL_RECEPTION_REQUEST(2, "VOICE CALL RECEPTION REQUEST"),
    CC_VOICE_CALL_RECEPTION_RESPONSE(2, "VOICE CALL RECEPTION RESPONSE"),
    CC_VOICE_CALL_CONNECTION_REQUEST(3, "VOICE CALL CONNECTION REQUEST"),
    CC_VOICE_CALL_CONNECTION_RESPONSE(3, "VOICE CALL CONNECTION RESPONSE"),
    CC_VOICE_CALL_ASSIGNMENT(4, "VOICE CALL ASSIGNMENT"),
    CC_VOICE_CALL_ASSIGNMENT_DUPLICATE(5, "VOICE CALL ASSIGNMENT DUPLICATE"),
    CC_DATA_CALL_REQUEST(9, "DATA CALL REQUEST"),
    CC_DATA_CALL_RESPONSE(9, "DATA CALL RESPONSE"),
    CC_DATA_CALL_RECEPTION_REQUEST(10, "DATA CALL RECEPTION REQUEST"),
    CC_DATA_CALL_RECEPTION_RESPONSE(10, "DATA CALL RECEPTION RESPONSE"),
    CC_DATA_CALL_ASSIGNMENT_DUPLICATE(13, "DATA CALL ASSIGNMENT DUPLICATE"),
    CC_DATA_CALL_ASSIGNMENT(14, "DATA CALL ASSIGNMENT"),
    CC_IDLE(16, "IDLE"),
    CC_DISCONNECT_REQUEST(17, "DISCONNECT REQUEST"),
    CC_DISCONNECT(17, "DISCONNECT"),

    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     *
     * @param value for the entry
     */
    private NXDNMessageType(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Value for the enumeration type
     * @return value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Display label
     * @return label
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Utility method to look up the inbound control channel message type from a value.
     *
     * @param value to lookup
     * @return matching entry or UNKNOWN if the value is not recognized.
     */
    public static NXDNMessageType getCCInbound(int value)
    {
        return switch(value)
        {
            case 1 -> CC_VOICE_CALL_REQUEST;
            case 2 -> CC_VOICE_CALL_RECEPTION_REQUEST;
            case 3 -> CC_VOICE_CALL_CONNECTION_REQUEST;
            case 4 -> CC_VOICE_CALL_ASSIGNMENT;
            case 5 -> CC_VOICE_CALL_ASSIGNMENT_DUPLICATE;
            case 9 -> CC_DATA_CALL_REQUEST;
            case 10 -> CC_DATA_CALL_RECEPTION_REQUEST;
            case 13 -> CC_DATA_CALL_ASSIGNMENT_DUPLICATE;
            case 14 -> CC_DATA_CALL_ASSIGNMENT;
            case 16 -> CC_IDLE;
            case 17 -> CC_DISCONNECT_REQUEST;
            default -> UNKNOWN;
        };
    }

    /**
     * Utility method to look up the outbound control channel message type from a value.
     *
     * @param value to lookup
     * @return matching entry or UNKNOWN if the value is not recognized.
     */
    public static NXDNMessageType getCCOutbound(int value)
    {
        return switch(value)
        {
            case 1 -> CC_VOICE_CALL_RESPONSE;
            case 2 -> CC_VOICE_CALL_RECEPTION_RESPONSE;
            case 3 -> CC_VOICE_CALL_CONNECTION_RESPONSE;
            case 4 -> CC_VOICE_CALL_ASSIGNMENT;
            case 5 -> CC_VOICE_CALL_ASSIGNMENT_DUPLICATE;
            case 9 -> CC_DATA_CALL_RESPONSE;
            case 10 -> CC_DATA_CALL_RECEPTION_RESPONSE;
            case 13 -> CC_DATA_CALL_ASSIGNMENT_DUPLICATE;
            case 14 -> CC_DATA_CALL_ASSIGNMENT;
            case 16 -> CC_IDLE;
            case 17 -> CC_DISCONNECT;
            default -> UNKNOWN;
        };
    }

    /**
     * Utility method to look up the inbound traffic channel message type from a value.
     *
     * @param value to lookup
     * @return matching entry or UNKNOWN if the value is not recognized.
     */
    public static NXDNMessageType getTCOutbound(int value)
    {
        return switch(value)
        {
            case 1 -> TC_VOICE_CALL;
            case 3 -> TC_VOICE_CALL_INITIALIZATION_VECTOR;
            case 7 -> TC_TRANSMISSION_RELEASE_EXTENSION;
            case 8 -> TC_TRANSMISSION_RELEASE;
            case 9 -> TC_DATA_CALL_HEADER;
            case 11 -> TC_DATA_CALL_BLOCK;
            case 12 -> TC_DATA_CALL_ACKNOWLEDGE;
            case 15 -> TC_HEADER_DELAY;
            case 48 -> TC_STATUS_INQUIRY_REQUEST;
            case 49 -> TC_STATUS_INQUIRY_RESPONSE;
            case 50 -> TC_STATUS_REQUEST;
            case 51 -> TC_STATUS_RESPONSE;
            case 52 -> TC_REMOTE_CONTROL_REQUEST;
            case 53 -> TC_REMOTE_CONTROL_RESPONSE;
            case 54 -> TC_REMOTE_CONTROL_REQUEST_WITH_ESN;
            case 55 -> TC_REMOTE_CONTROL_RESPONSE_WITH_ESN;
            case 56 -> TC_SHORT_DATA_CALL_REQUEST_HEADER;
            case 57 -> TC_SHORT_DATA_CALL_BLOCK;
            case 58 -> TC_SHORT_DATA_CALL_INITIALIZATION_VECTOR;
            case 59 -> TC_SHORT_DATA_CALL_RESPONSE;
            default -> UNKNOWN;
        };
    }
}
