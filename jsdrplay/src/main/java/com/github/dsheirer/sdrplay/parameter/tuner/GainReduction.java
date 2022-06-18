/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package com.github.dsheirer.sdrplay.parameter.tuner;

import com.github.dsheirer.sdrplay.device.DeviceType;
import java.util.EnumSet;

/**
 * RSP Gain Reduction values and LNA states for each RSP model.
 *
 * Use lookup() as convenience method to find a gain reduction entry for a device type and frequency band.
 *
 * Reference: SDRplay API Specification, Gain Reduction Tables
 *  and https://github.com/SDRplay/RSPTCPServer/blob/master/rsp_tcp.c
 */
public enum GainReduction
{
    RSP1_BAND_0_12(FrequencyBand.BAND_0_12,
            new int[]{  3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,53,50,47,44,41,58,55,52,49,46,43,45,42,58,55,52,49,46,43,41,38,35,32,29,26,23,20 }),
    RSP1_BAND_12_60(FrequencyBand.BAND_12_60,
            new int[]{  3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,53,50,47,44,41,58,55,52,49,46,43,45,42,58,55,52,49,46,43,41,38,35,32,29,26,23,20 }),
    RSP1_BAND_60_250(FrequencyBand.BAND_60_250,
            new int[]{  3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,53,50,47,44,41,58,55,52,49,46,43,45,42,58,55,52,49,46,43,41,38,35,32,29,26,23,20 }),
    RSP1_BAND_250_420(FrequencyBand.BAND_250_420,
            new int[]{  3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,53,50,47,44,41,58,55,52,49,46,43,45,42,58,55,52,49,46,43,41,38,35,32,29,26,23,20 }),
    RSP1_BAND_420_1000(FrequencyBand.BAND_420_1000,
            new int[]{  3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 1, 1, 1, 1, 1, 0, 0, 0, 0 },
            new int[]{ 59,57,54,52,50,47,45,43,40,38,36,33,31,29,27,24,22,27,24,22,32,29,27,25,22,27,25,22,20 }),
    RSP1_BAND_1000_2000(FrequencyBand.BAND_1000_2000,
            new int[]{  3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,57,55,52,50,48,46,43,41,44,42,53,51,49,47,44,42,45,43,40,38,36,34,31,29,27,25,22,20 }),

