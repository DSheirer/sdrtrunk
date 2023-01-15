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
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceInfo;

/**
 * RSPduo Tuner 2 discovered tuner.
 *
 * When constructed with a master-slave bridge, this device will be used in master-slave mode.  If the bridge is not
 * provided as construction time, the device will be configured for single-tuner operation.
 */
public class DiscoveredRspDuoTuner2 extends DiscoveredRspTuner<IControlRspDuoTuner2>
{
    private MasterSlaveBridge mMasterSlaveBridge;

    /**
     * Constructs an instance configured for a single-tuner
     * @param deviceInfo describing the tuner
     * @param channelizerType to use for the tuner once started
     */
    public DiscoveredRspDuoTuner2(DeviceInfo deviceInfo, ChannelizerType channelizerType)
    {
        super(deviceInfo, channelizerType);
    }

    /**
     * Constructs an instance configured as Slave Tuner 2.
     * @param deviceInfo describing the tuner
     * @param channelizerType to use for the tuner once started
     * @param bridge for synchronizing with the master tuner 1.
     */
    public DiscoveredRspDuoTuner2(DeviceInfo deviceInfo, ChannelizerType channelizerType, MasterSlaveBridge bridge)
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

                    if(mMasterSlaveBridge != null)
                    {
                        if(mTuner.getTunerController() instanceof RspDuoTuner2Controller rspDuoTuner2Controller)
                        {
                            mMasterSlaveBridge.setSlave(rspDuoTuner2Controller);
                        }
                    }
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
        return DiscoveredRspDuoTuner1.RSP_DUO_ID_PREFIX + "2 SER#" + getDeviceInfo().getSerialNumber();
    }

    /**
     * ID for the slave tuner device, if one is present in the system.
     * @return slave tuner ID
     */
    public String getMasterId()
    {
        return DiscoveredRspDuoTuner1.RSP_DUO_ID_PREFIX + "1 SER#" + getDeviceInfo().getSerialNumber();
    }
}
