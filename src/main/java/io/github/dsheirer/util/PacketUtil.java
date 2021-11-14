package io.github.dsheirer.util;

import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.lrrp.token.Position;
import io.github.dsheirer.module.decode.ip.lrrp.token.Token;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import org.jdesktop.swingx.mapviewer.GeoPosition;

public class PacketUtil {
    public static GeoPosition extractGeoPosition(IPacket packet) {
        if (packet instanceof IPV4Packet) {
            IPV4Packet ipPacket = (IPV4Packet) packet;

            if (ipPacket.getPayload() instanceof UDPPacket) {
                UDPPacket udpPacket = (UDPPacket) ipPacket.getPayload();
                return extractGeoPosition(udpPacket);
            }

            return null;
        }

        if (packet instanceof UDPPacket) {
            UDPPacket udpPacket = (UDPPacket) packet;

            if (udpPacket.getPayload() instanceof LRRPPacket) {
                LRRPPacket lrrpPacket = (LRRPPacket) udpPacket.getPayload();
                return extractGeoPosition(lrrpPacket);
            }

            return null;
        }

        if (packet instanceof LRRPPacket) {
            LRRPPacket lrrpPacket = (LRRPPacket) packet;

            for (Token token: lrrpPacket.getTokens())
            {
                if (token instanceof Position) {
                    Position position = (Position) token;
                    return new GeoPosition(position.getLatitude(), position.getLongitude());
                }
            }
        }

        return null;
    }
}
