/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.mototrbo.xcmp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.UnknownPacket;

public class XCMPPacketFactory
{
    /**
     * Creates an XCMP Packet payload
     * @param message containing the full packet payload
     * @param offset to the start of the XCMP message
     * @return XCMP packet parser
     */
    public static IPacket create(CorrectedBinaryMessage message, int offset, XCMPMessageType messageType)
    {
        switch(messageType)
        {
            case NETWORK_FREQUENCY_FILE:
                return new NetworkFrequencyFile(message, offset);
            case UNKNOWN:
            default:
                return new UnknownPacket(message, offset);
        }
    }
}
