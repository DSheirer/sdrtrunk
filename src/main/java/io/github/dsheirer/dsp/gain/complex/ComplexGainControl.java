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
package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.sample.SampleUtils;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSamples;

import java.text.DecimalFormat;
import java.util.Arrays;

public class ComplexGainControl implements IComplexGainControl
{
    public static final float OBJECTIVE_ENVELOPE = 1.0f;
    public static final float MINIMUM_ENVELOPE = 0.0001f;

    /**
     * Dynamic gain control for incoming sample stream to amplify or attenuate
     * all samples toward an objective unity)gain, using the maximum envelope
     * value detected in the stream history window.
     *
     * Uses the specified damping factor to limit gain swings.  Damping factor
     * is applied against the delta between current gain value and a recalculated
     * gain value to limit how quickly the gain value will increase or decrease.
     */
    public ComplexGainControl()
    {
    }

    /**
     * Processes the complex I & Q samples and applies gain to achieve an objective unity
     * gain for the single sample in the buffer that has the largest envelope.
     * @param i samples
     * @param q samples
     * @return processed/amplified buffer
     */
    @Override public ComplexSamples process(float[] i, float[] q)
    {
        float maxEnvelope = MINIMUM_ENVELOPE;

        for(int x = 0; x < i.length; x++)
        {
            maxEnvelope = Math.max(maxEnvelope, Complex.envelope(i[x], q[x]));
        }

        float gain = OBJECTIVE_ENVELOPE / maxEnvelope;

        float[] iProcessed = new float[i.length];
        float[] qProcessed = new float[q.length];

        for(int x = 0; x < i.length; x++)
        {
            iProcessed[x] = i[x] * gain;
            qProcessed[x] = q[x] * gain;
        }

        return new ComplexSamples(iProcessed, qProcessed);
    }

    public static void main(String[] args)
    {
        int sampleSize = 2048;
        ComplexSamples samples = SampleUtils.generateComplexRandomVectorLength(sampleSize);
        ComplexGainControl scalar = new ComplexGainControl();
        VectorComplexGainControl vector = new VectorComplexGainControl();

        boolean validate = false;

        if(validate)
        {
            ComplexSamples scalarSamples = scalar.process(samples.i(), samples.q());
            ComplexSamples vectorSamples = vector.process(samples.i(), samples.q());

            System.out.println("SCALAR I: " + Arrays.toString(scalarSamples.i()));
            System.out.println("VECTOR I: " + Arrays.toString(vectorSamples.i()));
            System.out.println("SCALAR Q: " + Arrays.toString(scalarSamples.q()));
            System.out.println("VECTOR Q: " + Arrays.toString(vectorSamples.q()));
        }
        else
        {
            double accumulator = 0.0d;
            int iterations = 10_000_000;
            long start = System.currentTimeMillis();

            for(int x = 0; x < iterations; x++)
            {
//                ComplexSamples processed = scalar.process(samples.i(), samples.q());
                ComplexSamples processed = vector.process(samples.i(), samples.q());
                accumulator += processed.i()[3];
            }

            double elapsed = System.currentTimeMillis() - start;
            DecimalFormat df = new DecimalFormat("0.000");
            System.out.println("Accumulator: " + accumulator);
            System.out.println("Test Complete.  Elapsed Time: " + df.format(elapsed / 1000.0d) + " seconds");
        }
    }
}
