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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Aggregates a sequence of talker alias full link control messages to reconstruct the talker alias.
 */
public class TalkerAliasAssembler
{
    //Timeslot 1 fragments
    private TalkerAliasHeader mHeaderTs1;
    private TalkerAliasBlock1 mBlock1Ts1;
    private TalkerAliasBlock2 mBlock2Ts1;
    private TalkerAliasBlock3 mBlock3Ts1;

    //Timeslot 2 fragments
    private TalkerAliasHeader mHeaderTs2;
    private TalkerAliasBlock1 mBlock1Ts2;
    private TalkerAliasBlock2 mBlock2Ts2;
    private TalkerAliasBlock3 mBlock3Ts2;

    //Flags indicating if we have any non-null fragments for the specified timeslot to support resetting.
    private boolean mEmptyTs1 = true;
    private boolean mEmptyTs2 = true;

    /**
     * Constructs an instance.
     */
    public TalkerAliasAssembler()
    {
    }

    /**
     * Resets the contents of either timeslot 1 or 2 when a terminator or idle message is received.
     * @param timeslot 1 or 2
     */
    public void reset(int timeslot)
    {
        if(timeslot == 1 && !mEmptyTs1)
        {
            mHeaderTs1 = null;
            mBlock1Ts1 = null;
            mBlock2Ts1 = null;
            mBlock3Ts1 = null;
            mEmptyTs1 = true;
        }
        else if(timeslot == 2 && !mEmptyTs2)
        {
            mHeaderTs2 = null;
            mBlock1Ts2 = null;
            mBlock2Ts2 = null;
            mBlock3Ts2 = null;
            mEmptyTs2 = true;
        }
    }

    /**
     * Processes the FLC talker alias header or block and returns a fully assembled alias when all fragments are received.
     * @param message containing FLC talker alias header or blocks 1-3
     * @return fully assembled talker alias, if available, or null.
     */
    public TalkerAliasComplete process(FullLCMessage message)
    {
        switch(message.getOpcode())
        {
            case FULL_STANDARD_TALKER_ALIAS_HEADER:
            case FULL_HYTERA_TALKER_ALIAS_HEADER:
                if(message instanceof TalkerAliasHeader tah)
                {
                    if(message.getTimeslot() == 1)
                    {
                        mHeaderTs1 = tah;
                        mEmptyTs1 = false;
                        return assemble(1);
                    }
                    else
                    {
                        mHeaderTs2 = tah;
                        mEmptyTs2 = false;
                        return assemble(2);
                    }
                }
            case FULL_STANDARD_TALKER_ALIAS_BLOCK_1:
            case FULL_HYTERA_TALKER_ALIAS_BLOCK_1:
                if(message instanceof TalkerAliasBlock1 tab1)
                {
                    if(message.getTimeslot() == 1)
                    {
                        mBlock1Ts1 = tab1;
                        mEmptyTs1 = false;
                        return assemble(1);
                    }
                    else
                    {
                        mBlock1Ts2 = tab1;
                        mEmptyTs2 = false;
                        return assemble(2);
                    }
                }
            case FULL_STANDARD_TALKER_ALIAS_BLOCK_2:
            case FULL_HYTERA_TALKER_ALIAS_BLOCK_2:
                if(message instanceof TalkerAliasBlock2 tab2)
                {
                    if(message.getTimeslot() == 1)
                    {
                        mBlock2Ts1 = tab2;
                        mEmptyTs1 = false;
                        return assemble(1);
                    }
                    else
                    {
                        mBlock2Ts2 = tab2;
                        mEmptyTs2 = false;
                        return assemble(2);
                    }
                }
            case FULL_STANDARD_TALKER_ALIAS_BLOCK_3:
            case FULL_HYTERA_TALKER_ALIAS_BLOCK_3:
                if(message instanceof TalkerAliasBlock3 tab3)
                {
                    if(message.getTimeslot() == 1)
                    {
                        mBlock3Ts1 = tab3;
                        mEmptyTs1 = false;
                        return assemble(1);
                    }
                    else
                    {
                        mBlock3Ts2 = tab3;
                        mEmptyTs2 = false;
                        return assemble(2);
                    }
                }
        }

        return null;
    }

    /**
     * Assembles the complete alias.
     * @param timeslot to assemble.
     * @return assembled alias or null.
     */
    private TalkerAliasComplete assemble(int timeslot)
    {
        if(timeslot == 1)
        {
            return assemble(mHeaderTs1, mBlock1Ts1, mBlock2Ts1, mBlock3Ts1);
        }
        else
        {
            return assemble(mHeaderTs2, mBlock1Ts2, mBlock2Ts2, mBlock3Ts2);
        }
    }

    /**
     * Assembles a complete alias from the header and block fragments.
     * @param header of the alias
     * @param block1 of the alias, optional
     * @param block2 of the alias, optional
     * @param block3 of the alias, optional
     * @return assembled alias or null.
     */
    private TalkerAliasComplete assemble(TalkerAliasHeader header,
                                         TalkerAliasBlock1 block1,
                                         TalkerAliasBlock2 block2,
                                         TalkerAliasBlock3 block3)
    {
        if(header != null)
        {
            int bitLength = header.getTotalBitLength();

            if(1 <= bitLength && bitLength <= 49)
            {
                return new TalkerAliasComplete(header.getPayloadFragment(), header.getFormat(), header.getCharacterLength(),
                        header.getTimestamp(), header.getTimeslot());
            }
            else if(50 <= bitLength && bitLength <= 105 && block1 != null)
            {
                CorrectedBinaryMessage message = new CorrectedBinaryMessage(105);
                message.load(0, header.getPayloadFragment());
                message.load(49, block1.getPayloadFragment());
                return new TalkerAliasComplete(message, header.getFormat(), header.getCharacterLength(),
                        header.getTimestamp(), header.getTimeslot());
            }
            else if(106 <= bitLength && bitLength <= 161 && block1 != null && block2 != null)
            {
                CorrectedBinaryMessage message = new CorrectedBinaryMessage(161);
                message.load(0, header.getPayloadFragment());
                message.load(49, block1.getPayloadFragment());
                message.load(105, block2.getPayloadFragment());
                return new TalkerAliasComplete(message, header.getFormat(), header.getCharacterLength(),
                        header.getTimestamp(), header.getTimeslot());
            }
            else if(162 <= bitLength && bitLength <= 217 && block1 != null && block2 != null && block3 != null)
            {
                CorrectedBinaryMessage message = new CorrectedBinaryMessage(217);
                message.load(0, header.getPayloadFragment());
                message.load(49, block1.getPayloadFragment());
                message.load(105, block2.getPayloadFragment());
                message.load(161, block3.getPayloadFragment());
                return new TalkerAliasComplete(message, header.getFormat(), header.getCharacterLength(),
                        header.getTimestamp(), header.getTimeslot());
            }
        }

        return null;
    }
}
