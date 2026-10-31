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
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.audio.AudioSegment;

/**
 * Implemented by broadcasters that consume real-time AudioSegments directly rather than waiting for
 * completed AudioRecording files.  BroadcastModel dispatches AudioSegments to broadcasters that
 * implement this interface in addition to the standard Listener AudioRecording path.
 *
 * Note: this does not extend Listener AudioSegment because AbstractAudioBroadcaster already implements
 * Listener AudioRecording, and Java does not permit a class to implement the same generic interface with
 * two different type arguments.
 */
public interface IAudioSegmentBroadcaster
{
    /**
     * Receives an AudioSegment.  Caller has incremented the consumer count once on behalf of this
     * broadcaster, so the implementation owns one decrement responsibility.
     */
    void receiveAudioSegment(AudioSegment audioSegment);
}
