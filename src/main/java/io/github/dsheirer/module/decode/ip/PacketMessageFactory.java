/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.ip;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.ip.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Header;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import io.github.dsheirer.module.decode.p25.message.pdu.packet.sndcp.SNDCPPacketHeader;
import io.github.dsheirer.module.decode.p25.reference.IPHeaderCompression;

/**
 * Message parser factory for packet based binary sequences.
 */
public class PacketMessageFactory
{
    /**
     * Creates an IP packet message parser
     *
     * @param message containing an IP packet
     * @param offset to the IP packet within the message
     * @return constructed packet message parser
     */
    public static IPacket create(SNDCPPacketHeader sndcpPacketHeader, BinaryMessage message, int offset)
    {
        if(sndcpPacketHeader.getIPHeaderCompression() == IPHeaderCompression.NONE)
        {
            int version = IPV4Header.getIPVersion(message, offset);

            switch(version)
            {
                case 4:
                    return new IPV4Packet(message, offset);
                default:
                    return new UnknownPacket(message, offset);
            }
        }
        else
        {
            return new UnknownCompressedHeaderPacket(sndcpPacketHeader, message, offset);
        }
    }

    /**
     * Creates an IP packet protocol payload message parser.
     *
     * @param protocol of the IP packet payload
     * @param binaryMessage containing the IP packet and payload
     * @param offset to the start of the IP packet payload
     * @return constructed packet message parser
     */
    public static IPacket create(IPProtocol protocol, BinaryMessage binaryMessage, int offset)
    {
        switch(protocol)
        {
            case UDP:
                return new UDPPacket(binaryMessage, offset);
            default:
                return new UnknownPacket(binaryMessage, offset);
        }
    }

    /**
     * Creates a UDP/IP packet payload parser
     *
     * @param destinationPort for the packet (to identify the protocol/format)
     * @param binaryMessage containing the IP/UDP payload
     * @param offset in the message to the start of the payload
     * @return constructed packet messge parser
     */
    public static IPacket createUDPPayload(int destinationPort, BinaryMessage binaryMessage, int offset)
    {
        switch(destinationPort)
        {
            case 4001: //Location Service
                break;
            case 4004: //XCMP Service
                break;
            case 4005: //Automatic Registration Service
                return new ARSPacket(binaryMessage, offset);
            case 4007: //Text Message Service
                break;
            case 4008: //Telemetry Service
                break;
        }

        return new UnknownPacket(binaryMessage, offset);
    }
}
