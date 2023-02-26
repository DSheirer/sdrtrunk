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

package io.github.dsheirer.module.decode.ip;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.ip.cellocator.MCGPHeader;
import io.github.dsheirer.module.decode.ip.cellocator.MCGPMessageFactory;
import io.github.dsheirer.module.decode.ip.icmp.ICMPPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Header;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.xcmp.XCMPPacket;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import io.github.dsheirer.module.decode.ip.udp.UDPPort;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPPacketHeader;
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
    public static IPacket create(SNDCPPacketHeader sndcpPacketHeader, CorrectedBinaryMessage message, int offset)
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
     * Creates an IP packet message parser
     *
     * @param message containing an IP packet
     * @param offset to the IP packet within the message
     * @return constructed packet message parser
     */
    public static IPacket create(CorrectedBinaryMessage message, int offset)
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

    /**
     * Creates an IP packet protocol payload message parser.
     *
     * @param protocol of the IP packet payload
     * @param binaryMessage containing the IP packet and payload
     * @param offset to the start of the IP packet payload
     * @return constructed packet message parser
     */
    public static IPacket create(IPProtocol protocol, CorrectedBinaryMessage binaryMessage, int offset)
    {
        switch(protocol)
        {
            case ICMP:
                return new ICMPPacket(binaryMessage, offset);
            case UDP:
                return new UDPPacket(binaryMessage, offset);
            default:
                return new UnknownPacket(binaryMessage, offset);
        }
    }

    /**
     * Creates a UDP/IP packet payload parser
     *
     * @param sourcePort for the packet (to identify the protocol/format)
     * @param destinationPort for the packet (to identify the protocol/format)
     * @param binaryMessage containing the IP/UDP payload
     * @param offset in the message to the start of the payload
     * @return constructed packet message parser
     */
    public static IPacket createUDPPayload(UDPPort sourcePort, UDPPort destinationPort, CorrectedBinaryMessage binaryMessage, int offset)
    {
        switch(destinationPort.getValue())
        {
            case 231: //Cellocator
                if(MCGPHeader.isCellocatorMessage(binaryMessage, offset))
                {
                    return MCGPMessageFactory.create(binaryMessage, offset);
                }
                break;
            case 4001: //Location Service
                return new LRRPPacket(binaryMessage, offset);
            case 4004: //XCMP Service
                return new XCMPPacket(binaryMessage, offset);
            case 4005: //Automatic Registration Service
                return new ARSPacket(binaryMessage, offset);
            case 4007: //Text Message Service
                break;
            case 4008: //Telemetry Service
                break;
            case 4009: //Over The Air Programming (OTAP)
                break;
            case 4012: //Battery Management
                break;
            case 4013: //Job Ticket Server
                break;
        }

        switch(sourcePort.getValue())
        {
            case 231:
                //Cellocator
                if(MCGPHeader.isCellocatorMessage(binaryMessage, offset))
                {
                    return MCGPMessageFactory.create(binaryMessage, offset);
                }
                break;
        }

        //This is normally source or destination port 231, but can be on other ports as well.
        if(MCGPHeader.isCellocatorMessage(binaryMessage, offset))
        {
            return MCGPMessageFactory.create(binaryMessage, offset);
        }

        return new UnknownPacket(binaryMessage, offset);
    }
}
