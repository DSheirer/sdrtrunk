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
package io.github.dsheirer.source.tuner.rtl.r8x;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;

/**
 * RTL-2832 with embedded R820T or R828D tuner configuration
 */
public abstract class R8xTunerConfiguration extends RTL2832TunerConfiguration
{
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 3_180_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 1_782_030_000;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.98;
    public static final int DC_SPIKE_AVOID_BUFFER = 5000;
    /**
     * Default gain in tenths of a dB
     */
    public static final int DEFAULT_GAIN = 328;

    //Legacy gain settings, used to convert configurations from the previous R8x driver's master, LNA and mixer gains
    private boolean mLegacyManualGain = false;
    private Integer mLegacyLNAGain;
    private Integer mLegacyMixerGain;

    /**
     * Default constructor for Jackson
     */
    public R8xTunerConfiguration()
    {
        super(MINIMUM_TUNABLE_FREQUENCY_HZ, MAXIMUM_TUNABLE_FREQUENCY_HZ, DEFAULT_GAIN);
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public R8xTunerConfiguration(String uniqueID)
    {
        super(uniqueID, DEFAULT_GAIN);
    }

    /**
     * Converts the legacy master gain setting: AUTOMATIC, MANUAL (using the LNA and mixer gains) or a gain value
     * such as GAIN_327 in tenths of a dB.
     */
    @JsonSetter("masterGain")
    public void setLegacyMasterGain(String masterGain)
    {
        if("AUTOMATIC".equals(masterGain))
        {
            setAGC(true);
        }
        else if("MANUAL".equals(masterGain))
        {
            mLegacyManualGain = true;
            updateLegacyManualGain();
        }
        else
        {
            Integer gain = parseLegacyGain(masterGain);

            if(gain != null)
            {
                setGain(gain);
            }
        }
    }

    /**
     * Converts the legacy LNA gain setting, such as GAIN_222 in tenths of a dB.
     */
    @JsonSetter("lnagain")
    public void setLegacyLNAGain(String lnaGain)
    {
        mLegacyLNAGain = parseLegacyGain(lnaGain);
        updateLegacyManualGain();
    }

    /**
     * Converts the legacy mixer gain setting, such as GAIN_105 in tenths of a dB.
     */
    @JsonSetter("mixerGain")
    public void setLegacyMixerGain(String mixerGain)
    {
        mLegacyMixerGain = parseLegacyGain(mixerGain);
        updateLegacyManualGain();
    }

    /**
     * Sets the gain to the sum of the legacy LNA and mixer gains when the legacy master gain is MANUAL.
     */
    private void updateLegacyManualGain()
    {
        if(mLegacyManualGain && mLegacyLNAGain != null && mLegacyMixerGain != null)
        {
            setGain(mLegacyLNAGain + mLegacyMixerGain);
        }
    }
}
