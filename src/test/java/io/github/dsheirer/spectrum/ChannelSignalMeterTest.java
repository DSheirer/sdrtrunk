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

import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.spectrum.converter.FullScaleDecibelConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jtransforms.fft.FloatFFT_1D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Builds dBFS spectrum frames the same way as the channel spectral display (unity gain window, JTransforms DFT,
 * full scale decibel converter) from a tone plus white noise of known levels, and checks the channel power and SNR.
 */
public class ChannelSignalMeterTest
{
    private static final double SAMPLE_RATE = 50000.0;
    private static final int BANDWIDTH = 12500;
    private static final int DFT_SIZE = 4096;
    private static final double NOISE_VARIANCE = 1E-4;
    private static final double TOLERANCE_DB = 0.3;
    private static final WindowType[] WINDOWS = {WindowType.BLACKMAN_HARRIS_7, WindowType.HANN, WindowType.HAMMING,
        WindowType.NONE, WindowType.FLAT_TOP};

    /**
     * Averages the readings of 50 frames.  Each reading is one frame, so this averages the power the same way as the
     * meter's 500 ms reading interval.
     */
    private static double[] measure(WindowType windowType, double toneMagnitude, double toneOffset)
    {
        List<ChannelSignalMeter.Reading> readings = new ArrayList<>();
        ChannelSignalMeter meter = new ChannelSignalMeter(readings::add, 0);
        meter.setChannel(SAMPLE_RATE, BANDWIDTH, () -> NOISE_VARIANCE / SAMPLE_RATE);
        meter.setWindowNoiseBandwidth(ComplexDftProcessor.getNoiseBandwidth(windowType));

        float[] window = ComplexDftProcessor.createWindow(windowType, DFT_SIZE, true);
        FloatFFT_1D fft = new FloatFFT_1D(DFT_SIZE);
        Random random = new Random(42);
        double sigma = Math.sqrt(NOISE_VARIANCE / 2.0);
        double phase = 0.0;
        double increment = 2.0 * Math.PI * toneOffset / SAMPLE_RATE;

        for(int frame = 0; frame < 50; frame++)
        {
            float[] samples = new float[DFT_SIZE * 2];

            for(int x = 0; x < DFT_SIZE; x++)
            {
                samples[2 * x] = (float)((random.nextGaussian() * sigma + toneMagnitude * Math.cos(phase)) * window[x]);
                samples[2 * x + 1] = (float)((random.nextGaussian() * sigma + toneMagnitude * Math.sin(phase)) * window[x]);
                phase += increment;
            }

            fft.complexForward(samples);
            meter.receive(FullScaleDecibelConverter.convert(samples));
        }

        double power = 0.0;
        double noise = 0.0;

        for(ChannelSignalMeter.Reading reading: readings)
        {
            power += Math.pow(10.0, reading.dbfs() / 10.0);
            noise = reading.noiseDbfs();
        }

        power /= readings.size();
        double noisePower = Math.pow(10.0, noise / 10.0);
        return new double[]{10.0 * Math.log10(power), 10.0 * Math.log10((power - noisePower) / noisePower), noise};
    }

    private static double expectedNoiseDb()
    {
        return 10.0 * Math.log10(NOISE_VARIANCE / SAMPLE_RATE * BANDWIDTH);
    }

    /**
     * Test: noise-only channel power matches the injected noise within the channel bandwidth, for each window.
     */
    @Test
    void noiseOnly()
    {
        for(WindowType windowType: WINDOWS)
        {
            double[] result = measure(windowType, 0.0, 0.0);
            assertEquals(expectedNoiseDb(), result[0], TOLERANCE_DB, windowType + " channel dBFS");
            assertEquals(expectedNoiseDb(), result[2], 0.05, windowType + " noise dBFS");
        }
    }

    /**
     * Test: tone plus noise power and SNR, with the tone off of a bin center, for each window.
     */
    @Test
    void toneAndNoise()
    {
        for(WindowType windowType: WINDOWS)
        {
            double magnitude = 0.01;
            double[] result = measure(windowType, magnitude, 1234.5);
            double tone = magnitude * magnitude;
            double noise = NOISE_VARIANCE / SAMPLE_RATE * BANDWIDTH;
            System.out.printf("%s dBFS %.2f (expected %.2f) SNR %.2f (expected %.2f)%n", windowType, result[0],
                10.0 * Math.log10(tone + noise), result[1], 10.0 * Math.log10(tone / noise));
            assertEquals(10.0 * Math.log10(tone + noise), result[0], TOLERANCE_DB, windowType + " channel dBFS");
            assertEquals(10.0 * Math.log10(tone / noise), result[1], TOLERANCE_DB, windowType + " SNR");
        }
    }

    /**
     * Test: no noise density gives the channel power without an SNR.
     */
    @Test
    void unknownNoise()
    {
        List<ChannelSignalMeter.Reading> readings = new ArrayList<>();
        ChannelSignalMeter meter = new ChannelSignalMeter(readings::add, 0);
        meter.setChannel(SAMPLE_RATE, BANDWIDTH, () -> Double.NaN);
        float[] frame = new float[DFT_SIZE];
        java.util.Arrays.fill(frame, -100.0f);
        meter.receive(frame);
        assertEquals(1, readings.size());
        assertFalse(readings.getFirst().hasSnr());
    }
}
