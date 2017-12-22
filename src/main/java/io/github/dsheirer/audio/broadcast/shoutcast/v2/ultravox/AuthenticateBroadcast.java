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

import io.github.dsheirer.util.XTEA;

import java.util.Arrays;

public class AuthenticateBroadcast extends UltravoxMessage
{
    public static final String STREAM_ID_ERROR = "Stream ID Error";

    /**
     * Client request to server to send authentication credentials
     */
    public AuthenticateBroadcast()
    {
        super(UltravoxMessageType.AUTHENTICATE_BROADCAST);
    }


    /**
     * Server response to client request
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     *
     * @param data bytes received from the server
     */
    AuthenticateBroadcast(byte[] data)
    {
        super(data);
    }

    /**
     * Set credential to authenticate the stream with the server
     *
     * @param encryptionKey received from the server in the RequestCipher message
     * @param streamID that we're going to stream to
     * @param userID for the stream.  Note: this is optional and can be null.  The server currently ignores this value.
     * @param password for the stream.
     */
    public void setCredentials(String encryptionKey, int streamID, String userID, String password)
    {
        XTEA xtea = new XTEA(encryptionKey);

        StringBuilder sb = new StringBuilder();

        sb.append(ULTRAVOX_VERSION);
        sb.append(":");
        sb.append(streamID);
        sb.append(":");
        sb.append(userID != null ? encrypt(xtea, userID) : "");
        sb.append(":");
        sb.append(password != null ? encrypt(xtea, password) : "");

        setPayload(sb.toString());
    }

    /**
     * Encrypts the value using the XTEA block cipher algorithm.  The byte array is sliced into 8-byte chunks and each
     * chunk is encrypted into a 16 hexadecimal character string.  All encrypted chunks are concatenated and returned
     * as a single string.  If the value byte array is not an even multiple of 8 characters, it will be padded with
     * zero valued bytes to make it an even multiple of eight.
     *
     * @param xtea instance of the XTEA algorithm with a loaded encryption key
     * @param value to encode
     * @return value encrypted into one or more multiples of 16 hexadecimal characters
     */
    private static String encrypt(XTEA xtea, byte[] value)
    {
        if(value.length % 8 != 0 || value.length == 0)
        {
            int newLength = ((value.length / 8) + 1) * 8;
            value = Arrays.copyOf(value, newLength);
        }

        StringBuilder sb = new StringBuilder();

        for(int x = 0; x < value.length; x += 8)
        {
            byte[] plaintext = Arrays.copyOfRange(value, x, x + 8);
            byte[] encrypted = xtea.encrypt(plaintext);

            for(byte b: encrypted)
            {
                sb.append(String.format("%02x", b));
            }
        }

        return sb.toString();
    }

    /**
     * Encrypts the value using the XTEA block cipher algorithm and returns the encrypted value as a 16 hex value
     *
     * @param xtea instance of the XTEA algorithm with a loaded encryption key
     * @param value to encode
     * @return encrypted password as 64-bit, 16 character hexadecimal value
     */
    private static String encrypt(XTEA xtea, String value)
    {
        return encrypt(xtea, value.getBytes());
    }
}
