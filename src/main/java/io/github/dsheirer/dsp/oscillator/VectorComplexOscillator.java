/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.dsp.oscillator;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.VectorUtilities;
import java.util.Arrays;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complex oscillator that uses JDK17 SIMD vector operations to generate complex sample arrays.
 *
 * Note: this class uses a bank of oscillators that are each rotated synchronously, where the oscillator is similar to
 * the ComplexOscillator class, but where each oscillator is offset in phase by one sample more than the previous and
 * the entire bank is rotated at the sample phase times the SIMD lane width for each sample generation increment.
 */
public class VectorComplexOscillator extends AbstractOscillator implements IComplexOscillator
{
    private static final Logger mLog = LoggerFactory.getLogger(VectorComplexOscillator.class);
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    private float[] mPreviousInphases;
    private float[] mPreviousQuadratures;
    private float[] mGainInitials; //Set to 3.0f as the first constant in the gain calculation

    /**
     * Constructs an instance
     *
     * @param frequency  in hertz
     * @param sampleRate in hertz
     */
    public VectorComplexOscillator(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);

        mGainInitials = new float[VECTOR_SPECIES.length()];
        Arrays.fill(mGainInitials, 3.0f);
    }

    @Override
    protected void update()
    {
        super.update();

        float cosineAngle = (float)FastMath.cos(getAnglePerSample());
        float sineAngle = (float)FastMath.sin(getAnglePerSample());

        if(mPreviousInphases == null || mPreviousQuadratures == null)
        {
            mPreviousInphases = new float[VECTOR_SPECIES.length()];
            mPreviousQuadratures = new float[VECTOR_SPECIES.length()];

            mPreviousInphases[0] = 1.0f;
        }

        float gain;

        //Setup the previous sample arrays where each index is offset one sample of rotation from the previous sample.
        // We don't touch index 0 so that it can maintain the previous phase offset and all other indices are updated
        // relative to index 0.
        for(int x = 1; x < VECTOR_SPECIES.length(); x++)
        {
            gain = (3.0f - ((mPreviousInphases[x - 1] * mPreviousInphases[x - 1]) +
                    (mPreviousQuadratures[x - 1] * mPreviousQuadratures[x - 1]))) / 2.0f;
            mPreviousInphases[x] = ((mPreviousInphases[x - 1] * cosineAngle) - (mPreviousQuadratures[x - 1] * sineAngle)) * gain;
            mPreviousQuadratures[x] = ((mPreviousInphases[x - 1] * sineAngle) + (mPreviousQuadratures[x -1] * cosineAngle)) * gain;
        }
    }

    /**
     * Generates complex samples.
     * @param sampleCount number of samples to generate and length of the resulting float array.
     * @return generated samples
     */
    @Override public float[] generate(int sampleCount)
    {
        if(sampleCount % VECTOR_SPECIES.length() != 0)
        {
            throw new IllegalArgumentException("Requested sample count [" + sampleCount +
                    "] must be a power of 2 and a multiple of the SIMD lane width [" + VECTOR_SPECIES.length() + "]");
        }

        float[] samples = new float[sampleCount * 2];
        FloatVector previousInphase = FloatVector.fromArray(VECTOR_SPECIES, mPreviousInphases, 0);
        FloatVector previousQuadrature = FloatVector.fromArray(VECTOR_SPECIES, mPreviousQuadratures, 0);
        FloatVector gainInitials = FloatVector.fromArray(VECTOR_SPECIES, mGainInitials, 0);

        //Sine and cosine angle per sample, with the rotation angle multiplied by the SIMD lane width
        float cosAngle = (float)(FastMath.cos(getAnglePerSample() * VECTOR_SPECIES.length()));
        float sinAngle = (float)(FastMath.sin(getAnglePerSample() * VECTOR_SPECIES.length()));

        FloatVector gain, inphase, quadrature;

        int gainCounter = 0;

        for(int samplePointer = 0; samplePointer < sampleCount; samplePointer += VECTOR_SPECIES.length())
        {
            if(++gainCounter % 10 == 0)
            {
                gain = gainInitials.sub(previousInphase.pow(2.0f).add(previousQuadrature.pow(2.0f))).div(2.0f);
                inphase = previousInphase.mul(cosAngle).sub(previousQuadrature.mul(sinAngle)).mul(gain);
                quadrature = previousInphase.mul(sinAngle).add(previousQuadrature.mul(cosAngle)).mul(gain);
            }
            else
            {
                inphase = previousInphase.mul(cosAngle).sub(previousQuadrature.mul(sinAngle));
                quadrature = previousInphase.mul(sinAngle).add(previousQuadrature.mul(cosAngle));
            }

            float[] interleaved = VectorUtilities.interleave(inphase, quadrature);
            System.arraycopy(interleaved, 0, samples, samplePointer * 2, interleaved.length);

            previousInphase = inphase;
            previousQuadrature = quadrature;
        }

        previousInphase.intoArray(mPreviousInphases, 0);
        previousQuadrature.intoArray(mPreviousQuadratures, 0);

        return samples;
    }

    /**
     * Generates complex samples.
     * @param sampleCount number of samples to generate and length of the resulting float array.
     * @param timestamp of the first sample
     * @return generated samples
     */
    @Override public ComplexSamples generateComplexSamples(int sampleCount, long timestamp)
    {
        if(sampleCount % VECTOR_SPECIES.length() != 0)
        {
            throw new IllegalArgumentException("Requested sample count [" + sampleCount +
                    "] must be a power of 2 and a multiple of the SIMD lane width [" + VECTOR_SPECIES.length() + "]");
        }

        float[] iSamples = new float[sampleCount];
        float[] qSamples = new float[sampleCount];

        FloatVector previousInphase = FloatVector.fromArray(VECTOR_SPECIES, mPreviousInphases, 0);
        FloatVector previousQuadrature = FloatVector.fromArray(VECTOR_SPECIES, mPreviousQuadratures, 0);
        FloatVector gainInitials = FloatVector.fromArray(VECTOR_SPECIES, mGainInitials, 0);

        //Sine and cosine angle per sample, with the rotation angle multiplied by the SIMD lane width
        float cosAngle = (float)(FastMath.cos(getAnglePerSample() * VECTOR_SPECIES.length()));
        float sinAngle = (float)(FastMath.sin(getAnglePerSample() * VECTOR_SPECIES.length()));

        FloatVector gain, inphase, quadrature;

        for(int samplePointer = 0; samplePointer < sampleCount; samplePointer += VECTOR_SPECIES.length())
        {
            gain = gainInitials.sub(previousInphase.pow(2.0f).add(previousQuadrature.pow(2.0f))).div(2.0f);
            inphase = previousInphase.mul(cosAngle).sub(previousQuadrature.mul(sinAngle)).mul(gain);
            quadrature = previousInphase.mul(sinAngle).add(previousQuadrature.mul(cosAngle)).mul(gain);

            inphase.intoArray(iSamples, samplePointer);
            quadrature.intoArray(qSamples, samplePointer);

            previousInphase = inphase;
            previousQuadrature = quadrature;
        }

        previousInphase.intoArray(mPreviousInphases, 0);
        previousQuadrature.intoArray(mPreviousQuadratures, 0);

        return new ComplexSamples(iSamples, qSamples, timestamp);
    }
}
