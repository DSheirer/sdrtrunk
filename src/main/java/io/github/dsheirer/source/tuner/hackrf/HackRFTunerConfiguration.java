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
package io.github.dsheirer.source.tuner.hackrf;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFSampleRate;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;

public class HackRFTunerConfiguration extends TunerConfiguration
{
    private HackRFSampleRate mSampleRate = HackRFSampleRate.RATE_5_0;
    private HackRFLNAGain mLNAGain = HackRFLNAGain.GAIN_16;  // We can see some signal at this gain
    private HackRFVGAGain mVGAGain = HackRFVGAGain.GAIN_16;  // We can see some signal at this gain
    private boolean mAmplifierEnabled = false;  //Probably should start off disabled

    /**
     * Default constructor for JAXB
     */
    public HackRFTunerConfiguration()
    {
        super(HackRFTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, HackRFTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    public HackRFTunerConfiguration(String uniqueID)
    {
        super(uniqueID);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.HACKRF_ONE;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "amplifier_enabled")
    public boolean getAmplifierEnabled()
    {
        return mAmplifierEnabled;
    }

    public void setAmplifierEnabled(boolean enabled)
    {
        mAmplifierEnabled = enabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lna_gain")
    public HackRFLNAGain getLNAGain()
    {
        return mLNAGain;
    }

    public void setLNAGain(HackRFLNAGain lnaGain)
    {
        mLNAGain = lnaGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "vga_gain")
    public HackRFVGAGain getVGAGain()
    {
        return mVGAGain;
    }

    public void setVGAGain(HackRFVGAGain vgaGain)
    {
        mVGAGain = vgaGain;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
    public HackRFSampleRate getSampleRate()
    {
        return mSampleRate;
    }

    public void setSampleRate(HackRFSampleRate sampleRate)
    {
        mSampleRate = sampleRate;
    }
}
