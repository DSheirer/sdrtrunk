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
package io.github.dsheirer.source.tuner.fcd.proplusV2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

public class FCD2TunerConfiguration extends TunerConfiguration
{
    private boolean mGainLNA = true;
    private boolean mGainMixer = true;

    /**
     * Default constructor for JAXB
     */
    public FCD2TunerConfiguration()
    {
        super(FCD2TunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, FCD2TunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.FUNCUBE_DONGLE_PRO_PLUS;
    }

    public FCD2TunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public boolean getGainLNA()
    {
        return mGainLNA;
    }

    public void setGainLNA(boolean gain)
    {
        mGainLNA = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public boolean getGainMixer()
    {
        return mGainMixer;
    }

    public void setGainMixer(boolean gain)
    {
        mGainMixer = gain;
    }
}
