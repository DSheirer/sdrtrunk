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

package io.github.dsheirer.gui.spectrum;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the waterfall noise floor tracking and dB to palette index mapping.
 */
public class WaterfallScalerTest
{
    private static final int BINS = 4096;

    /**
     * Noise bins in dB: exponentially distributed power with the given mean power in dB.
     */
    private static float[] noise(Random random, float meanDb)
    {
        float[] bins = new float[BINS];

        for(int x = 0; x < BINS; x++)
        {
            double power = -Math.log(1.0 - random.nextDouble());
            bins[x] = (float)(meanDb + 10.0 * Math.log10(power));
        }

        return bins;
    }

    private static int index(byte value)
    {
        return WaterfallScaler.paletteIndex(value & 0xFF, WaterfallScaler.RANGE_DEFAULT);
    }

    /**
     * Scaler with one DFT frame per row.
     */
    private static WaterfallScaler single()
    {
        WaterfallScaler scaler = new WaterfallScaler();
        scaler.setAveraging(1);
        return scaler;
    }

    private static double medianIndex(byte[] row, int from, int to)
    {
        int[] values = new int[to - from];

        for(int x = from; x < to; x++)
        {
            values[x - from] = index(row[x]);
        }

        Arrays.sort(values);
        return values[values.length / 2];
    }

    @Test
    void selectFindsTheKthSmallestValue()
    {
        Random random = new Random(1);
        float[] values = new float[1001];

        for(int x = 0; x < values.length; x++)
        {
            values[x] = random.nextFloat() * 100 - 50;
        }

        float[] sorted = values.clone();
        Arrays.sort(sorted);

        for(int k: new int[]{0, 1, 250, 500, 999, 1000})
        {
            assertEquals(sorted[k], WaterfallScaler.select(values.clone(), k), "k " + k);
        }
    }

    @Test
    void contrastDoesNotDependOnTheAbsoluteNoiseFloorLevel()
    {
        for(float floor: new float[]{-60, -100, -140})
        {
            WaterfallScaler scaler = single();
            float[] bins = noise(new Random(2), floor);

            //A 6 dB signal across bins 1000 - 1199
            for(int x = 1000; x < 1200; x++)
            {
                bins[x] = floor + 6.0f;
            }

            byte[] row = scaler.process(bins);
            double noiseMedian = medianIndex(row, 0, 1000);
            double signal = index(row[1100]);

            //Noise median sits in the lower part of the palette, above the background
            assertTrue(noiseMedian > 25 && noiseMedian < 50, "floor " + floor + " noise median " + noiseMedian);
            //6 dB signal is 7.6 dB above the noise median (1.6 dB below mean power): about 43 palette steps
            assertEquals(43.0, signal - noiseMedian, 3.0, "floor " + floor);
        }
    }

    /**
     * Strong signals in a fraction of the bins move the 25th percentile up the noise distribution: about 1.5 dB at
     * 25% occupancy and 3.8 dB (the noise median) at 50% occupancy.  A mean of the dB values would move 5 and 10 dB.
     */
    @Test
    void signalsInMuchOfTheSpanBarelyRaiseTheFloor()
    {
        float[] noise = noise(new Random(3), -100);
        WaterfallScaler empty = single();
        empty.process(noise);

        assertEquals(1.5, occupiedFloor(noise, 0.25) - empty.getFloor(), 0.5);
        assertEquals(3.8, occupiedFloor(noise, 0.5) - empty.getFloor(), 0.5);
    }

    private static float occupiedFloor(float[] noise, double occupancy)
    {
        float[] bins = noise.clone();

        for(int x = 0; x < (int)(BINS * occupancy); x++)
        {
            bins[x] += 20.0f;
        }

        WaterfallScaler scaler = single();
        scaler.process(bins);
        return scaler.getFloor();
    }

    @Test
    void floorIsSmoothedAcrossRowsJumpsLargeStepsAndResets()
    {
        WaterfallScaler scaler = single();
        Random random = new Random(4);
        scaler.process(noise(random, -100));
        float initial = scaler.getFloor();

        //A 5 dB step moves the floor only part way on the next row
        scaler.process(noise(random, -95));
        float stepped = scaler.getFloor() - initial;
        assertTrue(stepped > 0.5 && stepped < 1.5, "stepped " + stepped);

        //A 20 dB step jumps straight to the new level
        scaler.process(noise(random, -80));
        assertEquals(initial + 20.0, scaler.getFloor(), 1.0);

        //Reset restarts from the next row
        scaler.reset();
        scaler.process(noise(random, -100));
        assertEquals(initial, scaler.getFloor(), 1.0);
    }

