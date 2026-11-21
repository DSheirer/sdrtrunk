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

import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Event categories for filtering, using the same groupings of decode event types as the Now Playing event filters.
 */
public enum EventCategory
{
    VOICE_CALL("Voice Call", DecodeEventType.VOICE_CALLS),
    ENCRYPTED_CALL("Encrypted Call", DecodeEventType.VOICE_CALLS_ENCRYPTED),
    DATA("Data", DecodeEventType.DATA_CALLS),
    COMMAND("Command", DecodeEventType.COMMANDS),
    REGISTRATION("Registration", DecodeEventType.REGISTRATION),
    OTHER("Other", DecodeEventType.OTHERS);

    private final String mLabel;
    private final Set<DecodeEventType> mTypes;

    EventCategory(String label, EnumSet<DecodeEventType> types)
    {
        mLabel = label;
        mTypes = EnumSet.copyOf(types);
    }

    /**
     * Event types in this category
     */
    public Set<DecodeEventType> getTypes()
    {
        return mTypes;
    }

    /**
     * Indicates if the event type belongs to this category
     */
    public boolean contains(DecodeEventType type)
    {
        return mTypes.contains(type);
    }

    /**
     * Category of the event type
     */
    public static EventCategory from(DecodeEventType type)
    {
        for(EventCategory category: values())
        {
            if(category.contains(type))
            {
                return category;
            }
        }

        return OTHER;
    }

    /**
     * SQL condition that matches the events in this category.  The values are enumeration names, so they are safe
     * to include in the statement.
     */
    public String getWhereClause()
    {
        return "event_type IN (" + mTypes.stream().map(type -> "'" + type.name() + "'").sorted()
            .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
