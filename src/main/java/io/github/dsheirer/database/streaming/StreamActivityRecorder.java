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
import io.github.dsheirer.audio.broadcast.activity.StreamActivityModel;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.sample.Listener;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the stream activity rows in the database: each new row and each status change is queued with the database
 * manager, which writes the newest snapshot of each row in the background.
 *
 * Rows that an earlier run of the application left pending or in progress are marked not sent when the database
 * opens.
 */
public class StreamActivityRecorder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamActivityRecorder.class);
    public static final String INTERRUPTED_DETAIL = "sdrtrunk stopped before the stream was done with the call";
    private static final int MAX_HANDLES = 20_000;

    private final DatabaseManager mDatabaseManager;
    private final StreamActivityModel mModel;
    private final long mStartTime = System.currentTimeMillis();
    /**
     * Database row handle by row id, for the rows that can still change.  Oldest are dropped.
     */
    private final Map<Long,DatabaseManager.RowHandle> mHandles = new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long,DatabaseManager.RowHandle> eldest)
        {
            return size() > MAX_HANDLES;
        }
    };
    private final Listener<StreamActivity> mActivityListener = this::receive;
    private final Listener<DatabaseEvent> mDatabaseListener = this::receive;

    /**
     * Constructs an instance
     * @param databaseManager to store the rows
     * @param model with the rows
     */
    public StreamActivityRecorder(DatabaseManager databaseManager, StreamActivityModel model)
    {
        mDatabaseManager = databaseManager;
        mModel = model;
    }

    public void start()
    {
        mModel.addListener(mActivityListener);
        mDatabaseManager.addListener(mDatabaseListener);

        if(mDatabaseManager.isConnected())
        {
            markInterrupted();
        }
    }

    public void stop()
    {
        mModel.removeListener(mActivityListener);
        mDatabaseManager.removeListener(mDatabaseListener);
    }

    /**
     * New or updated row
     */
    private void receive(StreamActivity activity)
    {
        if(activity == null)
        {
            return;
        }

        DatabaseManager.RowHandle handle;

        synchronized(mHandles)
        {
            handle = mHandles.computeIfAbsent(activity.getId(), id -> mDatabaseManager.createHandle());

            //A final status doesn't change again
            if(activity.getStatus().isFinal())
            {
                mHandles.remove(activity.getId());
            }
        }

        mDatabaseManager.save(handle, toRecord(activity));
    }

    private void receive(DatabaseEvent event)
    {
        if(event.type() == DatabaseEvent.Type.CONNECTED)
        {
            markInterrupted();
        }
    }

    /**
     * Marks the rows that earlier runs left pending or in progress as not sent.
     */
    private void markInterrupted()
    {
        mDatabaseManager.submit(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getStreamActivityRepository().markInterrupted(connection, mStartTime,
                    INTERRUPTED_DETAIL);
            }
        }).whenComplete((count, error) -> {
            if(error != null)
            {
                LOGGER.warn("Unable to update the stream activity of an earlier run - " + error.getMessage());
            }
            else if(count != null && count > 0)
            {
                mDatabaseManager.notifyStreamActivityChanged();
            }
        });
    }

    /**
     * Database snapshot of the row
     */
    public static StreamActivityRecord toRecord(StreamActivity activity)
    {
        StreamActivityRecord record = new StreamActivityRecord();
        record.setCallKey(activity.getCallKey());
        record.setStreamName(activity.getStreamName());
        record.setServerType(activity.getServerType());
        record.setCallStart(activity.getCallStart());
        record.setDuration(activity.getDuration());
        record.setTo(activity.getTo());
        record.setFrom(activity.getFrom());
        record.setAliasList(activity.getAliasList());
        record.setSelectingAliases(activity.getSelectingAliases());
        record.setProtocol(activity.getProtocol());
        record.setSystem(activity.getSystem());
        record.setSite(activity.getSite());
        record.setChannel(activity.getChannel());
        record.setStatus(activity.getStatus());
        record.setDetail(activity.getDetail());
        record.setStatusTime(activity.getStatusTime());
        return record;
    }

    /**
     * Row for a stored record
     */
    public static StreamActivity toActivity(StreamActivityRecord record)
    {
        StreamActivity.Builder call = new StreamActivity.Builder()
            .callKey(record.getCallKey())
            .callStart(record.getCallStart())
            .duration(record.getDuration())
            .to(record.getTo())
            .from(record.getFrom())
            .aliasList(record.getAliasList())
            .selectingAliases(record.getSelectingAliases())
            .protocol(record.getProtocol())
            .system(record.getSystem())
            .site(record.getSite())
            .channel(record.getChannel());
        return StreamActivity.restore(call, record.getStreamName(), record.getServerType(), record.getStatus(),
            record.getDetail(), record.getStatusTime());
    }
}
