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

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector implementation of long control soft sync detector.
 */
public class NXDNControlSoftSyncDetectorVector256 extends NXDNControlSoftSyncDetector
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;
    private static final float[] SYNC_EXTENDED = new float[8];

    static
    {
        //Create an array of 8x elements containing the final 6x symbols for SIMD loading.  Leave indices 0 and 1 at
        //default zero so that the overlap doesn't contribute to the count.
        SYNC_EXTENDED[2] = SYMBOLS[16];
        SYNC_EXTENDED[3] = SYMBOLS[17];
        SYNC_EXTENDED[4] = SYMBOLS[18];
        SYNC_EXTENDED[5] = SYMBOLS[19];
        SYNC_EXTENDED[6] = SYMBOLS[20];
        SYNC_EXTENDED[7] = SYMBOLS[21];
    }

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 10 soft
     * symbols that include this recent soft symbol.
     *
     * @return correlation score.
     */
    public float calculate()
    {
        FloatVector accumulator = FloatVector.zero(VECTOR_SPECIES);
        accumulator = FloatVector.fromArray(VECTOR_SPECIES, SYMBOLS, 0)
                .fma(FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer), accumulator);
        accumulator = FloatVector.fromArray(VECTOR_SPECIES, SYMBOLS, 8)
                .fma(FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer + 8), accumulator);
        accumulator = FloatVector.fromArray(VECTOR_SPECIES, SYNC_EXTENDED, 0)
                .fma(FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer + 14), accumulator);
        return accumulator.reduceLanes(VectorOperators.ADD);
    }
}
