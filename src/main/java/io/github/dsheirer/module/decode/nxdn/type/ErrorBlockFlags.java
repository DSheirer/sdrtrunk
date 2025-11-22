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

package io.github.dsheirer.module.decode.nxdn.type;

import java.util.ArrayList;
import java.util.List;

/**
 * Error flags for transmitted data blocks.
 */
public class ErrorBlockFlags
{
    private final int mFlags;

    /**
     * Constructs an instance
     * @param flags in a 16-bit field where MSB = block 15 and LSB = block 1.
     */
    public ErrorBlockFlags(int flags)
    {
        mFlags = flags;
    }

    @Override
    public String toString()
    {
        if(mFlags == 0)
        {
            return "NO ERRORS";
        }
        else
        {
            List<Integer> errorBlocks = new ArrayList<>();

            for(int x = 0; x < 16; x++)
            {
                if((mFlags & x) == x)
                {
                    errorBlocks.add(x);
                }
            }

            return "ERROR BLOCKS " + errorBlocks;
        }
    }
}
