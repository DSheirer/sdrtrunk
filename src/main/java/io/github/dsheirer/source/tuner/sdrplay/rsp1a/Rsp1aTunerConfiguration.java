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

package io.github.dsheirer.source.tuner.sdrplay.rsp1a;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;

/**
 * RSP1A tuner configuration
 */
public class Rsp1aTunerConfiguration extends RspTunerConfiguration
{
    private boolean mRfNotch;
    private boolean mRfDabNotch;
    private boolean mBiasT;

    /**
     * Constructs an instance
     * @param uniqueId for the tuner
     */
    public Rsp1aTunerConfiguration(String uniqueId)
    {
        super(uniqueId);
    }

    /**
     * JAXB constructor
     */
    public Rsp1aTunerConfiguration()
    {
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_1A;
    }

    /**
     * Indicates if the RF notch is enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rf_notch")
    public boolean isRfNotch()
    {
        return mRfNotch;
    }

    /**
     * Sets the enabled state of the RF notch
     * @param enabled
     */
    public void setRfNotch(boolean enabled)
    {
        mRfNotch = enabled;
    }

    /**
     * Indicates if the RF Digital Audio Broadcast (DAB) notch is enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "dab_notch")
    public boolean isRfDabNotch()
    {
        return mRfDabNotch;
    }

    /**
     * Sets the enabled state of the RF DAB notch
     * @param enabled
     */
    public void setRfDabNotch(boolean enabled)
    {
        mRfDabNotch = enabled;
    }

    /**
     * Indicates if the Bias-T is enabled.
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
