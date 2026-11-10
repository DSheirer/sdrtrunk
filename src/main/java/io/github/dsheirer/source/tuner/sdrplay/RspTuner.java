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

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.IControlRspDuoTuner1;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.IControlRspDuoTuner2;

/**
 * Generic RSP tuner with a device specific tuner controller implementation.
 */
public class RspTuner extends Tuner
{
    /**
     * Constructs an instance
     * @param tunerController for controlling the tuner
     * @param tunerErrorListener to process errors from this tuner
     * @param channelizerType to use with this tuner
     */
    public RspTuner(RspTunerController tunerController, ITunerErrorListener tunerErrorListener, ChannelizerType channelizerType)
    {
        super(tunerController, tunerErrorListener, channelizerType);
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        //12-bits per sample, 2 samples per frame, 10 MHz sample rate
        return 320_000_000;
    }

    @Override
    public String getUniqueID()
    {
        return getRspTunerController().getControlRsp().getDeviceInfo().serialNumber();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.RSP;
    }

    /**
     * RSP Tuner controller for this tuner
     */
    public RspTunerController getRspTunerController()
    {
        return (RspTunerController)getTunerController();
    }

    @Override
    public String getPreferredName()
    {
        IControlRsp controlRsp = getRspTunerController().getControlRsp();
        DeviceInfo deviceInfo = controlRsp.getDeviceInfo();
        String name = DiscoveredRspTuner.getModelName(deviceInfo.model()) + " SER:" + deviceInfo.serialNumber();

        if(controlRsp instanceof IControlRspDuoTuner1)
        {
            return name + " Tuner 1";
        }
        else if(controlRsp instanceof IControlRspDuoTuner2)
        {
            return name + " Tuner 2";
        }

        return name;
    }

    @Override
    public double getSampleSize()
    {
        return getRspTunerController().getControlRsp().getSampleRateEnumeration().getSampleSize();
    }
}
