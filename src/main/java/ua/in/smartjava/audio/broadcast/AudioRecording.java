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
package ua.in.smartjava.audio.broadcast;

import ua.in.smartjava.channel.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioRecording implements Comparable<AudioRecording>
{
    private final static Logger mLog = LoggerFactory.getLogger(StreamManager.class);

    private Path mPath;
    private long mStartTime;
    private long mRecordingLength;
    private AtomicInteger mPendingReplayCount = new AtomicInteger();
    private Metadata mMetadata;

    /**
     * Audio recording that is ready to be streamed
     *
     * @param path to the ua.in.smartjava.audio recording file
     * @param metadata associated with the recording
     * @param start time of recording in milliseconds since epoch
     * @param recordingLength in milliseconds
     */
    public AudioRecording(Path path, Metadata metadata, long start, long recordingLength)
    {
        mPath = path;
        mMetadata = metadata;
        mStartTime = start;
        mRecordingLength = recordingLength;
    }

    /**
     * Path to the completed ua.in.smartjava.audio recording
     */
    public Path getPath()
    {
        return mPath;
    }

    /**
     * Optional ua.in.smartjava.audio metadata for the recording.
     */
    public Metadata getMetadata()
    {
        return mMetadata;
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
}