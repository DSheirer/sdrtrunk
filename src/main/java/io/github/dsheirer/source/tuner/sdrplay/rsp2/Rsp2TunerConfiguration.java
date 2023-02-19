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

package io.github.dsheirer.source.tuner.sdrplay.rsp2;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerConfiguration;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp2AntennaSelection;

/**
 * RSP2 tuner configuration
 */
public class Rsp2TunerConfiguration extends RspTunerConfiguration
{
    private boolean mBiasT;
    private boolean mExternalReferenceOutput;
    private boolean mRfNotch;
    private Rsp2AntennaSelection mAntennaSelection = Rsp2AntennaSelection.ANT_A;

    /**
     * Constructs an instance
     * @param uniqueId for the tuner
     */
    public Rsp2TunerConfiguration(String uniqueId)
    {
        super(uniqueId);
    }

    /**
     * JAXB constructor
     */
    public Rsp2TunerConfiguration()
    {
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.RSP_2;
    }

    /**
     * Indicates if the external reference output is enabled.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "external_reference_output")
    public boolean isExternalReferenceOutput()
    {
        return mExternalReferenceOutput;
    }

    /**
     * Sets the enabled state of the external reference output.
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

    /**
     * Indicates if the RF Notch is enabled
     */
    @JacksonXmlProperty(isAttribute = true, localName = "rf_notch")
    public boolean isRfNotch()
    {
        return mRfNotch;
    }

    /**
     * Sets the enabled state of the RF Notch.
     */
    public void setRfNotch(boolean rfNotch)
    {
        mRfNotch = rfNotch;
    }

    /**
     * Antenna selection
     */
    @JacksonXmlProperty(isAttribute = true, localName = "antenna")
    public Rsp2AntennaSelection getAntennaSelection()
    {
        return mAntennaSelection;
    }

    /**
     * Sets the antenna selection
     */
    public void setAntennaSelection(Rsp2AntennaSelection selection)
    {
        mAntennaSelection = selection;
    }
}
