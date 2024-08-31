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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

import io.github.dsheirer.source.tuner.sdrplay.api.Version;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceType;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_RxChannelParamsT;
import java.lang.foreign.MemorySegment;

/**
 * Factory for creating tuner parameters structures
 */
public class TunerParametersFactory
{
    /**
     * Creats a tuner parameters instance for the specified device type
     * @param deviceType to create
     * @param rxChannelParams sdrplay_api_RxChannelParamsT structure
     * @return tuner parameters
     */
    public static TunerParameters create(Version version, DeviceType deviceType, MemorySegment rxChannelParams)
    {
        MemorySegment tunerParams = sdrplay_api_RxChannelParamsT.tunerParams(rxChannelParams);

        switch(deviceType)
        {
            case RSP1 -> {
                return new Rsp1TunerParameters(tunerParams);
            }
            //RSP1A and RSP1B share the same tuner parameters structures
            case RSP1A, RSP1B -> {
                MemorySegment rsp1aTunerParams = sdrplay_api_RxChannelParamsT.rsp1aTunerParams(rxChannelParams);
                return new Rsp1aTunerParameters(rxChannelParams, rsp1aTunerParams);
            }
            case RSP2 -> {
                MemorySegment rsp2TunerParams = sdrplay_api_RxChannelParamsT.rsp2TunerParams(rxChannelParams);
                return new Rsp2TunerParameters(rxChannelParams, rsp2TunerParams);
            }
            case RSPduo -> {
                if(version.gte(Version.V3_14))
                {
                    MemorySegment rspDuoTunerParams = io.github.dsheirer.source.tuner.sdrplay.api.v3_15.sdrplay_api_RxChannelParamsT.rspDuoTunerParams(rxChannelParams);
                    return new RspDuoTunerParametersV3_14(rxChannelParams, rspDuoTunerParams);
                }
                else if(version.gte(Version.V3_07))
                {
                    MemorySegment rspDuoTunerParams = sdrplay_api_RxChannelParamsT.rspDuoTunerParams(rxChannelParams);
                    return new RspDuoTunerParametersV3_07(rxChannelParams, rspDuoTunerParams);
                }
                else
                {
                    throw new IllegalArgumentException("Unrecognized API version: " + version);
                }
            }
            case RSPdx, RSPdxR2 -> {
                MemorySegment rspDxTunerParams = sdrplay_api_RxChannelParamsT.rspDxTunerParams(rxChannelParams);
                return new RspDxTunerParameters(rxChannelParams, rspDxTunerParams);
            }
            default -> throw new IllegalArgumentException("Unrecognized device type: " + deviceType);
        }
    }
}
