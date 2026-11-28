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
import java.util.List;
import java.util.stream.Collectors;

/**
 * One call that was selected for one stream, the aliases that selected it and what the stream did with it.
 *
 * The call details don't change.  The status, detail and server type change as the stream handles the call; they are
 * set through the model so that its listeners hear about the change.  Thread safe.
 */
public class StreamActivity
{
    private final long mId;
    private final long mCallKey;
    private final String mStreamName;
    private final long mCallStart;
    private final long mDuration;
    private final String mTo;
    private final String mFrom;
    private final String mAliasList;
    private final List<SelectingAlias> mSelectingAliases;
    private final String mProtocol;
    private final String mSystem;
    private final String mSite;
    private final String mChannel;
    private volatile BroadcastServerType mServerType;
    private volatile StreamStatus mStatus;
    private volatile String mDetail;
    private volatile long mStatusTime;

    /**
     * An alias of an identifier in the call that has the stream.
     * @param alias name
     * @param identifier that the alias matched, e.g. To 1234 or From 5678
     */
    public record SelectingAlias(String alias, String identifier)
    {
        @Override
        public String toString()
        {
            return identifier != null && !identifier.isEmpty() ? alias + " (" + identifier + ")" : alias;
        }
    }

    /**
     * Creates a row for a call that was stored in the database (e.g. by an earlier run of the application).
     * @param call details
     * @param streamName of the stream
     * @param serverType of the stream, or null
     * @param status of the call for the stream
     * @param detail of the status, or null
     * @param statusTime of the last status change
     * @return row with an id of 0
     */
    public static StreamActivity restore(Builder call, String streamName, BroadcastServerType serverType,
                                         StreamStatus status, String detail, long statusTime)
    {
        StreamActivity activity = new StreamActivity(0, call, streamName, status, detail);
        activity.mServerType = serverType;
        activity.mStatusTime = statusTime;
        return activity;
    }

    StreamActivity(long id, Builder builder, String streamName, StreamStatus status, String detail)
    {
        mId = id;
        mCallKey = builder.mCallKey;
        mStreamName = streamName;
        mCallStart = builder.mCallStart;
        mDuration = builder.mDuration;
        mTo = builder.mTo;
        mFrom = builder.mFrom;
        mAliasList = builder.mAliasList;
        mSelectingAliases = List.copyOf(builder.mSelectingAliases);
        mProtocol = builder.mProtocol;
        mSystem = builder.mSystem;
        mSite = builder.mSite;
        mChannel = builder.mChannel;
        mStatus = status;
        mDetail = detail;
        mStatusTime = System.currentTimeMillis();
    }

    /**
     * Unique row id within this session
     */
    public long getId()
    {
        return mId;
    }

    /**
     * Identity of the call and stream that is the same for the live row and its database row: call start, call key,
     * stream name and to (a patch call can be sent to one stream for more than one patched talkgroup).
     */
    public String getKey()
    {
        return mCallStart + "|" + mCallKey + "|" + mStreamName + "|" + mTo;
    }

    /**
     * Audio segment id of the call, which is the call key of the call in the Calls tab.
     */
    public long getCallKey()
    {
        return mCallKey;
    }

    public String getStreamName()
    {
        return mStreamName;
    }

    /**
     * Server type of the stream, or null when there's no such stream.
     */
    public BroadcastServerType getServerType()
    {
        return mServerType;
    }

    /**
     * Sets the server type
     * @return true if it changed
     */
    boolean setServerType(BroadcastServerType serverType)
    {
        if(mServerType != serverType)
        {
            mServerType = serverType;
            return true;
        }

        return false;
    }

    public long getCallStart()
    {
        return mCallStart;
    }

    /**
     * Call audio duration in milliseconds
     */
    public long getDuration()
    {
        return mDuration;
    }

    public String getTo()
    {
        return mTo;
    }

    public String getFrom()
    {
        return mFrom;
    }

    public String getAliasList()
    {
        return mAliasList;
    }

    /**
     * Aliases in the call that have this stream, which are the reason that the call was sent to it.
     */
    public List<SelectingAlias> getSelectingAliases()
    {
        return mSelectingAliases;
    }

    /**
     * Selecting aliases as a comma separated list
     */
    public String getSelectingAliasText()
    {
        return mSelectingAliases.stream().map(SelectingAlias::toString).collect(Collectors.joining(", "));
    }

    public String getProtocol()
    {
        return mProtocol;
    }

    public String getSystem()
    {
        return mSystem;
    }

    public String getSite()
    {
        return mSite;
    }

    public String getChannel()
    {
        return mChannel;
    }

    public StreamStatus getStatus()
    {
        return mStatus;
    }

    /**
     * Explanation of the status, e.g. the reason that the call wasn't sent, or null.
     */
    public String getDetail()
    {
        return mDetail;
    }

    /**
     * Time of the last status change
     */
    public long getStatusTime()
    {
        return mStatusTime;
    }

    /**
     * Sets the status.  A final status is not replaced, so that a late report (e.g. a stream that clears its queue on
     * disconnect) doesn't overwrite the outcome.
     * @return true if the status changed
     */
    synchronized boolean setStatus(StreamStatus status, String detail)
    {
        if(mStatus.isFinal() || (mStatus == status && java.util.Objects.equals(mDetail, detail)))
        {
            return false;
        }

        mStatus = status;
        mDetail = detail;
        mStatusTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public String toString()
    {
        return "Stream [" + mStreamName + "] call [" + mCallKey + "] to [" + mTo + "] from [" + mFrom + "] status [" +
            mStatus + (mDetail != null ? " - " + mDetail : "") + "] aliases [" + getSelectingAliasText() + "]";
    }

    /**
     * Call details shared by the rows of a call, one row per stream.
     */
    public static class Builder
    {
        private long mCallKey;
        private long mCallStart;
        private long mDuration;
        private String mTo;
        private String mFrom;
        private String mAliasList;
        private List<SelectingAlias> mSelectingAliases = List.of();
        private String mProtocol;
        private String mSystem;
        private String mSite;
        private String mChannel;

        public Builder callKey(long callKey)
        {
            mCallKey = callKey;
            return this;
        }

        public Builder callStart(long callStart)
        {
            mCallStart = callStart;
            return this;
        }

        public Builder duration(long duration)
        {
            mDuration = duration;
            return this;
        }

        public Builder to(String to)
        {
            mTo = to;
            return this;
        }

        public Builder from(String from)
        {
            mFrom = from;
            return this;
        }

        public Builder aliasList(String aliasList)
        {
            mAliasList = aliasList;
            return this;
        }

        public Builder selectingAliases(List<SelectingAlias> selectingAliases)
        {
            mSelectingAliases = selectingAliases != null ? selectingAliases : List.of();
            return this;
        }

        public Builder protocol(String protocol)
        {
            mProtocol = protocol;
            return this;
        }

        public Builder system(String system)
        {
            mSystem = system;
            return this;
        }

        public Builder site(String site)
        {
            mSite = site;
            return this;
        }

        public Builder channel(String channel)
        {
            mChannel = channel;
            return this;
        }
    }
}
