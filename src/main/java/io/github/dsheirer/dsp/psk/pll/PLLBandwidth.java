/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.dsp.psk.pll;

import java.util.Map;
import java.util.TreeMap;

/**
 * Phase Locked Loop (PLL) tracking bandwidth - controls how fast/slow the PLL tracks the carrier frequency
 */
public enum PLLBandwidth
{
    //NOTE: static bandwidth of 200 produced the best results for P25P1 releases prior to 0.3.4b2

    BW_400(400.0, 0, 1),
    BW_300(300.0, 2, 3),
    BW_250(250.0, 4, 5),
    BW_200(200.0, 6, 7);

    private double mLoopBandwidth;
    private int mRangeStart;
    private int mRangeEnd;

    PLLBandwidth(double loopBandwidth, int start, int end)
    {
        mLoopBandwidth = loopBandwidth;
        mRangeStart = start;
        mRangeEnd = end;
    }

    private static Map<Integer,PLLBandwidth> LOOKUP_MAP = new TreeMap<>();

    static
    {
        for(PLLBandwidth pllBandwidth : PLLBandwidth.values())
        {
            for(int x = pllBandwidth.getRangeStart(); x <= pllBandwidth.getRangeEnd(); x++)
            {
                LOOKUP_MAP.put(x, pllBandwidth);
            }
        }
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
    public static PLLBandwidth fromSyncCount(int syncCount)
    {
        PLLBandwidth mapValue = LOOKUP_MAP.get(syncCount);
        if (mapValue != null) {
            return mapValue;
        }

        return BW_200;
    }
}
