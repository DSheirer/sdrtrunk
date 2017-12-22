/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox;

public enum UltravoxMessageType
{
    AUTHENTICATE_BROADCAST(0x1001),
    SETUP_BROADCAST(0x1002),
    NEGOTIATE_BUFFER_SIZE(0x1003),
    STANDBY(0x1004),
    TERMINATE_BROADCAST(0x1005),
    FLUSH_CACHED_METADATA(0x1006),
    NEGOTIATE_MAX_PAYLOAD_SIZE(0x1008),
    REQUEST_CIPHER(0x1009),
    STREAM_MIME_TYPE(0x1040),
    CONFIGURE_ICY_NAME(0x1100),
    CONFIGURE_ICY_GENRE(0x1101),
    CONFIGURE_ICY_URL(0x1102),
    CONFIGURE_ICY_PUBLIC(0x1103),
    CACHEABLE_XML_METADATA(0x3902),
    PASS_THROUGH_XML_METADATA(0x5902),
    MP3_DATA(0x7000),
    UNKNOWN(0x0000);

    private int mValue;

    private UltravoxMessageType(int value)
    {
        mValue = value;
    }

    public int getValue()
    {
        return mValue;
    }

    public static UltravoxMessageType fromValue(int value)
    {
        switch(value)
        {
            case 0x1001:
                return AUTHENTICATE_BROADCAST;
            case 0x1002:
                return SETUP_BROADCAST;
            case 0x1003:
                return NEGOTIATE_BUFFER_SIZE;
            case 0x1004:
                return STANDBY;
            case 0x1005:
                return TERMINATE_BROADCAST;
            case 0x1006:
                return FLUSH_CACHED_METADATA;
            case 0x1008:
                return NEGOTIATE_MAX_PAYLOAD_SIZE;
            case 0x1009:
                return REQUEST_CIPHER;
            case 0x1040:
                return STREAM_MIME_TYPE;
            case 0x1100:
                return CONFIGURE_ICY_NAME;
            case 0x1101:
                return CONFIGURE_ICY_GENRE;
            case 0x1102:
                return CONFIGURE_ICY_URL;
            case 0x1103:
                return CONFIGURE_ICY_PUBLIC;
            case 0x3902:
                return CACHEABLE_XML_METADATA;
            case 0x5902:
                return PASS_THROUGH_XML_METADATA;
            case 0x7000:
                return MP3_DATA;
            default:
                return UNKNOWN;
        }
    }
}
