/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.ip.Header;
import io.github.dsheirer.module.decode.p25.reference.IPHeaderCompression;
import io.github.dsheirer.module.decode.p25.reference.PDUType;
import io.github.dsheirer.module.decode.p25.reference.UDPHeaderCompression;

/**
 * SNDCP header for a packet message.  This header contains IP and UDP compression indicators.
 *
 * Note: this header can be constructed with no data to ensure that it is never null for the case
 * of packet messages that do not contain an SNDCP header.
 */
public class SNDCPPacketHeader extends Header
{
    private static final int[] PDU_TYPE = {0, 1, 2, 3};
    private static final int[] NSAPI = {4, 5, 6, 7};
    private static final int[] PACKET_HEADER_COMPRESSION = {8, 9, 10, 11};
    private static final int[] DATAGRAM_HEADER_COMPRESSION = {12, 13, 14, 15};

    private boolean mOutboundMessage;

    public SNDCPPacketHeader(BinaryMessage message, boolean outboundMessage)
    {
        super(message, 0);
        mOutboundMessage = outboundMessage;
    }

    @Override
    public int getLength()
    {
        return getMessage().size();
    }

    /**
     * Constructs an empty header with no data.
     */
    public SNDCPPacketHeader(boolean outboundMessage)
    {
        this(new BinaryMessage(0), outboundMessage);
    }

    public boolean isOutboundMessage()
    {
        return mOutboundMessage;
    }

    /**
     * Indicates if this header has data or if it is an empty header
     */
    public boolean hasData()
    {
        return getLength() == 16;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(hasData())
        {
            sb.append(getPDUType());
            sb.append(" NSAPI:").append(getNSAPI());
            sb.append(" IP COMPRESSION:").append(getIPHeaderCompression());
            sb.append(" UDP COMPRESSION:").append(getUDPHeaderCompression());
        }
        else
        {
            sb.append("NO SNDCP HEADER");
        }

        return sb.toString();
    }

    public PDUType getPDUType()
    {
        if(hasData())
        {
            return PDUType.fromValue(getMessage().getInt(PDU_TYPE), isOutboundMessage());
        }

        return isOutboundMessage() ? PDUType.OUTBOUND_UNKNOWN : PDUType.INBOUND_UNKNOWN;
    }

    public int getNSAPI()
    {
        return getMessage().getInt(NSAPI);
    }

    public IPHeaderCompression getIPHeaderCompression()
    {
        return IPHeaderCompression.fromValue(getMessage().getInt(PACKET_HEADER_COMPRESSION));
    }

    public UDPHeaderCompression getUDPHeaderCompression()
    {
        return UDPHeaderCompression.fromValue(getMessage().getInt(DATAGRAM_HEADER_COMPRESSION));
    }
}
