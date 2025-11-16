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

package io.github.dsheirer.dsp.symbol;

import java.util.Arrays;

/**
 * Implements a circular buffer delay line for dibits.
 */
public class DibitDelayLine
{
    protected Dibit[] mDelayLine;
    public int mPointer;

    /**
     * Constructs an instance
     * @param length of the delay line in dibits.
     */
    public DibitDelayLine(int length)
    {
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
        mPointer %= mDelayLine.length;
        return ejected;
    }

    /**
     * Overwrites the most recently added dibits in the delay line with the provided sequence.  This can be used when
     * a sync pattern is detected to overwrite the calculated symbols with the known sync pattern.
     * @param dibits to overwrite
     */
    public void update(Dibit[] dibits)
    {
        mPointer = mPointer - dibits.length;

        if(mPointer < 0)
        {
            mPointer += mDelayLine.length;
        }

        for(Dibit dibit: dibits)
        {
            mDelayLine[mPointer++] = dibit;
            mPointer %= mDelayLine.length;
        }
    }

    /**
     * Adjusts the pointer to correct a dibit stuff/delete.
     * @param offset to adjust.
     */
    public void adjustPointer(int offset)
    {
        mPointer += offset;

        if(mPointer < 0)
        {
            mPointer += mDelayLine.length;
        }
        else if(mPointer >= mDelayLine.length)
        {
            mPointer -= mDelayLine.length;
        }
    }


    /**
     * Returns the most recently stored Dibit.
     */
    public Dibit getLast()
    {
        int pointer = mPointer - 1;

        if(pointer < 0)
        {
            pointer += mDelayLine.length;
        }

        return mDelayLine[pointer];
    }

    public void log()
    {
        StringBuilder sb = new StringBuilder();

        int pointer = mPointer;
        for(int i = 0; i < mDelayLine.length; i++)
        {
            Dibit dibit = mDelayLine[pointer];
            sb.append(dibit.getBit1() ? "1" : "0");
            sb.append(dibit.getBit2() ? "1" : "0");
            sb.append(" ");

            pointer++;
            pointer = pointer % mDelayLine.length;
        }

        System.out.println(sb);
    }
}
