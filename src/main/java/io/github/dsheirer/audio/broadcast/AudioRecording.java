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
import io.github.dsheirer.identifier.IdentifierCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
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
     * Increments the count of pending replays.
     */
    public void addPendingReplay()
    {
        mPendingReplayCount.incrementAndGet();
    }

    /**
     * Decrements the count of pending replays.
     */
    public void removePendingReplay()
    {
        mPendingReplayCount.decrementAndGet();
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
