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
package io.github.dsheirer.dsp.fft;

import io.github.dsheirer.vector.calibrate.Implementation;
import java.lang.management.ManagementFactory;
import java.util.Random;
import jdk.incubator.vector.FloatVector;
import org.jtransforms.fft.FloatFFT_1D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verifies ComplexFFT and RealFFT against a double precision reference DFT, the scalar and vector implementations
 * against each other, the inverse round trip, the batch transforms and that no memory is allocated per transform.
 */
public class ComplexFFTTest
{
    /**
     * Maximum error relative to the RMS output magnitude, which is about 10 x float epsilon x log2(N) for the largest
     * sizes tested.
     */
    private static final double RELATIVE_TOLERANCE = 2e-6;
    /**
     * Implementations under test: both scalar FFTs, vector as the factory creates it, and vector using the four-step
     * transform for single transforms of every size.
     */
    private enum Variant
    {
        SCALAR,
        SCALAR_JTRANSFORMS,
        VECTOR,
        VECTOR_FOUR_STEP;

        ComplexFFT create(int size)
        {
            return switch(this)
            {
                case SCALAR -> ComplexFFTFactory.getComplexFFT(Implementation.SCALAR, size);
                case SCALAR_JTRANSFORMS -> ComplexFFTFactory.getComplexFFT(Implementation.SCALAR, size,
                    ComplexFFTFactory.Scalar.JTRANSFORMS);
                case VECTOR -> ComplexFFTFactory.getComplexFFT(Implementation.VECTOR_SIMD_PREFERRED, size);
                case VECTOR_FOUR_STEP -> new VectorComplexFFT(size, true);
            };
        }

        boolean isScalar()
        {
            return this == SCALAR || this == SCALAR_JTRANSFORMS;
        }
    }

    /**
     * Variants that this CPU can run: the vector classes are only created directly where the vector FFT is supported.
     */
    private static final Variant[] VARIANTS = VectorComplexFFT.isVectorSupported() ? Variant.values() :
        new Variant[]{Variant.SCALAR, Variant.SCALAR_JTRANSFORMS, Variant.VECTOR};

    /**
     * Sizes covering each radix, rows of length one, small sizes, the polyphase channelizer channel counts, the noise
     * floor estimator sizes, powers of two and sizes where the matrix isn't square.
     */
    private static final int[] SIZES = {1, 2, 3, 4, 5, 6, 8, 9, 10, 12, 15, 16, 20, 25, 27, 30, 32, 40, 45, 50, 60, 64,
        72, 75, 80, 90, 96, 100, 120, 125, 128, 150, 160, 192, 200, 240, 250, 256, 320, 384, 400, 480, 500, 512, 640,
        800, 1000, 1024, 1200, 1250, 2048, 2187, 3125, 4096, 6000, 8192};

    /**
     * Larger sizes, compared to MixedRadixComplexFFT since the reference DFT is slow at these sizes.
     */
    private static final int[] LARGE_SIZES = {16384, 32768, 62500, 65536, 125000, 500000};

    private static float[] randomSamples(int length, Random random)
    {
        float[] samples = new float[length];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return samples;
    }

    /**
     * Double precision DFT of interleaved complex samples.
     * @param samples interleaved complex
     * @param sign -1 for the forward transform, +1 for the inverse
     * @return interleaved complex result
     */
    static double[] referenceDFT(float[] samples, int sign)
    {
        int size = samples.length / 2;
        double[] cos = new double[size];
        double[] sin = new double[size];

        for(int x = 0; x < size; x++)
        {
            cos[x] = Math.cos(2.0 * Math.PI * x / size);
            sin[x] = sign * Math.sin(2.0 * Math.PI * x / size);
        }

        double[] result = new double[size * 2];

        for(int k = 0; k < size; k++)
        {
            double sumI = 0.0;
            double sumQ = 0.0;
            int index = 0;

            for(int n = 0; n < size; n++)
            {
                double i = samples[2 * n];
                double q = samples[2 * n + 1];
                sumI += i * cos[index] - q * sin[index];
                sumQ += i * sin[index] + q * cos[index];
                index += k;

                if(index >= size)
                {
                    index -= size;
                }
            }

            result[2 * k] = sumI;
            result[2 * k + 1] = sumQ;
        }

        return result;
    }

