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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris;

/**
 * Assembles a talker alias from talker alias blocks 1-4
 */
public class HarrisTalkerAliasAssembler
{
    private LCHarrisTalkerAliasBlock1 mBlock1;
    private LCHarrisTalkerAliasBlock2 mBlock2;
    private LCHarrisTalkerAliasBlock3 mBlock3;
    private LCHarrisTalkerAliasBlock4 mBlock4;
    private long mTimestamp;

    /**
     * Constructs an instance.
     */
    public HarrisTalkerAliasAssembler()
    {
    }

    /**
     * Resets the contents when a terminator or idle message is received.
     */
    public void reset()
    {
        mBlock1 = null;
        mBlock2 = null;
        mBlock3 = null;
        mBlock4 = null;
    }

    /**
     * Processes the FLC talker alias message and returns a fully assembled alias when at least 2 fragments are received.
     * @param lcw containing Harris LC talker alias blocks 1-4
     * @return fully assembled talker alias, if available, or null.
     */
    public LCHarrisTalkerAliasComplete process(LCHarrisTalkerAliasBase lcw, long timestamp)
    {
        mTimestamp = timestamp;

        if(lcw.isValid())
        {
            switch(lcw.getOpcode())
            {
                case L3HARRIS_TALKER_ALIAS_BLOCK_1:
                    if(lcw instanceof LCHarrisTalkerAliasBlock1 block1)
                    {
                        mBlock1 = block1;
                        return assemble();
                    }
                    break;
                case L3HARRIS_TALKER_ALIAS_BLOCK_2:
                    if(lcw instanceof LCHarrisTalkerAliasBlock2 block2)
                    {
                        mBlock2 = block2;
                        return assemble();
                    }
                    break;
                case L3HARRIS_TALKER_ALIAS_BLOCK_3:
                    if(lcw instanceof LCHarrisTalkerAliasBlock3 block3)
                    {
                        mBlock3 = block3;
                        return assemble();
                    }
                    break;
                case L3HARRIS_TALKER_ALIAS_BLOCK_4:
                    if(lcw instanceof LCHarrisTalkerAliasBlock4 block4)
                    {
                        mBlock4 = block4;
                        return assemble();
                    }
                    break;
            }
        }

        return null;
    }

    /**
     * Assembles a complete alias from the 4 fragment blocks
     * @return assembled alias or null.
     */
    private LCHarrisTalkerAliasComplete assemble()
    {
        if(mBlock1 != null && mBlock2 != null)
        {
            String fragment2 = mBlock2.getPayloadFragmentString();
            String fragment3 = mBlock3 != null ? mBlock3.getPayloadFragmentString() : null;
            String fragment4 = mBlock4 != null ? mBlock4.getPayloadFragmentString() : null;
            return new LCHarrisTalkerAliasComplete(mBlock1.getMessage(), fragment2, fragment3, fragment4, mTimestamp);
        }

        return null;
    }
}
