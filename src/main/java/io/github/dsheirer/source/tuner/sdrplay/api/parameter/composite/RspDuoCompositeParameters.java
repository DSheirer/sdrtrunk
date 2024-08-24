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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite;

import io.github.dsheirer.source.tuner.sdrplay.api.Version;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceType;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.ControlParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.RspDuoDeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDuoTunerParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.TunerParametersFactory;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DeviceParamsT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_RxChannelParamsT;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * RSPduo Composite parameters (device and two tuners)
 */
public class RspDuoCompositeParameters extends CompositeParameters<RspDuoDeviceParameters, RspDuoTunerParameters>
{
    private final RspDuoTunerParameters mTunerBParameters;
    private final ControlParameters mControlBParameters;

    /**
     * Constructs an instance from the foreign memory segment
     *
     * @param version for constructing the correct version structure.
     * @param deviceParams structure in foreign memory
     * @param arena for allocating additional memory segments for the sub-structures.
     */
    public RspDuoCompositeParameters(Version version, MemorySegment deviceParams, Arena arena)
    {
        super(version, DeviceType.RSPduo, deviceParams, arena);

        MemorySegment addressRxChannelB = sdrplay_api_DeviceParamsT.rxChannelB(deviceParams);
        MemorySegment rxChannelB = addressRxChannelB.reinterpret(sdrplay_api_RxChannelParamsT.sizeof(), arena, null);
        mTunerBParameters = (RspDuoTunerParameters) TunerParametersFactory.create(version, DeviceType.RSPduo, rxChannelB);

        MemorySegment rxChannelBCtrlParams = sdrplay_api_RxChannelParamsT.ctrlParams(rxChannelB);
        mControlBParameters = new ControlParameters(rxChannelBCtrlParams);
    }

    /**
     * Tuner B Tuner Parameters
     */
    public RspDuoTunerParameters getTunerBParameters()
    {
        return mTunerBParameters;
    }

    /**
     * Tuner B Control Parameters
     */
    public ControlParameters getControlBParameters()
    {
        return mControlBParameters;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Device Composite Parameters\n");
        sb.append("\tDevice Parameters:\n").append(getDeviceParameters()).append("\n");
        sb.append("\tTuner Channel A:\n").append(getTunerAParameters()).append("\n");
        sb.append("\tTuner Channel B:\n").append(getTunerBParameters()).append("\n");
        return sb.toString();
    }
}
