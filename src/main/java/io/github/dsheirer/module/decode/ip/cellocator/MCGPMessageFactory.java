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

package io.github.dsheirer.module.decode.ip.cellocator;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Creates an MCGP packet and header for parsing the contents of the packet.
 */
public class MCGPMessageFactory
{
    /**
     * Creates an MCGP packet message from the binary message at the specified binary message offset.
     * @param message containing transmitted bits
     * @param offset into the message for the start of the MCGP payload
     * @return MCGP packet message parser
     */
    public static MCGPPacket create(CorrectedBinaryMessage message, int offset)
    {
        MCGPHeader header = new MCGPHeader(message, offset);
        int payloadOffset = offset + MCGPHeader.HEADER_LENGTH;

        switch(header.getMessageType())
        {
            case INBOUND_GENERIC_COMMAND:
                return new GenericCommandMessage(header, message, payloadOffset);
            case INBOUND_GENERAL_ACKNOWLEDGE:
                return new AcknowledgeMessage(header, message, payloadOffset);
            case INBOUND_PROGRAMMING_COMMAND:
                return new ProgrammingCommandMessage(header, message, payloadOffset);
            case OUTBOUND_LOCATION_STATUS:
                return new LocationStatusMessage(header, message, payloadOffset);
            case OUTBOUND_PROGRAMMING_STATUS:
                return new ProgrammingStatusMessage(header, message, payloadOffset);
            case OUTBOUND_FORWARDED_REALTIME_DATA:
            case OUTBOUND_FORWARDED_LOGGED_DATA_FRAGMENT:
                return new ForwardedDataMessage(header, message, payloadOffset);

            case INBOUND_MODULAR_REQUEST:
            case OUTBOUND_MODULAR_RESPONSE:
                //Both messages are variable length and use message type 9 so we have to inspect the total message
                //length field for the response message which should be 0 for the request variant and non-zero for
                //the response variant.
                if(ModularResponseMessage.getTotalMessageLength(message, payloadOffset) > 0)
                {
                    header.setMessageType(MCGPMessageType.OUTBOUND_MODULAR_RESPONSE);
                    return new ModularResponseMessage(header, message, payloadOffset);
                }
                else
                {
                    return new ModularRequestMessage(header, message, payloadOffset);
                }

            case UNKNOWN:
            default:
                return new UnknownMCGPMessage(header, message, offset);
        }
    }
}
