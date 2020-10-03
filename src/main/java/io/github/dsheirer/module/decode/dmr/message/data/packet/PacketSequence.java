/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.packet;

import io.github.dsheirer.module.decode.dmr.message.data.block.DataBlock;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Preamble;
import io.github.dsheirer.module.decode.dmr.message.data.header.PacketSequenceHeader;
import io.github.dsheirer.module.decode.dmr.message.data.header.ProprietaryDataHeader;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequence of DMR packets that contain a header and one or more data blocks.
 */
public class PacketSequence
{
    private int mTimeslot;
    private PacketSequenceHeader mPacketSequenceHeader;
    private ProprietaryDataHeader mProprietaryDataHeader;
    private List<Preamble> mPreambles = new ArrayList<>();
    private List<DataBlock> mDataBlocks = new ArrayList<>();

    public PacketSequence(int timeslot)
    {
        mTimeslot = timeslot;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("PACKET SEQUENCE ");
        sb.append(" TS:").append(getTimeslot());
        if(mPacketSequenceHeader != null)
        {
            sb.append(mPacketSequenceHeader.toString());
        }
        sb.append(" HEADER:").append(hasPacketSequenceHeader());
        sb.append(" PROPRIETARY HEADER:").append(hasProprietaryDataHeader());
        sb.append(" DATA BLOCKS:").append(mDataBlocks.size());
        sb.append(" PREAMBLES:").append(mPreambles.size());
        return sb.toString();
    }

    /**
     * Indicates if this packet sequence is complete with a primary header, an optional proprietary header and data
     * blocks where the count of data blocks and proprietary header equals the blocks to follow count from the
     * primary header.
     */
    public boolean isComplete()
    {
        int count = 0;

        if(hasProprietaryDataHeader())
        {
            count++;
        }

        count += mDataBlocks.size();

        if(hasPacketSequenceHeader())
        {
            return getPacketSequenceHeader().getBlocksToFollow() == count;
        }

        return false;
    }

    public int getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Packet sequence header
     */
    public void setPacketSequenceHeader(PacketSequenceHeader header)
    {
        mPacketSequenceHeader = header;
    }

    /**
     * Proprietary data header
     */
    public void setProprietaryHeader(ProprietaryDataHeader header)
    {
        mProprietaryDataHeader = header;
    }


    /**
     * Adds a preamble to this packet sequence
     */
    public void addPreamble(Preamble preamble)
    {
        mPreambles.add(preamble);
    }

    /**
     * Adds a data block to this sequence
     */
    public void addDataBlock(DataBlock dataBlock)
    {
        mDataBlocks.add(dataBlock);
    }

    /**
     * Primary packet sequence header
     */
    public PacketSequenceHeader getPacketSequenceHeader()
    {
        return mPacketSequenceHeader;
    }

    /**
     * Indicates if this packet sequence has a primary header
     */
    public boolean hasPacketSequenceHeader()
    {
        return mPacketSequenceHeader != null;
    }

    /**
     * Secondary/Proprietary packet sequence header
     */
    public ProprietaryDataHeader getProprietaryDataHeader()
    {
        return mProprietaryDataHeader;
    }

    /**
     * Indicates if this packet sequence has a secondary proprietary header
     */
    public boolean hasProprietaryDataHeader()
    {
        return mProprietaryDataHeader != null;
    }

    /**
     * Data blocks for this packet sequence
     */
    public List<DataBlock> getDataBlocks()
    {
        return mDataBlocks;
    }

    /**
     * Indicates if this packet sequence has captured any data blocks.
     */
    public boolean hasDataBlocks()
    {
        return !mDataBlocks.isEmpty();
    }
}
