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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.response;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequenceMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import io.github.dsheirer.module.decode.p25.reference.PacketResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response message
 */
public class ResponseMessage extends PDUSequenceMessage
{
    private List<Identifier> mIdentifiers;

    /**
     * Construct an instance
     * @param PDUSequence containing the response message
     * @param nac value
     * @param timestamp for the message
     */
    public ResponseMessage(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    private ResponseHeader getHeader()
    {
        return (ResponseHeader)getPDUSequence().getHeader();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("NAC:").append(getNAC());
        if(!isValid())
        {
            sb.append("***CRC-FAIL*** ");
        }

        sb.append("PDU RESPONSE");

        if(getHeader().hasSourceLLID())
        {
            sb.append(" FROM:").append(getHeader().getSourceLLID());
        }

        sb.append(" TO:").append(getHeader().getTargetLLID());
        sb.append(" ").append(getResponseText());
        return sb.toString();
    }

    public String getResponseText()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getHeader().getResponse());

        if(getPDUSequence().isComplete())
        {
            if(getHeader().getResponse() == PacketResponse.SELECTIVE_RETRY)
            {
                sb.append(" - RESEND BLOCKS ").append(getMissingBlockNumbers());
            }
            else
            {
                if(getPDUSequence().getDataBlocks().size() > 0)
                {
                    sb.append(" BLOCKS TO FOLLOW:").append(getPDUSequence().getDataBlocks().size());
                    sb.append(" DATA BLOCKS:").append(getPDUSequence().getDataBlocks().size());

                    if(!getPDUSequence().getDataBlocks().isEmpty())
                    {
                        sb.append(" MSG:");

                        for(DataBlock dataBlock: getPDUSequence().getDataBlocks())
                        {
                            sb.append(dataBlock.getMessage().toHexString());
                        }
                    }
                }
            }
        }
        else
        {
            sb.append(" *INCOMPLETE - RECEIVED ").append(getPDUSequence().getDataBlocks().size()).append("/")
                    .append(getHeader().getBlocksToFollowCount()).append(" DATA BLOCKS");
        }

        return sb.toString();
    }

    /**
     * Identifies missing block numbers for Selective Retry type response messages.
     */
    public List<Integer> getMissingBlockNumbers()
    {
        int dataBlockCount = getPDUSequence().getDataBlocks().size();

        if(0 < dataBlockCount && dataBlockCount <= 2)
        {
            List<Integer> missingBlockNumbers = new ArrayList<>();

            int blockOffset = 0;

            for(DataBlock dataBlock: getPDUSequence().getDataBlocks())
            {
                missingBlockNumbers.addAll(getMissingBlockNumbers(dataBlock, blockOffset))            ;

                blockOffset += 64;
            }

            Collections.sort(missingBlockNumbers);
            return missingBlockNumbers;
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Identifies the missing block numbers for a selective retry type response message from the
     * appended data block.  Up to 127 (max block size for a PDU) missing blocks can be identified
     * across 2 data blocks with each data block holding a maximum of 64 block indices.
     *
     * The format for the missing block indices is arranged in a super easy (not) to parse format:
     *
     *  7  6  5  4  3  2  1  0
     * 15 14 13 12 ...
     * ...
     * 63 62 61 60 59 58 57 54
     *
     * @param dataBlock
     * @param offset
     * @return
     */
    private List<Integer> getMissingBlockNumbers(DataBlock dataBlock, int offset)
    {
        List<Integer> missingBlockNumbers = new ArrayList<>();

        int blockIndex = 7;

        BinaryMessage message = dataBlock.getMessage();

        for(int index = 0; index < 64; index++)
        {
            if(!message.get(index))
            {
                missingBlockNumbers.add(blockIndex + offset);
            }

            blockIndex--;

            if((blockIndex ^ 8) == 0)
            {
                blockIndex += 8;
            }
        }

        return missingBlockNumbers;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getHeader().getTargetLLID());

            if(getHeader().hasSourceLLID())
            {
                mIdentifiers.add(getHeader().getSourceLLID());
            }
        }

        return mIdentifiers;
    }
}
