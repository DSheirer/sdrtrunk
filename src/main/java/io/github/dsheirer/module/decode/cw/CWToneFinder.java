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

package io.github.dsheirer.module.decode.cw;

import java.util.Arrays;

/**
 * Detects the presence and frequency of a steady tone in short frames of audio, using a bank of Goertzel tone
 * detectors spaced 25 Hz apart from 300 to 2500 Hz.  A tone is present when:
 * <ul>
 *     <li>the strongest detector is between 325 and 2000 Hz, so that tones above the CW range, such as a 2175 Hz
 *     tone remote control guard tone, are not detected through their leakage into the CW range</li>
 *     <li>the strongest detector is at least 13 dB above the median of all of the detectors</li>
 *     <li>at least half of the total power is within +/- 2 detectors of the strongest detector, which rejects noise
 *     and compressed audio with several spectral peaks</li>
 * </ul>
 */
public class CWToneFinder
{
    public static final double MINIMUM_FREQUENCY = 300.0;
    public static final double MAXIMUM_FREQUENCY = 2500.0;
    public static final double MAXIMUM_TONE_FREQUENCY = 2000.0;
    public static final double FREQUENCY_STEP = 25.0;
    public static final int NO_TONE = -1;
    private static final double MINIMUM_PEAK_TO_MEDIAN_RATIO = 20.0;
    private static final double MINIMUM_PEAK_CONCENTRATION = 0.5;
    private static final int PEAK_WIDTH_BINS = 2;

    private final double mSampleRate;
    private final float[] mWindow;
    private final double[] mCoefficients;
    private final double[] mPowers;

    /**
     * Constructs an instance
     * @param sampleRate of the audio
     * @param frameLength number of samples per frame
     */
    public CWToneFinder(double sampleRate, int frameLength)
    {
        mSampleRate = sampleRate;
        mWindow = new float[frameLength];

        for(int x = 0; x < frameLength; x++)
        {
            mWindow[x] = (float)(0.5 - 0.5 * Math.cos(2.0 * Math.PI * x / (frameLength - 1)));
        }

        int bins = getBinCount();
        mCoefficients = new double[bins];
        mPowers = new double[bins];

        for(int bin = 0; bin < bins; bin++)
        {
            mCoefficients[bin] = 2.0 * Math.cos(2.0 * Math.PI * getFrequency(bin) / mSampleRate);
        }
    }

    /**
     * Number of tone detector bins
     */
    public static int getBinCount()
    {
        return (int)((MAXIMUM_FREQUENCY - MINIMUM_FREQUENCY) / FREQUENCY_STEP) + 1;
    }

    /**
     * Frequency of the tone detector bin
     */
    public static double getFrequency(int bin)
    {
        return MINIMUM_FREQUENCY + bin * FREQUENCY_STEP;
    }

    /**
     * Finds the tone in the frame.
     * @param samples circular buffer containing the frame
     * @param offset of the first frame sample in the buffer
     * @return bin of the tone, or {@link #NO_TONE}
     */
    public int find(float[] samples, int offset)
    {
        int length = mWindow.length;

        for(int bin = 0; bin < mCoefficients.length; bin++)
        {
            double coefficient = mCoefficients[bin];
            double s1 = 0.0;
            double s2 = 0.0;

            for(int x = 0; x < length; x++)
            {
                double s0 = samples[(offset + x) % samples.length] * mWindow[x] + coefficient * s1 - s2;
                s2 = s1;
                s1 = s0;
            }

            mPowers[bin] = s1 * s1 + s2 * s2 - coefficient * s1 * s2;
        }

        int peak = 0;

        for(int bin = 1; bin < mPowers.length; bin++)
        {
            if(mPowers[bin] > mPowers[peak])
            {
                peak = bin;
            }
        }

        double[] sorted = mPowers.clone();
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];

        if(peak == 0 || getFrequency(peak) > MAXIMUM_TONE_FREQUENCY || mPowers[peak] <= 0.0 ||
            mPowers[peak] < median * MINIMUM_PEAK_TO_MEDIAN_RATIO)
        {
            return NO_TONE;
        }

        double total = 0.0;
        double peakPower = 0.0;

        for(int bin = 0; bin < mPowers.length; bin++)
        {
            total += mPowers[bin];

            if(Math.abs(bin - peak) <= PEAK_WIDTH_BINS)
            {
                peakPower += mPowers[bin];
            }
        }

        return peakPower >= total * MINIMUM_PEAK_CONCENTRATION ? peak : NO_TONE;
    }
}
