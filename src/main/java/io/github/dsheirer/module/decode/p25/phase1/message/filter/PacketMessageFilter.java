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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.cellocator.MCGPPacket;
import io.github.dsheirer.module.decode.ip.icmp.ICMPPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.xcmp.XCMPPacket;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPPacketMessage;
import java.util.function.Function;

/**
 * Filter for packet messages
 */
public class PacketMessageFilter extends Filter<IMessage,String>
{
    private static final String KEY_CELLOCATOR = "Cellocator Packet";
    private static final String KEY_IPV4_OTHER = "IPV4 Other Packet";
    private static final String KEY_IPV4_UDP_ICMP = "IPV4 UDP/IP ICMP Packet";
    private static final String KEY_IPV4_UDP_OTHER = "IPV4 UDP/IP Other Packet";
    private static final String KEY_MOTOROLA_ARS = "Motorola Registration (ARS) Packet";
    private static final String KEY_MOTOROLA_LRRP = "Motorola Location Report (LRRP) Packet";
    private static final String KEY_MOTOROLA_XCMP = "Motorola Extensible (XCMP) Packet";
    private static final String KEY_SNDCP = "SNDCP Data Packet";
    private static final String KEY_UNKNOWN = "Unknown Packet";
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructor
     */
    public PacketMessageFilter()
    {
        super("Packet Messages");
        add(new FilterElement<>(KEY_CELLOCATOR));
        add(new FilterElement<>(KEY_IPV4_OTHER));
        add(new FilterElement<>(KEY_IPV4_UDP_ICMP));
        add(new FilterElement<>(KEY_IPV4_UDP_OTHER));
        add(new FilterElement<>(KEY_MOTOROLA_ARS));
        add(new FilterElement<>(KEY_MOTOROLA_LRRP));
        add(new FilterElement<>(KEY_MOTOROLA_XCMP));
        add(new FilterElement<>(KEY_SNDCP));
        add(new FilterElement<>(KEY_UNKNOWN));
    }

    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof PacketMessage && super.canProcess(message);
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            if(message instanceof PacketMessage packetMessage)
            {
                IPacket packet = packetMessage.getPacket();

                if(packet instanceof MCGPPacket)
                {
                    return KEY_CELLOCATOR;
                }
                else if(packet instanceof ICMPPacket)
                {
                    return KEY_IPV4_UDP_ICMP;
                }
                else if(packet instanceof UDPPacket)
                {
                    return KEY_IPV4_UDP_OTHER;
                }
                else if(packet instanceof IPV4Packet)
                {
                    return KEY_IPV4_OTHER;
                }
                else if(packet instanceof SNDCPPacketMessage)
                {
                    return KEY_SNDCP;
                }
                else if(packet instanceof ARSPacket)
                {
                    return KEY_MOTOROLA_ARS;
                }
                else if(packet instanceof LRRPPacket)
                {
                    return KEY_MOTOROLA_LRRP;
                }
                else if(packet instanceof XCMPPacket)
                {
                    return KEY_MOTOROLA_XCMP;
                }

                else
                {
                    return KEY_UNKNOWN;
                }
            }
            return null;
        }
    }
}
