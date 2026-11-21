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
package io.github.dsheirer.audio.playback;

/**
 * Notification that an audio channel started playing a call, or stopped playing.
 *
 * @param channelIndex of the audio channel in the audio output (0 = left/mono, 1 = right)
 * @param channelName of the audio channel (LEFT, RIGHT or MONO)
 * @param channelCount number of channels in the audio output
 * @param audioSegmentId of the live call audio segment that is playing, or 0.  This is the call key of the call in
 * the call manager.
 * @param replayCallId database id of the recorded call that is replaying, or 0.
 */
public record ChannelPlayback(int channelIndex, String channelName, int channelCount, long audioSegmentId,
                              long replayCallId)
{
    /**
     * Indicates if the channel is playing a live call or replaying a recorded call
     */
    public boolean isPlaying()
    {
        return audioSegmentId != 0 || replayCallId != 0;
    }

    /**
     * Indicates if the channel is replaying a recorded call
     */
    public boolean isReplay()
    {
        return replayCallId != 0;
    }
}