    /**
     * Error of the actual values relative to the RMS of the expected values.
     */
    static double relativeError(double[] expected, float[] actual)
    {
        double sumSquares = 0.0;
        double maximum = 0.0;

        for(int x = 0; x < expected.length; x++)
        {
            sumSquares += expected[x] * expected[x];
            maximum = Math.max(maximum, Math.abs(expected[x] - actual[x]));
        }

        double rms = Math.sqrt(sumSquares / expected.length);
        return maximum / Math.max(rms, 1e-30);
    }

    private static double[] toDouble(float[] values)
    {
        double[] result = new double[values.length];

        for(int x = 0; x < values.length; x++)
        {
            result[x] = values[x];
        }

        return result;
    }

    private static void assertCloseToReference(double[] expected, float[] actual, String message)
    {
        assertClose(expected, actual, RELATIVE_TOLERANCE, message);
    }

    /**
     * Asserts that two float results agree within the sum of their tolerances to the reference.
     */
    private static void assertAgree(float[] expected, float[] actual, String message)
    {
        assertClose(toDouble(expected), actual, 2 * RELATIVE_TOLERANCE, message);
    }

    private static void assertClose(double[] expected, float[] actual, double tolerance, String message)
    {
        double error = relativeError(expected, actual);
        assertTrue(error < tolerance, message + " relative error " + error);
    }

    @Test
    void forwardMatchesReferenceDFT()
    {
        Random random = new Random(1);

        for(int size : SIZES)
        {
            float[] samples = randomSamples(size * 2, random);
            double[] expected = referenceDFT(samples, -1);

            for(Variant variant : VARIANTS)
            {
                float[] actual = samples.clone();
                variant.create(size).forward(actual);
                assertCloseToReference(expected, actual, variant + " forward size " + size);
            }
        }
    }

    @Test
    void inverseMatchesReferenceDFT()
    {
        Random random = new Random(2);

        for(int size : SIZES)
        {
            float[] samples = randomSamples(size * 2, random);
            double[] expected = referenceDFT(samples, 1);
            double[] scaled = expected.clone();

            for(int x = 0; x < scaled.length; x++)
            {
                scaled[x] /= size;
            }

            for(Variant variant : VARIANTS)
            {
                ComplexFFT fft = variant.create(size);
                float[] unscaled = samples.clone();
                fft.inverse(unscaled, false);
                assertCloseToReference(expected, unscaled, variant + " inverse size " + size);
                float[] actual = samples.clone();
                fft.inverse(actual, true);
                assertCloseToReference(scaled, actual, variant + " scaled inverse size " + size);
            }
        }
    }

    @Test
    void offsetTransformsLeaveOtherSamples()
    {
        Random random = new Random(3);

        for(int size : new int[]{1, 6, 96, 1024, 2000})
        {
            float[] samples = randomSamples(size * 2 + 10, random);

            for(Variant variant : VARIANTS)
            {
                ComplexFFT fft = variant.create(size);
                float[] actual = samples.clone();
                fft.forward(actual, 4);
                fft.inverse(actual, 4, false);
                float[] block = new float[size * 2];
                System.arraycopy(samples, 4, block, 0, size * 2);
                double[] expected = referenceDFT(referenceDFTAsFloat(block, -1), 1);

                for(int x = 0; x < 4; x++)
                {
                    assertEquals(samples[x], actual[x], 0.0f, variant + " leading sample size " + size);
                    assertEquals(samples[samples.length - 1 - x], actual[actual.length - 1 - x], 0.0f,
                        variant + " trailing sample size " + size);
                }

                float[] actualBlock = new float[size * 2];
                System.arraycopy(actual, 4, actualBlock, 0, size * 2);
                assertCloseToReference(expected, actualBlock, variant + " offset size " + size);
            }
        }
    }

