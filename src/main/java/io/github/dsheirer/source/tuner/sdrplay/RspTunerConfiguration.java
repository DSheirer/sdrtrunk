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

package io.github.dsheirer.source.tuner.sdrplay;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import io.github.dsheirer.source.tuner.sdrplay.rsp1.Rsp1TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp1a.Rsp1aTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp1b.Rsp1bTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rsp2.Rsp2TunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner1Configuration;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.RspDuoTuner2Configuration;
import io.github.dsheirer.source.tuner.sdrplay.rspDx.RspDxTunerConfiguration;

/**
 * Abstract RSP tuner configuration
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Rsp1TunerConfiguration.class, name = "rsp1TunerConfiguration"),
        @JsonSubTypes.Type(value = Rsp1aTunerConfiguration.class, name = "rsp1aTunerConfiguration"),
        @JsonSubTypes.Type(value = Rsp1bTunerConfiguration.class, name = "rsp1bTunerConfiguration"),
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
    private int mLNA = 0;
    private int mBasebandGainReduction = 50;
    private AgcMode mAgcMode = AgcMode.ENABLE;

    /**
     * JAXB Constructor
     */
    public RspTunerConfiguration()
    {
        super(RspTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, RspTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
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
     * LNA setting
     * @return LNA setting
     */
    @JacksonXmlProperty(isAttribute = true, localName = "lna")
    public int getLNA()
    {
        return mLNA;
    }

    /**
     * Sets the LNA state
     * @param LNA state, 0 - xx, varies by tuner and frequency range.
     */
    public void setLNA(int lna)
    {
        mLNA = lna;
    }

    /**
     * Baseband gain reduction
     * @return gain reduction in range 20-59 dB
     */
    @JacksonXmlProperty(isAttribute = true, localName = "gr")
    public int getBasebandGainReduction()
    {
        return mBasebandGainReduction;
    }

    /**
     * Sets the baseband gain reduction value.
     * @param basebandGainReduction value
     */
    public void setBasebandGainReduction(int basebandGainReduction)
    {
        mBasebandGainReduction = basebandGainReduction;
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
