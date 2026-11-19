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

package io.github.dsheirer.dsp.filter.equalizer;

import io.github.dsheirer.dsp.filter.IIRBiQuadraticFilter;

/**
 * 10-band graphic equalizer using cascaded IIR biquad peak filters, for decoded voice audio at 8 kHz.
 *
 * Center frequencies are spaced about 0.47 octave apart from 200 Hz to 3.5 kHz:
 *   200, 280, 390, 550, 770, 1100, 1500, 2100, 2900 and 3500 Hz
 *
 * Each band has an adjustable gain from -12 dB to +12 dB.
 * Q (bandwidth) is fixed at 3.0, the usual value for this band spacing.  With Q 2.0, neighboring
 * bands overlap enough that, for example, +5 to +7 dB on the 390 Hz - 3.5 kHz bands added up to +11.2 dB at 1.5 kHz;
 * with Q 3.0 the same settings peak at +9.2 dB.
 */
public class GraphicEqualizer
{
    public static final int BAND_COUNT = 10;
    public static final double DEFAULT_Q = 3.0;
    public static final double MIN_GAIN_DB = -12.0;
    public static final double MAX_GAIN_DB = 12.0;

    public static final double[] DEFAULT_CENTER_FREQUENCIES = {
        200.0, 280.0, 390.0, 550.0, 770.0, 1100.0, 1500.0, 2100.0, 2900.0, 3500.0
    };
    public static final String[] BAND_LABELS = {
        "200 Hz", "280 Hz", "390 Hz", "550 Hz", "770 Hz",
        "1.1 kHz", "1.5 kHz", "2.1 kHz", "2.9 kHz", "3.5 kHz"
    };

    private final IIRBiQuadraticFilter[] mBands;
    private final double[] mGainDb;
    private final double mSampleRate;
    private boolean mEnabled;

    /**
     * Creates a 10-band graphic equalizer.
     *
     * @param sampleRate audio sample rate (typically 8000 Hz for decoded P25 audio)
     */
    public GraphicEqualizer(double sampleRate)
    {
        mSampleRate = sampleRate;
        mEnabled = false;
        mGainDb = new double[BAND_COUNT];
        mBands = new IIRBiQuadraticFilter[BAND_COUNT];

        for(int i = 0; i < BAND_COUNT; i++)
        {
            mGainDb[i] = 0.0;
            mBands[i] = new IIRBiQuadraticFilter(IIRBiQuadraticFilter.Type.PEAK,
                DEFAULT_CENTER_FREQUENCIES[i], sampleRate, DEFAULT_Q, 0.0);
        }
    }

    /**
     * Indicates if the equalizer is enabled.
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Sets the enabled state of the equalizer.
     */
    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    /**
     * Gets the gain in dB for the specified band.
     *
     * @param band index (0-9)
     * @return gain in dB
     */
    public double getBandGain(int band)
    {
        if(band >= 0 && band < BAND_COUNT)
        {
            return mGainDb[band];
        }
        return 0.0;
    }

    /**
     * Sets the gain for a specific band.
     *
     * @param band index (0-9)
     * @param gainDb gain in dB, clamped to [-12, +12]
     */
    public void setBandGain(int band, double gainDb)
    {
        if(band >= 0 && band < BAND_COUNT)
        {
            gainDb = Math.max(MIN_GAIN_DB, Math.min(MAX_GAIN_DB, gainDb));
            mGainDb[band] = gainDb;

            // Reconfigure the biquad filter with new gain
            mBands[band].configure(IIRBiQuadraticFilter.Type.PEAK,
                DEFAULT_CENTER_FREQUENCIES[band], mSampleRate, DEFAULT_Q, gainDb);
        }
    }

    /**
     * Sets all band gains at once.
     *
     * @param gains array of 10 gain values in dB
     */
    public void setBandGains(double[] gains)
    {
        if(gains != null)
        {
            for(int i = 0; i < Math.min(gains.length, BAND_COUNT); i++)
            {
                setBandGain(i, gains[i]);
            }
        }
    }

    /**
     * Gets all band gains.
     *
     * @return array of 10 gain values in dB
     */
    public double[] getBandGains()
    {
        return mGainDb.clone();
    }

    /**
     * Resets all band gains to 0 dB (flat response).
     */
    public void reset()
    {
        for(int i = 0; i < BAND_COUNT; i++)
        {
            setBandGain(i, 0.0);
            mBands[i].reset();
        }
    }

    /**
     * Processes a single audio sample through all 10 EQ bands in series.
     *
     * @param sample input audio sample
     * @return filtered audio sample
     */
    public float process(float sample)
    {
        if(!mEnabled)
        {
            return sample;
        }

        double result = sample;

        for(int i = 0; i < BAND_COUNT; i++)
        {
            // Skip bands with 0 dB gain (no processing needed)
            if(mGainDb[i] != 0.0)
            {
                result = mBands[i].filter(result);
            }
        }

        return (float) result;
    }

    /**
     * Processes an array of audio samples through the equalizer.
     *
     * @param samples input audio buffer
     * @return processed audio buffer (same array, modified in place)
     */
    public float[] process(float[] samples)
    {
        if(!mEnabled || samples == null)
        {
            return samples;
        }

        for(int s = 0; s < samples.length; s++)
        {
            double result = samples[s];

            for(int i = 0; i < BAND_COUNT; i++)
            {
                if(mGainDb[i] != 0.0)
                {
                    result = mBands[i].filter(result);
                }
            }

            samples[s] = (float) result;
        }

        return samples;
    }
}