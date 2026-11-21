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

package io.github.dsheirer.web.api;

import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.call.CallType;
import io.github.dsheirer.database.call.IdentifierFilter;
import io.github.dsheirer.database.event.EventCategory;
import io.github.dsheirer.database.event.EventSearchCriteria;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.protocol.Protocol;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Parses call and event filters from request parameters.  REST query parameters and WebSocket subscription parameters
 * use the same names and rules, so a client filters history and live data the same way.
 *
 * Call filters: to, from (text matched like the Calls tab search), toValue (exactly this talkgroup or radio, including
 * patched talkgroups), talkerAlias, protocol, type, system, since, until.
 * Event filters: talkgroup, radio (text), category, type, protocol, system, details, since, until.
 * Times are ISO-8601 or epoch milliseconds.
 */
public final class RecordFilters
{
    private RecordFilters()
    {
    }

    /**
     * Call filter
     * @param parameter reads a parameter by name, returning null when absent
     * @throws ApiException 400 listing every invalid parameter
     */
    public static CallSearchCriteria calls(Function<String,String> parameter)
    {
        List<ApiException.FieldError> errors = new ArrayList<>();
        Protocol protocol = parseEnum(parameter, "protocol", Protocol.class, errors);
        CallType type = parseEnum(parameter, "type", CallType.class, errors);
        long since = parseTime(parameter, "since", errors);
        long until = parseTime(parameter, "until", errors);
        IdentifierFilter to = text(parameter, "to");
        String toValue = clean(parameter.apply("toValue"));

        if(toValue != null)
        {
            Long value = CallSearchCriteria.parseValue(toValue);

            if(value == null)
            {
                errors.add(new ApiException.FieldError("toValue", "must be a talkgroup or radio number"));
            }
            else if(!to.isEmpty())
            {
                errors.add(new ApiException.FieldError("toValue", "can't be combined with to"));
            }
            else
            {
                to = IdentifierFilter.value(value);
            }
        }

        check(errors);
        return new CallSearchCriteria(to, text(parameter, "from"), clean(parameter.apply("talkerAlias")),
            protocol, type, clean(parameter.apply("system")), since, until);
    }

    /**
     * Event filter
     * @param parameter reads a parameter by name, returning null when absent
     * @throws ApiException 400 listing every invalid parameter
     */
    public static EventSearchCriteria events(Function<String,String> parameter)
    {
        List<ApiException.FieldError> errors = new ArrayList<>();
        EventCategory category = parseEnum(parameter, "category", EventCategory.class, errors);
        DecodeEventType type = parseEnum(parameter, "type", DecodeEventType.class, errors);
        Protocol protocol = parseEnum(parameter, "protocol", Protocol.class, errors);
        long since = parseTime(parameter, "since", errors);
        long until = parseTime(parameter, "until", errors);
        check(errors);
        return new EventSearchCriteria(text(parameter, "talkgroup"), text(parameter, "radio"), category, type,
            protocol, clean(parameter.apply("system")), clean(parameter.apply("details")), since, until);
    }

    /**
     * Boolean parameter: true or false, or the default when absent
     */
    public static boolean flag(Function<String,String> parameter, String name, boolean defaultValue)
    {
        String value = clean(parameter.apply(name));

        if(value == null)
        {
            return defaultValue;
        }

        return switch(value.toLowerCase(Locale.ROOT))
        {
            case "true" -> true;
            case "false" -> false;
            default -> throw ApiException.invalid(name, "must be true or false");
        };
    }

    private static IdentifierFilter text(Function<String,String> parameter, String name)
    {
        String value = clean(parameter.apply(name));
        return value == null ? IdentifierFilter.none() : IdentifierFilter.text(value);
    }

    private static <E extends Enum<E>> E parseEnum(Function<String,String> parameter, String name, Class<E> type,
                                                   List<ApiException.FieldError> errors)
    {
        String value = clean(parameter.apply(name));

        if(value == null)
        {
            return null;
        }

        try
        {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        }
        catch(IllegalArgumentException iae)
        {
            String values = Arrays.toString(type.getEnumConstants());
            errors.add(new ApiException.FieldError(name, "must be one of " + (values.length() > 300 ?
                values.substring(0, 300) + "...]" : values)));
            return null;
        }
    }

    private static long parseTime(Function<String,String> parameter, String name,
                                  List<ApiException.FieldError> errors)
    {
        String value = clean(parameter.apply(name));

        if(value == null)
        {
            return 0;
        }

        try
        {
            return Long.parseLong(value);
        }
        catch(NumberFormatException nfe)
        {
            try
            {
                return Instant.parse(value).toEpochMilli();
            }
            catch(DateTimeParseException dtpe)
            {
                errors.add(new ApiException.FieldError(name, "must be an ISO-8601 time or epoch milliseconds"));
                return 0;
            }
        }
    }

    private static String clean(String value)
    {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void check(List<ApiException.FieldError> errors)
    {
        if(!errors.isEmpty())
        {
            throw ApiException.invalid(errors);
        }
    }
}
