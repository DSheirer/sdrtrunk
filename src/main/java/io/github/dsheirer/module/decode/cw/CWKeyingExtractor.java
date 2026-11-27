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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extracts the on/off keying of a CW tone from a buffer of audio as a sequence of key-down and key-up durations with
 * 1 millisecond resolution.
 *
 * The tone magnitude is measured with a quadrature mixer and a 4 millisecond moving average (boxcar) filter.  Two side
 * detectors measure the magnitude at +/- 250 Hz, where the boxcar filter response has its first null, so that a pure
 * keyed tone has almost no side magnitude.  A key-down element is only accepted when the tone magnitude dominates the
 * side magnitudes and the tone frequency is steady across the element (measured from the phase slope of the mixer
 * output in each half of the element).  This rejects swept tones, voice and other audio passing through the tone
 * frequency.
 */
public class CWKeyingExtractor
{
    private static final double WINDOW_SECONDS = 0.004;
    private static final double SIDE_OFFSET_HZ = 1.0 / WINDOW_SECONDS;
    private static final double MINIMUM_ON_OFF_RATIO = 4.0;
    private static final double MINIMUM_SIDE_RATIO = 2.0;
    private static final int MINIMUM_RUN_MILLISECONDS = 5;
    private static final int DRIFT_EDGE_MILLISECONDS = 3;
    private static final int DRIFT_MINIMUM_HALF_MILLISECONDS = 4;
    private static final double MAXIMUM_DRIFT_HZ = 40.0;

    private CWKeyingExtractor()
    {
    }

    /**
     * Extracts the keying runs.
     * @param audio samples
     * @param sampleRate of the audio
     * @param frequency of the CW tone
     * @return runs, starting and ending with key-down runs, or an empty list if no keying is detected.
     */
    public static List<MorseCodeParser.KeyingRun> extract(float[] audio, double sampleRate, double frequency)
    {
        int decimation = (int)Math.round(sampleRate / 1000.0);
        float[][] tonePhasor = getPhasor(audio, sampleRate, frequency, decimation);
        float[] tone = tonePhasor[0];
        float[] phase = tonePhasor[1];
        float[] lower = getPhasor(audio, sampleRate, frequency - SIDE_OFFSET_HZ, decimation)[0];
        float[] upper = getPhasor(audio, sampleRate, frequency + SIDE_OFFSET_HZ, decimation)[0];

        if(tone.length == 0)
        {
            return new ArrayList<>();
        }

        float onLevel = percentile(tone, 0.95);
        float offLevel = percentile(tone, 0.20);

        if(onLevel <= 0.0f || onLevel < offLevel * MINIMUM_ON_OFF_RATIO)
        {
            return new ArrayList<>();
        }

        //Key state with hysteresis
        float span = onLevel - offLevel;
        float onThreshold = offLevel + 0.55f * span;
        float offThreshold = offLevel + 0.45f * span;
        boolean[] keyed = new boolean[tone.length];
        boolean state = false;

        for(int x = 0; x < tone.length; x++)
        {
            state = state ? tone[x] > offThreshold : tone[x] > onThreshold;
            keyed[x] = state;
        }

        //Reject key-down runs where the tone doesn't dominate the side frequencies
        List<int[]> runs = toRuns(keyed);

        for(int[] run: runs)
        {
            if(run[0] == 1)
            {
                double toneSum = 0.0;
                double sideSum = 0.0;

                for(int x = run[1]; x < run[1] + run[2]; x++)
                {
                    toneSum += tone[x];
                    sideSum += Math.max(lower[x], upper[x]);
                }

                if(toneSum < sideSum * MINIMUM_SIDE_RATIO || getDrift(phase, run[1], run[2]) > MAXIMUM_DRIFT_HZ)
                {
                    Arrays.fill(keyed, run[1], run[1] + run[2], false);
                }
            }
        }

        //Absorb glitches shorter than the minimum run length into the surrounding state
        runs = toRuns(keyed);

        for(int[] run: runs)
        {
            if(run[2] < MINIMUM_RUN_MILLISECONDS && run[1] > 0 && run[1] + run[2] < keyed.length)
            {
                Arrays.fill(keyed, run[1], run[1] + run[2], keyed[run[1] - 1]);
            }
        }

        List<MorseCodeParser.KeyingRun> keyingRuns = new ArrayList<>();

        for(int[] run: toRuns(keyed))
        {
            boolean on = run[0] == 1;

            //Drop leading key-up and short leading glitches
            if(keyingRuns.isEmpty() && (!on || run[2] < MINIMUM_RUN_MILLISECONDS))
            {
                continue;
            }

            keyingRuns.add(new MorseCodeParser.KeyingRun(on, run[2]));
        }

        //Drop trailing key-up
        while(!keyingRuns.isEmpty() && !keyingRuns.getLast().keyed())
        {
            keyingRuns.removeLast();
        }

        return keyingRuns;
    }