    private static float[] referenceDFTAsFloat(float[] samples, int sign)
    {
        double[] result = referenceDFT(samples, sign);
        float[] values = new float[result.length];

        for(int x = 0; x < result.length; x++)
        {
            values[x] = (float)result[x];
        }

        return values;
    }

    @Test
    void roundTripRestoresSamples()
    {
        Random random = new Random(4);

        for(int size : SIZES)
        {
            float[] samples = randomSamples(size * 2, random);

            for(Variant variant : VARIANTS)
            {
                ComplexFFT fft = variant.create(size);
                float[] actual = samples.clone();
                fft.forward(actual);
                fft.inverse(actual, true);
                assertCloseToReference(toDouble(samples), actual, variant + " round trip size " + size);
            }
        }
    }

    @Test
    void splitMatchesInterleaved()
    {
        Random random = new Random(15);

        for(int size : new int[]{1, 4, 20, 96, 320, 400, 800, 1024, 2187, 4096, 6000, 62500})
        {
            float[] samples = randomSamples(size * 2, random);
            float[] i = new float[size];
            float[] q = new float[size];

            for(Variant variant : VARIANTS)
            {
                ComplexFFT fft = variant.create(size);

                for(int x = 0; x < size; x++)
                {
                    i[x] = samples[2 * x];
                    q[x] = samples[2 * x + 1];
                }

                float[] expected = samples.clone();
                fft.forward(expected);
                fft.forward(i, q);

                for(int x = 0; x < size; x++)
                {
                    assertEquals(expected[2 * x], i[x], variant + " split inphase size " + size + " index " + x);
                    assertEquals(expected[2 * x + 1], q[x], variant + " split quadrature size " + size + " index " + x);
                }
            }
        }
    }

    @Test
    void largeSizesMatchMixedRadix()
    {
        Random random = new Random(5);

        for(int size : LARGE_SIZES)
        {
            float[] samples = randomSamples(size * 2, random);
            float[] expected = samples.clone();
            new MixedRadixComplexFFT(size).forward(expected);

            for(Variant variant : VARIANTS)
            {
                ComplexFFT fft = variant.create(size);
                float[] actual = samples.clone();
                fft.forward(actual);
                assertAgree(expected, actual, variant + " size " + size);
                fft.inverse(actual, true);
                assertCloseToReference(toDouble(samples), actual, variant + " round trip size " + size);
            }
        }
    }

    @Test
    void batchMatchesSingleTransforms()
    {
        Random random = new Random(6);
        int[] counts = {1, 3, 8, 17, 64, 100};

        for(int size : SIZES)
        {
            if(size > 2048)
            {
                continue;
            }

            for(int count : counts)
            {
                int offset = 6;
                float[] samples = randomSamples(offset + size * 2 * count + 4, random);

                for(Variant variant : VARIANTS)
                {
                    ComplexFFT fft = variant.create(size);
                    String context = variant + " size " + size + " count " + count;
                    float[] batch = samples.clone();
                    fft.forward(batch, offset, count);
                    float[] batchInverse = samples.clone();
                    fft.inverse(batchInverse, offset, count, true);

                    for(int x = 0; x < offset; x++)
                    {
                        assertEquals(samples[x], batch[x], 0.0f, "leading sample " + context);
                        assertEquals(samples[x], batchInverse[x], 0.0f, "leading sample " + context);
                    }

                    for(int x = offset + size * 2 * count; x < samples.length; x++)
                    {
                        assertEquals(samples[x], batch[x], 0.0f, "trailing sample " + context);
                        assertEquals(samples[x], batchInverse[x], 0.0f, "trailing sample " + context);
                    }

                    for(int block = 0; block < count; block++)
                    {
                        int start = offset + block * size * 2;
                        float[] input = new float[size * 2];
                        System.arraycopy(samples, start, input, 0, size * 2);
                        float[] forward = input.clone();
                        fft.forward(forward);
                        float[] inverse = input.clone();
                        fft.inverse(inverse, true);
                        float[] actualForward = new float[size * 2];
                        System.arraycopy(batch, start, actualForward, 0, size * 2);
                        float[] actualInverse = new float[size * 2];
                        System.arraycopy(batchInverse, start, actualInverse, 0, size * 2);
                        assertAgree(forward, actualForward, "forward " + context);
                        assertAgree(inverse, actualInverse, "inverse " + context);
                    }
                }
            }
        }
    }

