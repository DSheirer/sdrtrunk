/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.RspDuoDevice;
import io.github.dsheirer.source.tuner.sdrplay.RspException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base control wrapper for an RSPduo Tuner 1
 */
public abstract class ControlRspDuoTuner1 extends ControlRspDuo implements IControlRspDuoTuner1
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlRspDuoTuner1.class);

    /**
     * Constructs an instance
     * @param deviceInfo for the device
     */
    public ControlRspDuoTuner1(DeviceInfo deviceInfo)
    {
        super(deviceInfo);
    }

    @Override
    public boolean isAmNotch() throws RspException
    {
        return query("get AM notch", RspDuoDevice::isTuner1AmNotch);
    }

    @Override
    public void setAmNotch(boolean enabled) throws RspException
    {
        command("set AM notch", device -> device.setTuner1AmNotch(enabled));
    }

    @Override
    public RspDuoAmPortSelection getAmPort() throws RspException
    {
        return query("get AM port", device -> RspDuoAmPortSelection.fromAmPort(device.tuner1AmPort()));
    }

    @Override
    public void setAmPort(RspDuoAmPortSelection port) throws RspException
    {
        command("set AM port " + port, device -> device.setTuner1AmPort(port.getAmPort()));
    }

    /**
     * Maximum LNA index value as determined by frequency range using the API section 5. Gain Reduction Table values.
     * @return maximum (valid) LNA index value.
     */
    @Override
    public int getMaximumLNASetting()
    {
        try
        {
            long frequency = getTunedFrequency();

            if(getAmPort() == RspDuoAmPortSelection.PORT_1) //Hi-Z Port
            {
                return 4;
            }

            if(frequency < 60_000_000)
            {
                return 6;
            }
            else if(frequency < 1_000_000_000)
            {
                return 9;
            }
            else
            {
                return 6;
            }
        }
        catch(RspException se)
        {
            mLog.error("Error getting tuned frequency while determining maximum LNA setting.");
        }

        return 4; //Use the most restrictive setting as a default.
    }
}
