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

package io.github.dsheirer.util;

import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.Point2d;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token.Token;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * Packet utility methods.
 */
public class PacketUtil
{
    /**
     * Extracts a plottable position from a packet.
     * @param packet to inspect and process
     * @return geo position
     */
    public static GeoPosition extractGeoPosition(IPacket packet)
    {
        if(packet instanceof IPV4Packet)
        {
            IPV4Packet ipPacket = (IPV4Packet) packet;

            if(ipPacket.getPayload() instanceof UDPPacket)
            {
                UDPPacket udpPacket = (UDPPacket) ipPacket.getPayload();
                return extractGeoPosition(udpPacket);
            }

            return null;
        }

        if(packet instanceof UDPPacket)
        {
            UDPPacket udpPacket = (UDPPacket) packet;

            if(udpPacket.getPayload() instanceof LRRPPacket)
            {
                LRRPPacket lrrpPacket = (LRRPPacket) udpPacket.getPayload();
                return extractGeoPosition(lrrpPacket);
            }

            return null;
        }

        if(packet instanceof LRRPPacket)
        {
            LRRPPacket lrrpPacket = (LRRPPacket) packet;

            for(Token token : lrrpPacket.getTokens())
            {
                if(token instanceof Point2d)
                {
                    Point2d point2d = (Point2d) token;
                    return new GeoPosition(point2d.getLatitude(), point2d.getLongitude());
                }
            }
        }

        return null;
    }
}
