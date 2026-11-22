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

/**
 * Spectrum trace averaging and peak hold.  Thread safe.
 *
 * Averaging is an exponential moving average of each bin's linear power with separate rates for rising and falling
 * values.  A falling bin moves 1 / averaging of the way to the new value each frame, and a rising bin moves
 * ATTACK_RATIO times faster, so signals appear quickly while the noise floor stays steady.  Averaging linear power
 * instead of decibels keeps the result at the true mean power.  Averaging decibel values reads about 2.5 dB low for
 * noise.
 *
 * Peak hold keeps the highest recent value of each bin from the unaveraged frames, falling PEAK_DECAY_DB per frame.
 */
public class SpectrumAverager
{
    public static final int AVERAGING_MINIMUM = 1;
    public static final int AVERAGING_MAXIMUM = 20;
    public static final int AVERAGING_DEFAULT = 4;
    public static final double ATTACK_RATIO = 2.5;
    public static final float PEAK_DECAY_DB = 0.15f;
    private static final float MINIMUM_DB = -196.0f;
    private static final double LN10_OVER_10 = Math.log(10.0) / 10.0;

    private int mAveraging = AVERAGING_DEFAULT;
    private boolean mPeakHold = false;
    private boolean mResetPeak = false;
    //Double precision so the average can fall more than 70 dB in one frame without float rounding error
    private double[] mAveragedPower;
    private float[] mPeakBins;

    /**
     * Processed spectrum for one DFT frame.
     * @param average bins in dB after averaging
     * @param peak hold bins in dB, or null when peak hold is off
     */
    public record Frame(float[] average, float[] peak)
    {
    }

    /**
     * Sets the averaging, from AVERAGING_MINIMUM (no averaging) to AVERAGING_MAXIMUM (slowest).
     */
    public synchronized void setAveraging(int averaging)
    {
        mAveraging = Math.max(AVERAGING_MINIMUM, Math.min(AVERAGING_MAXIMUM, averaging));
    }

    /**
     * Turns peak hold on or off.  Turning it on starts from the next frame.
     */
    public synchronized void setPeakHold(boolean peakHold)
    {
        mPeakHold = peakHold;
        mResetPeak = true;
    }

    /**
     * Restarts the peak hold from the next frame, for example after a frequency change.
     */
    public synchronized void resetPeak()
    {
        mResetPeak = true;
    }

    /**
     * Clears the averaging history and the peak hold.
     */
    public synchronized void reset()
    {
        mAveragedPower = null;
        mPeakBins = null;
    }

    /**
     * Processes one frame of DFT bins in dB.
     */
    public synchronized Frame process(float[] bins)
    {
        int size = bins.length;
        double decay = 1.0 / mAveraging;
        double attack = Math.min(1.0, ATTACK_RATIO * decay);

        if(mAveragedPower == null || mAveragedPower.length != size)
        {
            mAveragedPower = new double[size];

            for(int x = 0; x < size; x++)
            {
                mAveragedPower[x] = toPowerDouble(bins[x]);
            }
        }
        else
        {
            for(int x = 0; x < size; x++)
            {
                double power = toPowerDouble(bins[x]);
                double average = mAveragedPower[x];
                mAveragedPower[x] = average + ((power - average) * (power > average ? attack : decay));
            }
        }

        float[] averaged = new float[size];

        for(int x = 0; x < size; x++)
        {
            averaged[x] = mAveragedPower[x] > 0 ?
                    Math.max(MINIMUM_DB, (float)(10.0 * Math.log10(mAveragedPower[x]))) : MINIMUM_DB;
        }

        float[] peak = null;

        if(mPeakHold)
        {
            if(mPeakBins == null || mPeakBins.length != size || mResetPeak)
            {
                mResetPeak = false;
                mPeakBins = bins.clone();
            }
            else
            {
                for(int x = 0; x < size; x++)
                {
                    mPeakBins[x] = Math.max(bins[x], mPeakBins[x] - PEAK_DECAY_DB);
                }
            }

            peak = mPeakBins.clone();
        }
        else
        {
            mPeakBins = null;
        }

        return new Frame(averaged, peak);
    }

    /**
     * Converts decibels to linear power.
     */
    public static float toPower(float decibels)
    {
        return (float)Math.exp(decibels * LN10_OVER_10);
    }

    private static double toPowerDouble(float decibels)
    {
        return Math.exp(decibels * LN10_OVER_10);
    }

    /**
     * Converts linear power to decibels.
     */
    public static float toDecibels(float power)
    {
        return power > 0 ? Math.max(MINIMUM_DB, (float)(10.0 * Math.log10(power))) : MINIMUM_DB;
    }
}
