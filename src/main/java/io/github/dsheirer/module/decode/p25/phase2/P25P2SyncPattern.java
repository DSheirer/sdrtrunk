/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.dsp.symbol.Dibit;

public class P25P2SyncPattern
{
    private static final Dibit[] SYNC_PATTERN = {
        Dibit.D01_PLUS_3,Dibit.D01_PLUS_3,Dibit.D01_PLUS_3,Dibit.D11_MINUS_3,
        Dibit.D01_PLUS_3,Dibit.D01_PLUS_3,Dibit.D11_MINUS_3,Dibit.D01_PLUS_3,
        Dibit.D01_PLUS_3,Dibit.D01_PLUS_3,Dibit.D01_PLUS_3,Dibit.D11_MINUS_3,
        Dibit.D11_MINUS_3,Dibit.D11_MINUS_3,Dibit.D01_PLUS_3,Dibit.D11_MINUS_3,
        Dibit.D11_MINUS_3,Dibit.D11_MINUS_3,Dibit.D11_MINUS_3,Dibit.D11_MINUS_3};

    public static int getBitErrorCount(Dibit[] dibits)
    {
        if(dibits.length != 20)
        {
            throw new IllegalArgumentException("Sync pattern dibit array must be 20 dibits (40 bits) long");
        }

        int bitErrorCount = 0;

        for(int x = 0; x < 20; x++)
        {
            if(SYNC_PATTERN[x] != dibits[x])
            {
                int errorMask = SYNC_PATTERN[x].getLowValue() ^ dibits[x].getLowValue();

                if(errorMask == 3)
                {
                    bitErrorCount += 2;
                }
                else
                {
                    bitErrorCount++;
                }
            }
        }

        return bitErrorCount;
    }
}
