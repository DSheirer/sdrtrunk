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

package io.github.dsheirer.source.tuner.sdrplay.rspDx;

import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.RspDxDevice;
import io.github.dsheirer.source.tuner.sdrplay.ControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.RspException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control wrapper for an RSPdx and RSPdx-R2 Device
 */
public class ControlRspDx extends ControlRsp<RspDxDevice> implements IControlRspDx
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlRspDx.class);

    /**
     * Constructs an instance
     * @param deviceInfo for the device
     */
    public ControlRspDx(DeviceInfo deviceInfo)
    {
        super(deviceInfo, RspDxDevice.class);
    }

    @Override
    public boolean isBiasT() throws RspException
    {
        return query("get Bias-T", RspDxDevice::isBiasT);
    }

    @Override
    public void setBiasT(boolean enabled) throws RspException
    {
        command("set Bias-T", device -> device.setBiasT(enabled));
    }

    @Override
    public boolean isRfNotch() throws RspException
    {
        return query("get RF notch", RspDxDevice::isRfNotch);
    }

    @Override
    public void setRfNotch(boolean enabled) throws RspException
    {
        command("set RF notch", device -> device.setRfNotch(enabled));
    }

    @Override
    public boolean isRfDabNotch() throws RspException
    {
        return query("get RF DAB notch", RspDxDevice::isRfDabNotch);
    }

    @Override
    public void setRfDabNotch(boolean enabled) throws RspException
    {
        command("set RF DAB notch", device -> device.setRfDabNotch(enabled));
    }

    @Override
    public boolean isHighDynamicRange() throws RspException
    {
        return query("get HDR mode", RspDxDevice::isHdr);
    }

    @Override
    public void setHighDynamicRange(boolean enabled) throws RspException
    {
        command("set HDR mode", device -> device.setHdr(enabled));
    }

    @Override
    public RspDxAntennaSelection getAntenna() throws RspException
    {
        return query("get antenna", device -> RspDxAntennaSelection.fromAntenna(device.antenna()));
    }

    @Override
    public void setAntenna(RspDxAntennaSelection antenna) throws RspException
    {
        command("set antenna " + antenna, device -> device.setAntenna(antenna.getAntenna()));
    }

    @Override
    public void setHdrModeBandwidth(HdrModeBandwidth bandwidth) throws RspException
    {
        if(bandwidth == null || bandwidth.getBandwidth() == null)
        {
            throw new RspException("Invalid HDR mode bandwidth: " + bandwidth);
        }

        command("set HDR mode bandwidth " + bandwidth, device -> device.setHdrBandwidth(bandwidth.getBandwidth()));
    }

    @Override
    public HdrModeBandwidth getHdrModeBandwidth() throws RspException
    {
        return query("get HDR mode bandwidth", device -> HdrModeBandwidth.fromBandwidth(device.hdrBandwidth()));
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

            if(frequency < 12_000_000)
            {
                return 21;
            }
            else if(frequency < 50_000_000)
            {
                return 19;
            }
            else if(frequency < 60_000_000)
            {
                return 24;
            }
            else if(frequency < 250_000_000)
            {
                return 26;
            }
            else if(frequency < 420_000_000)
            {
                return 27;
            }
            else if(frequency < 1_000_000_000)
            {
                return 20;
            }
            else
            {
                return 18;
            }
        }
        catch(RspException se)
        {
            mLog.error("Error getting tuned frequency while determining maximum LNA setting.");
        }

        return 18; //Use the most restrictive setting as a default.
    }
}
