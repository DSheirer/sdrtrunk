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
package io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox;

import io.github.dsheirer.audio.broadcast.BroadcastFormat;

public class StreamMimeType extends UltravoxMessage
{
    /**
     * Client request to server to set stream MIME type
     */
    public StreamMimeType()
    {
        super(UltravoxMessageType.STREAM_MIME_TYPE);
    }

    /**
     * Server response to client request
     *
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     *
     * @param data bytes received from the server
     */
    StreamMimeType(byte[] data)
    {
        super(data);
    }

    /**
     * Sets the broadcastAudio format or MIME type
     * @param format of the stream
     */
    public void setFormat(BroadcastFormat format)
    {
        setPayload(format.getValue());
    }
}
