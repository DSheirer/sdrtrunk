/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.audio.convert.thumbdv.message.response;

import io.github.dsheirer.audio.convert.thumbdv.message.PacketField;

/**
 * Set channel format response
 */
public class SetChannelFormatResponse extends AmbeResponse
{
    public SetChannelFormatResponse(byte[] message)
    {
        super(message);
    }

    @Override
    public PacketField getType()
    {
        return PacketField.PKT_CHANNEL_FORMAT;
    }

    /**
     * Success or fail
     */
    public boolean isSuccessful()
    {
        byte[] payload = getPayload();

        return payload != null && payload.length ==1 && payload[0] == 0;
    }

    @Override
    public String toString()
    {
        return "SET CHANNEL FORMAT " + (isSuccessful() ? "SUCCESSFUL" : "**FAILED**");
    }
}
