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
package io.github.dsheirer.source.tuner.rtl.e4k;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KLNAGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KMixerGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.IFGain;

/**
 * RTL-2832 with embedded E4000 tuner configuration
 */
public class E4KTunerConfiguration extends RTL2832TunerConfiguration
{
    private E4KGain mMasterGain = E4KGain.MANUAL;
    private E4KMixerGain mMixerGain = E4KMixerGain.GAIN_4;
    private E4KLNAGain mLNAGain = E4KLNAGain.GAIN_PLUS_200;
    private IFGain mIFGain = IFGain.LINEARITY_8;

    /**
     * Default constructor for JAXB
     */
    public E4KTunerConfiguration()
    {
        super(E4KEmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ, E4KEmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.ELONICS_E4000;
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public E4KTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "master_gain")
    public E4KGain getMasterGain()
    {
        return mMasterGain;
    }

    public void setMasterGain(E4KGain gain)
    {
        mMasterGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public E4KMixerGain getMixerGain()
    {
        return mMixerGain;
    }

    public void setMixerGain(E4KMixerGain mixerGain)
    {
        mMixerGain = mixerGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public E4KLNAGain getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(E4KLNAGain lnaGain)
    {
        mLNAGain = lnaGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "if_gain")
    public IFGain getIFGain()
    {
        return mIFGain;
    }

    public void setIFGain(IFGain ifGain)
    {
        mIFGain = ifGain;
    }
}