    @Test
    void batchMatchesReferenceDFT()
    {
        Random random = new Random(7);

        for(int size : new int[]{2, 5, 12, 80, 96, 100, 120, 240, 320, 400, 512})
        {
            int count = 70;
            float[] samples = randomSamples(size * 2 * count, random);

            for(Variant variant : VARIANTS)
            {
                float[] actual = samples.clone();
                variant.create(size).inverse(actual, 0, count, true);

                for(int block = 0; block < count; block += 23)
                {
                    float[] input = new float[size * 2];
                    System.arraycopy(samples, block * size * 2, input, 0, size * 2);
                    double[] expected = referenceDFT(input, 1);

                    for(int x = 0; x < expected.length; x++)
                    {
                        expected[x] /= size;
                    }

                    float[] actualBlock = new float[size * 2];
                    System.arraycopy(actual, block * size * 2, actualBlock, 0, size * 2);
                    assertCloseToReference(expected, actualBlock, variant + " batch inverse size " + size);
                }
            }
        }
    }

    /**
     * Each column of the column FFT matches the reference DFT of that column, for each radix and for one or more
     * vectors of columns, and multiply() matches a scalar complex multiply.
     */
    @Test
    void columnFFTMatchesReferenceDFT()
    {
        assumeTrue(VectorComplexFFT.isVectorSupported());
        Random random = new Random(9);
        int lanes = FloatVector.SPECIES_PREFERRED.length();

        for(int length : new int[]{1, 2, 3, 4, 5, 6, 8, 12, 15, 16, 20, 25, 27, 32, 60, 64, 80, 96, 100, 125, 128, 400})
        {
            ColumnFFT fft = new ColumnFFT(length);

            for(int width = lanes; width <= 3 * lanes; width += lanes)
            {
                float[] i = randomSamples(length * width, random);
                float[] q = randomSamples(length * width, random);
                float[] resultI = i.clone();
                float[] resultQ = q.clone();
                fft.transform(resultI, resultQ, width, new float[4][length * width]);

                for(int column = 0; column < width; column++)
                {
                    float[] samples = new float[length * 2];
                    float[] actual = new float[length * 2];

                    for(int row = 0; row < length; row++)
                    {
                        samples[2 * row] = i[row * width + column];
                        samples[2 * row + 1] = q[row * width + column];
                        actual[2 * row] = resultI[row * width + column];
                        actual[2 * row + 1] = resultQ[row * width + column];
                    }

                    assertCloseToReference(referenceDFT(samples, -1), actual,
                        "length " + length + " width " + width + " column " + column);
                }

                float[] twiddleI = randomSamples(width + 2, random);
                float[] twiddleQ = randomSamples(width + 2, random);
                float[] productI = i.clone();
                float[] productQ = q.clone();
                fft.multiply(productI, productQ, 0, twiddleI, twiddleQ, 2, width);

                for(int x = 0; x < width; x++)
                {
                    assertEquals(i[x] * twiddleI[x + 2] - q[x] * twiddleQ[x + 2], productI[x], "multiply inphase");
                    assertEquals(i[x] * twiddleQ[x + 2] + q[x] * twiddleI[x + 2], productQ[x], "multiply quadrature");
                }
            }
        }
    }

