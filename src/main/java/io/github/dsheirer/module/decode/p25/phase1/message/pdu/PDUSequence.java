/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1.message.pdu;

import io.github.dsheirer.message.IBitErrorProvider;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet Data Sequence comprised of a Packet Data Header and zero or more Data Blocks.
 */
public class PDUSequence implements IBitErrorProvider
{
    private long mTimestamp;
    private int mNAC;
    private PDUHeader mHeader;
    private List<DataBlock> mDataBlocks = new ArrayList<>();

    public PDUSequence(PDUHeader pduHeader, long timestamp, int nac)
    {
        mHeader = pduHeader;
        mTimestamp = timestamp;
        mNAC = nac;
    }

    /**
     * Network Access Code (NAC)
     */
    public int getNAC()
    {
        return mNAC;
    }

    /**
     * Timestamp when the header was transmitted
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Indicates if this message contains all of the data blocks specified by the header
     */
    public boolean isComplete()
    {
        return getHeader().getBlocksToFollowCount() == mDataBlocks.size();
    }

    /**
     * Adds the deinterleaved, corrected binary message to this sequence as a datablock.  The data
     * block is decoded according to the header confirmed/unconfirmed indicator.
     *
     * @param dataBlock for (un)confirmed data
     */
    public void addDataBlock(DataBlock dataBlock)
    {
        if(dataBlock != null)
        {
            mDataBlocks.add(dataBlock);
        }
    }

    public boolean hasDataBlock(int index)
    {
        return getDataBlock(index) != null;
    }

    public DataBlock getDataBlock(int index)
    {
        if(index < mDataBlocks.size())
        {
            return mDataBlocks.get(index);
        }

        return null;
    }

    /**
     * Data blocks that follow the header
     */
    public List<DataBlock> getDataBlocks()
    {
        return mDataBlocks;
    }

    /**
     * Packet Data Unit Header
     */
    public PDUHeader getHeader()
    {
        return mHeader;
    }

    @Override
    public int getBitsProcessedCount()
    {
        int processed = getHeader().getBitsProcessedCount();

        for(DataBlock dataBlock : mDataBlocks)
        {
            processed += dataBlock.getBitsProcessedCount();
        }

        return processed;
    }

    @Override
    public int getBitErrorsCount()
    {
        int errorCount = getHeader().getBitErrorsCount();

        for(DataBlock dataBlock : mDataBlocks)
        {
            errorCount += dataBlock.getBitErrorsCount();
        }

        return errorCount;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("NAC:").append(getNAC());

        if(!isComplete())
        {
            sb.append(" *INCOMPLETE - RECEIVED ").append(mDataBlocks.size()).append("/")
                .append(getHeader().getBlocksToFollowCount()).append(" DATA BLOCKS");
        }

        sb.append(" ").append(getHeader().toString());

        sb.append(" DATA BLOCKS:").append(mDataBlocks.size());

        if(!mDataBlocks.isEmpty())
        {
            sb.append(" MSG:");

            for(DataBlock dataBlock: mDataBlocks)
            {
                sb.append(dataBlock.getMessage().toHexString());
            }
        }

        return sb.toString();
    }
}
