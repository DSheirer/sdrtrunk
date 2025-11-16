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

package io.github.dsheirer.module.decode.p25.phase1.sync;

import io.github.dsheirer.dsp.symbol.Dibit;

/**
 * P25P1 sync detector that processes hard symbol (ie dibit) decisions.
 */
public class P25P1HardSyncDetector extends P25P1SyncDetector
{
    private static final int MAXIMUM_BIT_ERROR = 4;
    private long mValue;

    /**
     * Constructs an instance
     */
    public P25P1HardSyncDetector()
    {
    }

    /**
     * Processes the dibit.
     *
     * @param dibit to process
     * @return true if a sync pattern is detected.
     */
    public boolean process(Dibit dibit)
    {
        mValue = (Long.rotateLeft(mValue, 2) & SYNC_MASK) + dibit.getValue();
        return Long.bitCount(mValue ^ SYNC_PATTERN) <= MAXIMUM_BIT_ERROR;
    }
}