    @Test
    void averagingCompletesARowEveryNFramesWithTheMeanPower()
    {
        WaterfallScaler scaler = new WaterfallScaler();
        scaler.setAveraging(2);
        float[] low = new float[BINS];
        float[] high = new float[BINS];
        java.util.Arrays.fill(low, -100.0f);
        java.util.Arrays.fill(high, -100.0f);

        //One bin at -100 dB then -90 dB: mean power is 10 * log10((1 + 10) / 2) = 7.4 dB above -100 dB
        low[10] = -100.0f;
        high[10] = -90.0f;

        assertNull(scaler.process(low));
        byte[] row = scaler.process(high);
        assertNotNull(row);
        float level = ((row[10] & 0xFF) - WaterfallScaler.FLOOR_LEVEL) * WaterfallScaler.LEVEL_STEP_DB;
        assertEquals(7.4, level, WaterfallScaler.LEVEL_STEP_DB);

        assertNull(scaler.process(low));
        assertNotNull(scaler.process(low));
    }

    /**
     * Averaging two frames should cut the noise spread (about 5.6 dB for one frame) to about 3.5 dB, so a weak signal
     * stands further out of the noise.
     */
    @Test
    void averagingSeparatesWeakSignalsFromTheNoise()
    {
        double single = separation(1);
        double averaged = separation(2);
        assertTrue(averaged > single * 1.1, "single " + single + " averaged " + averaged);
    }

    /**
     * Mean level difference between a 3 dB signal and the noise, relative to the noise level spread, in level codes.
     */
    private static double separation(int frames)
    {
        WaterfallScaler scaler = new WaterfallScaler();
        scaler.setAveraging(frames);
        Random random = new Random(6);
        byte[] row = null;

        while(row == null)
        {
            float[] bins = noise(random, -100);

            for(int x = 0; x < BINS / 2; x++)
            {
                double power = -Math.log(1.0 - random.nextDouble()) + Math.pow(10, 0.3);
                bins[x] = (float)(-100 + 10.0 * Math.log10(power));
            }

            row = scaler.process(bins);
        }

        double[] signal = stats(row, 0, BINS / 2);
        double[] noise = stats(row, BINS / 2, BINS);
        return (signal[0] - noise[0]) / noise[1];
    }

    private static double[] stats(byte[] row, int from, int to)
    {
        double sum = 0;
        double squares = 0;

        for(int x = from; x < to; x++)
        {
            int level = row[x] & 0xFF;
            sum += level;
            squares += level * level;
        }

        double mean = sum / (to - from);
        return new double[]{mean, Math.sqrt(squares / (to - from) - mean * mean)};
    }

    @Test
    void smallerRangesGiveMoreContrast()
    {
        int level6dB = WaterfallScaler.FLOOR_LEVEL + Math.round(6.0f / WaterfallScaler.LEVEL_STEP_DB);
        int floor = WaterfallScaler.paletteIndex(WaterfallScaler.FLOOR_LEVEL, WaterfallScaler.RANGE_DEFAULT);
        assertEquals(WaterfallScaler.FLOOR_INDEX, floor, 0.5);

        int wide = WaterfallScaler.paletteIndex(level6dB, WaterfallScaler.RANGE_MAXIMUM) - floor;
        int normal = WaterfallScaler.paletteIndex(level6dB, WaterfallScaler.RANGE_DEFAULT) - floor;
        int narrow = WaterfallScaler.paletteIndex(level6dB, WaterfallScaler.RANGE_MINIMUM) - floor;
        assertTrue(wide < normal && normal < narrow, wide + " " + normal + " " + narrow);
        assertEquals(6.0 * 255 / WaterfallScaler.RANGE_DEFAULT, normal, 1.0);

        //The level codes reach the top of the palette at the widest range
        assertEquals(255, WaterfallScaler.paletteIndex(255, WaterfallScaler.RANGE_MAXIMUM));
    }

    /**
     * At startup the DFT runs on an empty sample buffer, which converts to -196 dB in every bin.  The first rows of
     * real noise must not show as strong signals while the floor reference catches up.
     */
    @Test
    void silentStartupFramesDoNotFlashTheFirstNoiseRows()
    {
        WaterfallScaler scaler = single();
        float[] silence = new float[BINS];
        Arrays.fill(silence, -196.0f);

        for(int x = 0; x < 3; x++)
        {
            scaler.process(silence);
        }

        Random random = new Random(7);

        for(int x = 0; x < 3; x++)
        {
            double median = medianIndex(scaler.process(noise(random, -100)), 0, BINS);
            assertTrue(median > 25 && median < 50, "row " + x + " noise median index " + median);
        }
    }

    @Test
    void indexesClampToThePalette()
    {
        WaterfallScaler scaler = single();
        float[] bins = noise(new Random(5), -100);
        bins[0] = 0.0f;
        bins[1] = -300.0f;
        byte[] row = scaler.process(bins);
        assertEquals(255, index(row[0]));
        assertEquals(0, index(row[1]));
    }
}
