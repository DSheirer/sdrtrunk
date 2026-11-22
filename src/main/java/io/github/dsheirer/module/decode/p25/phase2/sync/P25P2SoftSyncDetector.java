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

package io.github.dsheirer.module.decode.p25.phase2.sync;

import io.github.dsheirer.dsp.c4fm.DibitSyncDetector;
import io.github.dsheirer.dsp.c4fm.ISoftC4FMSyncDetector;
import java.util.Arrays;

/**
 * P25 Phase 2 soft sync detector.  Correlates the soft symbols (radians) against the 20-symbol S-ISCH sync pattern.
 */
public abstract class P25P2SoftSyncDetector extends DibitSyncDetector implements ISoftC4FMSyncDetector
{
    /**
     * P25 Phase 2 S-ISCH sync pattern: +333 -3 +33 -3 +3333 -333 +3 -33333
     */
    public static final long SYNC_PATTERN = 0x575D57F7FFL;
    protected static final int SYNC_DIBIT_LENGTH = 20;
    protected float[] mSymbols = new float[SYNC_DIBIT_LENGTH * 2];
    protected int mSymbolPointer = 0;

    /**
     * Constructs an instance
     */
    public P25P2SoftSyncDetector()
    {
        super(SYNC_PATTERN, SYNC_DIBIT_LENGTH);
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
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 20 soft
     * symbols that include this recent soft symbol.
     *
     * @param softSymbol to process that represents the soft demodulated symbol value in phase angle/radians.
     * @return correlation score.
     */
    @Override
    public float process(float softSymbol)
    {
        mSymbols[mSymbolPointer] = softSymbol;
        mSymbols[mSymbolPointer + SYNC_DIBIT_LENGTH] = softSymbol;
        mSymbolPointer++;
        mSymbolPointer %= SYNC_DIBIT_LENGTH;
        return calculate();
    }
}
