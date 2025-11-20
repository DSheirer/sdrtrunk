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

package io.github.dsheirer.module.decode.nxdn.layer1.sync.control;

import java.util.Arrays;

/**
 * NXDN soft sync detector for the extended sync pattern on the outbound control channel.  Uses soft demodulated
 * symbols to detect sync patterns in the demodulated symbol stream.
 */
public abstract class NXDNControlSoftSyncDetector extends NXDNControlSyncDetector
{
    protected float[] mSymbols = new float[CONTROL_SYNC_DIBIT_LENGTH * 2];
    protected int mSymbolPointer = 0;

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
    public abstract float calculate();

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
        mSymbols[mSymbolPointer + CONTROL_SYNC_DIBIT_LENGTH] = dibitSymbol;
        mSymbolPointer++;
        mSymbolPointer %= CONTROL_SYNC_DIBIT_LENGTH;
        return calculate();
    }
}
