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

package io.github.dsheirer.module.decode.dmr.message.data.mbc;

import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.header.MBCHeader;

import java.util.ArrayList;
import java.util.List;

/**
 * Reassembled Multi-Block CSBK Message
 */
public abstract class MultiCSBK extends CSBKMessage
{
    protected List<MBCContinuationBlock> mBlocks;

    /**
     * Constructs an instance
     *
     * @param header for the MBC sequence
     * @param continuationBlocks for the message
     */
    public MultiCSBK(MBCHeader header, List<MBCContinuationBlock> continuationBlocks)
    {
        super(header.getSyncPattern(), header.getMessage(), header.getCACH(), header.getSlotType(),
              header.getTimestamp(), header.getTimeslot());

        mBlocks = continuationBlocks;

        if(mBlocks == null)
        {
            mBlocks = new ArrayList<>();
        }

        //TODO: figure out how to determine validity
        setValid(true);
    }
}
