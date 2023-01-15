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

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspTuner;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSPduo Tuner 1 discovered tuner.
 *
 * When constructed with a master-slave bridge, this device will be used in master-slave mode.  If the bridge is not
 * provided as construction time, the device will be configured for single-tuner operation.
 */
public class DiscoveredRspDuoTuner1 extends DiscoveredRspTuner<IControlRspDuoTuner1>
{
    private static final Logger mLog = LoggerFactory.getLogger(DiscoveredRspTuner.class);
    public static final String RSP_DUO_ID_PREFIX = "RSPduo Tuner ";
    private MasterSlaveBridge mMasterSlaveBridge;

    /**
     * Constructs an instance configured for a single-tuner
     * @param deviceInfo describing the tuner
     * @param channelizerType to use for the tuner once started
     */
    public DiscoveredRspDuoTuner1(DeviceInfo deviceInfo, ChannelizerType channelizerType)
    {
        super(deviceInfo, channelizerType);
    }

    /**
     * Constructs an instance configured as Master Tuner 1.
     * @param deviceInfo describing the tuner
     * @param channelizerType to use for the tuner once started
     * @param bridge for synchronizing with the slaved tuner 2.
     */
    public DiscoveredRspDuoTuner1(DeviceInfo deviceInfo, ChannelizerType channelizerType, MasterSlaveBridge bridge)
    {
        super(deviceInfo, channelizerType);
        mMasterSlaveBridge = bridge;
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
                if(mMasterSlaveBridge != null)
                {
                    mTuner = TunerFactory.getRspDuoTuner(getDeviceInfo(), getChannelizerType(), this, mMasterSlaveBridge);

                    if(mTuner instanceof RspTuner rspTuner &&
                       rspTuner.getRspTunerController() instanceof RspDuoTuner1Controller rspDuoTuner1Controller)
                    {
                        mMasterSlaveBridge.setMaster(rspDuoTuner1Controller);
                    }
                }
                else
                {
                    mTuner = TunerFactory.getRspTuner(getDeviceInfo(), getChannelizerType(), this);
                }
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
                    mLog.error("Error", se);
                    setErrorMessage("Error starting tuner [" + getId() + "]");
                    mTuner = null;
                }
            }
        }
    }

    @Override
    public String getId()
    {
        return RSP_DUO_ID_PREFIX + "1 SER#" + getDeviceInfo().getSerialNumber();
    }

    /**
     * ID for the slave tuner device, if one is present in the system.
     * @return slave tuner ID
     */
    public String getSlaveId()
    {
        return RSP_DUO_ID_PREFIX + "2 SER#" + getDeviceInfo().getSerialNumber();
    }
}
