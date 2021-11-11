/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.protocol.Protocol;

public class MPT1327DecodeEvent extends DecodeEvent
{
    /**
     * Constructs a MPT1327 decode event
     * @param start
     */
    public MPT1327DecodeEvent(long start)
    {
        super(start);
        setProtocol(Protocol.MPT1327);
    }

    /**
     * Creates a new decode event builder with the specified start timestamp.
     * @param timeStart for the event
     * @return builder
     */
    public static DecodeEventBuilder builder(long timeStart)
    {
        DecodeEventBuilder decodeEventBuilder = new DecodeEventBuilder(timeStart);
        decodeEventBuilder.protocol(Protocol.MPT1327);
        return decodeEventBuilder;
    }
}
