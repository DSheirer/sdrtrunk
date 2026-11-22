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
package io.github.dsheirer.source.tuner.rtl.e4k;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;

/**
 * RTL-2832 with embedded E4000 tuner configuration
 */
public class E4KTunerConfiguration extends RTL2832TunerConfiguration
{
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 52_000_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 2_200_000_000l;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 15000;
    /**
     * Default gain in tenths of a dB
     */
    public static final int DEFAULT_GAIN = 240;
    /**
     * Default total IF gain in dB
     */
    public static final int DEFAULT_IF_GAIN = 24;

    private int mIFGain = DEFAULT_IF_GAIN;

    //Legacy gain settings, used to convert configurations from the previous E4000 driver's master, LNA and mixer gains
    private boolean mLegacyManualGain = false;
    private Integer mLegacyLNAGain;
    private Integer mLegacyMixerGain;

    /**
     * Default constructor for Jackson
     */
    public E4KTunerConfiguration()
    {
        super(MINIMUM_TUNABLE_FREQUENCY_HZ, MAXIMUM_TUNABLE_FREQUENCY_HZ, DEFAULT_GAIN);
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public E4KTunerConfiguration(String uniqueID)
    {
        super(uniqueID, DEFAULT_GAIN);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.ELONICS_E4000;
    }

    /**
     * Total IF gain, distributed across the six IF gain stages.
     * @return gain in dB
     */
    @JacksonXmlProperty(isAttribute = true, localName = "if_gain_db")
    public int getIFGainDb()
    {
        return mIFGain;
    }

    /**
     * Sets the total IF gain
     * @param gain in dB, in the range E4K_IF_GAIN_MINIMUM to E4K_IF_GAIN_MAXIMUM
     */
    public void setIFGainDb(int gain)
    {
        mIFGain = Math.clamp(gain, RTL2832TunerController.E4K_IF_GAIN_MINIMUM,
                RTL2832TunerController.E4K_IF_GAIN_MAXIMUM);
    }

    /**
     * Converts the legacy IF gain setting, such as LINEARITY_8 or SENSITIVITY_24, where the value is in dB.
     */
    @JsonSetter("ifgain")
    public void setLegacyIFGain(String ifGain)
    {
        Integer gain = parseLegacyGain(ifGain);

        if(gain != null)
        {
            setIFGainDb(gain);
        }
    }

    /**
     * Converts the legacy master gain setting: AUTOMATIC, MANUAL (using the LNA and mixer gains) or a gain value
     * such as PLUS_240 or MINUS_10 in tenths of a dB.
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
     * Converts the legacy LNA gain setting, such as GAIN_PLUS_200 or GAIN_MINUS_50 in tenths of a dB.
     */
    @JsonSetter("lnagain")
    public void setLegacyLNAGain(String lnaGain)
    {
        mLegacyLNAGain = parseLegacyGain(lnaGain);
        updateLegacyManualGain();
    }

    /**
     * Converts the legacy mixer gain setting, GAIN_4 or GAIN_12 in dB.
     */
    @JsonSetter("mixerGain")
    public void setLegacyMixerGain(String mixerGain)
    {
        Integer gain = parseLegacyGain(mixerGain);
        mLegacyMixerGain = gain != null ? gain * 10 : null;
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
