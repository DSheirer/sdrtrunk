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
package io.github.dsheirer.database.call;

import io.github.dsheirer.protocol.Protocol;

import java.util.List;
import java.util.Locale;

/**
 * Search filter for the calls table.  Empty text fields, empty identifier filters and null values don't filter.
 *
 * @param to filter for the TO identifier (talkgroup, patch group or radio): typed text or a selected talkgroup alias
 * @param from filter for the FROM identifier (radio): typed text or a selected radio alias
 * @param talkerAlias text matched against the decoded talker alias
 * @param protocol to match or null
 * @param callType to match or null
 * @param system text matched against the system name
 * @param startTime earliest call start time (ms since epoch) or 0
 * @param endTime latest call start time (ms since epoch) or 0
 */
public record CallSearchCriteria(IdentifierFilter to, IdentifierFilter from, String talkerAlias, Protocol protocol,
                                 CallType callType, String system, long startTime, long endTime)
{
    /**
     * Criteria without a call type filter
     */
    public CallSearchCriteria(IdentifierFilter to, IdentifierFilter from, String talkerAlias, Protocol protocol,
                              String system, long startTime, long endTime)
    {
        this(to, from, talkerAlias, protocol, null, system, startTime, endTime);
    }

    public CallSearchCriteria
    {
        to = to == null ? IdentifierFilter.none() : to;
        from = from == null ? IdentifierFilter.none() : from;
        talkerAlias = clean(talkerAlias);
        system = clean(system);
    }

    /**
     * Criteria that match every call
     */
    public static CallSearchCriteria all()
    {
        return new CallSearchCriteria(null, null, null, null, null, 0, 0);
    }

    private static String clean(String value)
    {
        return value == null ? "" : value.trim();
    }

    /**
     * Indicates if these criteria match every call.
     */
    public boolean isEmpty()
    {
        return to.isEmpty() && from.isEmpty() && talkerAlias.isEmpty() && system.isEmpty() && protocol == null &&
            callType == null && startTime == 0 && endTime == 0;
    }

    /**
     * Parses the text as a decimal identifier value, or null if it isn't a number.
     */
    static Long parseValue(String text)
    {
        if(text == null || text.isEmpty() || text.length() > 18)
        {
            return null;
        }

        for(int x = 0; x < text.length(); x++)
        {
            if(!Character.isDigit(text.charAt(x)))
            {
                return null;
            }
        }

        return Long.parseLong(text);
    }

    /**
     * Evaluates the criteria against a call in memory, using the same rules as the database query.  This is used to
     * decide whether a live call belongs in the currently displayed search results.
     */
    public boolean matches(CallRecord call)
    {
        if(!to.isEmpty() && !to.matches(call, call.getToId(), call.getToValue(), call.getToFullyQualified(),
            call.isToTalkgroup(), call.getPatchedTalkgroups()))
        {
            return false;
        }

        if(!from.isEmpty() && !from.matches(call, call.getFromId(), call.getFromValue(),
            call.getFromFullyQualified(), call.isFromRadio(), List.of()))
        {
            return false;
        }

        if(!talkerAlias.isEmpty() && !contains(call.getTalkerAlias(), talkerAlias))
        {
            return false;
        }

        if(protocol != null && !protocol.name().equals(call.getProtocol()))
        {
            return false;
        }

        if(callType != null && call.getCallType() != callType)
        {
            return false;
        }

        if(!system.isEmpty() && !contains(call.getSystem(), system))
        {
            return false;
        }

        if(startTime > 0 && call.getStartTime() < startTime)
        {
            return false;
        }

        return endTime <= 0 || call.getStartTime() <= endTime;
    }

    private static boolean contains(String value, String text)
    {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }
}
