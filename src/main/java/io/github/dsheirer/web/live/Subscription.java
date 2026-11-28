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

package io.github.dsheirer.web.live;

import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.database.event.EventSearchCriteria;
import java.util.HashSet;
import java.util.Set;

/**
 * A client's subscription to a topic, with its filter.
 */
class Subscription
{
    private final int mId;
    private final Topic mTopic;
    private volatile Filter mFilter;
    private long mSequence;
    //Call keys of active calls that this subscription was told about, so it hears about their end even when a change
    //(for example a duplicate flag) makes them stop matching the filter.  Hub lock.
    private final Set<Long> mActiveCalls = new HashSet<>();
    //audio.live: calls whose audio this subscription is receiving, with the count of frames dropped.  Hub lock.
    private final java.util.Map<Long,int[]> mAudioCalls = new java.util.HashMap<>();
    //calls: when the next call.progress is due, per call key.  Hub lock.
    private final java.util.Map<Long,Long> mProgressDue = new java.util.HashMap<>();
    //spectrum: parameters of the subscription
    private volatile Object mParameters;

    Subscription(int id, Topic topic, Filter filter)
    {
        mId = id;
        mTopic = topic;
        mFilter = filter;
    }

    int getId()
    {
        return mId;
    }

    Topic getTopic()
    {
        return mTopic;
    }

    Filter getFilter()
    {
        return mFilter;
    }

    void setFilter(Filter filter)
    {
        mFilter = filter;
    }

    /**
     * Next sequence number for a message sent on this subscription.  Connection send lock.
     */
    long nextSequence()
    {
        return mSequence++;
    }

    Set<Long> getActiveCalls()
    {
        return mActiveCalls;
    }

    /**
     * audio.live: call key to [frames sent, frames dropped, dropped frames not yet reported]
     */
    java.util.Map<Long,int[]> getAudioCalls()
    {
        return mAudioCalls;
    }

    java.util.Map<Long,Long> getProgressDue()
    {
        return mProgressDue;
    }

    Object getParameters()
    {
        return mParameters;
    }

    void setParameters(Object parameters)
    {
        mParameters = parameters;
    }

    /**
     * Subscription parameters
     * @param calls filter for the calls topic, or null
     * @param includeDuplicates calls topic: true to include calls marked as duplicates
     * @param events filter for the events topic, or null
     * @param channelId channels.status topic: only this channel, or null for all
     * @param progressIntervalMs calls topic: milliseconds between call.progress messages per call, or 0 for none
     */
    record Filter(CallSearchCriteria calls, boolean includeDuplicates, EventSearchCriteria events, Integer channelId,
                  int progressIntervalMs)
    {
        static final int DEFAULT_PROGRESS_INTERVAL_MS = 1000;

        Filter(CallSearchCriteria calls, boolean includeDuplicates, EventSearchCriteria events, Integer channelId)
        {
            this(calls, includeDuplicates, events, channelId, DEFAULT_PROGRESS_INTERVAL_MS);
        }

        boolean matches(CallRecord call)
        {
            return (includeDuplicates || !call.isDuplicate()) && (calls == null || calls.matches(call));
        }

        boolean matches(EventRecord event)
        {
            return events == null || events.matches(event);
        }

        boolean matchesChannel(int id)
        {
            return channelId == null || channelId == id;
        }
    }
}