    RSP1A_BAND_0_12(FrequencyBand.BAND_0_12,
            new int[]{  6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 4, 4, 3, 3, 3, 3, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,57,53,49,46,42,44,40,56,52,48,45,41,44,40,43,45,41,38,34,31,27,24,20 }),
    RSP1A_BAND_12_60(FrequencyBand.BAND_12_60,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 7, 7, 7, 7, 7, 6, 6, 5, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,42,58,54,51,47,43,46,42,44,41,43,42,44,40,43,45,42,38,34,31,27,24,20 }),
    RSP1A_BAND_60_250(FrequencyBand.BAND_60_250,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 7, 7, 7, 7, 7, 6, 6, 5, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,42,58,54,51,47,43,46,42,44,41,43,42,44,40,43,45,42,38,34,31,27,24,20 }),
    RSP1A_BAND_250_420(FrequencyBand.BAND_250_420,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 7, 7, 7, 7, 7, 6, 6, 5, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,42,58,54,51,47,43,46,42,44,41,43,42,44,40,43,45,42,38,34,31,27,24,20 }),
    RSP1A_BAND_420_1000(FrequencyBand.BAND_420_1000,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 8, 8, 8, 8, 7, 6, 6, 5, 5, 4, 4, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,44,41,56,52,49,45,41,44,46,42,45,41,44,40,44,40,42,46,42,38,35,31,27,24,20 }),
    RSP1A_BAND_1000_2000(FrequencyBand.BAND_1000_2000,
            new int[]{  8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 5, 5, 4, 4, 3, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,56,53,49,46,42,43,46,42,44,41,43,48,44,40,43,45,42,38,34,31,27,24,20 }),

    RSP2_BAND_0_12(FrequencyBand.BAND_0_12,
            new int[]{  8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 5, 5, 4, 4, 4, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,44,41,56,52,49,45,41,44,45,41,48,44,40,45,42,43,49,46,42,38,35,31,27,24,20 }),
    RSP2_BAND_12_60(FrequencyBand.BAND_12_60,
            new int[]{  8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 5, 5, 4, 4, 4, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,44,41,56,52,49,45,41,44,45,41,48,44,40,45,42,43,49,46,42,38,35,31,27,24,20 }),
    RSP2_BAND_60_250(FrequencyBand.BAND_60_250,
            new int[]{  8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 5, 5, 4, 4, 4, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,44,41,56,52,49,45,41,44,45,41,48,44,40,45,42,43,49,46,42,38,35,31,27,24,20 }),
    RSP2_BAND_250_420(FrequencyBand.BAND_250_420,
            new int[]{  8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 5, 5, 4, 4, 4, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,44,41,56,52,49,45,41,44,45,41,48,44,40,45,42,43,49,46,42,38,35,31,27,24,20 }),
    RSP2_BAND_420_1000(FrequencyBand.BAND_420_1000,
            new int[]{  5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 3, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,53,50,48,45,42,58,55,52,49,47,44,41,43,40,44,41,42,46,43,40,37,34,31,29,26,23,20 }),
    RSP2_BAND_1000_2000(FrequencyBand.BAND_1000_2000,
            new int[]{  4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,54,51,48,45,43,40,56,54,51,48,45,43,40,43,41,44,41,44,42,39,36,34,31,28,25,23,20 }),

    RSP_DUO_BAND_0_12(FrequencyBand.BAND_0_12,
            new int[]{  6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 4, 4, 3, 3, 3, 3, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,57,53,49,46,42,44,40,56,52,48,45,41,44,40,43,45,41,38,34,31,27,24,20 }),
    RSP_DUO_BAND_12_60(FrequencyBand.BAND_12_60,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 7, 7, 7, 7, 7, 6, 6, 5, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,42,58,54,51,47,43,46,42,44,41,43,42,44,40,43,45,42,38,34,31,27,24,20 }),
    RSP_DUO_BAND_60_250(FrequencyBand.BAND_60_250,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 7, 7, 7, 7, 7, 6, 6, 5, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,42,58,54,51,47,43,46,42,44,41,43,42,44,40,43,45,42,38,34,31,27,24,20 }),
    RSP_DUO_BAND_250_420(FrequencyBand.BAND_250_420,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 7, 7, 7, 7, 7, 6, 6, 5, 5, 4, 3, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,42,58,54,51,47,43,46,42,44,41,43,42,44,40,43,45,42,38,34,31,27,24,20 }),
    RSP_DUO_BAND_420_1000(FrequencyBand.BAND_420_1000,
            new int[]{  9, 9, 9, 9, 9, 9, 8, 8, 8, 8, 8, 7, 6, 6, 5, 5, 4, 4, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,44,41,56,52,49,45,41,44,46,42,45,41,44,40,44,40,42,46,42,38,35,31,27,24,20 }),
    RSP_DUO_BAND_1000_2000(FrequencyBand.BAND_1000_2000,
            new int[]{  8, 8, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 5, 5, 4, 4, 3, 2, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,56,53,49,46,42,43,46,42,44,41,43,48,44,40,43,45,42,38,34,31,27,24,20 }),

    RSP_DX_BAND_0_12(FrequencyBand.BAND_0_12,
            new int[]{ 18,18,18,18,18,18,17,16,14,13,12,11,10, 9, 7, 6, 5, 5, 5, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,41,40,43,42,42,41,41,40,42,42,47,44,40,43,42,42,41,38,34,31,27,24,20 }),
    RSP_DX_BAND_12_60(FrequencyBand.BAND_12_60,
            new int[]{ 26,26,26,26,26,25,23,22,20,19,17,16,14,13,11,10, 8, 7, 5, 5, 5, 3, 2, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,50,46,41,40,42,40,42,40,42,41,42,41,43,41,43,41,49,45,40,42,40,42,38,33,29,24,20 }),
    RSP_DX_BAND_60_250(FrequencyBand.BAND_60_250,
            new int[]{ 26,26,26,26,26,25,23,22,20,19,17,16,14,13,11,10, 8, 7, 5, 5, 5, 3, 2, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,50,46,41,40,42,40,42,40,42,41,42,41,43,41,43,41,49,45,40,42,40,42,38,33,29,24,20 }),
    RSP_DX_BAND_250_420(FrequencyBand.BAND_250_420,
            new int[]{ 27,27,27,27,27,26,24,23,21,20,18,17,15,14,12,11, 9, 8, 6, 6, 5, 3, 2, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,50,46,41,40,42,40,42,40,42,41,42,41,43,41,43,41,46,42,40,42,40,42,38,33,29,24,20 }),
    RSP_DX_BAND_420_1000(FrequencyBand.BAND_420_1000,
            new int[]{ 20,20,20,20,20,20,18,17,16,14,13,12,11, 9, 8, 7, 7, 5, 4, 3, 2, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,51,48,44,40,42,42,41,43,42,41,41,43,42,44,40,43,42,41,40,46,43,39,35,31,28,24,20 }),
    RSP_DX_BAND_1000_2000(FrequencyBand.BAND_1000_2000,
            new int[]{ 18,18,18,18,18,18,16,15,14,13,11,10, 9, 8, 7, 6, 6, 6, 5, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,44,40,43,42,41,41,43,42,41,41,40,48,45,41,40,42,42,41,42,39,35,31,27,24,20 }),

    //HiZ Port - RSP2 and RSPduo
    RSP_2_AND_DUO_HIZ_BAND_0_12(FrequencyBand.BAND_0_12,
            new int[]{  4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,54,51,48,45,43,40,56,54,51,48,45,43,40,43,41,44,41,44,42,39,36,34,31,28,25,23,20 }),
    RSP_2_AND_DUO_HIZ_BAND_12_60(FrequencyBand.BAND_12_60,
            new int[]{  4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,56,54,51,48,45,43,40,56,54,51,48,45,43,40,43,41,44,41,44,42,39,36,34,31,28,25,23,20 }),

    //RSPdx HDR Mode
    RSP_DX_HDR_BAND_0_2(FrequencyBand.BAND_0_12,
            new int[]{ 18,18,18,18,18,18,17,16,14,13,12,11,10, 9, 7, 6, 5, 5, 5, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0 },
            new int[]{ 59,55,52,48,45,41,41,40,43,42,42,41,41,40,42,42,47,44,40,43,42,42,41,38,34,31,27,24,20 }),

    UNKNOWN(FrequencyBand.UNKNOWN, new int[]{}, new int[]{});

    private FrequencyBand mFrequencyBand;
    private int[] mLnaStates;
    private int[] mGainReductions;

    GainReduction(FrequencyBand frequencyBand, int[] lnaStates, int[] gainReductions)
    {
        mFrequencyBand = frequencyBand;
        mLnaStates = lnaStates;
        mGainReductions = gainReductions;
    }

    /**
     * Identifies the index of the gain reduction value from the array of possible gain reduction values.
     * @param gainReductionValue to lookup index
     * @return matching index or -1 if a matching index is not found.
     */
    public int getGainIndex(int gainReductionValue)
    {
        for(int x = 0; x < mGainReductions.length; x++)
        {
            if(mGainReductions[x] == gainReductionValue)
            {
                return x;
            }
        }

        return -1;
    }

    /**
     * Minimum index for gain values
     */
    public static int MIN_GAIN_INDEX = 0;

    /**
     * Maximum index for gain values
     */
    public static int MAX_GAIN_INDEX = 28;

    /**
     * Number of discrete gain steps available for each entry
     */
    public static int GAIN_STEPS_AVAILABLE = 29;

    /**
     * RSP1 Gain Reduction Tables
     */
    public static EnumSet<GainReduction> RSP_1_GAINS = EnumSet.range(RSP1_BAND_0_12, RSP1_BAND_1000_2000);

    /**
     * RSP1A Gain Reduction Tables
     */
    public static EnumSet<GainReduction> RSP_1A_GAINS = EnumSet.range(RSP1A_BAND_0_12, RSP1A_BAND_1000_2000);

    /**
     * RSP2 Gain Reduction Tables
     */
    public static EnumSet<GainReduction> RSP_2_GAINS = EnumSet.range(RSP2_BAND_0_12, RSP2_BAND_1000_2000);

    /**
     * RSPduo Gain Reduction Tables
     */
    public static EnumSet<GainReduction> RSP_DUO_GAINS = EnumSet.range(RSP_DUO_BAND_0_12, RSP_DUO_BAND_1000_2000);

    /**
     * RSPdx Gain Reduction Tables
     */
    public static EnumSet<GainReduction> RSP_DX_GAINS = EnumSet.range(RSP_DX_BAND_0_12, RSP_DX_BAND_1000_2000);

    /**
     * RSPdx HDR Mode Gain Reduction Tables
     */
    public static EnumSet<GainReduction> RSP_DX_HDR_GAINS = EnumSet.of(RSP_DX_HDR_BAND_0_2);

    /**
     * RSP2 and RSPduo HiZ Port Gain Reduction Tables
     */
    public static EnumSet<GainReduction> RSP_HIZ_GAINS = EnumSet.range(RSP_2_AND_DUO_HIZ_BAND_0_12, RSP_2_AND_DUO_HIZ_BAND_12_60);

    /**
     * Lookup the gain reduction entry for a device that matches the frequency band.
     *
     * Note: directly use the RSP_DX_HDR_GAINS and RSP_HIZ_GAINS enumeration sets to match for HDR or HIZ modes.
     *
     * @param deviceType to lookup
     * @param frequencyBand to match
     * @return entry or UNKNOWN
     */
    public static GainReduction lookup(DeviceType deviceType, FrequencyBand frequencyBand)
    {
        switch(deviceType)
        {
            case RSP1 -> {
                return fromBand(frequencyBand, RSP_1_GAINS);
            }
            case RSP1A -> {
                return fromBand(frequencyBand, RSP_1A_GAINS);
            }
            case RSP2 -> {
                return fromBand(frequencyBand, RSP_2_GAINS);
            }
            case RSPduo -> {
                return fromBand(frequencyBand, RSP_DUO_GAINS);
            }
            case RSPdx -> {
                return fromBand(frequencyBand, RSP_DX_GAINS);
            }
        }

        return UNKNOWN;
    }

    /**
     * Lookup the gain reduction entry for a device that matches the specified frequency
     * @param deviceType to lookup
     * @param frequency to map to a frequency band
     * @return gain reduction entry or UNKNOWN
     */
    public static GainReduction lookup(DeviceType deviceType, long frequency)
    {
        FrequencyBand band = FrequencyBand.fromValue(frequency);
        return lookup(deviceType, band);
    }

    /**
     * Look up the gain reduction value that matches the frequency band from the set of gain reduction values.
     * @param frequencyBand to lookup
     * @param values to choose from
     * @return matching entry or UNKNOWN
     */
    private static GainReduction fromBand(FrequencyBand frequencyBand, EnumSet<GainReduction> values)
    {
        for(GainReduction gainReduction: values)
        {
            if(gainReduction.getFrequencyBand() == frequencyBand)
            {
                return gainReduction;
            }
        }

        return UNKNOWN;
    }

    /**
     * Frequency band for the range of LNA state and gain reduction values.
     */
    public FrequencyBand getFrequencyBand()
    {
        return mFrequencyBand;
    }

    /**
     * Array of LNA states
     */
    private int[] getLnaStates()
    {
        return mLnaStates;
    }

    /**
     * Array of gain reduction values
     */
    private int[] getGainReductions()
    {
        return mGainReductions;
    }

    /**
     * LNA state value for the specified gain index
     * @param index for the gain (0 - 29)
     * @return lna state
     */
    public int getLnaState(int index)
    {
        if(MIN_GAIN_INDEX <= index && index <= MAX_GAIN_INDEX)
        {
            return getLnaStates()[index];
        }

        throw new IllegalArgumentException("Unrecognized gain index [" + index + "] - valid range: " +
                MIN_GAIN_INDEX + " <> " + MAX_GAIN_INDEX);
    }

    /**
     * Gain reduction value for the specified gain index
     * @param index for the gain (0 - 29)
     * @return gain reduction value (dB)
     */
    public int getGainReduction(int index)
    {
        if(MIN_GAIN_INDEX <= index && index <= MAX_GAIN_INDEX)
        {
            return getGainReductions()[index];
        }

        throw new IllegalArgumentException("Unrecognized gain index [" + index + "] - valid range: " +
                MIN_GAIN_INDEX + " <> " + MAX_GAIN_INDEX);
    }

    /**
     * Indicates if this gain reduction entry is valid for the specified frequency by checking the frequency band
     * of this entry and testing for containment.
     * @param frequency to check
     * @return true if the frequency is contained in the frequency band for this entry.
     */
    public boolean isValidFor(long frequency)
    {
        return getFrequencyBand().contains(frequency);
    }
}
