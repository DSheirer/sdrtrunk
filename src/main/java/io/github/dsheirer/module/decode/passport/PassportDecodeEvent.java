/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.passport;

import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;

/**
 * Passport decode event
 */
public class PassportDecodeEvent extends DecodeEvent
{
    /**
     * Constructs a Passport decode event
     * @param start
     */
    public PassportDecodeEvent(DecodeEventType decodeEventType, long start)
    {
        super(decodeEventType, start);
        setProtocol(Protocol.PASSPORT);
    }

    /**
     * Creates a new decode event builder with the specified start timestamp.
     * @param timeStart for the event
     * @return builder
     */
    public static DecodeEventBuilder builder(DecodeEventType decodeEventType, long timeStart)
    {
        DecodeEventBuilder decodeEventBuilder = new DecodeEventBuilder(decodeEventType, timeStart);
        decodeEventBuilder.protocol(Protocol.PASSPORT);
        return decodeEventBuilder;
    }
}
