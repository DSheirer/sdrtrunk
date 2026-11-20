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

/**
 * Converts DFT results in dB to rows of waterfall level codes, and level codes to palette indexes.
 *
 * Optionally averages several DFT frames (in linear power) into each row.  Averaging reduces the random variation of
 * the noise bins (about 5.6 dB for a single frame), which is the only thing that makes weak signals statistically
 * easier to see, at the cost of fewer rows per second.
 *
 * The noise floor reference is a low percentile of the bins, so signals that fill much of the span don't raise it,
 * smoothed across rows so the waterfall background doesn't flicker.
 *
 * Each row stores a level code per bin: the bin level relative to the floor reference in LEVEL_STEP_DB steps.  The
 * level codes are mapped to palette indexes over a dB range above the floor (see paletteIndex()) when the row is
 * colored, so the range can change and the waterfall history can be recolored without the original DFT results.
 *
 * Not thread safe: process() on the DFT results thread only.  reset() and setAveraging() are safe from any thread.
 */
public class WaterfallScaler
{
    public static final int RANGE_MINIMUM = 20;
    public static final int RANGE_MAXIMUM = 80;
    public static final int RANGE_DEFAULT = 45;
    public static final int AVERAGING_MINIMUM = 1;
    public static final int AVERAGING_MAXIMUM = 4;
    public static final int AVERAGING_DEFAULT = 2;

    //Percentile of the bins used as the noise floor reference.  Robust to signals in up to 75% of the bins.
    static final double FLOOR_PERCENTILE = 0.25;
    //Smoothing applied to the floor reference each row: about 5 rows.  The percentile barely varies from row to row,
    //so this mostly limits jumps from bursts of signals while still following tuner gain changes quickly.
    static final float FLOOR_SMOOTHING = 0.2f;
    //Floor reference changes larger than this jump straight to the new level instead of smoothing, for example when
    //the first samples arrive after DFT frames of an empty buffer (-196 dB), or after a tuner or gain change.
    static final float FLOOR_JUMP_DB = 10.0f;
    //Level code resolution and the level code of the floor reference: codes cover -8 dB to +94 dB from the floor,
    //which covers the palette at the maximum range.
    static final float LEVEL_STEP_DB = 0.4f;
    static final int FLOOR_LEVEL = 20;
    //Palette index for the floor reference.  Noise bins spread above the 25th percentile, so this places the noise in
    //the lower part of the palette as texture with only the weakest noise bins at the background color.
    static final float FLOOR_INDEX = 16.0f;

    private static final float DB_TO_LN_POWER = (float)(Math.log(10.0) / 10.0);

    private float mFloor = Float.NaN;
    private volatile boolean mResetRequested;
    private volatile int mAveraging = AVERAGING_DEFAULT;
    private float[] mScratch = new float[0];
    private float[] mPowerSum = new float[0];
    private float[] mAverage = new float[0];
    private int mFrameCount;

    /**
     * Restarts floor tracking and frame averaging on the next frame, for example after the source changes.  Safe to
     * call from any thread.
     */
    public void reset()
    {
        mResetRequested = true;
    }

    /**
     * Sets the number of DFT frames averaged into each waterfall row.  Safe to call from any thread.
     * @param frames from AVERAGING_MINIMUM to AVERAGING_MAXIMUM
     */
    public void setAveraging(int frames)
    {
        mAveraging = Math.max(AVERAGING_MINIMUM, Math.min(AVERAGING_MAXIMUM, frames));
    }

    /**
     * Current noise floor reference in dB, or NaN before the first row.
     */
    public float getFloor()
    {
        return mFloor;
    }

