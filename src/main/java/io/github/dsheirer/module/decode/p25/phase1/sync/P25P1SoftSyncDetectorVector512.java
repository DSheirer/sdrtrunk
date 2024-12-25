/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD Vector 512 implementation of P25 Phase 1 Soft Sync Detector.
 */
public class P25P1SoftSyncDetectorVector512 extends P25P1SoftSyncDetector
{
    private static final boolean[] VECTOR_MASK = new boolean[]{false, false, false, false, false, false, false, false,
            true, true, true, true, true, true, true, true};
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_512;

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @return correlation score.
     */
    public float calculate()
    {
        //There are 24 symbols that we load into 2x vectors of 16.  We load the first 16 symbols into vector 1 and the
        //remaining 8 symbols into vector 2 using a vector mask that ignores the first 8 symbols and loads the second 8
        // symbols.  This sets the first 8 symbols to zero and those values are ignored during calculations.
        VectorMask<Float> mask = VectorMask.fromArray(VECTOR_SPECIES, VECTOR_MASK, 0);
        FloatVector accumulator = FloatVector.zero(VECTOR_SPECIES);
        accumulator = FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer)
                .fma(FloatVector.fromArray(VECTOR_SPECIES, SYNC_PATTERN_SYMBOLS, 0), accumulator);
        accumulator = FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer + 8, mask)
                .fma(FloatVector.fromArray(VECTOR_SPECIES, SYNC_PATTERN_SYMBOLS, 8), accumulator);
        return accumulator.reduceLanes(VectorOperators.ADD);
    }
}
