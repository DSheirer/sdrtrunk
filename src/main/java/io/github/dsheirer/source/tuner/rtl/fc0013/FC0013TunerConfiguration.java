/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.rtl.fc0013;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;

/**
 * RTL-2832 with embedded FC0013 tuner configuration
 */
public class FC0013TunerConfiguration extends RTL2832TunerConfiguration
{
    private FC0013EmbeddedTuner.LNAGain mLnaGain = FC0013EmbeddedTuner.LNAGain.G10;
    private boolean mAgc = false;

    /**
     * Default constructor for JAXB
     */
    public FC0013TunerConfiguration()
    {
        super(FC0013EmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ, FC0013EmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.FITIPOWER_FC0013;
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public FC0013TunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    /**
     * LNA gain value.
     * @return value
     */
    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public FC0013EmbeddedTuner.LNAGain getLnaGain()
    {
        return mLnaGain;
    }

    /**
     * Sets the LNA gain value.
     * @param lnaGain value.
     */
    public void setLnaGain(FC0013EmbeddedTuner.LNAGain lnaGain)
    {
        mLnaGain = lnaGain;
    }

    /**
     * Automatic Gain Control (AGC) state
     * @return AGC enabled state
     */
    @JacksonXmlProperty(isAttribute = true, localName = "agc")
    public boolean getAGC()
    {
        return mAgc;
    }

    /**
     * Sets the AGC
     * @param enabled state of the AGC
     */
    public void setAGC(boolean enabled)
    {
        mAgc = enabled;
    }
}
