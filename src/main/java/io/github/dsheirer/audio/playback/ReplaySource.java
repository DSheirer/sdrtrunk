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
 * Audio of a recorded call that is replayed through an audio channel.  The audio channel pulls the audio in 20 ms
 * intervals on the audio playback thread, and the user interface pauses, seeks and stops it from the FX thread.
 */
public class ReplaySource
{
    private final float[] mSamples;
    private final long mCallId;
    private final String mDescription;
    private volatile int mPosition;
    private volatile boolean mPaused;
    private volatile boolean mStopped;

    /**
     * Constructs an instance
     * @param samples of 8 kHz audio
     * @param callId database id of the call that is replayed
     * @param description of the call for display (e.g. TO and FROM)
     */
    public ReplaySource(float[] samples, long callId, String description)
    {
        mSamples = samples;
        mCallId = callId;
        mDescription = description;
    }

    /**
     * Database id of the replayed call
     */
    public long getCallId()
    {
        return mCallId;
    }

    /**
     * Description of the replayed call for display
     */
    public String getDescription()
    {
        return mDescription;
    }

    /**
     * Next 20 ms of audio, silence while paused, or null when the replay is finished or stopped.  Audio thread.
     */
    float[] next()
    {
        if(isFinished())
        {
            return null;
        }

        float[] audio = new float[AudioChannel.SAMPLES_PER_INTERVAL];

        if(mPaused)
        {
            return audio;
        }

        int position = mPosition;
        int length = Math.min(audio.length, mSamples.length - position);
        System.arraycopy(mSamples, position, audio, 0, length);
        mPosition = position + length;
        return audio;
    }

    /**
     * Indicates if the replay played to the end or was stopped
     */
    public boolean isFinished()
    {
        return mStopped || mPosition >= mSamples.length;
    }

    /**
     * Indicates if the replay played to the end, as opposed to being stopped or replaced
     */
    public boolean isCompleted()
    {
        return !mStopped && mPosition >= mSamples.length;
    }

    public boolean isPaused()
    {
        return mPaused;
    }

    public void setPaused(boolean paused)
    {
        mPaused = paused;
    }

    /**
     * Stops the replay.  The audio channel releases it at the next audio interval.
     */
    public void stop()
    {
        mStopped = true;
    }

    /**
     * Moves the replay position
     * @param fraction of the recording, 0.0 to 1.0
     */
    public void seek(double fraction)
    {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        mPosition = (int)Math.min(mSamples.length, Math.round(mSamples.length * clamped));
    }

    /**
     * Replay position in milliseconds
     */
    public long getPositionMillis()
    {
        return mPosition * 1000L / 8000L;
    }

    /**
     * Recording duration in milliseconds
     */
    public long getDurationMillis()
    {
        return mSamples.length * 1000L / 8000L;
    }

    /**
     * Replay position as a fraction of the recording, 0.0 to 1.0
     */
    public double getFraction()
    {
        return mSamples.length > 0 ? (double)mPosition / mSamples.length : 1.0;
    }
}
