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
package io.github.dsheirer.source.tuner.rtl.r8x;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;

/**
 * RTL-2832 with embedded R8xxx tuner configuration
 */
public abstract class R8xTunerConfiguration extends RTL2832TunerConfiguration
{
    private R8xEmbeddedTuner.MasterGain mMasterMasterGain = R8xEmbeddedTuner.MasterGain.GAIN_327;
    private R8xEmbeddedTuner.MixerGain mMixerGain = R8xEmbeddedTuner.MixerGain.GAIN_105;
    private R8xEmbeddedTuner.LNAGain mLNAGain = R8xEmbeddedTuner.LNAGain.GAIN_222;
    private R8xEmbeddedTuner.VGAGain mVGAGain = R8xEmbeddedTuner.VGAGain.GAIN_210;

    /**
     * Default constructor for JAXB
     */
    public R8xTunerConfiguration()
    {
        super(R8xEmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ, R8xEmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    /**
     * Constructs an instance
     *
     * @param uniqueID for the tuner
     */
    public R8xTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "master_gain")
    public R8xEmbeddedTuner.MasterGain getMasterGain()
    {
        return mMasterMasterGain;
    }

    public void setMasterGain(R8xEmbeddedTuner.MasterGain masterGain)
    {
        mMasterMasterGain = masterGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mixer_gain")
    public R8xEmbeddedTuner.MixerGain getMixerGain()
    {
        return mMixerGain;
    }

    public void setMixerGain(R8xEmbeddedTuner.MixerGain mixerGain)
    {
        mMixerGain = mixerGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public R8xEmbeddedTuner.LNAGain getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(R8xEmbeddedTuner.LNAGain lnaGain)
    {
        mLNAGain = lnaGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "vga_gain")
    public R8xEmbeddedTuner.VGAGain getVGAGain()
    {
        return mVGAGain;
    }

    public void setVGAGain(R8xEmbeddedTuner.VGAGain vgaGain)
    {
        mVGAGain = vgaGain;
    }
}
