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

public class NegotiateBufferSize extends UltravoxMessage
{
    /**
     * Client request to server to negotiate the server's buffer size
     */
    public NegotiateBufferSize()
    {
        super(UltravoxMessageType.NEGOTIATE_BUFFER_SIZE);
    }

    /**
     * Server response to client request
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     *
     * @param data bytes received from the server
     */
    NegotiateBufferSize(byte[] data)
    {
        super(data);
    }

    /**
     * Sets the desired and minimum requested buffer sizes to send to the server
     * @param desired buffer size in kilobytes
     * @param minimum buffer size in kilobytes
     */
    public void setBufferSize(int desired, int minimum)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(desired).append(":").append(minimum);

        setPayload(sb.toString());
    }

    /**
     * Negotiated buffer size from a server response ultravox.
     *
     * @return server negotiated buffer size, or zero if this is not a server response ultravox, or this is an error
     * server response ultravox, or the server response ultravox buffer size cannot be determined from the ultravox payload.
     */
    public int getBufferSize()
    {
        String payload = getPayload();

        if(payload != null && payload.startsWith("ACK:"))
        {
            try
            {
                int bufferSize = Integer.parseInt(payload.substring(4));
                return bufferSize;
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the value
            }
        }

        return 0;
    }
}
