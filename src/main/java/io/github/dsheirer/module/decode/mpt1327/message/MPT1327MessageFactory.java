/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.module.decode.mpt1327.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.mpt1327.MPTMessageType;

/**
 * Message factory for creating MPT-1327 messages
 */
public class MPT1327MessageFactory
{
    public static MPT1327BaseMessage create(MPTMessageType messageType, CorrectedBinaryMessage message, long timestamp)
    {
        switch(messageType)
        {
            case GTC_GO_TO_TRAFFIC_CHANNEL:
                return new GoToTrafficChannel(message, timestamp);
            case UNKNOWN:
            default:
                return new UnknownMessage(message, timestamp);
        }
    }

    /**
     * Indicates the number of additional 64-bit blocks beyond the two default 64-bit blocks for each message.
     * @param messageType for the specified message
     * @param message
     * @return
     */
    public static int getBlockLength(MPTMessageType messageType, CorrectedBinaryMessage message)
    {
        switch(messageType)
        {

        }

        return 0;
    }
}
