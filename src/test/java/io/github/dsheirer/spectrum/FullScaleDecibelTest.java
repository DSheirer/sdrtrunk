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

package io.github.dsheirer.spectrum;

import io.github.dsheirer.dsp.fft.ComplexFFTFactory;
import io.github.dsheirer.dsp.window.WindowFactory;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.spectrum.converter.FullScaleDecibelConverter;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.Random;
import org.jtransforms.fft.FloatFFT_1D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the dBFS computation: unity gain window, DFT and full scale decibel conversion, the same steps the
 * spectral display's DFT processor uses.
 */
public class FullScaleDecibelTest
{
    private static final WindowType[] WINDOWS = {WindowType.NONE, WindowType.HANN, WindowType.BLACKMAN_HARRIS_7,
            WindowType.FLAT_TOP, WindowType.KAISER};

    /**
     * Windows, transforms and converts the interleaved samples to dBFS, in display order (DC at size / 2).
     */
    private static float[] dbfs(float[] samples, WindowType window)
    {
        int size = samples.length / 2;
        WindowFactory.apply(ComplexDftProcessor.createWindow(window, size, true), samples);
        ComplexFFTFactory.getComplexFFT(size, ComplexFFTFactory.Scalar.JTRANSFORMS).forward(samples);
        return FullScaleDecibelConverter.convert(samples);
    }

    /**
     * Power of each bin relative to full scale: |X|^2 / N^2.
     */
    private static double[] power(float[] results)
    {
        int size = results.length / 2;
        double[] power = new double[size];

        for(int bin = 0; bin < size; bin++)
        {
            double i = results[2 * bin];
            double q = results[2 * bin + 1];
            power[bin] = (i * i + q * q) / ((double)size * size);
        }

        return power;
    }

    private static float[] tone(int size, int bin, double amplitude)
    {
        float[] samples = new float[size * 2];

        for(int k = 0; k < size; k++)
        {
            double phase = 2.0 * Math.PI * bin * k / size;
            samples[2 * k] = (float)(amplitude * Math.cos(phase));
            samples[2 * k + 1] = (float)(amplitude * Math.sin(phase));
        }

        return samples;
    }

    /**
     * The spectral display's FFT matches JTransforms, which the display used before, for every size and both
     * implementations: the power of each bin within 1E-5 of full scale (-50 dBFS), which is far below the error
     * the display could show for the strong bins, plus a dBFS match within 0.01 dB for the bins above -60 dBFS.
     */
    @Test
    void spectrumMatchesJTransformsForEverySizeAndImplementation()
    {
        Random random = new Random(3);

        for(DFTSize size: DFTSize.values())
        {
            int n = size.getSize();
            float[] samples = new float[n * 2];

            for(int x = 0; x < samples.length; x++)
            {
                samples[x] = (float)(random.nextGaussian() * 0.1) + (x % 2 == 0 ? 0.5f : 0.0f);
            }

            float[] window = ComplexDftProcessor.createWindow(WindowType.BLACKMAN_HARRIS_7, n, true);
            float[] expected = samples.clone();
            WindowFactory.apply(window, expected);
            new FloatFFT_1D(n).complexForward(expected);
            double[] expectedPower = power(expected);

            for(Implementation implementation: new Implementation[]{Implementation.SCALAR,
                Implementation.VECTOR_SIMD_PREFERRED})
            {
                float[] actual = samples.clone();
                WindowFactory.apply(window, actual);
                ComplexFFTFactory.getComplexFFT(implementation, n, ComplexFFTFactory.Scalar.JTRANSFORMS)
                    .forward(actual);
                double[] actualPower = power(actual);

                float[] expectedDecibels = FullScaleDecibelConverter.convert(expected.clone());
                float[] actualDecibels = FullScaleDecibelConverter.convert(actual.clone());

                for(int bin = 0; bin < n; bin++)
                {
                    assertEquals(expectedPower[bin], actualPower[bin], 1E-5, size + " " + implementation + " bin " +
                        bin);

                    if(expectedDecibels[bin] > -60.0f)
                    {
                        assertEquals(expectedDecibels[bin], actualDecibels[bin], 0.01, size + " " + implementation +
                            " dBFS bin " + bin);
                    }
                }
            }
        }
    }

    @Test
    void fullScaleToneIsZeroDbfsForEverySizeAndWindow()
    {
        for(DFTSize size: DFTSize.values())
        {
            for(WindowType window: WINDOWS)
            {
                int n = size.getSize();
                int bin = n / 8;
                float[] decibels = dbfs(tone(n, bin, 1.0), window);
                assertEquals(0.0, decibels[n / 2 + bin], 0.01, size + " " + window);
            }
        }
    }

    @Test
    void toneLevelFollowsAmplitude()
    {
        int n = 4096;
        //-20 dBFS tone at a negative frequency
        float[] decibels = dbfs(tone(n, -300, 0.1), WindowType.BLACKMAN_HARRIS_7);
        assertEquals(-20.0, decibels[n / 2 - 300], 0.01);
    }

    @Test
    void noiseFloorIsLoweredByTheProcessingGain()
    {
        //Complex white noise with total power of 1E-4 (-40 dBFS)
        double power = 1E-4;
        double sigma = Math.sqrt(power / 2.0);

        for(DFTSize size: new DFTSize[]{DFTSize.FFT01024, DFTSize.FFT08192})
        {
            for(WindowType window: new WindowType[]{WindowType.NONE, WindowType.BLACKMAN_HARRIS_7})
            {
                int n = size.getSize();
                double sum = 0.0;
                int frames = 8;
                Random random = new Random(7);

                for(int frame = 0; frame < frames; frame++)
                {
                    float[] samples = new float[n * 2];

                    for(int x = 0; x < samples.length; x++)
                    {
                        samples[x] = (float)(random.nextGaussian() * sigma);
                    }

                    for(float decibel: dbfs(samples, window))
                    {
                        sum += Math.pow(10.0, decibel / 10.0);
                    }
                }

                double measured = 10.0 * Math.log10(sum / (frames * n));
                double expected = -40.0 - 10.0 * Math.log10(n / ComplexDftProcessor.getNoiseBandwidth(window));
                assertEquals(expected, measured, 0.3, size + " " + window);
            }
        }
    }

    @Test
    void noiseBandwidthIsInTheExpectedRange()
    {
        assertEquals(1.0, ComplexDftProcessor.getNoiseBandwidth(WindowType.NONE), 0.001);
        assertEquals(1.5, ComplexDftProcessor.getNoiseBandwidth(WindowType.HANN), 0.01);
        double blackmanHarris = ComplexDftProcessor.getNoiseBandwidth(WindowType.BLACKMAN_HARRIS_7);
        assertTrue(blackmanHarris > 2.0 && blackmanHarris < 3.5, "BH7 ENBW " + blackmanHarris);
    }

    @Test
    void sameCoefficientIsAppliedToIAndQ()
    {
        float[] window = ComplexDftProcessor.createWindow(WindowType.HANN, 512, false);
        assertEquals(1024, window.length);

        for(int x = 0; x < 512; x++)
        {
            assertEquals(window[2 * x], window[2 * x + 1], 0.0f);
        }
    }
}
