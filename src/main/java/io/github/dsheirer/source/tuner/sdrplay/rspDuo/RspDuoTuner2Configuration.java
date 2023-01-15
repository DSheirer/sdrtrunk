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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;

/**
 * RSPduo tuner 1 configuration
 */
public class RspDuoTuner2Configuration extends RspTunerConfiguration
{
    private boolean mBiasT;
    private boolean mExternalReferenceOutput;
    private boolean mRfDabNotch;
    private boolean mRfNotch;

    /**
     * Constructs an instance
     * @param uniqueId for the tuner
     */
    public RspDuoTuner2Configuration(String uniqueId)
    {
        super(uniqueId);
    }

    /**
     * JAXB constructor
     */
    public RspDuoTuner2Configuration()
    {
    }


    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_DUO_2;
    }

    /**
     * Indicates if the RF Notch is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rf_notch")
    public boolean isRfNotch()
    {
        return mRfNotch;
    }

    /**
     * Sets the enabled state of the RF Notch
     */
    public void setRfNotch(boolean enabled)
    {
        mRfNotch = enabled;
    }

    /**
     * Indicates if the RF Digital Audio Broadcast (DAB) notch is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "dab_notch")
    public boolean isRfDabNotch()
    {
        return mRfDabNotch;
    }

    /**
     * Sets the enabled state of the RF Notch.
     */
    public void setRfDabNotch(boolean enabled)
    {
        mRfDabNotch = enabled;
    }

    /**
     * Indicates if the external reference output is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "external_reference_output")
    public boolean isExternalReferenceOutput()
    {
        return mExternalReferenceOutput;
    }

    /**
     * Sets the enabled state of the external reference output
     */
    public void setExternalReferenceOutput(boolean enabled)
    {
        mExternalReferenceOutput = enabled;
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "bias_t")
    public boolean isBiasT()
    {
        return mBiasT;
    }

    /**
     * Sets the enabled state of the Bias-T
     */
    public void setBiasT(boolean enabled)
    {
        mBiasT = enabled;
    }
}
