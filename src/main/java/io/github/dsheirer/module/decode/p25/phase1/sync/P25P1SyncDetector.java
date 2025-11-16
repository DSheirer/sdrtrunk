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
 * Base implementation of P25 Phase 1 sync detector.
 */
public abstract class P25P1SyncDetector
{
    public static final long SYNC_MASK = 0xFFFFFFFFFFFFL;
    public static final long SYNC_PATTERN = 0x5575F5FF77FFL;

    /**
     * Converts the P25P1 sync pattern to a float array of ideal phase values for each Dibit to use for correlation
     * against a stream of transmitted symbol phases.
     * @return symbols array representing the sync pattern.
     */
    public static float[] syncPatternToSymbols()
    {
        float[] symbols = new float[24];
        Dibit[] dibits = syncPatternToDibits();

        for(int x = 0; x < 24; x++)
        {
            symbols[x] = dibits[x].getIdealPhase();
        }

        return symbols;
    }

    /**
     * Converts the P25P1 sync pattern to an array of dibit symbols.  Note: only the +3 and -3 dibit symbols are used in
     * sync patterns.
     * @return dibit symbols array representing the sync pattern.
     */
    public static Dibit[] syncPatternToDibits()
    {
        Dibit[] dibits = new Dibit[24];
        long mask = 3;
        long dibitValue;

        for(int x = 0; x < 24; x++)
        {
            dibitValue = (SYNC_PATTERN & mask) >> (2 * x);

            if(dibitValue == 1)
            {
                dibits[23 - x] = Dibit.D01_PLUS_3;
            }
            else
            {
                dibits[23 - x] = Dibit.D11_MINUS_3;
            }

            mask = mask << 2;
        }

        return dibits;
    }
}
