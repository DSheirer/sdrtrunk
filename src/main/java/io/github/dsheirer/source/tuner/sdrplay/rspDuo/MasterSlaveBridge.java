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

import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge providing streaming control synchronization between master and slave devices.  Additionally, this bridge
 * allows the master device to notify the slave device of sample rate changes so that the slave can apply appropriate
 * sample rate settings such as bandwidth and decimation. The RSPduo's two tuners do not operate independently.
 * Streaming on the slave does not start until streaming on the master starts.  The master cannot be shutdown until the
 * slave is shutdown.  This class provides the bridging to ensure that these sequencing requirements are met.
 */
public class MasterSlaveBridge
{
    private static final Logger mLog = LoggerFactory.getLogger(MasterSlaveBridge.class);
    private RspDuoTuner1Controller mMaster;
    private RspDuoTuner2Controller mSlave;

    public MasterSlaveBridge()
    {
    }

    /**
     * Sets the master tuner controller
     * @param master to set
     */
    public void setMaster(RspDuoTuner1Controller master)
    {
        mMaster = master;
    }

    /**
     * Sets the slave tuner controller and transfers the sample rate setting from the master.
     * @param slave to set
     */
    public void setSlave(RspDuoTuner2Controller slave)
    {
        mSlave = slave;

        if(hasMasterControl())
        {
            //Transfer master sample rate setting to slave
            notifySampleRate(getMasterControl().getSampleRateEnumeration());
        }
    }

    /**
     * Access to the tuner 1 master controller
     */
    private ControlRspDuoTuner1Master getMasterControl()
    {
        if(mMaster != null && mMaster.getControlRsp() instanceof ControlRspDuoTuner1Master masterControl)
        {
            return masterControl;
        }

        return null;
    }

    /**
     * Indicates if we have access to a non-null master control
     * @return true if non-null.
     */
    private boolean hasMasterControl()
    {
        return getMasterControl() != null;
    }

    /**
     * A blocking call that ensures that the master device is streaming before returning.  This method is intended to
     * be called by the slave to ensure that the master is streaming before the slave initiates streaming.  Once the
     * slave invokes this method, the master will stream continuously until stop() is invoked on the master.
     */
    public void startMasterStreamContinuously()
    {
        if(hasMasterControl())
        {
            getMasterControl().startStreamContinuously();
        }
    }

    /**
     * A blocking call that ensures that the slave device stops streaming before returning.  This method is intended
     * to be invoked by the master device, just prior to stopping the master.
     */
    public void stopSlave()
    {
        if(mSlave != null)
        {
            mSlave.stop();
            mSlave = null;
        }
    }

    /**
     * Notifies the slave of sample rate changes in the master.
     * @param sampleRate being applied by the master device.
     */
    public void notifySampleRate(RspSampleRate sampleRate)
    {
        if(mSlave != null)
        {
            try
            {
                mSlave.setSampleRate(sampleRate);
            }
            catch(SDRPlayException se)
            {
                mLog.error("Error setting sample rate on RSPduo tuner 2 slave", se);
            }
        }
    }
}
