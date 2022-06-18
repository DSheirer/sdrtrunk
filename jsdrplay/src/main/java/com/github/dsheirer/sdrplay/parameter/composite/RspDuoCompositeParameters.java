/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package com.github.dsheirer.sdrplay.parameter.composite;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_DeviceParamsT;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_RxChannelParamsT;
import com.github.dsheirer.sdrplay.device.DeviceType;
import com.github.dsheirer.sdrplay.parameter.control.ControlParameters;
import com.github.dsheirer.sdrplay.parameter.device.RspDuoDeviceParameters;
import com.github.dsheirer.sdrplay.parameter.tuner.RspDuoTunerParameters;
import com.github.dsheirer.sdrplay.parameter.tuner.TunerParametersFactory;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

/**
 * RSPduo Composite parameters (device and two tuners)
 */
public class RspDuoCompositeParameters extends CompositeParameters<RspDuoDeviceParameters, RspDuoTunerParameters>
{
    private RspDuoTunerParameters mTunerBParameters;
    private ControlParameters mControlBParameters;

    /**
     * Constructs an instance from the foreign memory segment
     *
     * @param memorySegment for the composite structure in foreign memory
     * @param memorySession for allocating additional memory segments for the sub-structures.
     */
    public RspDuoCompositeParameters(MemorySegment memorySegment, MemorySession memorySession)
    {
        super(DeviceType.RSPduo, memorySegment, memorySession);

        MemoryAddress memoryAddressRxB = sdrplay_api_DeviceParamsT.rxChannelB$get(memorySegment);
        MemorySegment memorySegmentRxB = sdrplay_api_RxChannelParamsT.ofAddress(memoryAddressRxB, memorySession);
        mTunerBParameters = (RspDuoTunerParameters) TunerParametersFactory.create(DeviceType.RSPduo, memorySegmentRxB);

        MemorySegment tunerBControlParametersMemorySegment = sdrplay_api_RxChannelParamsT.ctrlParams$slice(memorySegmentRxB);
        mControlBParameters = new ControlParameters(tunerBControlParametersMemorySegment);
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
