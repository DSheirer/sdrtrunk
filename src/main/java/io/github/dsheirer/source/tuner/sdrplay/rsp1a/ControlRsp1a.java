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

package io.github.dsheirer.source.tuner.sdrplay.rsp1a;

import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.Rsp1aDevice;
import io.github.dsheirer.source.tuner.sdrplay.ControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.RspException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control wrapper for an RSP1A Device
 */
public class ControlRsp1a extends ControlRsp<Rsp1aDevice> implements IControlRsp1a
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlRsp1a.class);

    /**
     * Constructs an instance
     * @param deviceInfo for the device
     */
    public ControlRsp1a(DeviceInfo deviceInfo)
    {
        super(deviceInfo, Rsp1aDevice.class);
    }

    @Override
    public boolean isBiasT() throws RspException
    {
        return query("get Bias-T", Rsp1aDevice::isBiasT);
    }

    @Override
    public void setBiasT(boolean enabled) throws RspException
    {
        command("set Bias-T", device -> device.setBiasT(enabled));
    }

    @Override
    public boolean isRfDabNotch() throws RspException
    {
        return query("get RF DAB notch", Rsp1aDevice::isRfDabNotch);
    }

    @Override
    public void setRfDabNotch(boolean enabled) throws RspException
    {
        command("set RF DAB notch", device -> device.setRfDabNotch(enabled));
    }

    @Override
    public boolean isRfNotch() throws RspException
    {
        return query("get RF notch", Rsp1aDevice::isRfNotch);
    }

    @Override
    public void setRfNotch(boolean enabled) throws RspException
    {
        command("set RF notch", device -> device.setRfNotch(enabled));
    }

    /**
     * Frequency below which the LNA has the low (HF) band gain table.
     */
    protected long getLowBandLimit()
    {
        return 60_000_000;
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

            if(frequency < getLowBandLimit())
            {
                return 6;
            }
            else if(frequency < 1_000_000_000)
            {
                return 9;
            }
            else
            {
                return 8;
            }
        }
        catch(RspException se)
        {
            mLog.error("Error getting tuned frequency while determining maximum LNA setting.");
        }

        return 6; //Use the most restrictive setting as a default.
    }
}
