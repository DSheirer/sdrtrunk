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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IBitErrorProvider;
import io.github.dsheirer.module.decode.p25.P25Utils;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.DataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import java.util.List;

public abstract class AMBTCMessage extends P25P1Message implements IBitErrorProvider
{
    protected static final int[] HEADER_ADDRESS = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47};

    private PDUSequence mPDUSequence;

    public AMBTCMessage(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(nac, timestamp);
        mPDUSequence = PDUSequence;
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
        return (AMBTCHeader)getPDUSequence().getHeader();
    }

    public boolean hasDataBlock(int index)
    {
        return index < getPDUSequence().getDataBlocks().size() && getDataBlock(index) != null;
    }

    public UnconfirmedDataBlock getDataBlock(int index)
    {
        if(index < getPDUSequence().getDataBlocks().size())
        {
            DataBlock dataBlock = getPDUSequence().getDataBlocks().get(index);

            if(dataBlock instanceof UnconfirmedDataBlock udb)
            {
                return udb;
            }
        }

        return null;
    }


    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL;
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

            for(DataBlock dataBlock: getPDUSequence().getDataBlocks())
            {
                sb.append(dataBlock.getMessage().toHexString());
            }
        }

        sb.append(" COMPLETE:").append(getMessage().toHexString());

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
