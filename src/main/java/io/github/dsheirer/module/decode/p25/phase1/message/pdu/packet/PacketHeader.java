/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUHeader;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;

public class PacketHeader extends PDUHeader
{
    public static final int[] SAP_ID = {10, 11, 12, 13, 14, 15};
    public static final int FULL_MESSAGE_FLAG = 48;
    public static final int[] PAD_OCTET_COUNT = {59, 60, 61, 62, 63};
    public static final int SYNCHRONIZE_FLAG = 64;
    public static final int[] PACKET_SEQUENCE_NUMBER = {65,66};
    public static final int[] MESSAGE_FRAGMENT_SEQUENCE_NUMBER = {67,68,69};
    public static final int[] DATA_HEADER_OFFSET = {74, 75, 76, 77, 78, 79};

    public PacketHeader(CorrectedBinaryMessage message, boolean passesCRC)
    {
        super(message, passesCRC);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append(" *CRC-FAIL*");
        }

        sb.append(isConfirmationRequired() ? " CONFIRMED" : " UNCONFIRMED");
        sb.append(" ").append(getDirection());
        sb.append(" FMT:").append(getFormat().getLabel());
        sb.append(" SAP:").append(getServiceAccessPoint().name());
        sb.append(" VEND:").append(getVendor().getLabel());
        sb.append(isOutbound() ? "TO" : "FROM").append(" LLID:").append(getTargetLLID());
        sb.append(" BLKS TO FOLLOW:").append(getBlocksToFollowCount());

        if(isSynchronize())
        {
            sb.append(" PACKET:").append(getPacketSequenceNumber());
            sb.append(" MSG FRAGMENT:").append(getMessageFragmentSequenceNumber());
        }

        sb.append(" PAD OCTETS:").append(getPadOctetCount());
        sb.append(" HEADER OFFSET:").append(getDataHeaderOffset());

        return sb.toString();
    }

    /**
     * Service Access Point (SAP) - determines the network service that will process this packet
     */
    public ServiceAccessPoint getServiceAccessPoint()
    {
        return ServiceAccessPoint.fromValue(getMessage().getInt(SAP_ID));
    }

    /**
     * Indicates if this is the first time this packet header has been transmitted.  A value of false
     * indicates this is a retransmitted packet header.
     */
    public boolean isFullMessage()
    {
        return getMessage().get(FULL_MESSAGE_FLAG);
    }

    /**
     * Number of octets (bytes) that are appended to the end of the packet to make a full final block
     */
    public int getPadOctetCount()
    {
        return getMessage().getInt(PAD_OCTET_COUNT);
    }

    /**
     * Indicates that this header contains packet and message fragment sequence numbers
     */
    public boolean isSynchronize()
    {
        return getMessage().get(SYNCHRONIZE_FLAG);
    }

    /**
     * Packet sequence number for correct ordering by the receiver.
     */
    public int getPacketSequenceNumber()
    {
        return getMessage().getInt(PACKET_SEQUENCE_NUMBER);
    }

    /**
     * Message fragment sequence number for correct ordering by the receiver
     */
    public int getMessageFragmentSequenceNumber()
    {
        return getMessage().getInt(MESSAGE_FRAGMENT_SEQUENCE_NUMBER);
    }

    /**
     * Number of bytes in the data portion of the packet that are dedicated to the header.  This
     * offset is used to identify the start of the data portion of the message.
     */
    public int getDataHeaderOffset()
    {
        return getMessage().getInt(DATA_HEADER_OFFSET);
    }
}
