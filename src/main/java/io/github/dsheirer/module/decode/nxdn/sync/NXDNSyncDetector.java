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
    /**
     * Sync pattern length in dibits.
     */
    public abstract int getSyncPatternDibitLength();

    /**
     * Dibits array for the sync pattern
     * @return dibits array.
     */
    public abstract Dibit[] getSyncDibits();

    /**
     * Symbols array for the sync pattern.
     * @return array of (ideal) soft symbol values.
     */
    public abstract float[] getSyncSymbols();

    /**
     * Converts the sync pattern to dibits.
     * @param pattern to convert
     * @param dibitLength in dibits
     * @return array of dibits for the sync pattern.
     */
    protected static Dibit[] toDibits(long pattern, int dibitLength)
    {
        Dibit[] dibits = new Dibit[dibitLength];
        long mask = 3;
        int dibitValue;

        for(int x = 0; x < dibitLength; x++)
        {
            dibitValue = (int)((pattern & mask) >> (2 * x));

            dibits[dibitLength - x - 1] = switch(dibitValue)
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

    /**
     * Utility method to convert a dibit array into an array of ideal soft symbol values.
     * @param dibits for the sync pattern
     * @return ideal soft symbol array.
     */
    protected static float[] toSymbols(Dibit[] dibits)
    {
        float[] symbols = new float[dibits.length];

        for(int x = 0; x < dibits.length; x++)
        {
            symbols[x] = dibits[x].getIdealPhase();
        }

        return symbols;
    }
}
