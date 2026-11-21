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

package io.github.dsheirer.audio.broadcast.activity;

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Recent calls selected for streaming, one row per call and stream, with the outcome of each.  Kept in memory for
 * the session, newest last, up to a maximum number of rows.  The database keeps them across sessions (see
 * StreamActivityRecorder).  Thread safe: rows are added and updated by the
 * streaming threads.
 */
public class StreamActivityModel
{
    public static final int DEFAULT_MAXIMUM_ROWS = 5000;
    private final Deque<StreamActivity> mActivities = new ArrayDeque<>();
    private final Broadcaster<StreamActivity> mBroadcaster = new Broadcaster<>();
    private final AtomicLong mNextId = new AtomicLong(1);
    private final int mMaximumRows;

    public StreamActivityModel()
    {
        this(DEFAULT_MAXIMUM_ROWS);
    }

    /**
     * Constructs an instance
     * @param maximumRows to keep, oldest are dropped
     */
    public StreamActivityModel(int maximumRows)
    {
        mMaximumRows = Math.max(1, maximumRows);
    }

    /**
     * Adds a row for the call and stream.
     * @param call details
     * @param streamName of the stream
     * @param serverType of the stream, or null if there's no such stream
     * @param status initial status
     * @param detail for the status, or null
     * @return the row
     */
    public StreamActivity add(StreamActivity.Builder call, String streamName, BroadcastServerType serverType,
                              StreamStatus status, String detail)
    {
        StreamActivity activity = new StreamActivity(mNextId.getAndIncrement(), call, streamName, status, detail);
        activity.setServerType(serverType);

        synchronized(mActivities)
        {
            mActivities.addLast(activity);

            while(mActivities.size() > mMaximumRows)
            {
                mActivities.removeFirst();
            }

        }

        mBroadcaster.broadcast(activity);
        return activity;
    }

    /**
     * Updates the status of a row and notifies the listeners.  A row that has a final status keeps it.
     */
    public void update(StreamActivity activity, StreamStatus status, String detail)
    {
        if(activity != null && activity.setStatus(status, detail))
        {
            mBroadcaster.broadcast(activity);
        }
    }

    /**
     * Sets the server type of the stream that the row was sent to.
     */
    public void setServerType(StreamActivity activity, BroadcastServerType serverType)
    {
        if(activity != null && activity.setServerType(serverType))
        {
            mBroadcaster.broadcast(activity);
        }
    }

    /**
     * Rows, oldest first
     */
    public List<StreamActivity> getActivities()
    {
        synchronized(mActivities)
        {
            return new ArrayList<>(mActivities);
        }
    }

    /**
     * Adds a listener for added and updated rows.
     */
    public void addListener(Listener<StreamActivity> listener)
    {
        mBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<StreamActivity> listener)
    {
        mBroadcaster.removeListener(listener);
    }
}
