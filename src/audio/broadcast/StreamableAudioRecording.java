/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast;

import audio.metadata.AudioMetadata;

import java.nio.file.Path;

public class StreamableAudioRecording
{
    private Path mPath;
    private AudioMetadata mAudioMetadata;
    private long mRecordingLengthMilliSeconds;

    /**
     * Audio recording that is ready to be streamed
     * @param path to the audio recording file
     * @param audioMetadata associated with the recording
     * @param recordingLength in milliseconds
     */
    public StreamableAudioRecording(Path path, AudioMetadata audioMetadata, long recordingLength)
    {
        mPath = path;
        mAudioMetadata = audioMetadata;
        mRecordingLengthMilliSeconds = recordingLength;
    }

    /**
     * Path to the completed audio recording
     */
    public Path getPath()
    {
        return mPath;
    }

    /**
     * Optional audio metadata for the recording.
     */
    public AudioMetadata getAudioMetadata()
    {
        return mAudioMetadata;
    }

    /**
     * Recording length in milliseconds
     */
    public long getRecordingLength()
    {
        return mRecordingLengthMilliSeconds;
    }
}
