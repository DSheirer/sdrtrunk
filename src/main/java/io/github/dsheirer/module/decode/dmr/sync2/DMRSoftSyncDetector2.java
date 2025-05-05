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

package io.github.dsheirer.module.decode.dmr.sync2;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.Arrays;

/**
 * DMR Sync Detector.  Uses soft demodulated symbols to detect sync patterns in the demodulated symbol stream.
 */
public class DMRSoftSyncDetector2 extends DMRSyncDetector2
{
    protected static final float MAX_POSITIVE = Dibit.D01_PLUS_3.getIdealPhase();
    protected static final float MAX_NEGATIVE = Dibit.D11_MINUS_3.getIdealPhase();
    protected static final float[] BASE_DATA_I = DMRSyncPattern.BASE_STATION_DATA.toISamples();
    protected static final float[] BASE_DATA_Q = DMRSyncPattern.BASE_STATION_DATA.toQSamples();

    protected float[] mISymbols = new float[48]; //2x longer than 48-bit/24-dibit DMR sync patterns.
    protected float[] mQSymbols = new float[48]; //2x longer than 48-bit/24-dibit DMR sync patterns.
    protected int mSymbolPointer = 0;

    public DMRSoftSyncDetector2()
    {
    }

    /**
     * Resets this sync detector and flushes any stored soft symbols from the buffer.
     */
    public void reset()
    {
        Arrays.fill(mISymbols, 0.0f);
        Arrays.fill(mQSymbols, 0.0f);
        mSymbolPointer = 0;
    }

    /**
     * Detected DMR sync pattern that produced the highest correlation score from the most recent call to process().
     * @return detected sync pattern.
     */
    public DMRSyncPattern getDetectedPattern()
    {
        return mDetectedPattern;
    }

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @return correlation score.
     */
    public float calculate()
    {
        float accumulator = 0.0f;

        for(int x = 0; x < 24; x++)
        {
            //The I branch of the symbol is always negative in a sync pattern - just add the negated value
            accumulator += -mISymbols[mSymbolPointer + x];
            accumulator += BASE_DATA_Q[x] * mQSymbols[mSymbolPointer + x];
        }

        return accumulator;
    }

    /**
     * Processes the demodulated soft symbol value into the buffer.
     */
    public void process(float i, float q)
    {
        mISymbols[mSymbolPointer] = i;
        mISymbols[mSymbolPointer + 24] = i;
        mQSymbols[mSymbolPointer] = q;
        mQSymbols[mSymbolPointer + 24] = q;
        mSymbolPointer++;
        mSymbolPointer %= 24;
    }

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @param i to process that represents the soft demodulated symbol value in phase angle/radians.
     * @return correlation score.
     */
    public float processAndCalculate(float i, float q)
    {
        process(i, q);
        return calculate();
    }
}
