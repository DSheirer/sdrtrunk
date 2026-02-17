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

import io.github.dsheirer.identifier.IdentifierCollection;

/**
 * Interface for broadcasters that support real-time audio streaming (as opposed to
 * the standard recording-then-upload approach). Real-time broadcasters receive audio
 * buffers as they are produced, with explicit start/stop signals for each audio segment.
 *
 * This interface is used by AudioStreamingManager to route audio directly to
 * real-time broadcasters without waiting for the audio segment to complete.
 */
public interface IRealTimeAudioBroadcaster
{
    /**
     * Called when a new audio segment starts. The broadcaster should prepare for
     * incoming audio (e.g., open a stream connection).
     *
     * @param identifiers the identifier collection for this audio segment
     */
    void startRealTimeStream(IdentifierCollection identifiers);

    /**
     * Called for each audio buffer as it arrives in real-time.
     * Buffers are 8 kHz mono float samples, normalized to [-1.0, 1.0].
     *
     * This method must return quickly and not block. Implementations should
     * queue the buffer for processing on a separate thread.
     *
     * @param audioBuffer the audio samples
     */
    void receiveRealTimeAudio(float[] audioBuffer);

    /**
     * Called when the audio segment is complete. The broadcaster should finalize
     * and close the stream.
     */
    void stopRealTimeStream();

    /**
     * Indicates if this broadcaster is currently capable of real-time streaming.
     * If false, the audio streaming manager will fall back to recording-based delivery.
     *
     * @return true if ready for real-time streaming
     */
    boolean isRealTimeReady();
}
