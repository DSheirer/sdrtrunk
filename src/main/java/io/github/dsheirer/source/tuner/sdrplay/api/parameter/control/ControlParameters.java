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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.control;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_ControlParamsT;
import java.lang.foreign.MemorySegment;

/**
 * Control Parameters structure (sdrplay_api_ControlParamsT)
 */
public class ControlParameters
{
    private final MemorySegment mControlParams;
    private final DcOffset mDcOffset;
    private final Decimation mDecimation;
    private final Agc mAgc;

    /**
     * Creates an instance from the foreign memory segment
     */
    public ControlParameters(MemorySegment controlParams)
    {
        mControlParams = controlParams;
        mDcOffset = new DcOffset(sdrplay_api_ControlParamsT.dcOffset(controlParams));
        mDecimation = new Decimation(sdrplay_api_ControlParamsT.decimation(controlParams));
        mAgc = new Agc(sdrplay_api_ControlParamsT.agc(controlParams));
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getControlParams()
    {
        return mControlParams;
    }

    /**
     * DC offset settings for DC and IQ correction
     */
    public DcOffset getDcOffset()
    {
        return mDcOffset;
    }

    /**
     * Decimation settings
     */
    public Decimation getDecimation()
    {
        return mDecimation;
    }

    /**
     * Automatic Gain Control (AGC) settings
     */
    public Agc getAgc()
    {
        return mAgc;
    }

    /**
     * Current ADSB mode
     */
    public AdsbMode getAdsbMode()
    {
        return AdsbMode.fromValue(sdrplay_api_ControlParamsT.adsbMode(getControlParams()));
    }

    /**
     * Sets ADSB mode
     */
    public void setAdsbMode(AdsbMode mode)
    {
        sdrplay_api_ControlParamsT.adsbMode(getControlParams(), mode.getValue());
    }
}
