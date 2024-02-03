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
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.DeviceParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.device.DeviceParametersFactory;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.TunerParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.TunerParametersFactory;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DevParamsT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DeviceParamsT;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_RxChannelParamsT;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Composite Device Parameters structure (sdrplay_api_DeviceParamsT) providing access to the device parameters and
 * the tuner 1 parameters.  Tuner 2 parameters are only accessible via the RSPduo sub-class implementation.
 *
 * Note: sub-class implementations will constrain access to the appropriate sub-structures of the DeviceParamsT
 * structure for each specific device.
 */
public class CompositeParameters<D extends DeviceParameters, T extends TunerParameters>
{
    private D mDeviceParameters;
    private T mTunerAParameters;
    private ControlParameters mControlAParameters;

    /**
     * Constructs an instance from the foreign memory segment
     *
     * @param version of the API
     * @param deviceType to create
     * @param memorySegment for the composite structure in foreign memory
     * @param arena for allocating additional memory segments for the sub-structures.
     */
    public CompositeParameters(Version version, DeviceType deviceType, MemorySegment memorySegment, Arena arena)
    {
        MemorySegment parametersMemoryAddress = sdrplay_api_DeviceParamsT.devParams$get(memorySegment);
        MemorySegment parametersMemorySegment = sdrplay_api_DevParamsT.ofAddress(parametersMemoryAddress, arena.scope());
        mDeviceParameters = (D) DeviceParametersFactory.create(deviceType, parametersMemorySegment);

        MemorySegment memoryAddressRxA = sdrplay_api_DeviceParamsT.rxChannelA$get(memorySegment);
        MemorySegment memorySegmentRxA = sdrplay_api_RxChannelParamsT.ofAddress(memoryAddressRxA, arena.scope());
        mTunerAParameters = (T) TunerParametersFactory.create(version, deviceType, memorySegmentRxA);

        MemorySegment tunerAControlParametersMemorySegment = sdrplay_api_RxChannelParamsT.ctrlParams$slice(memorySegmentRxA);
        mControlAParameters = new ControlParameters(tunerAControlParametersMemorySegment);
    }

    /**
     * Device parameters
     */
    public D getDeviceParameters()
    {
        return mDeviceParameters;
    }

    /**
     * Tuner A Parameters.
     *
     * Note: this is normally mapped to tuner 1.  In the RSPduo, this is mapped to Tuner 1 or Tuner 2, according to how
     * the user has setup the TunerSelect.
     */
    public T getTunerAParameters()
    {
        return mTunerAParameters;
    }

    /**
     * Tuner A Control Parameters
     *
     * Note: this is normally mapped to tuner 1.  In the RSPduo, this is mapped to Tuner 1 or Tuner 2, according to how
     * the user has setup the TunerSelect.
     */
    public ControlParameters getControlAParameters()
    {
        return mControlAParameters;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Composite Parameters\n");
        sb.append("\tDevice Parameters:\n").append(getDeviceParameters()).append("\n");
        sb.append("\tTuner A Parameters:\n").append(getTunerAParameters()).append("\n");
        return sb.toString();
    }
}