    @Test
    void scalarAndVectorAgree()
    {
        Random random = new Random(10);

        for(int size : SIZES)
        {
            int count = 11;
            float[] samples = randomSamples(size * 2 * count, random);
            ComplexFFT scalar = Variant.SCALAR.create(size);

            for(Variant variant : VARIANTS)
            {
                ComplexFFT vector = variant.create(size);
                float[] expected = samples.clone();
                float[] actual = samples.clone();
                scalar.forward(expected);
                vector.forward(actual);
                assertAgree(expected, actual, variant + " forward size " + size);
                expected = samples.clone();
                actual = samples.clone();
                scalar.inverse(expected, 0, count, true);
                vector.inverse(actual, 0, count, true);
                assertAgree(expected, actual, variant + " batch inverse size " + size);
            }
        }
    }

    @Test
    void matchesJTransforms()
    {
        Random random = new Random(11);

        for(int size : new int[]{96, 400, 512, 1024, 4096, 32768})
        {
            float[] samples = randomSamples(size * 2, random);
            float[] expected = samples.clone();
            new FloatFFT_1D(size).complexForward(expected);

            for(Variant variant : VARIANTS)
            {
                float[] actual = samples.clone();
                variant.create(size).forward(actual);
                assertAgree(expected, actual, variant + " size " + size);
            }
        }
    }

    @Test
    void realForwardMatchesReferenceAndJTransforms()
    {
        Random random = new Random(12);

        for(int size : new int[]{2, 4, 6, 8, 10, 16, 64, 96, 250, 256, 1024, 2048, 4096, 8192, 16384, 32768})
        {
            float[] real = randomSamples(size, random);
            float[] complex = new float[size * 2];

            for(int x = 0; x < size; x++)
            {
                complex[2 * x] = real[x];
            }

            double[] reference = size <= 8192 ? referenceDFT(complex, -1) : null;
            float[] jtransforms = real.clone();
            new FloatFFT_1D(size).realForward(jtransforms);

            for(Implementation implementation : new Implementation[]{Implementation.SCALAR,
                Implementation.VECTOR_SIMD_PREFERRED})
            {
                float[] actual = real.clone();
                new RealFFT(size, implementation).forward(actual);

                if(reference != null)
                {
                    //Packed reference: Re[0], Re[N/2], then Re[k], Im[k] for 0 < k < N/2
                    double[] packed = new double[size];
                    packed[0] = reference[0];
                    packed[1] = reference[size];
                    System.arraycopy(reference, 2, packed, 2, size - 2);
                    assertCloseToReference(packed, actual, implementation + " real size " + size);
                }

                assertAgree(jtransforms, actual, implementation + " JTransforms size " + size);

                //Split samples give the same results in the split layout
                float[] even = new float[size / 2];
                float[] odd = new float[size / 2];

                for(int x = 0; x < size / 2; x++)
                {
                    even[x] = real[2 * x];
                    odd[x] = real[2 * x + 1];
                }

                new RealFFT(size, implementation).forward(even, odd);

                float[] interleaved = new float[size];

                for(int x = 0; x < size / 2; x++)
                {
                    interleaved[2 * x] = even[x];
                    interleaved[2 * x + 1] = odd[x];
                }

                assertAgree(actual, interleaved, implementation + " split real size " + size);
            }
        }
    }

    @Test
    void unsupportedSizesThrow()
    {
        assertFalse(ComplexFFT.isSupported(0));
        assertFalse(ComplexFFT.isSupported(7));
        assertFalse(ComplexFFT.isSupported(98));
        assertThrows(IllegalArgumentException.class, () -> ComplexFFTFactory.getComplexFFT(Implementation.SCALAR, 0));
        assertThrows(IllegalArgumentException.class,
            () -> ComplexFFTFactory.getComplexFFT(Implementation.VECTOR_SIMD_PREFERRED, 14));
        assertThrows(IllegalArgumentException.class,
            () -> ComplexFFTFactory.getComplexFFT(Implementation.VECTOR_SIMD_256, 14));
        assertFalse(RealFFT.isSupported(1));
        assertFalse(RealFFT.isSupported(15));
        assertFalse(RealFFT.isSupported(14));
        assertThrows(IllegalArgumentException.class, () -> new RealFFT(9, Implementation.SCALAR));
    }

