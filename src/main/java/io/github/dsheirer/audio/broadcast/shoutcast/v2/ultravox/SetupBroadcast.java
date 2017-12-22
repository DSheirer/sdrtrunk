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

public class SetupBroadcast extends UltravoxMessage
{
    /**
     * Client request to server to specify the stream bit rate
     */
    public SetupBroadcast()
    {
        super(UltravoxMessageType.SETUP_BROADCAST);
    }

    /**
     * Server response to client request
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     *
     * @param data bytes received from the server
     */
    SetupBroadcast(byte[] data)
    {
        super(data);
    }

    /**
     * Sets the average and maximum bit rates.  Set both to the same value for a Constant Bit Rate (CBR) stream.
     *
     * @param average bit rate of the stream in kbps
     * @param maximum bit rate of the stream in kbps
     */
    public void setBitRate(int average, int maximum)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(average).append(":").append(maximum);

        setPayload(sb.toString());
    }
}
