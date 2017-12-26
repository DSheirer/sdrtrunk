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

public class ConfigureIcyURL extends UltravoxMessage
{
    /**
     * Client request to server to configure the ICY-URL metadata
     */
    public ConfigureIcyURL()
    {
        super(UltravoxMessageType.CONFIGURE_ICY_URL);
    }

    /**
     * Server response to client request
     * Package private constructor.  Use the UltravoxMessageFactory for this constructor.
     *
     * @param data bytes received from the server
     */
    ConfigureIcyURL(byte[] data)
    {
        super(data);
    }

    public void setURL(String url)
    {
        setPayload(url);
    }
}
