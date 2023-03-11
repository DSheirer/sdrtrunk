/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.source.tuner.sdrplay.api.device.Decimate;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Bandwidth;
import java.util.EnumSet;

/**
 * RSP device Sample Rate, Bandwidth and Decimation enumeration
 * Note: final effective sample rate must be greater than IF bandwidth setting to avoid aliasing.  The available IF
 * bandwidth values effectively dictate the available sample rates
 */
public enum RspSampleRate
{
    RATE_0_250(8_000_000, 16_000, Bandwidth.BW_0_300, Decimate.X32, "0.250 MHz (0.234 usable)"),
    RATE_0_500(8_000_000, 24_000, Bandwidth.BW_0_600, Decimate.X16, "0.500 MHz (0.476 usable)"),
    RATE_1_000(8_000_000, 100_000, Bandwidth.BW_1_536, Decimate.X8, "1.000 MHz (0.900 usable)"),
    RATE_1_500(6_000_000, 140_000, Bandwidth.BW_1_536, Decimate.X4, "1.500 MHz (1.360 usable)"),
    RATE_2_048(8_192_000, 248_000, Bandwidth.BW_1_536, Decimate.X4, "2.048 MHz (1.800 usable)"),
    RATE_3_000(6_000_000, 300_000, Bandwidth.BW_5_000, Decimate.X2, "3.000 MHz (2.700 usable)"),
    RATE_4_000(8_000_000, 340_000, Bandwidth.BW_5_000, Decimate.X2, "4.000 MHz (3.560 usable)"),
    RATE_5_000(5_000_000, 880_000, Bandwidth.BW_5_000, Decimate.X1, "5.000 MHz (4.120 usable)"),
    RATE_6_000(6_000_000, 800_000, Bandwidth.BW_6_000, Decimate.X1, "6.000 MHz (5.200 usable)"),
    RATE_7_000(7_000_000, 1_040_000, Bandwidth.BW_7_000, Decimate.X1, "7.000 MHz (5.960 usable)"),
    RATE_8_000(8_000_000, 1_060_000, Bandwidth.BW_8_000, Decimate.X1, "8.000 MHz (6.940 usable)"),
    RATE_9_000(9_000_000, 1_620_000, Bandwidth.BW_8_000, Decimate.X1, "9.000 MHz (7.380 usable)"),
    RATE_10_000(10_000_000, 1_500_000, Bandwidth.BW_8_000, Decimate.X1, "10.000 MHz (8.500 usable)"),

    /**
     * Note: in dual tuner mode, available sample rates are 6 or 8 MHz with an effective output rate of
     * 2 MHz.  Decimation is applied against the final 2 MHz rate, not the original 6/8 sampling rate.
     */
    DUO_RATE_0_500(8_000_000, 50_000, Bandwidth.BW_8_000, Decimate.X4, "0.500 MHz (0.450 usable)"),
    DUO_RATE_1_000(8_000_000, 50_000, Bandwidth.BW_8_000, Decimate.X2, "1.000 MHz (0.950 usable)"),
    DUO_RATE_1_500(6_000_000, 0, Bandwidth.BW_6_000, Decimate.X1, "1.500 MHz (1.500 usable)"),
    DUO_RATE_2_000(8_000_000, 0, Bandwidth.BW_8_000, Decimate.X1, "2.000 MHz (2.000 usable)"),

    UNDEFINED(0, 0, Bandwidth.UNDEFINED, Decimate.X1, "UNDEFINED");

    private final long mSampleRate;
    private final long mUnusable;
    private final Bandwidth mBandwidth;
    private final Decimate mDecimation;
    private final String mDescription;

    /**
     * Constructs an instance
     * @param sampleRate in hertz
     * @param unusable bandwidth (total) in Hertz due to IF or decimation filter roll-off at spectrum edges
     * @param bandwidth of the frequency spectrum
     * @param decimation to be applied to the original sample rate to achieve the effective sample rate
     * @param description for UI display
     */
    RspSampleRate(long sampleRate, long unusable, Bandwidth bandwidth, Decimate decimation, String description)
    {
        mSampleRate = sampleRate;
        mUnusable = unusable;
        mBandwidth = bandwidth;
        mDecimation = decimation;
        mDescription = description;
    }

    /**
     * Single tuner sample rates for all devices operating in single tuner mode
     */
    public static final EnumSet<RspSampleRate> SINGLE_TUNER_SAMPLE_RATES = EnumSet.range(RATE_0_250, RATE_10_000);

    /**
     * RSPduo dual-tuner mode sample rates
     */
    public static final EnumSet<RspSampleRate> DUAL_TUNER_SAMPLE_RATES = EnumSet.range(DUO_RATE_0_500, DUO_RATE_2_000);

    /**
     * Sample Rate
     * @return sample rate (Hz)
     */
    public long getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Usable bandwidth in range 0.0 to 1.0, where 1.0 is 100% usable bandwidth
     */
    public double getUsableBandwidth()
    {
        if(getSampleRate() != 0)
        {
            return 1.0 - ((double)mUnusable / (double)getEffectiveSampleRate());
        }

        return 0.0;
    }

    /**
     * Indicates if this is a sample rate supported by the RSPduo operating in dual-tuner mode.
     * @return true if this is a dual-tuner sample rate.
     */
    public boolean isDualTunerSampleRate()
    {
        return DUAL_TUNER_SAMPLE_RATES.contains(this);
    }

    /**
     * Sample size in bits.  Can be used to adjust spectral display for effective dynamic range.
     * @return sample size in bits.
     */
    public int getSampleSize()
    {
        return mDecimation.getSampleSize();
    }

    /**
     * Decimation rate to be applied against sample rate.
     * @return decimation value
     */
    public Decimate getDecimation()
    {
        return mDecimation;
    }

    /**
     * Effective Sample Rate (sample rate / decimation).  Note: for RSPduo operating in dual-tuner mode, an
     * inherent x4 decimation is applied in the hardware.
     * @return effective sample rate (Hz)
     */
    public long getEffectiveSampleRate()
    {
        if(isDualTunerSampleRate())
        {
            //Dual tuner sample rate is specified at 8 MHz, but there is an inherent x4 decimation to 2 MHz
            //on top of the user specified decimation rate.
            return getSampleRate() / getDecimation().getValue() / 4;
        }
        else
        {
            return getSampleRate() / getDecimation().getValue();
        }
    }

    /**
     * Bandwidth entry
     */
    public Bandwidth getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Overrides the default string representation to use the UI description/label.
     * @return string representation of the entry
     */
    @Override
    public String toString()
    {
        return mDescription;
    }
}
