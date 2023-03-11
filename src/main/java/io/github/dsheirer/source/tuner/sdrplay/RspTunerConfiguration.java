/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.GainReduction;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.Rsp1aTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.Rsp2TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner1Configuration;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner2Configuration;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.RspDxTunerConfiguration;

/**
 * Abstract RSP tuner configuration
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Rsp1aTunerConfiguration.class, name = "rsp1aTunerConfiguration"),
        @JsonSubTypes.Type(value = Rsp2TunerConfiguration.class, name = "rsp2TunerConfiguration"),
        @JsonSubTypes.Type(value = RspDuoTuner1Configuration.class, name = "rspDuoTuner1Configuration"),
        @JsonSubTypes.Type(value = RspDuoTuner2Configuration.class, name = "rspDuoTuner2Configuration"),
        @JsonSubTypes.Type(value = RspDxTunerConfiguration.class, name = "rspDxTunerConfiguration"),
})
public abstract class RspTunerConfiguration extends TunerConfiguration
{
    public static final RspSampleRate DEFAULT_SINGLE_TUNER_SAMPLE_RATE = RspSampleRate.RATE_8_000;
    public static final RspSampleRate DEFAULT_DUAL_TUNER_SAMPLE_RATE = RspSampleRate.DUO_RATE_2_000;

    private RspSampleRate mRspSampleRate = DEFAULT_SINGLE_TUNER_SAMPLE_RATE;
    private int mGain = 24;
    private AgcMode mAgcMode = AgcMode.ENABLE;

    /**
     * JAXB Constructor
     */
    public RspTunerConfiguration()
    {
    }

    /**
     * Constructs an instance with the specified unique id
     * @param uniqueId for the tuner
     */
    public RspTunerConfiguration(String uniqueId)
    {
        super(uniqueId);
    }

    /**
     * Sample rate for the tuner
     */
    @JacksonXmlProperty(isAttribute = true, localName = "sampleRate")
    public RspSampleRate getSampleRate()
    {
        return mRspSampleRate;
    }

    /**
     * Sets the sample rate for the tuner
     */
    public void setSampleRate(RspSampleRate rspSampleRate)
    {
        mRspSampleRate = rspSampleRate;
    }

    /**
     * Gain index to use
     * @return gain index, 0 - 28
     */
    @JacksonXmlProperty(isAttribute = true, localName = "gain")
    public int getGain()
    {
        return mGain;
    }

    /**
     * Sets the gain index
     * @param gain index, 0 - 28
     */
    public void setGain(int gain)
    {
        if(GainReduction.MIN_GAIN_INDEX <= gain && gain <= GainReduction.MAX_GAIN_INDEX)
        {
            mGain = gain;
        }
    }

    /**
     * IF AGC mode
     * @return mode
     */
    @JacksonXmlProperty(isAttribute = true, localName = "agcMode")
    public AgcMode getAgcMode()
    {
        return mAgcMode;
    }

    /**
     * Sets the IF AGC mode
     * @param mode to set
     */
    public void setAgcMode(AgcMode mode)
    {
        mAgcMode = mode;
    }
}
