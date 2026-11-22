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
package io.github.dsheirer.dsp.squelch;

import org.jtransforms.fft.FloatFFT_1D;

/**
 * Detects voice modulation on the envelope of a steady AM carrier.  The noise on a carrier's envelope is spectrally flat
 * across the channel, while voice energy is concentrated in the lower part of the voice band.  The detector compares
 * the average spectral density of a low and a high band, with band edges limited to the channel pass band.
 *
 * A single detection isn't proof of a voice transmission and a pause in speech isn't proof of a dead carrier, so the
 * results should be evaluated across a longer period.
 */
public class VoiceModulationDetector
{
    private static final float MAXIMUM_FREQUENCY = 2300.0f;
    private static final float MAXIMUM_FREQUENCY_BANDWIDTH_RATIO = 0.38f;
    private static final float LOW_BAND_START = 300.0f;
    private static final float LOW_BAND_STOP_RATIO = 0.52f;
    private static final float HIGH_BAND_START_RATIO = 0.65f;
    private static final double DETECTION_THRESHOLD_DB = 5.0;

    private final double mBandwidth;
    private FloatFFT_1D mFFT;
    private float[] mEnvelope;
    private float[] mWindow;
    private int mLowStartBin;
    private int mLowStopBin;
    private int mHighStartBin;
    private int mHighStopBin;
    private int mPointer = 0;

    /**
     * Constructs an instance
     * @param sampleRate of the envelope samples in hertz
     * @param bandwidth of the channel in hertz, spanning positive and negative frequencies
     * @param analysisSamples maximum number of samples per analysis.  This is reduced to a power of 2.
     */
    public VoiceModulationDetector(double sampleRate, double bandwidth, int analysisSamples)
    {
        mBandwidth = bandwidth;
        configure(sampleRate, analysisSamples);
    }

    /**
     * Updates the sample rate and resets this detector.
     * @param sampleRate of the envelope samples in hertz
     * @param analysisSamples maximum number of samples per analysis.  This is reduced to a power of 2.
     */
    public final void configure(double sampleRate, int analysisSamples)
    {
        int length = Integer.highestOneBit(analysisSamples);
        mFFT = new FloatFFT_1D(length);
        mEnvelope = new float[length];
        mWindow = new float[length];

        for(int x = 0; x < length; x++)
        {
            mWindow[x] = (float)(0.5 - 0.5 * Math.cos(2.0 * Math.PI * x / length));
        }

        float stop = Math.min(MAXIMUM_FREQUENCY, (float)mBandwidth * MAXIMUM_FREQUENCY_BANDWIDTH_RATIO);
        double hertzPerBin = sampleRate / length;
        mLowStartBin = (int)Math.ceil(LOW_BAND_START / hertzPerBin);
        mLowStopBin = (int)(stop * LOW_BAND_STOP_RATIO / hertzPerBin);
        mHighStartBin = (int)Math.ceil(stop * HIGH_BAND_START_RATIO / hertzPerBin);
        mHighStopBin = (int)(stop / hertzPerBin);
        reset();
    }

    /**
     * Discards any partial analysis buffer.
     */
    public void reset()
    {
        mPointer = 0;
    }

    /**
     * Adds an envelope sample to the analysis buffer.  Samples are ignored once the buffer is full.
     * @param envelope sample
     */
    public void receive(float envelope)
    {
        if(mPointer < mEnvelope.length)
        {
            mEnvelope[mPointer++] = envelope;
        }
    }

    /**
     * Analyzes the buffered samples and resets this detector.
     * @return true if the analysis buffer is full and the samples contain voice modulation.
     */
    public boolean analyze()
    {
        boolean full = mPointer == mEnvelope.length;
        mPointer = 0;

        if(!full)
        {
            return false;
        }

        float mean = 0.0f;

        for(float sample: mEnvelope)
        {
            mean += sample;
        }

        mean /= mEnvelope.length;

        for(int x = 0; x < mEnvelope.length; x++)
        {
            mEnvelope[x] = (mEnvelope[x] - mean) * mWindow[x];
        }

        mFFT.realForward(mEnvelope);
        double low = getBandPower(mLowStartBin, mLowStopBin);
        double high = getBandPower(mHighStartBin, mHighStopBin);
        return high > 0.0 && 10.0 * Math.log10(low / high) >= DETECTION_THRESHOLD_DB;
    }

    /**
     * Calculates the mean power of the bins from the real transform output of interleaved real and imaginary values.
     */
    private double getBandPower(int startBin, int stopBin)
    {
        double power = 0.0;

        for(int bin = startBin; bin < stopBin; bin++)
        {
            double real = mEnvelope[2 * bin];
            double imaginary = mEnvelope[2 * bin + 1];
            power += real * real + imaginary * imaginary;
        }

        return power / Math.max(1, stopBin - startBin);
    }
}