    /**
     * Adds the DFT results to the current row.
     * @param bins in dB
     * @return the row of level codes (unsigned bytes) when the row is complete, otherwise null
     */
    public byte[] process(float[] bins)
    {
        if(mResetRequested)
        {
            mResetRequested = false;
            mFloor = Float.NaN;
            mFrameCount = 0;
        }

        int frames = mAveraging;

        if(frames <= 1 && mFrameCount == 0)
        {
            return levels(bins);
        }

        if(mPowerSum.length != bins.length)
        {
            mPowerSum = new float[bins.length];
            mAverage = new float[bins.length];
            mFrameCount = 0;
        }

        if(mFrameCount == 0)
        {
            Arrays.fill(mPowerSum, 0.0f);
        }

        for(int x = 0; x < bins.length; x++)
        {
            mPowerSum[x] += (float)Math.exp(bins[x] * DB_TO_LN_POWER);
        }

        mFrameCount++;

        //The averaging can drop below the frames already summed - complete the row with what has been summed
        if(mFrameCount < frames)
        {
            return null;
        }

        float scale = 1.0f / mFrameCount;

        for(int x = 0; x < bins.length; x++)
        {
            mAverage[x] = (float)(10.0 * Math.log10(mPowerSum[x] * scale));
        }

        mFrameCount = 0;
        return levels(mAverage);
    }

    /**
     * Updates the floor reference and converts the bins to level codes.
     */
    private byte[] levels(float[] bins)
    {
        float percentile = percentile(bins);

        if(Float.isNaN(mFloor) || Float.isInfinite(mFloor) || Math.abs(percentile - mFloor) > FLOOR_JUMP_DB)
        {
            mFloor = percentile;
        }
        else
        {
            mFloor += (percentile - mFloor) * FLOOR_SMOOTHING;
        }

        byte[] row = new byte[bins.length];
        float floor = mFloor;

        for(int x = 0; x < bins.length; x++)
        {
            int level = Math.round((bins[x] - floor) / LEVEL_STEP_DB) + FLOOR_LEVEL;
            row[x] = (byte)Math.max(0, Math.min(255, level));
        }

        return row;
    }

    /**
     * Palette index for a level code.
     * @param level code (0 - 255)
     * @param range in dB from the noise floor reference to the top of the palette.  Smaller ranges give weak signals
     * more contrast against the noise and saturate strong signals sooner.
     * @return palette index (0 - 255)
     */
    public static int paletteIndex(int level, double range)
    {
        double db = (level - FLOOR_LEVEL) * LEVEL_STEP_DB;
        long index = Math.round(db * (WaterfallPalette.SIZE - 1) / range + FLOOR_INDEX);
        return (int)Math.max(0, Math.min(WaterfallPalette.SIZE - 1, index));
    }

    /**
     * Floor reference percentile of the bins, using quickselect on a scratch copy.
     */
    private float percentile(float[] bins)
    {
        if(mScratch.length != bins.length)
        {
            mScratch = new float[bins.length];
        }

        System.arraycopy(bins, 0, mScratch, 0, bins.length);
        return select(mScratch, (int)(FLOOR_PERCENTILE * (bins.length - 1)));
    }

    /**
     * Quickselect: the k-th smallest value, partially reordering the array.
     */
    static float select(float[] values, int k)
    {
        int left = 0;
        int right = values.length - 1;

        while(left < right)
        {
            //Median of three pivot
            int middle = (left + right) >>> 1;

            if(values[middle] < values[left])
            {
                swap(values, middle, left);
            }

            if(values[right] < values[left])
            {
                swap(values, right, left);
            }

            if(values[right] < values[middle])
            {
                swap(values, right, middle);
            }

            float pivot = values[middle];
            int i = left;
            int j = right;

            while(i <= j)
            {
                while(values[i] < pivot)
                {
                    i++;
                }

                while(values[j] > pivot)
                {
                    j--;
                }

                if(i <= j)
                {
                    swap(values, i, j);
                    i++;
                    j--;
                }
            }

            if(k <= j)
            {
                right = j;
            }
            else if(k >= i)
            {
                left = i;
            }
            else
            {
                return values[k];
            }
        }

        return values[k];
    }

    private static void swap(float[] values, int a, int b)
    {
        float temp = values[a];
        values[a] = values[b];
        values[b] = temp;
    }
}
