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

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_PowerOverloadCbParamT;
import java.lang.foreign.MemorySegment;

/**
 * Power Overload Callback Parameters (sdrplay_api_PowerOverloadCbParamT)
 */
public class PowerOverloadCallbackParameters
{
    private final PowerOverloadEventType mPowerOverloadEventType;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public PowerOverloadCallbackParameters(MemorySegment powerOverloadCbParam)
    {
        mPowerOverloadEventType = PowerOverloadEventType
                .fromValue(sdrplay_api_PowerOverloadCbParamT.powerOverloadChangeType(powerOverloadCbParam));
    }

    /**
     * Event type
     */
    public PowerOverloadEventType getPowerOverloadEvent()
    {
        return mPowerOverloadEventType;
    }
}
