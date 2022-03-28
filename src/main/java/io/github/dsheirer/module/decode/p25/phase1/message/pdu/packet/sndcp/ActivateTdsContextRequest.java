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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.ipv4.APCO25IpAddress;
import io.github.dsheirer.module.decode.p25.reference.DataSubscriberUnitType;
import io.github.dsheirer.module.decode.p25.reference.MDPConfigurationOption;
import io.github.dsheirer.module.decode.p25.reference.NetworkAddressType;

import java.util.ArrayList;
import java.util.List;

/**
 * Activate Trunking Data Service (TDS) Context Request
 */
public class ActivateTdsContextRequest extends SNDCPMessage
{
    private static final int[] VERSION = {4, 5, 6, 7};
    private static final int[] NSAPI = {8, 9, 10, 11};
    private static final int[] NAT = {12, 13, 14, 15};
    private static final int[] IPV4_ADDRESS = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33,
        34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] DSUT = {48, 49, 50, 51};
    private static final int[] UDP_IP_HEADER_COMPRESSION_BITMAP = {52, 53, 54, 55};
    private static final int[] IP_HEADER_COMPRESSION_BITMAP = {56, 57, 58, 59, 60, 61, 62, 63};
    private static final int TCP_IP_HEADER_COMPRESSION = 56;
    private static final int[] TCPSS = {64, 65, 66, 67};
    private static final int[] UDPSS = {68, 69, 70, 71};
    private static final int[] MDPCO = {72, 73, 74, 75, 76, 77, 78, 79};

    private Identifier mIPAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an SNDCP message parser instance.
     *
     * @param message containing the binary sequence
     * @param outbound where true is outbound (from repeater) and false is inbound (from mobile)
     */
    public ActivateTdsContextRequest(BinaryMessage message, boolean outbound)
    {
        super(message, outbound);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getPDUType());
        if(hasIPAddress())
        {
            sb.append(" IP:").append(getIPAddress());
            sb.append(" TYPE:").append(getNetworkAddressType());
        }

        sb.append(" NSAPI:").append(getNSAPI());

        return sb.toString();
    }

    /**
     * SNDCP Version Number
     */
    public int getVersion()
    {
        return getMessage().getInt(VERSION);
    }

    /**
     * Network Service Access Point Identifier
     */
    public int getNSAPI()
    {
        return getMessage().getInt(NSAPI);
    }

    /**
     * Network Address Type
     */
    public NetworkAddressType getNetworkAddressType()
    {
        return NetworkAddressType.fromValue(getMessage().getInt(NAT));
    }

    public boolean hasIPAddress()
    {
        NetworkAddressType nat = getNetworkAddressType();

        return nat == NetworkAddressType.IPV4_STATIC_ADDRESS ||
            nat == NetworkAddressType.IPV4_DYNAMIC_ADDRESS;
    }

    public Identifier getIPAddress()
    {
        if(mIPAddress == null && hasIPAddress())
        {
            if(isOutbound())
            {
                mIPAddress = APCO25IpAddress.createTo(getMessage().getInt(IPV4_ADDRESS));
            }
            else
            {
                mIPAddress = APCO25IpAddress.createFrom(getMessage().getInt(IPV4_ADDRESS));
            }
        }

        return mIPAddress;
    }

    /**
     * Identifies the type of user equipment
     */
    public DataSubscriberUnitType getDataSubsciberUnitType()
    {
        return DataSubscriberUnitType.fromValue(getMessage().getInt(DSUT));
    }

    /**
     * Indicates if TCP/IP header compression is either being requested or is in use
     */
    public boolean isTCPIPHeaderCompression()
    {
        return getMessage().get(TCP_IP_HEADER_COMPRESSION);
    }

    /**
     * Indicates if any form of IP header compression is being used/requested.
     */
    public boolean isIPHeaderCompression()
    {
        return getMessage().getInt(IP_HEADER_COMPRESSION_BITMAP) > 0;
    }

    /**
     * Number of concurrent TCP/IP compressed connections when TCP/IP header compression is being used.  See RFC 1144
     */
    public int getTCPIPStateSlots()
    {
        return getMessage().getInt(TCPSS);
    }

    /**
     * Number of concurrent UDP/IP compressed connnections when UDP/IP header compression is being used.
     */
    public int getUDPIPStateSlots()
    {
        return getMessage().getInt(UDPSS);
    }

    /**
     * Indicates if any form of UDP/IP header compression is being used/requested.
     */
    public boolean isUDPHeaderCompression()
    {
        return getMessage().getInt(UDP_IP_HEADER_COMPRESSION_BITMAP) > 0;
    }

    /**
     * MDP Configuration Option;
     */
    public MDPConfigurationOption getMDPConfigurationOptions()
    {
        return MDPConfigurationOption.fromValue(getMessage().getInt(MDPCO));
    }

    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasIPAddress())
            {
                mIdentifiers.add(getIPAddress());
            }
        }

        return mIdentifiers;
    }
}
