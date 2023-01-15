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
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceInfo;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceType;

/**
 * Discovered SDRplay RSP tuner
 */
public abstract class DiscoveredRspTuner<R extends IControlRsp> extends DiscoveredTuner
{
    private final DeviceInfo mDeviceInfo;
    private final ChannelizerType mChannelizerType;

    /**
     * Constructs an instance
     * @param deviceInfo to select the device from the API
     */
    public DiscoveredRspTuner(DeviceInfo deviceInfo, ChannelizerType channelizerType)
    {
        mDeviceInfo = deviceInfo;
        mChannelizerType = channelizerType;
    }

    /**
     * Information about the discovered RSP device
     * @return device info
     */
    public DeviceInfo getDeviceInfo()
    {
        return mDeviceInfo;
    }

    /**
     * Channelizer type to use for the tuner
     */
    protected ChannelizerType getChannelizerType()
    {
        return mChannelizerType;
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.RSP;
    }

    /**
     * Type of RSP tuner device
     */
    public DeviceType getDeviceType()
    {
        return mDeviceInfo.getDeviceType();
    }

    /**
     * Constructs and starts the tuner
     */
    @Override
    public void start()
    {
        if(isAvailable() && !hasTuner())
        {
            try
            {
                mTuner = TunerFactory.getRspTuner(getDeviceInfo(), getChannelizerType(), this);
            }
            catch(Exception se)
            {
                setErrorMessage("Tuner unavailable [" + getId() + "]");
                mTuner = null;
            }

            if(hasTuner())
            {
                try
                {
                    mTuner.start();
                }
                catch(Exception se)
                {
                    setErrorMessage("Error starting tuner [" + getId() + "]");
                    mTuner = null;
                }
            }
        }
    }

    @Override
    public String getId()
    {
        return getDeviceType().name() + " SER#" + getDeviceInfo().getSerialNumber();
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
