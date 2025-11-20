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

package io.github.dsheirer.module.decode.nxdn.layer1.sync.standard;

import java.util.Arrays;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD Vector 512 implementation of NXDN Soft Sync Detector.
 */
public class NXDNStandardSoftSyncDetectorVector512 extends NXDNStandardSoftSyncDetector
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_512;
    //Extend the symbols out to 16x using zero padding after the 10th symbol
    private static final float[] SYNC_EXTENDED = Arrays.copyOf(SYMBOLS, 16);

    /**
     * Processes the demodulated soft symbol value and returns a correlation value against the preceding 24 soft
     * symbols that include this recent soft symbol.
     *
     * @return correlation score.
     */
    public float calculate()
    {
        return FloatVector.fromArray(VECTOR_SPECIES, SYNC_EXTENDED, 0)
                .mul(FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer))
                .reduceLanes(VectorOperators.ADD);
    }
}
