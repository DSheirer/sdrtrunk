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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDuoAmPort;

/**
 * RSPduo tuner 1 configuration
 */
public class RspDuoTuner1Configuration extends RspTunerConfiguration
{
    private RspDuoAmPort mAmPort = RspDuoAmPort.PORT_2; //50-ohm port is default.
    private boolean mAmNotch;
    private boolean mExternalReferenceOutput;
    private boolean mRfDabNotch;
    private boolean mRfNotch;

    /**
     * Constructs an instance
     * @param uniqueId for the tuner
     */
    public RspDuoTuner1Configuration(String uniqueId)
    {
        super(uniqueId);
    }

    /**
     * JAXB constructor
     */
    public RspDuoTuner1Configuration()
    {
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_DUO_1;
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
     * Indicates if the AM Notch is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "am_notch")
    public boolean isAmNotch()
    {
        return mAmNotch;
    }

    /**
     * Sets the enabled state of the AM notch
     */
    public void setAmNotch(boolean amNotch)
    {
        mAmNotch = amNotch;
    }

    /**
     * AM port setting
     */
    @JacksonXmlProperty(isAttribute = true, localName = "am_port")
    public RspDuoAmPort getAmPort()
    {
        return mAmPort;
    }

    /**
     * Sets the AM port
     */
    public void setAmPort(RspDuoAmPort amPort)
    {
        mAmPort = amPort;
    }
}