    @Test
    void factoryCreatesImplementations()
    {
        //Scalar keeps each user's previous FFT: the mixed radix FFT, or JTransforms at power of two sizes
        for(Implementation scalar : new Implementation[]{Implementation.UNCALIBRATED, Implementation.SCALAR,
            Implementation.VECTOR_SIMD_256})
        {
            assertInstanceOf(MixedRadixComplexFFT.class, ComplexFFTFactory.getComplexFFT(scalar, 96));
            assertInstanceOf(MixedRadixComplexFFT.class, ComplexFFTFactory.getComplexFFT(scalar, 64));
            assertInstanceOf(MixedRadixComplexFFT.class, ComplexFFTFactory.getComplexFFT(scalar, 96,
                ComplexFFTFactory.Scalar.JTRANSFORMS));
            assertInstanceOf(JTransformsComplexFFT.class, ComplexFFTFactory.getComplexFFT(scalar, 4096,
                ComplexFFTFactory.Scalar.JTRANSFORMS));
        }

        //Where the vector FFT isn't supported, vector falls back to the same scalar FFTs
        boolean vector = VectorComplexFFT.isVectorSupported();
        Class<? extends ComplexFFT> mixedRadix = vector ? VectorComplexFFT.class : MixedRadixComplexFFT.class;
        Class<? extends ComplexFFT> jtransforms = vector ? VectorComplexFFT.class : JTransformsComplexFFT.class;
        assertInstanceOf(mixedRadix, ComplexFFTFactory.getComplexFFT(Implementation.VECTOR_SIMD_PREFERRED, 96));
        assertInstanceOf(jtransforms, ComplexFFTFactory.getComplexFFT(Implementation.VECTOR_SIMD_PREFERRED, 4096,
            ComplexFFTFactory.Scalar.JTRANSFORMS));
    }

    @Test
    void transformsDoNotAllocate()
    {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled());

        for(int size : new int[]{96, 400, 4096, 32768})
        {
            for(Variant variant : VARIANTS)
            {
                //JTransforms, which the scalar real FFT uses at power of two sizes, allocates when it uses its pool.
                //Where the vector FFT isn't supported, the vector variants fall back to the scalar FFTs.
                if((variant.isScalar() || !VectorComplexFFT.isVectorSupported()) && size >= 4096 &&
                    Integer.bitCount(size) == 1)
                {
                    continue;
                }

                ComplexFFT fft = variant.create(size);
                RealFFT realFFT = new RealFFT(size, variant.isScalar() ? Implementation.SCALAR :
                    Implementation.VECTOR_SIMD_PREFERRED);
                float[] samples = randomSamples(size * 2 * 4, new Random(13));
                float[] real = randomSamples(size, new Random(14));

                for(int x = 0; x < 200; x++)
                {
                    exercise(fft, realFFT, samples, real);
                }

                long thread = Thread.currentThread().threadId();
                long before = bean.getThreadAllocatedBytes(thread);

                for(int x = 0; x < 100; x++)
                {
                    exercise(fft, realFFT, samples, real);
                }

                long allocated = bean.getThreadAllocatedBytes(thread) - before;
                assertTrue(allocated < 1024, variant + " size " + size + " allocated " + allocated + " bytes");
            }
        }
    }

    private static void exercise(ComplexFFT fft, RealFFT realFFT, float[] samples, float[] real)
    {
        fft.forward(samples);
        fft.inverse(samples, true);
        fft.forward(samples, 0, 4);
        fft.inverse(samples, 0, 4, true);
        realFFT.forward(real);
    }
}
