/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import java.util.Arrays;

public class DibitDelayLine
{
    private Dibit[] mDelayLine;
    private int mPointer;
    private int mLength;

    public DibitDelayLine(int length)
    {
        mLength = length;
        mDelayLine = new Dibit[length];
        Arrays.fill(mDelayLine, Dibit.D00_PLUS_1);
    }

    /**
     * Inserts the dibit into the delay line and returns the oldest dibit from the delay line.
     * @param dibit to insert
     * @return oldest dibit
     */
    public Dibit insert(Dibit dibit)
    {
        mDelayLine[mPointer++] = dibit;
        mPointer %= mLength;
        return mDelayLine[mPointer];
    }

    /**
     * Overwrites the dibits in the delay line with the provided sequence.
     * @param dibits to overwrite
     */
    public void update(Dibit[] dibits)
    {
        for(Dibit dibit: dibits)
        {
            mDelayLine[mPointer++] = dibit;
            mPointer %= mLength;
        }
    }

    public static void main(String[] args)
    {
        DibitDelayLine delay = new DibitDelayLine(24);

        Dibit[] dibits = DMRSyncPattern.BASE_STATION_DATA.toDibits();

        for(Dibit dibit: dibits)
        {
            Dibit returned = delay.insert(dibit);
            System.out.println("Insert " + dibit + " Return:" + returned);
        }

        System.out.println("Fully stuffed!");

        for(Dibit dibit: dibits)
        {
            Dibit returned = delay.insert(dibit);
            System.out.println("Insert " + dibit + " Return:" + returned);
        }
    }
}
