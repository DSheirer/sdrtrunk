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
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.data.header.MBCHeader;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a Multi-Block CSBK message into a MultiCSBK message
 */
public class MBCAssembler
{
    private MBCHeader mTS0Header;
    private MBCHeader mTS1Header;
    private List<MBCContinuationBlock> mTS0ContinuationBlocks = new ArrayList<>();
    private List<MBCContinuationBlock> mTS1ContinuationBlocks = new ArrayList<>();

    /**
     * Constructs an instance
     */
    public MBCAssembler()
    {
    }

    /**
     * Processes the MBC header
     * @param header to process
     */
    public void process(MBCHeader header)
    {
        if(header != null && header.isValid())
        {
            reset(header.getTimeslot());

            if(header.getTimeslot() == 0)
            {
                mTS0Header = header;
            }
            else
            {
                mTS1Header = header;
            }
        }
    }

    /**
     * Processes the MBC continuation block
     * @param continuationBlock to process
     * @return assembled multi-block CSBK if this is the final block, null otherwise
     */
    public CSBKMessage process(MBCContinuationBlock continuationBlock)
    {
        if(continuationBlock != null)
        {
            if(continuationBlock.getTimeslot() == 0)
            {
                mTS0ContinuationBlocks.add(continuationBlock);

                if(continuationBlock.isLastBlock())
                {
                    if(mTS0Header != null)
                    {
                        CSBKMessage csbk = CSBKMessageFactory.create(mTS0Header, new ArrayList<>(mTS0ContinuationBlocks));
                        reset(0);
                        return csbk;
                    }
                    else
                    {
                        reset(0);
                    }
                }
            }
            else
            {
                mTS1ContinuationBlocks.add(continuationBlock);

                if(continuationBlock.isLastBlock())
                {
                    if(mTS1Header != null)
                    {
                        CSBKMessage csbk = CSBKMessageFactory.create(mTS1Header, new ArrayList<>(mTS1ContinuationBlocks));
                        reset(1);
                        return csbk;
                    }
                    else
                    {
                        reset(1);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Resets the specified timeslot message sequence
     * @param timeslot to reset
     */
    public void reset(int timeslot)
    {
        if(timeslot == 0)
        {
            mTS0Header = null;
            mTS0ContinuationBlocks.clear();
        }
        else
        {
            mTS1Header = null;
            mTS1ContinuationBlocks.clear();
        }
    }
}
