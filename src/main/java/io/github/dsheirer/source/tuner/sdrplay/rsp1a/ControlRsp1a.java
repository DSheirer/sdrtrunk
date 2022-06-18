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

package io.github.dsheirer.source.tuner.sdrplay.rsp1a;

import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.device.Rsp1aDevice;
import io.github.dsheirer.source.tuner.sdrplay.ControlRsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control wrapper for an RSP1A Device
 */
public class ControlRsp1a extends ControlRsp<Rsp1aDevice> implements IControlRsp1a
{
    private Logger mLog = LoggerFactory.getLogger(ControlRsp1a.class);

    /**
     * Constructs an instance
     * @param device for the device
     */
    public ControlRsp1a(Rsp1aDevice device)
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
    public boolean isRfDabNotch() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getDevice().getTuner().isRfDabNotch();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setRfDabNotch(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getDevice().getTuner().setRfDabNotch(enabled);
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
}
