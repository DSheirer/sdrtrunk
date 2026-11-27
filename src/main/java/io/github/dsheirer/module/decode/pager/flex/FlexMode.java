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

package io.github.dsheirer.module.decode.pager.flex;

/**
 * FLEX transmission modes, identified by the 16-bit code in the sync 1 word.  The mode sets the symbol rate and the
 * number of FSK levels used after the frame information word.
 */
public enum FlexMode
{
    FLEX_1600_2(0x870C, 1600, 2),
    FLEX_3200_4(0xB068, 1600, 4),
    FLEX_3200_2(0x7B18, 3200, 2),
    FLEX_6400_4(0xDEA0, 3200, 4),
    FLEX_6400_4_ALTERNATE(0x4C7C, 3200, 4);

    private static final int MAX_BIT_ERRORS = 3;

    private final int mSyncCode;
    private final int mSymbolRate;
    private final int mLevels;

    FlexMode(int syncCode, int symbolRate, int levels)
    {
        mSyncCode = syncCode;
        mSymbolRate = symbolRate;
        mLevels = levels;
    }

    /**
     * 16-bit sync code
     */
    public int getSyncCode()
    {
        return mSyncCode;
    }

    /**
     * Symbol rate for the data portion of the frame (1600 or 3200)
     */
    public int getSymbolRate()
    {
        return mSymbolRate;
    }

    /**
     * Number of FSK levels (2 or 4) for the data portion of the frame.
     */
    public int getLevels()
    {
        return mLevels;
    }

    /**
     * Bit rate for the data portion of the frame.
     */
    public int getBitRate()
    {
        return mSymbolRate * (mLevels == 4 ? 2 : 1);
    }

    /**
     * Number of interleaved phases (A, B, C, D) that carry data in this mode.
     */
    public int getPhaseCount()
    {
        return getBitRate() / 1600;
    }

    /**
     * Looks up the mode from the sync code, allowing up to 3 bit errors.
     * @param syncCode 16-bit code
     * @return matching mode or null
     */
    public static FlexMode fromSyncCode(int syncCode)
    {
        for(FlexMode mode: values())
        {
            if(Integer.bitCount((mode.mSyncCode ^ syncCode) & 0xFFFF) <= MAX_BIT_ERRORS)
            {
                return mode;
            }
        }

        return null;
    }

    @Override
    public String toString()
    {
        return getBitRate() + "/" + mLevels;
    }
}
