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

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import java.util.Collection;
import java.util.function.Function;

/**
 * This is used as a base for {@link IDecodeEvent} types.
 * This will take a list of {@link IDecodeEvent}s and generate filters for the list.
 * This will default canProcess to TRUE. Override if other value or evaluation is needed.
 */
public class EventFilter extends Filter<IDecodeEvent,DecodeEventType>
{
    private EventKeyExtractor mKeyExtractor = new EventKeyExtractor();

    /**
     * Constructs an instance
     * @param name of this filter
     * @param decodeEventTypes to filter against
     */
    public EventFilter(String name, Collection<DecodeEventType> decodeEventTypes)
    {
        super(name);

        for (DecodeEventType decodeEventType : decodeEventTypes)
        {
            add(new FilterElement<>(decodeEventType));
        }
    }

    /**
     * Key extractor function
     * @return function.
     */
    @Override
    public Function<IDecodeEvent, DecodeEventType> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor function for decode events to extract the decode event type
     */
    public static class EventKeyExtractor implements Function<IDecodeEvent,DecodeEventType>
    {
        @Override
        public DecodeEventType apply(IDecodeEvent iDecodeEvent)
        {
            return iDecodeEvent.getEventType();
        }
    }
}
