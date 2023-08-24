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

/**
 * DMR Sync Detector.  Uses soft demodulated symbols to detect sync patterns in the demodulated symbol stream.
 */
public class ScalarDmrSyncDetector
{
    private static final float MAX_POSITIVE = Dibit.D01_PLUS_3.getIdealPhase();
    private static final float MAX_NEGATIVE = Dibit.D11_MINUS_3.getIdealPhase();

    private float[] mSymbols = new float[48]; //2x longer than 48-bit/24-dibit DMR sync patterns.
    private float[] mSyncBsData = new float[24];

    private int mSymbolPointer = 0;

    public ScalarDmrSyncDetector()
    {
        mSyncBsData = DMRSyncPattern.BASE_STATION_DATA.toSymbols();
    }

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     * @param dibitSymbol to process that represents the soft demodulated symbol value in phase angle/radians.
     * @return correlation score.
     */
    public float process(float dibitSymbol)
    {
        //Constrain symbol to maximum positive or negative value to limit noisy symbols.
        dibitSymbol = Math.min(dibitSymbol, MAX_POSITIVE);
        dibitSymbol = Math.max(dibitSymbol, MAX_NEGATIVE);

        mSymbols[mSymbolPointer] = dibitSymbol;
        mSymbols[mSymbolPointer + 24] = dibitSymbol;
        mSymbolPointer++;
        mSymbolPointer %= 24;

        float accumulator = 0;

        for(int x = 0; x < 24; x++)
        {
            accumulator += mSyncBsData[x] * mSymbols[mSymbolPointer + x];
        }

        return accumulator;
    }
}
