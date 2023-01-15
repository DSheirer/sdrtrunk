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
import io.github.dsheirer.source.tuner.sdrplay.api.DeviceSelectionMode;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.device.RspDuoDevice;
import java.util.EnumSet;

/**
 * RSPduo with tuner 1 configured as single tuner.
 */
public class ControlRspDuoTuner1Single extends ControlRspDuoTuner1
{
    /**
     * Constructs an instance
     *
     * @param device to control
     */
    public ControlRspDuoTuner1Single(RspDuoDevice device)
    {
        super(device);
    }

    @Override
    public DeviceSelectionMode getDeviceSelectionMode()
    {
        return DeviceSelectionMode.SINGLE_TUNER_1;
    }

    @Override
    public EnumSet<RspSampleRate> getSupportedSampleRates()
    {
        return RspSampleRate.SINGLE_TUNER_SAMPLE_RATES;
    }

    @Override
    public void setSampleRate(RspSampleRate sampleRate) throws SDRPlayException
    {
        if(sampleRate.isDualTunerSampleRate())
        {
            throw new SDRPlayException("Use single-tuner sample rates only");
        }

        super.setSampleRate(sampleRate);
    }
}
