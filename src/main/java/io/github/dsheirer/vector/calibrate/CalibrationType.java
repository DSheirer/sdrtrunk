/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate;

/**
 * Calibrations used by the system
 *
 * Version numbers in each calibration can be incremented any time the calibration changes
 * so that the calibration will automatically run.
 */
public enum CalibrationType
{
    //There's an oddity with these two vector implementations where it takes ~30x longer when it runs
    //after any other calibrations ... so we always sort to top to run this one first
    OSCILLATOR_COMPLEX("Complex Oscillator", 1),
    GAIN_COMPLEX("Complex Gain", 1),

    AIRSPY_SAMPLE_CONVERTER("Airspy Sample Converter", 1),
    AIRSPY_UNPACKED_INTERLEAVED_ITERATOR("Airspy Unpacked Interleaved Iterator", 1),
    AIRSPY_UNPACKED_ITERATOR("Airspy Unpacked Iterator", 1),
    AM_DEMODULATOR("AM Demodulator", 1),
    DC_REMOVAL_REAL("Real DC Removal Filter", 1),
    DMR_SOFT_SYNC_DETECTOR("DMR Soft Sync Detector", 1),
    DIFFERENTIAL_DEMODULATOR("DQPSK Demodulator", 1),
    FILTER_FIR("FIR Filter", 1),
    FILTER_HALF_BAND_REAL_11_TAP("Real Half-Band Decimation Filter - 11 Tap", 1),
    FILTER_HALF_BAND_REAL_15_TAP("Real Half-Band Decimation Filter - 15 Tap", 1),
    FILTER_HALF_BAND_REAL_23_TAP("Real Half-Band Decimation Filter - 23 Tap", 1),
    FILTER_HALF_BAND_REAL_63_TAP("Real Half-Band Decimation Filter - 63 Tap", 1),
    FILTER_HALF_BAND_REAL_DEFAULT("Real Half-Band Decimation Filter - Default", 1),
    FM_DEMODULATOR("FM Demodulator", 2),
    GAIN_CONTROL_COMPLEX("Complex Gain Control", 1),
    HILBERT_TRANSFORM("Hilbert Transform", 1),
    INTERPOLATOR("Interpolator", 1),
    MAGNITUDE("Magnitude", 1),
    MIXER_COMPLEX("Complex Mixer", 1),
    OSCILLATOR_REAL("Real Oscillator", 1),
    SQUELCHING_FM_DEMODULATOR("Squelching FM Demodulator", 1),
    WINDOW("Window", 1);

    private String mDescription;
    private int mVersion;

    /**
     * Constructs an instance
     * @param description
     * @param version of the calibration.
     */
    CalibrationType(String description, int version)
    {
        mDescription = description;
        mVersion = version;
    }

    /**
     * Formulates a unique value for the entry for use as a key value.
     */
    public String getPreferenceKey()
    {
        return name() + " - V" + getVersion();
    }

    public String getDescription()
    {
        return mDescription;
    }

    /**
     * Version of the calibration
     */
    public int getVersion()
    {
        return mVersion;
    }

    @Override public String toString()
    {
        return getDescription();
    }
}
