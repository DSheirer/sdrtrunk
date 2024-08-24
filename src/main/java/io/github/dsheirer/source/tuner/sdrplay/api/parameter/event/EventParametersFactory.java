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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.event;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_EventParamsT;
import java.lang.foreign.MemorySegment;

/**
 * Event Parameters structure (sdrplay_api_EventParamsT)
 */
public class EventParametersFactory
{
    /**
     * Gain event callback parameters
     */
    public static GainCallbackParameters createGainCallbackParameters(MemorySegment eventParams)
    {
        return new GainCallbackParameters(sdrplay_api_EventParamsT.gainParams(eventParams));
    }

    /**
     * Power overload event callback parameters
     */
    public static PowerOverloadCallbackParameters createPowerOverloadCallbackParameters(MemorySegment eventParams)
    {
        return new PowerOverloadCallbackParameters(sdrplay_api_EventParamsT.powerOverloadParams(eventParams));
    }

    /**
     * RSPduo mode event callback parameters
     */
    public static RspDuoModeCallbackParameters createRspDuoModeCallbackParameters(MemorySegment eventParams)
    {
        return new RspDuoModeCallbackParameters(sdrplay_api_EventParamsT.rspDuoModeParams(eventParams));
    }
}
