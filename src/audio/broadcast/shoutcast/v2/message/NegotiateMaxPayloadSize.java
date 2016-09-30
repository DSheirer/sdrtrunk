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

public class NegotiateMaxPayloadSize extends UltravoxMessage
{
    /**
     * Client request to server to negotiate maximum payload size
     *
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     */
    NegotiateMaxPayloadSize()
    {
        super(UltravoxMessageType.NEGOTIATE_MAX_PAYLOAD_SIZE);
    }

    /**
     * Server response to client request
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     *
     * @param data bytes received from the server
     */
    NegotiateMaxPayloadSize(byte[] data)
    {
        super(data);
    }

    /**
     * Sets the maximum payload size.
     *
     * @param desiredMaximum payload size (no larger than 16377)
     * @param minimum acceptable payload size (no larger than 16377)
     */
    public void setMaximumPayloadSize(int desiredMaximum, int minimum)
    {
        assert(desiredMaximum <= 16377);
        assert(minimum <= 16377);

        StringBuilder sb = new StringBuilder();
        sb.append(desiredMaximum).append(":").append(minimum);

        setPayload(sb.toString());
    }

    /**
     * Negotiated maximum payload size from a server response message.
     *
     * @return server negotiated maximum payload size, or zero if this is not a server response message, or this is an
     * error server response message, or the server response message maximum payload size cannot be determined from the
     * message payload.
     */
    public int getMaximumPayloadSize()
    {
        String payload = getPayload();

        if(payload != null && payload.startsWith("ACK:"))
        {
            try
            {
                int maximumPayloadSize = Integer.parseInt(payload.substring(4));
                return maximumPayloadSize;
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the value
            }
        }

        return 0;
    }
}
