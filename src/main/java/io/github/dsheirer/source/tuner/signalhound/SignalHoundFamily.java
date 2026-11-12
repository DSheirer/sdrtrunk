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

package io.github.dsheirer.source.tuner.signalhound;

import io.github.dsheirer.signalhound.SignalHoundApi;
import java.util.Locale;

/**
 * Signal Hound device families, one for each Signal Hound API, with the limits and front-end controls of each family
 * from the SDK headers (bb_api.h, sm_api.h and sp_api.h).
 *
 * The family names are used in tuner IDs, which persist the tuner configurations, so they must not change.
 */
public enum SignalHoundFamily
{
    /**
     * BB60A, BB60C and BB60D.  Gain (0-3) and attenuation (0-30 dB in 10 dB steps) are automatic unless both are set.
     */
    BB60(SignalHoundApi.BB, "BB60", 40_000_000, 9_000L, 6_400_000_000L, 3, 10, true, false),

    /**
     * SM200A/B/C and SM435B/C.  Attenuation 0-30 dB in 5 dB steps, and an RF preselector up to 645 MHz.
     */
    SM(SignalHoundApi.SM, "SM", 50_000_000, 100_000L, 20_600_000_000L, 6, 5, false, true),

    /**
     * SP145A and SP145B.  Attenuation 0-30 dB in 5 dB steps, and an RF preselector on the SP145B up to 520 MHz.
     */
    SP145(SignalHoundApi.SP, "SP145", 61_440_000, 9_000L, 15_000_000_000L, 6, 5, false, true);

    //SM435 models tune higher than the SM200
    private static final long SM435_MAXIMUM_FREQUENCY = 44_200_000_000L;

    private final SignalHoundApi mApi;
    private final String mLabel;
    private final int mBaseSampleRate;
    private final long mMinimumFrequency;
    private final long mMaximumFrequency;
    private final int mMaximumAttenuation;
    private final int mAttenuationStepDb;
    private final boolean mHasGain;
    private final boolean mHasPreselector;

    SignalHoundFamily(SignalHoundApi api, String label, int baseSampleRate, long minimumFrequency,
                      long maximumFrequency, int maximumAttenuation, int attenuationStepDb, boolean hasGain,
                      boolean hasPreselector)
    {
        mApi = api;
        mLabel = label;
        mBaseSampleRate = baseSampleRate;
        mMinimumFrequency = minimumFrequency;
        mMaximumFrequency = maximumFrequency;
        mMaximumAttenuation = maximumAttenuation;
        mAttenuationStepDb = attenuationStepDb;
        mHasGain = hasGain;
        mHasPreselector = hasPreselector;
    }

    /**
     * Family of the devices that an API drives.
     */
    public static SignalHoundFamily of(SignalHoundApi api)
    {
        return switch(api)
        {
            case BB -> BB60;
            case SM -> SM;
            case SP -> SP145;
        };
    }

    /**
     * API that drives the family.
     */
    public SignalHoundApi getApi()
    {
        return mApi;
    }

    /**
     * I/Q sample rate before decimation, in Hertz.
     */
    public int getBaseSampleRate()
    {
        return mBaseSampleRate;
    }

    /**
     * Lowest tunable frequency, in Hertz.
     */
    public long getMinimumFrequency()
    {
        return mMinimumFrequency;
    }

    /**
     * Highest tunable frequency of the family's default model, in Hertz.
     */
    public long getMaximumFrequency()
    {
        return mMaximumFrequency;
    }

    /**
     * Highest tunable frequency of a model, in Hertz.
     * @param model as reported by the device, such as SM435B
     */
    public long getMaximumFrequency(String model)
    {
        if(this == SM && model != null && model.toUpperCase(Locale.ROOT).startsWith("SM435"))
        {
            return SM435_MAXIMUM_FREQUENCY;
        }

        return mMaximumFrequency;
    }

    /**
     * Highest attenuator setting, where each setting is {@link #getAttenuationStepDb()} dB.
     */
    public int getMaximumAttenuation()
    {
        return mMaximumAttenuation;
    }

    /**
     * Attenuation in dB of each attenuator setting.
     */
    public int getAttenuationStepDb()
    {
        return mAttenuationStepDb;
    }

    /**
     * Indicates if the family has a manual gain setting, in addition to the attenuator.
     */
    public boolean hasGain()
    {
        return mHasGain;
    }

    /**
     * Highest gain setting, for families that have one.
     */
    public int getMaximumGain()
    {
        return mHasGain ? 3 : 0;
    }

    /**
     * Indicates if devices of the family can have an RF preselector.  For the SP145, only the SP145B has one.
     */
    public boolean hasPreselector()
    {
        return mHasPreselector;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
