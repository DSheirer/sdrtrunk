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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRplay;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite.RspDxCompositeParameters;

/**
 * RSPdx Device
 */
public class RspDxDevice extends Device<RspDxCompositeParameters, RspDxTuner>
{
    private RspDxTuner mTuner1;

    /**
     * Constructs an SDRPlay RSPdx device from the foreign memory segment
     *
     * @param sdrPlay api instance that created this device
     * @param deviceStruct parser
     */
    RspDxDevice(SDRplay sdrPlay, IDeviceStruct deviceStruct)
    {
        super(sdrPlay, deviceStruct);
    }


    @Override
    public RspDxTuner getTuner() throws SDRPlayException
    {
        if(!isSelected())
        {
            throw new SDRPlayException("Device must be selected before accessing the tuner");
        }

        if(mTuner1 == null)
        {
            mTuner1 = new RspDxTuner(RspDxDevice.this, getAPI(), getTunerSelect(),
                    getCompositeParameters().getDeviceParameters(), getCompositeParameters().getTunerAParameters(),
                    getCompositeParameters().getControlAParameters());
        }

        return mTuner1;
    }
}
