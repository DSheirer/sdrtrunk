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
package io.github.dsheirer.source.tuner.rtl.r820t;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;

/**
 * RTL-2832 with embedded R820T tuner configuration
 */
public class R820TTunerConfiguration extends RTL2832TunerConfiguration
{
    private R820TEmbeddedTuner.R820TGain mMasterGain = R820TEmbeddedTuner.R820TGain.GAIN_327;
    private R820TEmbeddedTuner.R820TMixerGain mMixerGain = R820TEmbeddedTuner.R820TMixerGain.GAIN_105;
    private R820TEmbeddedTuner.R820TLNAGain mLNAGain = R820TEmbeddedTuner.R820TLNAGain.GAIN_222;
    private R820TEmbeddedTuner.R820TVGAGain mVGAGain = R820TEmbeddedTuner.R820TVGAGain.GAIN_210;

    /**
     * Default constructor for JAXB
     */
    public R820TTunerConfiguration()
    {
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.RAFAELMICRO_R820T;
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public R820TTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "master_gain")
    public R820TEmbeddedTuner.R820TGain getMasterGain()
    {
        return mMasterGain;
    }

    public void setMasterGain(R820TEmbeddedTuner.R820TGain gain)
    {
        mMasterGain = gain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public R820TEmbeddedTuner.R820TMixerGain getMixerGain()
    {
        return mMixerGain;
    }

    public void setMixerGain(R820TEmbeddedTuner.R820TMixerGain mixerGain)
    {
        mMixerGain = mixerGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public R820TEmbeddedTuner.R820TLNAGain getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(R820TEmbeddedTuner.R820TLNAGain lnaGain)
    {
        mLNAGain = lnaGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "vga_gain")
    public R820TEmbeddedTuner.R820TVGAGain getVGAGain()
    {
        return mVGAGain;
    }

    public void setVGAGain(R820TEmbeddedTuner.R820TVGAGain vgaGain)
    {
        mVGAGain = vgaGain;
    }
}
