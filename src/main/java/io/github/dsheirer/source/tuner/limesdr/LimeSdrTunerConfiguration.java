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

package io.github.dsheirer.source.tuner.limesdr;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

/**
 * LimeSDR tuner configuration
 */
public class LimeSdrTunerConfiguration extends TunerConfiguration
{
    private int mSampleRate = LimeSdrSampleRate.DEFAULT.getRate();
    private int mGain = LimeSdrTunerController.GAIN_DEFAULT;
    private String mAntenna = LimeSdrTunerController.ANTENNA_DEFAULT;

    /**
     * Default constructor for Jackson
     */
    public LimeSdrTunerConfiguration()
    {
        super(LimeSdrTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, LimeSdrTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    /**
     * Constructs an instance
     * @param uniqueID for the tuner
     */
    public LimeSdrTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @Override
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public TunerType getTunerType()
    {
        return TunerType.LIMESDR;
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

    /**
     * Combined receive gain in dB, 0 to 73.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "gain")
    public int getGain()
    {
        return mGain;
    }

    public void setGain(int gain)
    {
        mGain = gain;
    }

    /**
     * Receive antenna port name, such as LNAW.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "antenna")
    public String getAntenna()
    {
        return mAntenna;
    }

    public void setAntenna(String antenna)
    {
        mAntenna = antenna;
    }
}
