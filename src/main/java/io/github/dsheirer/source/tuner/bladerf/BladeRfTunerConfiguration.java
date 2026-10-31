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

package io.github.dsheirer.source.tuner.bladerf;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.bladerf.GainMode;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

/**
 * bladeRF tuner configuration.
 */
public class BladeRfTunerConfiguration extends TunerConfiguration
{
    private int mSampleRate = BladeRfSampleRate.DEFAULT.getRate();
    private GainMode mGainMode = BladeRfTunerController.GAIN_MODE_DEFAULT;
    private int mGain = BladeRfTunerController.GAIN_DEFAULT;
    private boolean mBiasT = false;

    /**
     * Default constructor for Jackson
     */
    public BladeRfTunerConfiguration()
    {
        super(BladeRfTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, BladeRfTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    /**
     * Constructs an instance
     * @param uniqueID for the tuner
     */
    public BladeRfTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public TunerType getTunerType()
    {
        return TunerType.BLADERF;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
    public int getSampleRate()
    {
        return mSampleRate;
    }

    public void setSampleRate(int sampleRate)
    {
        mSampleRate = sampleRate;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "gain_mode")
    public GainMode getGainMode()
    {
        return mGainMode;
    }

    public void setGainMode(GainMode gainMode)
    {
        mGainMode = gainMode;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "gain")
    public int getGain()
    {
        return mGain;
    }

    public void setGain(int gain)
    {
        mGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "bias_t")
    public boolean isBiasT()
    {
        return mBiasT;
    }

    public void setBiasT(boolean enabled)
    {
        mBiasT = enabled;
    }
}
