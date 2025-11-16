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
import java.util.Arrays;

/**
 * P25P1 Sync Detector.  Uses soft demodulated symbols to detect sync patterns in the demodulated symbol stream.
 */
public abstract class P25P1SoftSyncDetector extends P25P1SyncDetector
{
    protected static final float MAX_POSITIVE = Dibit.D01_PLUS_3.getIdealPhase();
    protected static final float MAX_NEGATIVE = Dibit.D11_MINUS_3.getIdealPhase();
    protected static final float[] SYNC_PATTERN_SYMBOLS = syncPatternToSymbols();
    protected float[] mSymbols = new float[48]; //2x longer than 48-bit/24-dibit DMR sync patterns.
    protected int mSymbolPointer = 0;

    public P25P1SoftSyncDetector()
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
     * Worker method to be implemented by the subclass implementation.
     *
     * @return sync correlation score.
     */
    protected abstract float calculate();

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @param dibitSymbol to process that represents the soft demodulated symbol value in phase angle/radians.
     * @return correlation score.
     */
    public float process(float dibitSymbol)
    {
        mSymbols[mSymbolPointer] = dibitSymbol;
        mSymbols[mSymbolPointer + 24] = dibitSymbol;
        mSymbolPointer++;
        mSymbolPointer %= 24;
        return calculate();
    }
}
