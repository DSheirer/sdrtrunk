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

package io.github.dsheirer.module.decode.nxdn.sync.standard;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD Vector 64 implementation of NXDN Soft Sync Detector.
 */
public class NXDNStandardSoftSyncDetectorVector64 extends NXDNStandardSoftSyncDetector
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_64;

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @return correlation score.
     */
    public float calculate()
    {
        FloatVector accumulator = FloatVector.zero(VECTOR_SPECIES);

        for(int x = 0; x < STANDARD_SYNC_DIBIT_LENGTH; x += VECTOR_SPECIES.length())
        {
            accumulator = FloatVector.fromArray(VECTOR_SPECIES, SYMBOLS, x)
                    .fma(FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer + x), accumulator);
        }

        return accumulator.reduceLanes(VectorOperators.ADD);
    }
}
