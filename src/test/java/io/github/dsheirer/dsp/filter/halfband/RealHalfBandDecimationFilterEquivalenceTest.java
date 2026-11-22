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
package io.github.dsheirer.dsp.filter.halfband;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.window.WindowType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import jdk.incubator.vector.FloatVector;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that each vector half-band decimation filter produces the same output as the scalar filter, apart from a
 * constant sample delay that some vector implementations add by holding extra overlap samples.
 */
public class RealHalfBandDecimationFilterEquivalenceTest
{
    private static final int BLOCK_SIZE = 2048;
    private static final int BLOCKS = 4;
    private static final int MAXIMUM_DELAY = 80;
    private static final float TOLERANCE = 1e-5f;

    private static DynamicTest implementation(String name, int length, int bits,
                                              Function<float[], IRealDecimationFilter> constructor)
    {
        return DynamicTest.dynamicTest(name, () -> matchesScalar(name, length, bits, constructor));
    }

    /**
     * Test: each vector implementation matches the scalar implementation.
     */
    @TestFactory
    Stream<DynamicTest> vectorMatchesScalar()
    {
        List<DynamicTest> arguments = new ArrayList<>();
        arguments.add(implementation("11Tap64Bit", 11, 64, VectorRealHalfBandDecimationFilter11Tap64Bit::new));
        arguments.add(implementation("11Tap128Bit", 11, 128, VectorRealHalfBandDecimationFilter11Tap128Bit::new));
        arguments.add(implementation("11Tap256Bit", 11, 256, VectorRealHalfBandDecimationFilter11Tap256Bit::new));
        arguments.add(implementation("11Tap512Bit", 11, 512, VectorRealHalfBandDecimationFilter11Tap512Bit::new));
        arguments.add(implementation("15Tap64Bit", 15, 64, VectorRealHalfBandDecimationFilter15Tap64Bit::new));
        arguments.add(implementation("15Tap128Bit", 15, 128, VectorRealHalfBandDecimationFilter15Tap128Bit::new));
        arguments.add(implementation("15Tap256Bit", 15, 256, VectorRealHalfBandDecimationFilter15Tap256Bit::new));
        arguments.add(implementation("15Tap512Bit", 15, 512, VectorRealHalfBandDecimationFilter15Tap512Bit::new));
        arguments.add(implementation("23Tap64Bit", 23, 64, VectorRealHalfBandDecimationFilter23Tap64Bit::new));
        arguments.add(implementation("23Tap128Bit", 23, 128, VectorRealHalfBandDecimationFilter23Tap128Bit::new));
        arguments.add(implementation("23Tap256Bit", 23, 256, VectorRealHalfBandDecimationFilter23Tap256Bit::new));
        arguments.add(implementation("23Tap512Bit", 23, 512, VectorRealHalfBandDecimationFilter23Tap512Bit::new));
        arguments.add(implementation("63Tap64Bit", 63, 64, VectorRealHalfBandDecimationFilter63Tap64Bit::new));
        arguments.add(implementation("63Tap128Bit", 63, 128, VectorRealHalfBandDecimationFilter63Tap128Bit::new));
        arguments.add(implementation("63Tap256Bit", 63, 256, VectorRealHalfBandDecimationFilter63Tap256Bit::new));
        arguments.add(implementation("63Tap512Bit", 63, 512, VectorRealHalfBandDecimationFilter63Tap512Bit::new));

        for(int length : new int[]{11, 15, 23, 63})
        {
            arguments.add(implementation("Default64Bit/" + length, length, 64, VectorRealHalfBandDecimationFilter64Bit::new));
            arguments.add(implementation("Default128Bit/" + length, length, 128, VectorRealHalfBandDecimationFilter128Bit::new));
            arguments.add(implementation("Default256Bit/" + length, length, 256, VectorRealHalfBandDecimationFilter256Bit::new));
            arguments.add(implementation("Default512Bit/" + length, length, 512, VectorRealHalfBandDecimationFilter512Bit::new));
        }

        return arguments.stream();
    }

    /**
     * Asserts that vector output matches scalar output for random samples, at the vector filter's constant delay.
     */
    private static void matchesScalar(String name, int length, int bits, Function<float[], IRealDecimationFilter> constructor)
    {
        Assumptions.assumeTrue(FloatVector.SPECIES_PREFERRED.vectorBitSize() >= bits,
            "CPU doesn't support " + bits + "-bit SIMD");

        float[] coefficients = FilterFactory.getHalfBand(length, WindowType.BLACKMAN);
        IRealDecimationFilter scalar = new RealHalfBandDecimationFilter(coefficients);
        IRealDecimationFilter vector = constructor.apply(coefficients);
        Random random = new Random(length * 1000L + bits);

        float[] input = new float[BLOCK_SIZE * BLOCKS];
        float[] scalarOutput = new float[BLOCK_SIZE / 2 * BLOCKS];
        float[] vectorOutput = new float[BLOCK_SIZE / 2 * BLOCKS];

        for(int x = 0; x < input.length; x++)
        {
            input[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        for(int block = 0; block < BLOCKS; block++)
        {
            float[] samples = new float[BLOCK_SIZE];
            System.arraycopy(input, block * BLOCK_SIZE, samples, 0, BLOCK_SIZE);
            System.arraycopy(scalar.decimateReal(samples.clone()), 0, scalarOutput, block * BLOCK_SIZE / 2, BLOCK_SIZE / 2);
            System.arraycopy(vector.decimateReal(samples.clone()), 0, vectorOutput, block * BLOCK_SIZE / 2, BLOCK_SIZE / 2);
        }

        //The scalar filter is the reference convolution delayed by (length - 1) input samples
        assertEquals(0.0, error(scalarOutput, input, coefficients, length - 1), TOLERANCE, "Scalar reference");

        //Find the input sample delay that best aligns the vector output with the reference convolution
        int bestDelay = -1;
        double bestError = Double.MAX_VALUE;

        for(int delay = 0; delay <= MAXIMUM_DELAY; delay++)
        {
            double error = error(vectorOutput, input, coefficients, delay);

            if(error < bestError)
            {
                bestError = error;
                bestDelay = delay;
            }
        }

        assertEquals(0.0, bestError, TOLERANCE, name + " doesn't match the reference filter at any delay (best " +
            "delay " + bestDelay + " input samples, max error " + bestError + ")");
    }

    /**
     * Maximum absolute error between the decimated output and the reference convolution of the input, delayed by the
     * number of input samples.  Skips the first block, where the filter is filling.
     */
    private static double error(float[] output, float[] input, float[] coefficients, int delay)
    {
        double error = 0.0;

        for(int x = BLOCK_SIZE / 2; x < output.length; x++)
        {
            double expected = 0.0;

            for(int k = 0; k < coefficients.length; k++)
            {
                int index = 2 * x + k - delay;

                if(index >= 0 && index < input.length)
                {
                    expected += coefficients[k] * input[index];
                }
            }

            error = Math.max(error, Math.abs(output[x] - expected));
        }

        return error;
    }
}
