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

package io.github.dsheirer.module.decode.dmr.sync;

import io.github.dsheirer.dsp.symbol.Dibit;
import java.util.Arrays;

/**
 * DMR Sync Detector.  Uses soft demodulated symbols to detect sync patterns in the demodulated symbol stream.
 */
public abstract class DMRSoftSyncDetector extends DMRSyncDetector
{
    protected static final float MAX_POSITIVE = Dibit.D01_PLUS_3.getIdealPhase();
    protected static final float MAX_NEGATIVE = Dibit.D11_MINUS_3.getIdealPhase();
    protected static final float[] BASE_DATA = DMRSyncPattern.BASE_STATION_DATA.toSymbols();
    protected static final float[] BASE_VOICE = DMRSyncPattern.BASE_STATION_VOICE.toSymbols();
    protected static final float[] MOBILE_DATA = DMRSyncPattern.MOBILE_STATION_DATA.toSymbols();
    protected static final float[] MOBILE_VOICE = DMRSyncPattern.MOBILE_STATION_VOICE.toSymbols();
    protected static final float[] DIRECT_DATA_1 = DMRSyncPattern.DIRECT_DATA_TIMESLOT_1.toSymbols();
    protected static final float[] DIRECT_DATA_2 = DMRSyncPattern.DIRECT_DATA_TIMESLOT_2.toSymbols();
    protected static final float[] DIRECT_VOICE_1 = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1.toSymbols();
    protected static final float[] DIRECT_VOICE_2 = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2.toSymbols();

    protected float[] mSymbols = new float[48]; //2x longer than 48-bit/24-dibit DMR sync patterns.
    protected int mSymbolPointer = 0;

    public DMRSoftSyncDetector()
    {
    }

    /**
     * Resets this sync detector and flushes any stored soft symbols from the buffer.
     */
    public void reset()
    {
        Arrays.fill(mSymbols, 0.0f);
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
     * Worker method to be implemented by the subclass implementation.
     * @return best sync correlation score where the best detected pattern is available at getDetectedPattern().
     */
    public abstract float calculate();

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @param dibitSymbol to process that represents the soft demodulated symbol value in phase angle/radians.
     */
    public void process(float dibitSymbol)
    {
        mSymbols[mSymbolPointer] = dibitSymbol;
        mSymbols[mSymbolPointer + 24] = dibitSymbol;
        mSymbolPointer++;
        mSymbolPointer %= 24;
    }

    /**
     * Process the symbol and calculate the sync detection score.
     * @param symbol to process
     * @return sync detection score.
     */
    public float processAndCalculate(float symbol)
    {
        process(symbol);
        return calculate();
    }
}
