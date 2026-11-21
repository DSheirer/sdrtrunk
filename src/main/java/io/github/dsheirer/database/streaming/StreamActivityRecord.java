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

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.database.DatabaseRecord;
import java.util.List;

/**
 * Database row of one call that was selected for one stream: a snapshot of a {@link StreamActivity}.
 */
public class StreamActivityRecord implements DatabaseRecord
{
    private long mId;
    private long mCallKey;
    private String mStreamName;
    private BroadcastServerType mServerType;
    private long mCallStart;
    private long mDuration;
    private String mTo;
    private String mFrom;
    private String mAliasList;
    private List<StreamActivity.SelectingAlias> mSelectingAliases = List.of();
    private String mProtocol;
    private String mSystem;
    private String mSite;
    private String mChannel;
    private StreamStatus mStatus = StreamStatus.PENDING;
    private String mDetail;
    private long mStatusTime;

    @Override
    public long getId()
    {
        return mId;
    }

    @Override
    public void setId(long id)
    {
        mId = id;
    }

    /**
     * Audio segment id of the call, which is the call key of the call in the calls table.  Unique within one run of
     * the application only.
     */
    public long getCallKey()
    {
        return mCallKey;
    }

    public void setCallKey(long callKey)
    {
        mCallKey = callKey;
    }

    public String getStreamName()
    {
        return mStreamName;
    }

    public void setStreamName(String streamName)
    {
        mStreamName = streamName;
    }

    /**
     * Server type of the stream, or null when there's no such stream
     */
    public BroadcastServerType getServerType()
    {
        return mServerType;
    }

    public void setServerType(BroadcastServerType serverType)
    {
        mServerType = serverType;
    }

    public long getCallStart()
    {
        return mCallStart;
    }

    public void setCallStart(long callStart)
    {
        mCallStart = callStart;
    }

    /**
     * Call audio duration in milliseconds
     */
    public long getDuration()
    {
        return mDuration;
    }

    public void setDuration(long duration)
    {
        mDuration = duration;
    }

    public String getTo()
    {
        return mTo;
    }

    public void setTo(String to)
    {
        mTo = to;
    }

    public String getFrom()
    {
        return mFrom;
    }

    public void setFrom(String from)
    {
        mFrom = from;
    }

    public String getAliasList()
    {
        return mAliasList;
    }

    public void setAliasList(String aliasList)
    {
        mAliasList = aliasList;
    }

    public List<StreamActivity.SelectingAlias> getSelectingAliases()
    {
        return mSelectingAliases;
    }

    public void setSelectingAliases(List<StreamActivity.SelectingAlias> selectingAliases)
    {
        mSelectingAliases = selectingAliases != null ? List.copyOf(selectingAliases) : List.of();
    }

    public String getProtocol()
    {
        return mProtocol;
    }

    public void setProtocol(String protocol)
    {
        mProtocol = protocol;
    }

    public String getSystem()
    {
        return mSystem;
    }

    public void setSystem(String system)
    {
        mSystem = system;
    }

    public String getSite()
    {
        return mSite;
    }

    public void setSite(String site)
    {
        mSite = site;
    }

    public String getChannel()
    {
        return mChannel;
    }

    public void setChannel(String channel)
    {
        mChannel = channel;
    }

    public StreamStatus getStatus()
    {
        return mStatus;
    }

    public void setStatus(StreamStatus status)
    {
        mStatus = status != null ? status : StreamStatus.PENDING;
    }

    public String getDetail()
    {
        return mDetail;
    }

    public void setDetail(String detail)
    {
        mDetail = detail;
    }

    /**
     * Time of the last status change
     */
    public long getStatusTime()
    {
        return mStatusTime;
    }

    public void setStatusTime(long statusTime)
    {
        mStatusTime = statusTime;
    }
}
