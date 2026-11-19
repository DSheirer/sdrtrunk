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

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Measures a channel's power within the channel bandwidth from dBFS spectrum frames and compares it to the tuner noise
 * floor to produce the channel's signal level and signal-to-noise ratio.
 *
 * Uses the spectrum frames that a channel spectral display already calculates, so the measurement costs only a sum
 * over the in-band bins of each frame.  The frames come from a FullScaleDecibelConverter, lowest frequency first, with
 * the channel centered in the frame.  Each bin's power is the window's noise bandwidth times the noise power per bin
 * width, so the in-band power is the sum of the in-band bin powers divided by the window noise bandwidth (in bins).
 * That is also correct for a tone, whose power spreads across the main lobe bins by the same factor.
 *
 * Frames are averaged and a reading is produced every 500 milliseconds.
 *
 * Receive frames on one thread.  The configuration can change from any thread.
 */
public class ChannelSignalMeter implements DFTResultsListener
{
    private static final long READING_INTERVAL_MS = 500;

    /**
     * Channel measurement configuration.
     * @param sampleRate of the channel samples
     * @param bandwidth of the channel in Hertz
     * @param noiseDensity supplier of the tuner noise density in full-scale units per Hertz, or NaN when unknown
     */
    private record Configuration(double sampleRate, int bandwidth, DoubleSupplier noiseDensity) {}

    /**
     * Channel signal reading.
     * @param dbfs channel power (signal plus noise) within the channel bandwidth, relative to full scale
     * @param snr signal-to-noise ratio in dB, or NaN when the noise floor is unknown.  Negative infinity when the
     * channel power doesn't exceed the noise floor.
     * @param noiseDbfs noise floor within the channel bandwidth, relative to full scale, or NaN when unknown
     */
    public record Reading(double dbfs, double snr, double noiseDbfs)
    {
        /**
         * Indicates if the signal-to-noise ratio is available
         */
        public boolean hasSnr()
        {
            return !Double.isNaN(snr);
        }
    }

    private final Consumer<Reading> mListener;
    private final long mReadingInterval;
    private volatile Configuration mConfiguration;
    private volatile double mWindowNoiseBandwidth = 1.0;
    private Configuration mAccumulatorConfiguration;
    private double mPowerAccumulator;
    private int mFrameCount;
    private double mMeasuredBandwidth;
    private long mReadingStart;

    /**
     * Constructs an instance
     * @param listener to receive readings on the frame thread
     */
    public ChannelSignalMeter(Consumer<Reading> listener)
    {
        this(listener, READING_INTERVAL_MS);
    }

    /**
     * Constructs an instance with a custom reading interval, for tests.
     * @param listener to receive readings on the frame thread
     * @param readingInterval in milliseconds, or 0 for a reading from each frame
     */
    ChannelSignalMeter(Consumer<Reading> listener, long readingInterval)
    {
        mListener = listener;
        mReadingInterval = readingInterval;
    }

    /**
     * Sets the channel to measure, or clears it.
     * @param sampleRate of the channel samples, or 0 to clear the channel
     * @param bandwidth of the channel in Hertz, or 0 to clear the channel
     * @param noiseDensity supplier of the tuner noise density in full-scale units per Hertz, or null when unavailable
     */
    public void setChannel(double sampleRate, int bandwidth, DoubleSupplier noiseDensity)
    {
        mConfiguration = (sampleRate > 0 && bandwidth > 0) ? new Configuration(sampleRate, bandwidth, noiseDensity) : null;
    }

    /**
     * Sets the noise bandwidth of the DFT window, in bins.
     */
    public void setWindowNoiseBandwidth(double noiseBandwidth)
    {
        mWindowNoiseBandwidth = noiseBandwidth > 0 ? noiseBandwidth : 1.0;
    }

    /**
     * Receives a dBFS spectrum frame for the channel.
     */
    @Override
    public void receive(float[] frame)
    {
        Configuration configuration = mConfiguration;

        if(configuration == null || frame == null || frame.length == 0)
        {
            return;
        }

        long now = System.currentTimeMillis();

        //Restart on a channel change
        if(configuration != mAccumulatorConfiguration)
        {
            mAccumulatorConfiguration = configuration;
            mPowerAccumulator = 0.0;
            mFrameCount = 0;
            mReadingStart = now;
        }

        int bins = frame.length;
        double binWidth = configuration.sampleRate() / bins;
        int halfWidth = Math.min(bins / 2 - 1, (int)Math.floor(configuration.bandwidth() / 2.0 / binWidth));
        int center = bins / 2;
        double power = 0.0;

        for(int bin = center - halfWidth; bin <= center + halfWidth; bin++)
        {
            power += Math.pow(10.0, frame[bin] / 10.0);
        }

        mPowerAccumulator += power / mWindowNoiseBandwidth;
        mMeasuredBandwidth = (2 * halfWidth + 1) * binWidth;
        mFrameCount++;

        if(now - mReadingStart >= mReadingInterval)
        {
            mListener.accept(getReading(mPowerAccumulator / mFrameCount, mMeasuredBandwidth,
                configuration.noiseDensity()));
            mPowerAccumulator = 0.0;
            mFrameCount = 0;
            mReadingStart = now;
        }
    }

    /**
     * Creates a reading.
     * @param power within the measured bandwidth in full-scale units
     * @param measuredBandwidth in Hertz
     * @param noiseDensity supplier, or null
     */
    static Reading getReading(double power, double measuredBandwidth, DoubleSupplier noiseDensity)
    {
        double dbfs = 10.0 * Math.log10(power);
        double density = noiseDensity != null ? noiseDensity.getAsDouble() : Double.NaN;

        if(Double.isFinite(density) && density > 0.0)
        {
            double noise = density * measuredBandwidth;
            double snr = power > noise ? 10.0 * Math.log10((power - noise) / noise) : Double.NEGATIVE_INFINITY;
            return new Reading(dbfs, snr, 10.0 * Math.log10(noise));
        }

        return new Reading(dbfs, Double.NaN, Double.NaN);
    }
}
