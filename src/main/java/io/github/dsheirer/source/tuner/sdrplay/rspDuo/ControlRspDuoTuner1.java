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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoDevice;
import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoTuner1;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.ControlParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDuoAmPort;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.TunerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base control wrapper for an RSPduo Tuner 1
 */
public abstract class ControlRspDuoTuner1 extends ControlRspDuo<RspDuoTuner1> implements IControlRspDuoTuner1
{
    private Logger mLog = LoggerFactory.getLogger(ControlRspDuoTuner1.class);

    /**
     * Constructs an instance
     *
     * @param device to control
     */
    public ControlRspDuoTuner1(RspDuoDevice device)
    {
        super(device);
    }

    /**
     * Access tuner 1.
     * @return tuner
     * @throws SDRPlayException if the device is not started.
     */
    protected RspDuoTuner1 getTuner() throws SDRPlayException
    {
        if(hasDevice())
        {
            return (RspDuoTuner1) getDevice().getTuner();
        }

        throw new SDRPlayException("RSPduo device is not started");
    }

    @Override
    public TunerSelect getTunerSelect()
    {
        return TunerSelect.TUNER_1;
    }

    @Override
    protected ControlParameters getControlParameters()
    {
        return getDevice().getCompositeParameters().getControlAParameters();
    }

    @Override
    protected TunerParameters getTunerParameters()
    {
        return getDevice().getCompositeParameters().getTunerAParameters();
    }

    @Override
    public boolean isAmNotch() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getTuner().isAmNotch();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setAmNotch(boolean enabled) throws SDRPlayException
    {
        if(hasDevice())
        {
            getTuner().setAmNotch(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public RspDuoAmPort getAmPort() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getTuner().getAmPort();
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }

    @Override
    public void setAmPort(RspDuoAmPort port) throws SDRPlayException
    {
        if(hasDevice())
        {
            getTuner().setAmPort(port);
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
            RspDuoAmPort port = getAmPort();

            if(port == RspDuoAmPort.PORT_1) //Hi-Z Port
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
        catch(SDRPlayException se)
        {
            mLog.error("Error getting tuned frequency while determining maximum LNA setting.");
        }

        return 4; //Use the most restrictive setting as a default.
    }
}
