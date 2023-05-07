/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.vector;

import io.github.dsheirer.sample.complex.ComplexSamples;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with Project Panama SIMD vectors in JDK 17+.
 */
public class VectorUtilities
{
    private static final Logger mLog = LoggerFactory.getLogger(VectorUtilities.class);
    private static boolean mSpeciesMismatchLogged = false;

    /**
     * Checks the species to determine if it is compatible with the preferred species for the runtime CPU
     * and logs a warning if the species' lane width is wider than the preferred species ... which would be
     * hugely inefficient.
     *
     * @param species to test
     */
    public static void checkSpecies(VectorSpecies<Float> species)
    {
        if(FloatVector.SPECIES_PREFERRED.length() < species.length())
        {
            if(!mSpeciesMismatchLogged)
            {
                mLog.warn("CPU supports maximum SIMD instructions of " + FloatVector.SPECIES_PREFERRED);
                mSpeciesMismatchLogged = true;
            }
        }
    }

    /**
     * Checks the I/Q sample array length to be an integer multiple of the SIMD lane width.
     * @param i samples
     * @param q samples
     * @param species used for SIMD operations
     */
    public static void checkComplexArrayLength(float[] i, float[] q, VectorSpecies<Float> species)
    {
        if(i.length % species.length() != 0)
        {
            throw new IllegalArgumentException("I/Q buffer lengths [" + i.length + "] must be a power of 2 multiple of " +
                    "SIMD lane width [" + species.length() + "]");
        }
    }

    /**
     * Checks the I/Q sample array length to be an integer multiple of the SIMD lane width.
     * @param array samples
     * @param species used for SIMD operations
     */
    public static void checkArrayLength(float[] array, VectorSpecies<Float> species)
    {
        if(array.length % species.length() != 0)
        {
            throw new IllegalArgumentException("Buffer array length [" + array.length + "] must be a power of 2 multiple " +
                    "of SIMD lane width [" + species.length() + "]");
        }
    }

    /**
     * Checks the I/Q sample array length to be an integer multiple of the SIMD lane width.
     * @param samples buffer
     * @param species used for SIMD operations
     */
    public static void checkComplexArrayLength(ComplexSamples samples, VectorSpecies<Float> species)
    {
        checkComplexArrayLength(samples.i(), samples.q(), species);
    }

    /**
     * Creates a vector mask for deinterleaving I samples from an interleaved complex sample vector.
     * @param species of SIMD
     * @return vector mask
     */
    public static VectorMask<Float> getIVectorMask(VectorSpecies<Float> species)
    {
        switch(species.length())
        {
            case 2:
                return VectorMask.fromArray(species, new boolean[]{true,false}, 0);
            case 4:
                return VectorMask.fromArray(species, new boolean[]{true,false,true,false}, 0);
            case 8:
                return VectorMask.fromArray(species, new boolean[]{true,false,true,false,true,false,true,false}, 0);
            case 16:
                return VectorMask.fromArray(species, new boolean[]{true,false,true,false,true,false,true,false,true,
                        false,true,false,true,false,true,false}, 0);
            default:
                throw new IllegalArgumentException("Unrecognized vector species: " + species);
        }
    }

    /**
     * Creates a vector mask for deinterleaving Q samples from an interleaved complex sample vector.
     * @param species of SIMD
     * @return vector mask
     */
    public static VectorMask<Float> getQVectorMask(VectorSpecies<Float> species)
    {
        switch(species.length())
        {
            case 2:
                return VectorMask.fromArray(species, new boolean[]{false,true}, 0);
            case 4:
                return VectorMask.fromArray(species, new boolean[]{false,true,false,true}, 0);
            case 8:
                return VectorMask.fromArray(species, new boolean[]{false,true,false,true,false,true,false,true}, 0);
            case 16:
                return VectorMask.fromArray(species, new boolean[]{false,true,false,true,false,true,false,true,false,true,
                        false,true,false,true,false,true}, 0);
            default:
                throw new IllegalArgumentException("Unrecognized vector species: " + species);
        }
    }

    /**
     * Creates an index map for routing inphase samples into a complex sample array
     * @param species to determine lane width
     * @return index map
     */
    public static int[] getIIndexMap(VectorSpecies<Float> species)
    {
        switch(species.length())
        {
            case 2:
                return new int[]{0,2};
            case 4:
                return new int[]{0,2,4,6};
            case 8:
                return new int[]{0,2,4,6,8,10,12,14};
            case 16:
                return new int[]{0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30};
            default:
                throw new IllegalArgumentException("Unrecognized vector species: " + species);
        }
    }

    /**
     * Creates an index map for routing quadrature samples into a complex sample array
     * @param species to determine lane width
     * @return index map
     */
    public static int[] getQIndexMap(VectorSpecies<Float> species)
    {
        switch(species.length())
        {
            case 2:
                return new int[]{1,3};
            case 4:
                return new int[]{1,3,5,7};
            case 8:
                return new int[]{1,3,5,7,9,11,13,15};
            case 16:
                return new int[]{1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31};
            default:
                throw new IllegalArgumentException("Unrecognized vector species: " + species);
        }
    }

    /**
     * Interleaves the samples from the I and Q vectors into a complex sample array
     * @param iVector samples
     * @param qVector samples
     * @return interleaved sample array
     */
    public static float[] interleave(FloatVector iVector, FloatVector qVector)
    {
        float[] interleaved = new float[iVector.length() * 2];
        float[] i = new float[iVector.length()];
        float[] q = new float[qVector.length()];

        iVector.intoArray(i, 0);
        qVector.intoArray(q, 0);

        for(int x = 0; x < i.length; x++)
        {
            interleaved[2 * x] = i[x];
            interleaved[2 * x + 1] = q[x];
        }

        return interleaved;
    }
}
