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

package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEventType;

/**
 * Event filter for decode event types that are not specified in other event filters.
 *
 * This is intended as a catch-all for any elements of the DecodeEventType enumeration that may get added in the
 * future and are not deliberately added to the various event filter groupings listed in the enumeration.
 */
public class AllOtherEventFilter extends EventFilter
{
    /**
     * Constructor
     */
    public AllOtherEventFilter()
    {
        super("Other", DecodeEventType.OTHERS);
    }
}
