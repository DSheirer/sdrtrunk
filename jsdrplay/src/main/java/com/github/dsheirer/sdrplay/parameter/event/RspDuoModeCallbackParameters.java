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

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_RspDuoModeCbParamT;

import java.lang.foreign.MemorySegment;

/**
 * RSP-Duo Mode Callback Parameters (sdrplay_api_RspDuoModeCbParamT)
 */
public class RspDuoModeCallbackParameters
{
    private RspDuoModeEventType mRspDuoModeEventType;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public RspDuoModeCallbackParameters(MemorySegment memorySegment)
    {
        mRspDuoModeEventType = RspDuoModeEventType.fromValue(sdrplay_api_RspDuoModeCbParamT.modeChangeType$get(memorySegment));
    }

    /**
     * Event type
     */
    public RspDuoModeEventType getRspDuoModeEvent()
    {
        return mRspDuoModeEventType;
    }
}
