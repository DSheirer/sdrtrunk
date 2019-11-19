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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IBitErrorProvider;
import io.github.dsheirer.module.decode.p25.P25Utils;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.P25Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;

import java.util.List;

public abstract class UMBTCMessage extends P25Message implements IBitErrorProvider
{
    protected static final int[] HEADER_ADDRESS = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] BLOCK_0_OPCODE = {2, 3, 4, 5, 6, 7};

    private PDUSequence mPDUSequence;

    public UMBTCMessage(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(nac, timestamp);
        mPDUSequence = PDUSequence;
    }

    @Override
    protected String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMessageStub());
        sb.append(" ").append(getOpcode());
        P25Utils.pad(sb, 30);

        return sb.toString();
    }

    public UMBTCHeader getHeader()
    {
        return (UMBTCHeader)getPDUSequence().getHeader();
    }

    public Opcode getOpcode()
    {
        if(hasDataBlock(0))
        {
            return Opcode.fromValue(getDataBlock(0).getMessage().getInt(BLOCK_0_OPCODE),
                getHeader().getDirection(), getHeader().getVendor());
        }

        return Opcode.OSP_UNKNOWN;
    }

    public boolean hasDataBlock(int index)
    {
        return getDataBlock(index) != null;
    }

    public UnconfirmedDataBlock getDataBlock(int index)
    {
        DataBlock dataBlock = getPDUSequence().getDataBlocks().get(index);

        if(dataBlock instanceof UnconfirmedDataBlock)
        {
            return (UnconfirmedDataBlock)dataBlock;
        }

        return null;
    }


    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL;
    }

    /**
     * List of identifiers provided by the message
     */
    public abstract List<Identifier> getIdentifiers();

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("NAC:").append(getNAC());

        if(!getPDUSequence().isComplete())
        {
            sb.append(" *INCOMPLETE - RECEIVED ").append(getPDUSequence().getDataBlocks().size()).append("/")
                .append(getPDUSequence().getHeader().getBlocksToFollowCount()).append(" DATA BLOCKS");
        }

        sb.append(" ").append(getPDUSequence().getHeader().toString());

        sb.append(" DATA BLOCKS:").append(getPDUSequence().getDataBlocks().size());

        if(!getPDUSequence().getDataBlocks().isEmpty())
        {
            sb.append(" MSG:");

            for(DataBlock dataBlock : getPDUSequence().getDataBlocks())
            {
                sb.append(dataBlock.getMessage().toHexString());
            }
        }

        return sb.toString();
    }

    public PDUSequence getPDUSequence()
    {
        return mPDUSequence;
    }

    @Override
    public int getBitsProcessedCount()
    {
        return getPDUSequence().getBitsProcessedCount();
    }

    @Override
    public int getBitErrorsCount()
    {
        return getPDUSequence().getBitErrorsCount();
    }
}
