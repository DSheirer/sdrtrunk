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

package io.github.dsheirer.module.decode.p25.message.pdu.ambtc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.message.IBitErrorProvider;
import io.github.dsheirer.module.decode.p25.P25Utils;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.pdu.DataBlock;
import io.github.dsheirer.module.decode.p25.message.pdu.PacketSequence;
import io.github.dsheirer.module.decode.p25.message.pdu.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.List;

public abstract class AMBTCMessage extends P25Message implements IBitErrorProvider
{
    protected static final int[] HEADER_ADDRESS = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47};

    private PacketSequence mPacketSequence;

    public AMBTCMessage(PacketSequence packetSequence, int nac, long timestamp)
    {
        super(nac, timestamp);
        mPacketSequence = packetSequence;
    }

    @Override
    protected String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMessageStub());
        sb.append(" ").append(getHeader().getOpcode());
        P25Utils.pad(sb, 30);

        return sb.toString();
    }

    public AMBTCHeader getHeader()
    {
        return (AMBTCHeader)getPacketSequence().getHeader();
    }

    public boolean hasDataBlock(int index)
    {
        return getDataBlock(index) != null;
    }

    public UnconfirmedDataBlock getDataBlock(int index)
    {
        DataBlock dataBlock = getPacketSequence().getDataBlocks().get(index);

        if(dataBlock instanceof UnconfirmedDataBlock)
        {
            return (UnconfirmedDataBlock)dataBlock;
        }

        return null;
    }


    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL;
    }

    /**
     * List of identifiers provided by the message
     */
    public abstract List<IIdentifier> getIdentifiers();

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("NAC:").append(getNAC());

        if(!getPacketSequence().isComplete())
        {
            sb.append(" *INCOMPLETE - RECEIVED ").append(getPacketSequence().getDataBlocks().size()).append("/")
                .append(getPacketSequence().getHeader().getBlocksToFollowCount()).append(" DATA BLOCKS");
        }

        sb.append(" ").append(getPacketSequence().getHeader().toString());

        sb.append(" DATA BLOCKS:").append(getPacketSequence().getDataBlocks().size());

        if(!getPacketSequence().getDataBlocks().isEmpty())
        {
            sb.append(" MSG:");

            for(DataBlock dataBlock: getPacketSequence().getDataBlocks())
            {
                sb.append(dataBlock.getMessage().toHexString());
            }
        }

        sb.append(" COMPLETE:").append(getMessage().toHexString());

        return sb.toString();
    }

    public PacketSequence getPacketSequence()
    {
        return mPacketSequence;
    }

    private void extractMessage()
    {
        //There are 16 bits in the header
        int length = 16;

        int blockCount = getPacketSequence().getHeader().getBlocksToFollowCount();

        //Each block provides 108 bits/12 bytes and the final block uses 32-bits for CRC
        length += (blockCount * 96);

        CorrectedBinaryMessage consolidatedMessage = new CorrectedBinaryMessage(length);

        //Transfer 2 octets from header
        AMBTCHeader header = (AMBTCHeader)getPacketSequence().getHeader();

        int dataOctetsValue = header.getDataOctets();
        consolidatedMessage.load(0, 16, dataOctetsValue);

        if(getPacketSequence().isComplete())
        {
            int offset = 16;
            for(DataBlock dataBlock: getPacketSequence().getDataBlocks())
            {
                if(dataBlock instanceof UnconfirmedDataBlock)
                {
                    BinaryMessage dataBlockMessage = dataBlock.getMessage();

                    for(int x = 0; x < dataBlockMessage.size(); x++)
                    {
                        if(dataBlockMessage.get(x))
                        {
                            consolidatedMessage.set(x + offset);
                        }
                    }

                    offset += dataBlockMessage.size();
                }
            }
        }
        else
        {
            setValid(false);
        }

        setMessage(consolidatedMessage);
    }

    @Override
    public int getBitsProcessedCount()
    {
        return getPacketSequence().getBitsProcessedCount();
    }

    @Override
    public int getBitErrorsCount()
    {
        return getPacketSequence().getBitErrorsCount();
    }
}
