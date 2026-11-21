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

package io.github.dsheirer.database.streaming;

import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import java.util.Locale;
import java.util.Set;

/**
 * Stream activity search criteria.  Empty values match everything.
 * @param streamName to match exactly, or null for all streams
 * @param statuses to match, or empty for all statuses
 * @param text to find (case-insensitive) in the to, from, selecting aliases, detail, stream, alias list, system or
 * channel, or empty
 */
public record StreamActivitySearchCriteria(String streamName, Set<StreamStatus> statuses, String text)
{
    public StreamActivitySearchCriteria
    {
        statuses = statuses != null ? Set.copyOf(statuses) : Set.of();
        text = text != null ? text.trim() : "";
    }

    /**
     * Criteria that match every row
     */
    public static StreamActivitySearchCriteria all()
    {
        return new StreamActivitySearchCriteria(null, Set.of(), "");
    }

    public boolean isEmpty()
    {
        return streamName == null && statuses.isEmpty() && text.isEmpty();
    }

    /**
     * Indicates if the live row matches these criteria, the same way that the database search does.
     */
    public boolean matches(StreamActivity activity)
    {
        if(streamName != null && !streamName.equals(activity.getStreamName()))
        {
            return false;
        }

        if(!statuses.isEmpty() && !statuses.contains(activity.getStatus()))
        {
            return false;
        }

        if(text.isEmpty())
        {
            return true;
        }

        String search = text.toLowerCase(Locale.ROOT);

        for(String value: new String[]{activity.getTo(), activity.getFrom(), activity.getSelectingAliasText(),
            activity.getDetail(), activity.getStreamName(), activity.getAliasList(), activity.getSystem(),
            activity.getChannel()})
        {
            if(value != null && value.toLowerCase(Locale.ROOT).contains(search))
            {
                return true;
            }
        }

        return false;
    }
}
