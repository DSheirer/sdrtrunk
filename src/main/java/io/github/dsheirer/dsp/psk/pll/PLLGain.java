/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.psk.pll;

/**
 * Phase Locked Loop (PLL) tracking bandwidth - controls how fast/slow the PLL tracks the carrier frequency
 */
public enum PLLGain
{
    //NOTE: static gain level of 200 produced the best results for releases prior to 0.3.4b2

    LEVEL_1(150.0, 0, 1),
    LEVEL_2(175.0, 2, 4),
    LEVEL_3(200.0, 5, 6),
    LEVEL_4(200.0, 7, 8),
    LEVEL_5(200.0, 9, 10);

    private double mLoopBandwidth;
    private int mRangeStart;
    private int mRangeEnd;

    PLLGain(double loopBandwidth, int start, int end)
    {
        mLoopBandwidth = loopBandwidth;
        mRangeStart = start;
        mRangeEnd = end;
    }

    /**
     * Loop bandwidth setting
     */
    public double getLoopBandwidth()
    {
        return mLoopBandwidth;
    }

    public int getRangeStart()
    {
        return mRangeStart;
    }

    public int getRangeEnd()
    {
        return mRangeEnd;
    }

    /**
     * Look up PLL gain from a running sync detection count.
     *
     * @param syncCount 0-10
     * @return PLL gain appropriate for the sync detection value
     */
    public static PLLGain fromSyncCount(int syncCount)
    {
        if(syncCount <= LEVEL_1.getRangeEnd())
        {
            return LEVEL_1;
        }
        else if(syncCount <= LEVEL_2.getRangeEnd())
        {
            return LEVEL_2;
        }
        else if(syncCount <= LEVEL_3.getRangeEnd())
        {
            return LEVEL_3;
        }
        else if(syncCount <= LEVEL_4.getRangeEnd())
        {
            return LEVEL_4;
        }
        else
        {
            return LEVEL_5;
        }
    }
}
