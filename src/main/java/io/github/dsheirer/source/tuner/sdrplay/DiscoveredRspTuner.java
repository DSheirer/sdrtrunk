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
import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.RspModel;

/**
 * Discovered SDRplay RSP tuner
 */
public abstract class DiscoveredRspTuner<R extends IControlRsp> extends DiscoveredTuner
{
    private final DeviceInfo mDeviceInfo;
    private final DeviceSelectionMode mDeviceSelectionMode;
    private final ChannelizerType mChannelizerType;

    /**
     * Constructs an instance
     * @param deviceInfo to select the device from the API
     * @param deviceSelectionMode for selecting the device from the API
     * @param channelizerType to use for the tuner
     */
    public DiscoveredRspTuner(DeviceInfo deviceInfo, DeviceSelectionMode deviceSelectionMode,
                              ChannelizerType channelizerType)
    {
        mDeviceInfo = deviceInfo;
        mDeviceSelectionMode = deviceSelectionMode;
        mChannelizerType = channelizerType;
    }

    /**
     * Constructs an instance for single tuner 1 selection mode.
     * @param deviceInfo to select the device from the API
     * @param channelizerType to use for the tuner
     */
    public DiscoveredRspTuner(DeviceInfo deviceInfo, ChannelizerType channelizerType)
    {
        this(deviceInfo, DeviceSelectionMode.SINGLE_TUNER_1, channelizerType);
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
     * Device selection mode for the discovered tuner
     */
    public DeviceSelectionMode getDeviceSelectionMode()
    {
        return mDeviceSelectionMode;
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
     * RSP model
     */
    public RspModel getModel()
    {
        return mDeviceInfo.model();
    }

    /**
     * Model name used in tuner IDs and display names.  Note: these names are used for tuner configuration IDs and
     * must not change.
     * @param model of RSP
     * @return model name
     */
    public static String getModelName(RspModel model)
    {
        return switch(model)
        {
            case RSP1 -> "RSP1";
            case RSP1A -> "RSP1A";
            case RSP1B -> "RSP1B";
            case RSP2 -> "RSP2";
            case RSPDUO -> "RSPduo";
            case RSPDX -> "RSPdx";
            case RSPDX_R2 -> "RSPdxR2";
            default -> "UNKNOWN";
        };
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
                mTuner = TunerFactory.getRspTuner(getDeviceInfo(), getDeviceSelectionMode(), getChannelizerType(), this);
            }
            catch(Exception se)
            {
                setErrorMessage("Tuner unavailable [" + getId() + "] - " + se.getMessage());
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
                    setErrorMessage("Error starting tuner [" + getId() + "] - " + se.getMessage());
                    mTuner = null;
                }
            }
        }
    }

    @Override
    public String getId()
    {
        return getModelName(getModel()) + " SER#" + getDeviceInfo().serialNumber();
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
