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

package com.github.dsheirer.sdrplay.parameter.tuner;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_RxChannelParamsT;
import com.github.dsheirer.sdrplay.device.DeviceType;

import java.lang.foreign.MemorySegment;

/**
 * Factory for creating tuner parameters structures
 */
public class TunerParametersFactory
{
    /**
     * Creats a tuner parameters instance for the specified device type
     * @param deviceType to create
     * @param memorySegment for the sdrplay_api_RxChannelParamsT structure
     * @return tuner parameters
     */
    public static TunerParameters create(DeviceType deviceType, MemorySegment memorySegment)
    {
        MemorySegment tunerParametersMemorySegment = sdrplay_api_RxChannelParamsT.tunerParams$slice(memorySegment);

        switch(deviceType)
        {
            case RSP1 -> {
                return new Rsp1TunerParameters(tunerParametersMemorySegment);
            }
            case RSP1A -> {
                MemorySegment rsp1AMemorySegment = sdrplay_api_RxChannelParamsT.rsp1aTunerParams$slice(memorySegment);
                return new Rsp1aTunerParameters(memorySegment, rsp1AMemorySegment);
            }
            case RSP2 -> {
                MemorySegment rsp2MemorySegment = sdrplay_api_RxChannelParamsT.rsp2TunerParams$slice(memorySegment);
                return new Rsp2TunerParameters(memorySegment, rsp2MemorySegment);
            }
            case RSPduo -> {
                MemorySegment rspDuoMemorySegment = sdrplay_api_RxChannelParamsT.rspDuoTunerParams$slice(memorySegment);
                return new RspDuoTunerParameters(memorySegment, rspDuoMemorySegment);
            }
            case RSPdx -> {
                MemorySegment rspDxMemorySegment = sdrplay_api_RxChannelParamsT.rspDxTunerParams$slice(memorySegment);
                return new RspDxTunerParameters(memorySegment, rspDxMemorySegment);
            }
            default -> throw new IllegalArgumentException("Unrecognized device type: " + deviceType);
        }
    }
}
