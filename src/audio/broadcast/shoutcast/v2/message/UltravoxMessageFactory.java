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
package audio.broadcast.shoutcast.v2.message;

public class UltravoxMessageFactory
{
    /**
     * Creates an empty message of the specified message type for sending to the server
     */
    public static UltravoxMessage getMessage(UltravoxMessageType type)
    {
        switch(type)
        {
            case AUTHENTICATE_BROADCAST:
                return new AuthenticateBroadcast();
            case CONFIGURE_ICY_GENRE:
                return new ConfigureIcyGenre();
            case CONFIGURE_ICY_NAME:
                return new ConfigureIcyName();
            case CONFIGURE_ICY_PUBLIC:
                return new ConfigureIcyPublic();
            case CONFIGURE_ICY_URL:
                return new ConfigureIcyURL();
            case FLUSH_CACHED_METADATA:
                return new FlushCachedMetadata();
            case MP3_DATA:
                return new MP3Audio();
            case NEGOTIATE_BUFFER_SIZE:
                return new NegotiateBufferSize();
            case NEGOTIATE_MAX_PAYLOAD_SIZE:
                return new NegotiateMaxPayloadSize();
            case REQUEST_CIPHER:
                return new RequestCipher();
            case SETUP_BROADCAST:
                return new SetupBroadcast();
            case STANDBY:
                return new Standby();
            case STREAM_MIME_TYPE:
                return new StreamMimeType();
            case TERMINATE_BROADCAST:
                return new TerminateBroadcast();
            case XML_METADATA:
                return new XMLMetadata();
            default:
                return null;
        }
    }

    /**
     * Creates an ultravox message from the server response bytes.
     *
     * @param bytes in network order (big-endian).
     *
     * @return constructed message or null if the message type cannot be determined or is unrecognized
     */
    public static UltravoxMessage getMessage(byte[] bytes)
    {
        UltravoxMessageType type = getMessageType(bytes);

        switch(type)
        {
            case AUTHENTICATE_BROADCAST:
                return new AuthenticateBroadcast(bytes);
            case CONFIGURE_ICY_GENRE:
                return new ConfigureIcyGenre(bytes);
            case CONFIGURE_ICY_NAME:
                return new ConfigureIcyName(bytes);
            case CONFIGURE_ICY_PUBLIC:
                return new ConfigureIcyPublic(bytes);
            case CONFIGURE_ICY_URL:
                return new ConfigureIcyURL(bytes);
            case FLUSH_CACHED_METADATA:
                return new FlushCachedMetadata(bytes);
            case MP3_DATA:
                return new MP3Audio(bytes);
            case NEGOTIATE_BUFFER_SIZE:
                return new NegotiateBufferSize(bytes);
            case NEGOTIATE_MAX_PAYLOAD_SIZE:
                return new NegotiateMaxPayloadSize(bytes);
            case REQUEST_CIPHER:
                return new RequestCipher(bytes);
            case SETUP_BROADCAST:
                return new SetupBroadcast(bytes);
            case STANDBY:
                return new Standby(bytes);
            case STREAM_MIME_TYPE:
                return new StreamMimeType(bytes);
            case TERMINATE_BROADCAST:
                return new TerminateBroadcast(bytes);
            case XML_METADATA:
                return new XMLMetadata(bytes);
            case UNKNOWN:
            default:
                return null;
        }
    }

    /**
     * Identifies the message type from the server response byte array
     *
     * @param data in little-endian format
     * @return message type or UNKNOWN if the message type cannot be determined.
     */
    private static UltravoxMessageType getMessageType(byte[] data)
    {
        if(data != null && data.length > 4)
        {
            int value = (data[2] << 8) + data[3];

            return UltravoxMessageType.fromValue(value);
        }

        return UltravoxMessageType.UNKNOWN;
    }
}
