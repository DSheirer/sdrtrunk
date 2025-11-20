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

package io.github.dsheirer.module.decode.nxdn.sync;

import io.github.dsheirer.dsp.symbol.Dibit;

/**
 * Base implementation of NXDN sync detector.
 */
public abstract class NXDNSyncDetector
{
    public static final int SYNC_MASK = 0xFFFFF;
    public static final int SYNC_PATTERN = 0xCDF59;

    /**
     * Converts the sync pattern to a float array of ideal phase values for each Dibit to use for correlation
     * against a stream of transmitted symbol phases.
     * @return symbols array representing the sync pattern.
     */
    public static float[] syncPatternToSymbols()
    {
        float[] symbols = new float[10];
        Dibit[] dibits = syncPatternToDibits();

        for(int x = 0; x < 10; x++)
        {
            symbols[x] = dibits[x].getIdealPhase();
        }

        return symbols;
    }

    /**
     * Converts the sync pattern to an array of dibit symbols.
     * @return dibit symbols array representing the sync pattern.
     */
    public static Dibit[] syncPatternToDibits()
    {
        Dibit[] dibits = new Dibit[10];
        int mask = 3;
        int dibitValue;

        for(int x = 0; x < 10; x++)
        {
            dibitValue = (SYNC_PATTERN & mask) >> (2 * x);

            dibits[9 - x] = switch(dibitValue)
            {
                case 0 -> Dibit.D00_PLUS_1;
                case 1 -> Dibit.D01_PLUS_3;
                case 2 -> Dibit.D10_MINUS_1;
                default -> Dibit.D11_MINUS_3;
            };

            mask = mask << 2;
        }

        return dibits;
    }
}
