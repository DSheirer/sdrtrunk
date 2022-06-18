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

package com.github.dsheirer.sdrplay.parameter.event;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_EventParamsT;

import java.lang.foreign.MemorySegment;

/**
 * Event Parameters structure (sdrplay_api_EventParamsT)
 */
public class EventParametersFactory
{
    private GainCallbackParameters mGainCallbackParameters;
    private PowerOverloadCallbackParameters mPowerOverloadCallbackParameters;
    private RspDuoModeCallbackParameters mRspDuoModeCallbackParameters;

    /**
     * Gain event callback parameters
     */
    public static GainCallbackParameters createGainCallbackParameters(MemorySegment memorySegment)
    {
        return new GainCallbackParameters(sdrplay_api_EventParamsT.gainParams$slice(memorySegment));
    }

    /**
     * Power overload event callback parameters
     */
    public static PowerOverloadCallbackParameters createPowerOverloadCallbackParameters(MemorySegment memorySegment)
    {
        return new PowerOverloadCallbackParameters(sdrplay_api_EventParamsT.powerOverloadParams$slice(memorySegment));
    }

    /**
     * RSPduo mode event callback parameters
     */
    public static RspDuoModeCallbackParameters createRspDuoModeCallbackParameters(MemorySegment memorySegment)
    {
        return new RspDuoModeCallbackParameters(sdrplay_api_EventParamsT.rspDuoModeParams$slice(memorySegment));
    }
}
