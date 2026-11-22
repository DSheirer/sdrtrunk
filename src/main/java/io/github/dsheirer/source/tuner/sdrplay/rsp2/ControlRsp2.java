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

package io.github.dsheirer.source.tuner.sdrplay.rsp2;

import io.github.dsheirer.sdrplay.DeviceInfo;
import io.github.dsheirer.sdrplay.Rsp2Device;
import io.github.dsheirer.source.tuner.sdrplay.ControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.RspException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control wrapper for an RSP2 Device
 */
public class ControlRsp2 extends ControlRsp<Rsp2Device> implements IControlRsp2
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlRsp2.class);
    private Rsp2AntennaSelection mAntennaSelection = Rsp2AntennaSelection.ANT_A;

    /**
     * Constructs an instance
     * @param deviceInfo for the device
     */
    public ControlRsp2(DeviceInfo deviceInfo)
    {
        super(deviceInfo, Rsp2Device.class);
    }

    @Override
    public boolean isBiasT() throws RspException
    {
        return query("get Bias-T", Rsp2Device::isBiasT);
    }

    @Override
    public void setBiasT(boolean enabled) throws RspException
    {
        command("set Bias-T", device -> device.setBiasT(enabled));
    }

    @Override
    public boolean isExternalReferenceOutput() throws RspException
    {
        return query("get external reference output", Rsp2Device::isExternalReferenceOutput);
    }

    @Override
    public void setExternalReferenceOutput(boolean enabled) throws RspException
    {
        command("set external reference output", device -> device.setExternalReferenceOutput(enabled));
    }

    @Override
    public boolean isRfNotch() throws RspException
    {
        return query("get RF notch", Rsp2Device::isRfNotch);
    }

    @Override
    public void setRfNotch(boolean enabled) throws RspException
    {
        command("set RF notch", device -> device.setRfNotch(enabled));
    }

    @Override
    public Rsp2AntennaSelection getAntennaSelection() throws RspException
    {
        return mAntennaSelection;
    }

    @Override
    public void setAntennaSelection(Rsp2AntennaSelection selection) throws RspException
    {
        command("set antenna " + selection, device -> {
            device.setAmPort(selection.getAmPort());
            device.setAntenna(selection.getAntenna());
        });

        mAntennaSelection = selection;
    }

    /**
     * Maximum LNA index value as determined by frequency range using the API section 5. Gain Reduction Table values.
     * @return maximum (valid) LNA index value.
     */
    @Override
    public int getMaximumLNASetting()
    {
        if(mAntennaSelection.isHighZ())
        {
            return 4;
        }

        try
        {
            long frequency = getTunedFrequency();

            if(frequency < 420_000_000)
            {
                return 8;
            }
            else
            {
                return 5;
            }
        }
        catch(RspException se)
        {
            mLog.error("Error getting tuned frequency while determining maximum LNA setting.");
        }

        return 4; //Use the most restrictive setting as a default.
    }
}