    /**
     * Converts the key states into runs of {state (1 or 0), start index, length}.
     */
    private static List<int[]> toRuns(boolean[] keyed)
    {
        List<int[]> runs = new ArrayList<>();
        int start = 0;

        for(int x = 1; x <= keyed.length; x++)
        {
            if(x == keyed.length || keyed[x] != keyed[start])
            {
                runs.add(new int[]{keyed[start] ? 1 : 0, start, x - start});
                start = x;
            }
        }

        return runs;
    }

    /**
     * Frequency drift across a key-down element in Hertz: the difference between the frequency offsets (phase slopes)
     * of the second and first halves of the element, excluding the keying transitions at either end.
     * @param phase of the mixer output at 1 millisecond intervals
     * @param start of the element
     * @param length of the element
     * @return absolute drift in Hertz, or 0 for elements that are too short to measure
     */
    static double getDrift(float[] phase, int start, int length)
    {
        int first = start + DRIFT_EDGE_MILLISECONDS;
        int last = start + length - 1 - DRIFT_EDGE_MILLISECONDS;
        int half = (last - first) / 2;

        if(half < DRIFT_MINIMUM_HALF_MILLISECONDS)
        {
            return 0.0;
        }

        double firstSlope = getPhaseSlope(phase, first, first + half);
        double secondSlope = getPhaseSlope(phase, last - half, last);
        return Math.abs(secondSlope - firstSlope) * 1000.0 / (2.0 * Math.PI);
    }

    /**
     * Least squares slope (radians per millisecond) of the unwrapped phase between the indices (inclusive).
     */
    private static double getPhaseSlope(float[] phase, int from, int to)
    {
        int count = to - from + 1;
        double[] unwrapped = new double[count];
        unwrapped[0] = phase[from];

        for(int x = 1; x < count; x++)
        {
            double delta = phase[from + x] - phase[from + x - 1];
            delta -= 2.0 * Math.PI * Math.rint(delta / (2.0 * Math.PI));
            unwrapped[x] = unwrapped[x - 1] + delta;
        }

        double meanX = (count - 1) / 2.0;
        double meanY = 0.0;

        for(double value: unwrapped)
        {
            meanY += value;
        }

        meanY /= count;
        double numerator = 0.0;
        double denominator = 0.0;

        for(int x = 0; x < count; x++)
        {
            numerator += (x - meanX) * (unwrapped[x] - meanY);
            denominator += (x - meanX) * (x - meanX);
        }

        return denominator > 0 ? numerator / denominator : 0.0;
    }

    /**
     * Tone magnitude and phase at 1 millisecond intervals.
     * @return array of {magnitude, phase}
     */
    static float[][] getPhasor(float[] audio, double sampleRate, double frequency, int decimation)
    {
        int window = Math.max(1, (int)Math.round(sampleRate * WINDOW_SECONDS));
        double increment = 2.0 * Math.PI * frequency / sampleRate;
        float[] magnitude = new float[audio.length / decimation];
        float[] phaseAngle = new float[magnitude.length];
        double inphaseSum = 0.0;
        double quadratureSum = 0.0;
        double[] inphase = new double[window];
        double[] quadrature = new double[window];
        int pointer = 0;

        for(int x = 0; x < audio.length; x++)
        {
            double phase = increment * x;
            double i = audio[x] * Math.cos(phase);
            double q = -audio[x] * Math.sin(phase);
            inphaseSum += i - inphase[pointer];
            quadratureSum += q - quadrature[pointer];
            inphase[pointer] = i;
            quadrature[pointer] = q;
            pointer = (pointer + 1) % window;

            if((x + 1) % decimation == 0 && (x / decimation) < magnitude.length)
            {
                magnitude[x / decimation] = (float)(Math.sqrt(inphaseSum * inphaseSum + quadratureSum * quadratureSum) /
                    window);
                phaseAngle[x / decimation] = (float)Math.atan2(quadratureSum, inphaseSum);
            }
        }

        return new float[][]{magnitude, phaseAngle};
    }

    /**
     * Value at the percentile (0.0 - 1.0) of the values.
     */
    private static float percentile(float[] values, double percentile)
    {
        float[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[(int)Math.min(sorted.length - 1, Math.round(percentile * (sorted.length - 1)))];
    }
}
