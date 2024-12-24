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

package io.github.dsheirer.module.decode.dmr.sync;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD Vector 512 implementation of DMR Soft Sync Detector.
 */
public class DMRSoftSyncDetectorVector512 extends DMRSoftSyncDetector
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
        FloatVector symbols1 = FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer);
        FloatVector symbols2 = FloatVector.fromArray(VECTOR_SPECIES, mSymbols, mSymbolPointer + 8, mask);
        FloatVector accumulator = FloatVector.zero(VECTOR_SPECIES);
        FloatVector sync1, sync2;
        float bestScore, candidate;

        switch(mMode)
        {
            case AUTOMATIC:
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, BASE_DATA, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, BASE_DATA, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                bestScore = accumulator.reduceLanes(VectorOperators.ADD);
                mDetectedPattern = DMRSyncPattern.BASE_STATION_DATA;

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, BASE_VOICE, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, BASE_VOICE, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.BASE_STATION_VOICE;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_DATA, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_DATA, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.MOBILE_STATION_DATA;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_VOICE, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_VOICE, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.MOBILE_STATION_VOICE;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_1, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_1, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_1;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_2, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_2, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_2;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_1, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_1, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_2, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_2, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2;
                }

                return bestScore;
            case BASE_ONLY:
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, BASE_DATA, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, BASE_DATA, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                bestScore = accumulator.reduceLanes(VectorOperators.ADD);
                mDetectedPattern = DMRSyncPattern.BASE_STATION_DATA;

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, BASE_VOICE, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, BASE_VOICE, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.BASE_STATION_VOICE;
                }

                return bestScore;
            case MOBILE_ONLY:
                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_DATA, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_DATA, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                bestScore = accumulator.reduceLanes(VectorOperators.ADD);
                mDetectedPattern = DMRSyncPattern.MOBILE_STATION_DATA;

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_VOICE, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, MOBILE_VOICE, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.MOBILE_STATION_VOICE;
                }

                return bestScore;
            case DIRECT_ONLY:
                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_1, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_1, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                bestScore = accumulator.reduceLanes(VectorOperators.ADD);
                mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_1;

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_2, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_DATA_2, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.DIRECT_DATA_TIMESLOT_2;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_1, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_1, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1;
                }

                accumulator = FloatVector.zero(VECTOR_SPECIES);
                sync1 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_2, 0);
                sync2 = FloatVector.fromArray(VECTOR_SPECIES, DIRECT_VOICE_2, 8);

                accumulator = symbols1.fma(sync1, accumulator);
                accumulator = symbols2.fma(sync2, accumulator);
                candidate = accumulator.reduceLanes(VectorOperators.ADD);

                if(candidate > bestScore)
                {
                    bestScore = candidate;
                    mDetectedPattern = DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2;
                }

                return bestScore;
            default:
                throw new IllegalStateException("Unrecognized DMR Sync Detection Mode: " + mMode);
        }
    }
}
