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

import com.github.dsheirer.sdrplay.SDRPlayException;
import com.github.dsheirer.sdrplay.device.RspDuoDevice;
import com.github.dsheirer.sdrplay.device.RspDuoTuner2;
import com.github.dsheirer.sdrplay.device.TunerSelect;
import com.github.dsheirer.sdrplay.parameter.control.ControlParameters;
import com.github.dsheirer.sdrplay.parameter.tuner.TunerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base control wrapper for an RSPduo Tuner 2
 */
public abstract class ControlRspDuoTuner2 extends ControlRspDuo<RspDuoTuner2> implements IControlRspDuoTuner2
{
    private Logger mLog = LoggerFactory.getLogger(ControlRspDuoTuner2.class);

    /**
     * Constructs an instance
     *
     * @param device for the device, obtained from the API
     */
    public ControlRspDuoTuner2(RspDuoDevice device)
    {
        super(device);
    }

    /**
     * Access tuner 2.
     * @return tuner
     * @throws SDRPlayException if the device is not started.
     */
    protected RspDuoTuner2 getTuner() throws SDRPlayException
    {
        if(hasDevice())
        {
            return (RspDuoTuner2) getDevice().getTuner();
        }

        throw new SDRPlayException("RSPduo device is not started");
    }

    @Override
    public TunerSelect getTunerSelect()
    {
        return TunerSelect.TUNER_2;
    }

    @Override
    protected ControlParameters getControlParameters()
    {
        return getDevice().getCompositeParameters().getControlBParameters();
    }

    @Override
    protected TunerParameters getTunerParameters()
    {
        return getDevice().getCompositeParameters().getTunerBParameters();
    }

    @Override
    public boolean isBiasT() throws SDRPlayException
    {
        if(hasDevice())
        {
            return getTuner().isBiasT();
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
            getTuner().setBiasT(enabled);
        }
        else
        {
            throw new SDRPlayException("Device is not initialized");
        }
    }
}
