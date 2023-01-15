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

package io.github.dsheirer.source.tuner.sdrplay.rspDx;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.HdrModeBandwidth;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDxAntenna;

/**
 * RSPdx tuner configuration
 */
public class RspDxTunerConfiguration extends RspTunerConfiguration
{
    private boolean mBiasT;
    private boolean mHdrMode = false;
    private boolean mRfNotch;
    private boolean mRfDabNotch;
    private RspDxAntenna mAntenna = RspDxAntenna.ANTENNA_A;
    private HdrModeBandwidth mHdrModeBandwidth = HdrModeBandwidth.BANDWIDTH_1_700;

    /**
     * Constructs an instance
     * @param uniqueId for the tuner
     */
    public RspDxTunerConfiguration(String uniqueId)
    {
        super(uniqueId);
    }

    /**
     * JAXB constructor
     */
    public RspDxTunerConfiguration()
    {
    }


    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_DX;
    }

    /**
     * Indicates if HDR mode is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "hdr_mode")
    public boolean isHdrMode()
    {
        return mHdrMode;
    }

    /**
     * Sets enabled state of HDR mode
     */
    public void setHdrMode(boolean enabled)
    {
        mHdrMode = enabled;
    }

    /**
     * Indicates if RF notch is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rf_notch")
    public boolean isRfNotch()
    {
        return mRfNotch;
    }

    /**
     * Sets enabled state of RF notch
     */
    public void setRfNotch(boolean enabled)
    {
        mRfNotch = enabled;
    }

    /**
     * Indicates if RF Digital Audio Broadcast (DAB) notch is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "dab_notch")
    public boolean isRfDabNotch()
    {
        return mRfDabNotch;
    }

    /**
     * Sets enabled state of DAB notch
     */
    public void setRfDabNotch(boolean enabled)
    {
        mRfDabNotch = enabled;
    }

    /**
     * Indicates if Bias-T is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "bias_t")
    public boolean isBiasT()
    {
        return mBiasT;
    }

    /**
     * Sets enabled state of Bias-T
     */
    public void setBiasT(boolean enabled)
    {
        mBiasT = enabled;
    }

    /**
     * Antenna setting
     */
    @JacksonXmlProperty(isAttribute = true, localName = "antenna")
    public RspDxAntenna getAntenna()
    {
        return mAntenna;
    }

    /**
     * Sets the antenna.
     */
    public void setAntenna(RspDxAntenna antenna)
    {
        mAntenna = antenna;
    }

    /**
     * HDR mode bandwidth setting.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "hdr_mode_bandwidth")
    public HdrModeBandwidth getHdrModeBandwidth()
    {
        return mHdrModeBandwidth;
    }

    /**
     * Sets HDR mode bandwidth
     */
    public void setHdrModeBandwidth(HdrModeBandwidth hdrModeBandwidth)
    {
        mHdrModeBandwidth = hdrModeBandwidth;
    }
}
