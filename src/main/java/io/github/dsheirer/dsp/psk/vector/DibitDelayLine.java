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

package io.github.dsheirer.dsp.psk.vector;

import io.github.dsheirer.dsp.symbol.Dibit;
import java.util.Arrays;

/**
 * Implements a circular buffer delay line for dibits.
 */
public class DibitDelayLine
{
    private Dibit[] mDelayLine;
    public int mPointer;
    private int mLength;

    /**
     * Constructs an instance
     * @param length of the delay line in dibits.
     */
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
        Dibit ejected = mDelayLine[mPointer];
        mDelayLine[mPointer++] = dibit;
        mPointer %= mLength;
        return ejected;
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
}
