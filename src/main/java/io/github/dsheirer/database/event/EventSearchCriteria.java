/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.database.event;

import io.github.dsheirer.database.call.IdentifierFilter;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;
import java.util.Locale;

/**
 * Search criteria for the events table.  Empty or null values don't filter.
 *
 * @param talkgroup filter for the TO talkgroup or patch group, including the talkgroups patched into a patch group
 * @param radio filter for a radio identifier in either the FROM or the TO role (e.g. a unit-to-unit call or a page)
 * @param category of the event type, or null for any category
 * @param eventType decode event type, or null for any type
 * @param protocol or null for any protocol
 * @param system text that the system name contains
 * @param details text that the event details contain
 * @param startTime earliest event start time, or 0
 * @param endTime latest event start time, or 0
 */
public record EventSearchCriteria(IdentifierFilter talkgroup, IdentifierFilter radio, EventCategory category,
                                  DecodeEventType eventType, Protocol protocol, String system, String details,
                                  long startTime, long endTime)
{
    public EventSearchCriteria
    {
        talkgroup = talkgroup == null ? IdentifierFilter.none() : talkgroup;
        radio = radio == null ? IdentifierFilter.none() : radio;
        system = clean(system);
        details = clean(details);
    }

    /**
     * Criteria that match all events
     */
    public static EventSearchCriteria all()
    {
        return new EventSearchCriteria(null, null, null, null, null, null, null, 0, 0);
    }

    private static String clean(String value)
    {
        return value == null ? "" : value.trim();
    }

    /**
     * Indicates if these criteria match all events
     */
    public boolean isEmpty()
    {
        return talkgroup.isEmpty() && radio.isEmpty() && category == null && eventType == null && protocol == null &&
            system.isEmpty() && details.isEmpty() && startTime == 0 && endTime == 0;
    }

    /**
     * Indicates if the event matches these criteria, for live events that aren't in the database yet.  Matches the
     * same events as the database search.
     */
    public boolean matches(EventRecord event)
    {
        if(!talkgroup.isEmpty() && !talkgroup.matches(event, event.getToId(), event.getToValue(),
            event.getToFullyQualified(), event.isToTalkgroup(), event.getPatchedTalkgroups()))
        {
            return false;
        }

        if(!radio.isEmpty() && !radio.matches(event, event.getFromId(), event.getFromValue(),
            event.getFromFullyQualified(), event.isFromRadio(), List.of()) && !(event.isToRadio() &&
            radio.matches(event, event.getToId(), event.getToValue(), event.getToFullyQualified(), true, List.of())))
        {
            return false;
        }

        if(category != null && !category.contains(event.getEventTypeValue()))
        {
            return false;
        }

        if(eventType != null && !eventType.name().equals(event.getEventType()))
        {
            return false;
        }

        if(protocol != null && !protocol.name().equals(event.getProtocol()))
        {
            return false;
        }

        if(!system.isEmpty() && !contains(event.getSystem(), system))
        {
            return false;
        }

        if(!details.isEmpty() && !contains(event.getDetails(), details))
        {
            return false;
        }

        if(startTime > 0 && event.getStartTime() < startTime)
        {
            return false;
        }

        return endTime <= 0 || event.getStartTime() <= endTime;
    }

    private static boolean contains(String value, String text)
    {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }
}
