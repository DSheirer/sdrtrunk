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

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD Vector 512 implementation of P25 Phase 2 Soft Sync Detector.
 */
public class P25P2SoftSyncDetectorVector512 extends P25P2SoftSyncDetector
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_512;
    //Final pattern symbols, zero padded to the vector length, for the final (overlapping) vector of symbols
    private final float[] mTailPatternSymbols = new float[16];

    /**
     * Constructs an instance
     */
    public P25P2SoftSyncDetectorVector512()
    {
        System.arraycopy(getPatternSymbols(), 16, mTailPatternSymbols, 12, 4);
    }

    /**
     * Calculates the correlation of the preceding 20 soft symbols against the sync pattern.
     *
     * @return correlation score.
     */
    @Override
    public float calculate()
    {
        //There are 20 symbols that we load into 2x vectors of 16.  The second vector overlaps the first vector by 12
        //symbols (4-15), so it is multiplied against the final 4 pattern symbols (16-19) zero padded in the first 12
        //lanes.  Multiplying against zero is cheaper than a masked load.
        FloatVector accumulator = FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer)
                .mul(FloatVector.fromArray(VECTOR_SPECIES, getPatternSymbols(), 0));
        accumulator = FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer + 4)
                .fma(FloatVector.fromArray(VECTOR_SPECIES, mTailPatternSymbols, 0), accumulator);
        return accumulator.reduceLanes(VectorOperators.ADD);
    }
}
