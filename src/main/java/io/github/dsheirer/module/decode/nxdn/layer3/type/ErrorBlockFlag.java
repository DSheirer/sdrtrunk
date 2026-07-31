/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import java.util.ArrayList;
import java.util.List;

/**
 * Type D error block flags.  A 16-bit field where each block represents the error status of a transmitted block.
 */
public class ErrorBlockFlag extends Option
{
    /**
     * Constructs an instance
     * @param value of the field
     */
    public ErrorBlockFlag(int value)
    {
        super(value);
    }

    @Override
    public String toString()
    {
        if(mValue == 0)
        {
            return "NO BLOCK ERRORS";
        }

        List<Integer> blocks = new ArrayList<>();

        for(int x = 0; x < 16; x++)
        {
            if(isSet(x))
            {
                blocks.add(15 - x);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ERROR BLOCKS ").append(blocks);

        return sb.toString();
    }
}
