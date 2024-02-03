/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.source.tuner.sdrplay.ControlRsp;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.device.Rsp2Device;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp2AmPort;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.Rsp2AntennaSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control wrapper for an RSP2 Device
 */
public class ControlRsp2 extends ControlRsp<Rsp2Device> implements IControlRsp2
{
    private Logger mLog = LoggerFactory.getLogger(ControlRsp2.class);
    private Rsp2AntennaSelection mAntennaSelection = Rsp2AntennaSelection.ANT_A;

    /**
     * Constructs an instance
     * @param sdrplayApi to control the device
     * @param device to control
     */
    public ControlRsp2(Rsp2Device device)
    {
        super(device);
    }

    @Override
    public boolean isBiasT() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getDevice().getTuner().isBiasT();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setBiasT(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().getTuner().setBiasT(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public boolean isExternalReferenceOutput() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getDevice().getTuner().isExternalReferenceOutput();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setExternalReferenceOutput(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().getTuner().setExternalReferenceOutput(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public boolean isRfNotch() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getDevice().getTuner().isRfNotch();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setRfNotch(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().getTuner().setRfNotch(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public Rsp2AntennaSelection getAntennaSelection() throws SDRPlayException
    {
        return mAntennaSelection;
    }

    @Override
    public void setAntennaSelection(Rsp2AntennaSelection selection) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().getTuner().setAmPort(selection.getAmPort());
            getDevice().getTuner().setAntenna(selection.getAntenna());
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
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
            Rsp2AmPort port = getAntennaSelection().getAmPort();

            if(port == Rsp2AmPort.PORT_1_HIGH_Z)
            {
                return 4;
            }

            if(frequency < 420_000_000)
            {
                return 8;
            }
            else
            {
                return 5;
            }
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error getting tuned frequency while determining maximum LNA setting.");
        }

        return 4; //Use the most restrictive setting as a default.
    }
}
