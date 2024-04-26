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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.PacketMessageFactory;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.ConfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPPacketHeader;
import io.github.dsheirer.module.decode.p25.reference.IPHeaderCompression;
import io.github.dsheirer.module.decode.p25.reference.UDPHeaderCompression;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet Data Unit (PDU) sequence containing IP packet data.
 */
public class PacketMessage extends P25P1Message
{
//    private final static Logger mLog = LoggerFactory.getLogger(PacketMessage.class);

    private PDUSequence mPDUSequence;
    private BinaryMessage mPayload;
    private CorrectedBinaryMessage mPacketMessage;
    private SNDCPPacketHeader mSNDCPPacketHeader;
    private IPacket mPacket;
    private List<Identifier> mIdentifiers;

    public PacketMessage(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(null, nac, timestamp);
        mPDUSequence = PDUSequence;
    }

    /**
     * Packet Data Unit (PDU) header and data block(s) for this message
     */
    public PDUSequence getPDUSequence()
    {
        return mPDUSequence;
    }

    /**
     * Packet header from the packet sequence.
     */
    public PacketHeader getHeader()
    {
        return (PacketHeader)getPDUSequence().getHeader();
    }

    public SNDCPPacketHeader getSNDCPPacketHeader()
    {
        if(mSNDCPPacketHeader == null)
        {
            if(getPDUSequence().isComplete() && getHeader().getDataHeaderOffset() == 2)
            {
                if(getPDUSequence().getDataBlocks().size() >= 1)
                {
                    BinaryMessage message = getPDUSequence().getDataBlocks().get(0).getMessage().getSubMessage(0, 16);
                    mSNDCPPacketHeader = new SNDCPPacketHeader(message, getHeader().isOutbound());
                }
                else
                {
                    mSNDCPPacketHeader = new SNDCPPacketHeader(getHeader().isOutbound());
                }
            }
            else
            {
                mSNDCPPacketHeader = new SNDCPPacketHeader(getHeader().isOutbound());
            }
        }

        return mSNDCPPacketHeader;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(!getPDUSequence().isComplete())
        {
            sb.append(" *INCOMPLETE - RECEIVED ").append(getPDUSequence().getDataBlocks().size()).append("/")
                .append(getPDUSequence().getHeader().getBlocksToFollowCount()).append(" DATA BLOCKS");
        }

        if(getPacket() instanceof IPV4Packet)
        {
            sb.append(" LLID:").append(getHeader().getTargetLLID());

            SNDCPPacketHeader sndcpPacketHeader = getSNDCPPacketHeader();
            sb.append(" NSAPI:").append(sndcpPacketHeader.getNSAPI());

            if(sndcpPacketHeader.getIPHeaderCompression() != IPHeaderCompression.NONE)
            {
                sb.append("IP HEADER COMPRESSION:").append(sndcpPacketHeader.getIPHeaderCompression());
            }
            if(sndcpPacketHeader.getUDPHeaderCompression() != UDPHeaderCompression.NONE)
            {
                sb.append("UDP HEADER COMPRESSION:").append(sndcpPacketHeader.getUDPHeaderCompression());
            }

            sb.append(" ").append(getPacket());
        }
        else
        {
            sb.append(" ").append(getPDUSequence().getHeader().toString());

            if(getHeader().isConfirmationRequired())
            {
                sb.append(" DATA BLOCKS ").append(getDataBlockSequenceNumbers());
            }
            else
            {
                sb.append(" DATA BLOCKS:").append(getPDUSequence().getDataBlocks().size());
            }

            if(!getPDUSequence().getDataBlocks().isEmpty())
            {
                sb.append(" MSG:");

                for(DataBlock dataBlock : getPDUSequence().getDataBlocks())
                {
                    sb.append(dataBlock.getMessage().toHexString());
                }
            }

            sb.append(" PAYLOAD:").append(getPayloadMessage().toHexString());
            sb.append(" ").append(getPacket());
        }
        return sb.toString();
    }

    public List<Integer> getDataBlockSequenceNumbers()
    {
        List<Integer> numbers = new ArrayList<>();

        for(DataBlock dataBlock : getPDUSequence().getDataBlocks())
        {
            if(dataBlock instanceof ConfirmedDataBlock)
            {
                int sequenceNumber = ((ConfirmedDataBlock)dataBlock).getSequenceNumber();

                if(sequenceNumber != -1)
                {
                    numbers.add(sequenceNumber);
                }
            }
        }

        return numbers;
    }

    public IPacket getPacket()
    {
        if(mPacket == null)
        {
            mPacket = PacketMessageFactory.create(getSNDCPPacketHeader(), getPacketMessage(), 0);
        }

        return mPacket;
    }

    public CorrectedBinaryMessage getPacketMessage()
    {
        if(mPacketMessage == null)
        {
            int start = getHeader().getDataHeaderOffset() * 8;

            if(getPayloadMessage().size() >= start + 32)
            {
                int end = getPayloadMessage().size() - 32;  //CRC
                end -= (getHeader().getPadOctetCount() * 8);
                if(end > start)
                {
                    BinaryMessage message = getPayloadMessage().getSubMessage(start, end);
                    mPacketMessage = new CorrectedBinaryMessage(message);
                }
            }

            if(mPacketMessage == null)
            {
                mPacketMessage = new CorrectedBinaryMessage(0);
            }
        }

        return mPacketMessage;
    }

    /**
     * Combined binary payload of the data blocks including any header offset data and the 32-bit packet CRC.
     * Note: for confirmed packets, this payload does not include the data block sequence number and block CRC data.
     * Data blocks are assumed to be received in the correct order and no attempt is made to fill in for
     * any missing data blocks.
     */
    public BinaryMessage getPayloadMessage()
    {
        if(mPayload == null)
        {
            int octetCount = 0;

            if(getHeader().isConfirmationRequired())
            {
                //Confirmed 3/4 rate encoded data blocks
                octetCount += (16 * getHeader().getBlocksToFollowCount());
            }
            else
            {
                //Unconfirmed 1/2 rate encoded data blocks
                octetCount += (12 * getHeader().getBlocksToFollowCount());
            }

            mPayload = new BinaryMessage(octetCount * 8);

            int pointer = 0;

            List<DataBlock> dataBlocks = getPDUSequence().getDataBlocks();

            for(DataBlock dataBlock : dataBlocks)
            {
                BinaryMessage blockPayload = dataBlock.getMessage();
                mPayload.load(pointer, blockPayload);
                pointer += blockPayload.size();
            }
        }

        return mPayload;
    }

    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.IP_PACKET_DATA;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getHeader().getTargetLLID());

            if(getPacket() != null)
            {
                mIdentifiers.addAll(getPacket().getIdentifiers());
            }
        }

        return mIdentifiers;
    }
}
