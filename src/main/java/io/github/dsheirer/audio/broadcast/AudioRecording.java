/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamActivityModel;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.identifier.IdentifierCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioRecording implements Comparable<AudioRecording>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioRecording.class);

    private Path mPath;
    private long mStartTime;
    private long mRecordingLength;
    private AtomicInteger mPendingReplayCount = new AtomicInteger();
    private IdentifierCollection mIdentifierCollection;
    private Collection<BroadcastChannel> mBroadcastChannels;
    private StreamActivityModel mStreamActivityModel;
    private final Map<String,StreamActivity> mActivitiesByStream = new HashMap<>();
    /**
     * Stream activity row by the broadcaster that holds a pending replay of this recording.  The entry is removed
     * when the broadcaster releases the recording, so that each broadcaster releases it once.
     */
    private final Map<AbstractAudioBroadcaster<?>,StreamActivity> mPendingBroadcasters = new IdentityHashMap<>();
    private final Map<AbstractAudioBroadcaster<?>,StreamActivity> mActivitiesByBroadcaster = new IdentityHashMap<>();

    /**
     * Audio recording that is ready to be streamed
     *
     * @param path to the audio recording file
     * @param identifierCollection associated with the recording
     * @param start time of recording in milliseconds since epoch
     * @param recordingLength in milliseconds
     */
    public AudioRecording(Path path, Collection<BroadcastChannel> broadcastChannels,
                          IdentifierCollection identifierCollection, long start, long recordingLength)
    {
        mPath = path;
        mBroadcastChannels = broadcastChannels;
        mIdentifierCollection = identifierCollection;
        mStartTime = start;
        mRecordingLength = recordingLength;
    }

    /**
     * Path to the completed audio recording
     */
    public Path getPath()
    {
        return mPath;
    }

    /**
     * Collection of broadcast channels that this recording should be streamed to
     */
    public Collection<BroadcastChannel> getBroadcastChannels()
    {
        return mBroadcastChannels;
    }

    /**
     * Optional audio metadata/identifiers for the recording.
     */
    public IdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Indicates if this recording contains an optional identifier collection.
     */
    public boolean hasIdentifierCollection()
    {
        return mIdentifierCollection != null;
    }

    /**
     * Recording start time in milliseconds since epoch
     */
    public long getStartTime()
    {
        return mStartTime;
    }

    /**
     * Recording length in milliseconds
     */
    public long getRecordingLength()
    {
        return mRecordingLength;
    }


    /**
     * Implements comparable for sorting recordings based on start time in ascending order
     */
    @Override
    public int compareTo(AudioRecording otherRecording)
    {
        return Long.compare(getStartTime(), otherRecording.getStartTime());
    }

    /**
     * Sets the stream activity rows of this recording, one per stream name, so that the streams can report what they
     * do with the recording.
     * @param model that holds the rows
     * @param activitiesByStream row by stream (broadcast channel) name
     */
    public void setStreamActivities(StreamActivityModel model, Map<String,StreamActivity> activitiesByStream)
    {
        mStreamActivityModel = model;

        synchronized(mActivitiesByStream)
        {
            mActivitiesByStream.clear();
            mActivitiesByStream.putAll(activitiesByStream);
        }
    }

    /**
     * Stream activity row for the stream name, or null.
     */
    public StreamActivity getStreamActivity(String streamName)
    {
        synchronized(mActivitiesByStream)
        {
            return mActivitiesByStream.get(streamName);
        }
    }

    /**
     * Updates the status of the row for the stream name.  Used when there's no broadcaster for the stream.
     */
    public void setStatus(String streamName, StreamStatus status, String detail)
    {
        if(mStreamActivityModel != null)
        {
            mStreamActivityModel.update(getStreamActivity(streamName), status, detail);
        }
    }

    /**
     * Assigns the recording to the broadcaster of the stream and adds a pending replay that the broadcaster releases
     * when it is done with the recording.
     * @param broadcaster that will stream or upload the recording
     * @param streamName of the broadcaster, for the stream activity row
     */
    public void addBroadcaster(AbstractAudioBroadcaster<?> broadcaster, String streamName)
    {
        StreamActivity activity = getStreamActivity(streamName);

        synchronized(mPendingBroadcasters)
        {
            if(mActivitiesByBroadcaster.containsKey(broadcaster))
            {
                return;
            }

            mActivitiesByBroadcaster.put(broadcaster, activity);
            mPendingBroadcasters.put(broadcaster, activity);
            mPendingReplayCount.incrementAndGet();
        }

        if(mStreamActivityModel != null && activity != null)
        {
            mStreamActivityModel.setServerType(activity, broadcaster.getBroadcastConfiguration() != null ?
                broadcaster.getBroadcastConfiguration().getBroadcastServerType() : null);
        }
    }

    /**
     * Updates the status of this recording for the broadcaster.  A final status also releases the recording.
     * @param broadcaster that reports the status
     * @param status of the recording for the broadcaster
     * @param detail explaining the status, or null
     */
    public void setStatus(AbstractAudioBroadcaster<?> broadcaster, StreamStatus status, String detail)
    {
        StreamActivity activity;

        synchronized(mPendingBroadcasters)
        {
            activity = mActivitiesByBroadcaster.get(broadcaster);
        }

        if(mStreamActivityModel != null && activity != null)
        {
            mStreamActivityModel.update(activity, status, detail);
        }

        if(status.isFinal())
        {
            release(broadcaster);
        }
    }

    /**
     * Releases the pending replay of the broadcaster, so the temporary recording file can be deleted once every
     * broadcaster is done with it.  Releasing more than once has no effect.
     */
    public void release(AbstractAudioBroadcaster<?> broadcaster)
    {
        synchronized(mPendingBroadcasters)
        {
            if(mPendingBroadcasters.containsKey(broadcaster))
            {
                mPendingBroadcasters.remove(broadcaster);
                mPendingReplayCount.decrementAndGet();
            }
        }
    }

    /**
     * Indicates if there are any remaining pending replays.  Once the pending replay count is less than or equal to
     * zero, the recording can be deleted.
     */
    public boolean hasPendingReplays()
    {
        return mPendingReplayCount.get() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AudioRecording)) return false;
        return compareTo((AudioRecording) o) == 0;
    }
}
