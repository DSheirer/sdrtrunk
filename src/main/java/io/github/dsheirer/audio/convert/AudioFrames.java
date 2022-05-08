/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.convert;

import java.util.List;
import org.apache.commons.math3.util.FastMath;

public abstract class AudioFrames
{
    protected final int audioDuration;
    protected final List<byte[]> audioFrames;
    protected int mCurrentFrame = -1;

    /**
     * Create new AudioFrame
     * @param audioDuration total duration in milliseconds
     * @param audioFrames series of audio frames, split on frame boundaries
     */
    public AudioFrames(int audioDuration, List<byte[]> audioFrames)
    {
        this.audioDuration = audioDuration;
        this.audioFrames = audioFrames;
    }

    /**
     * Get total duration of frames
     * @return total duration in milliseconds
     */
    public int getDuration()
    {
        return audioDuration;
    }

    /**
     * Get all frames
     * @return list of byte arrays, with each element representing a single frame of audio
     */
    public List<byte[]> getFrames()
    {
        return audioFrames;
    }

    /**
     * Get current frame
     * @return byte array with the current frame of audio
     */
    public byte[] getCurrentFrame()
    {
        return audioFrames.get(mCurrentFrame);
    }

    /**
     * Get current frame duration
     * @return frame duration in milliseconds
     */
    public abstract int getCurrentFrameDuration();

    /**
     * Indicates if there is a frame before the current one
     * @return true if the frame exists, otherwise false
     */
    public boolean hasPrevFrame()
    {
        int prevFrame = mCurrentFrame - 1;
        return prevFrame >= 0 && prevFrame < audioFrames.size();
    }

    /**
     * Indicates if there is a frame after the current one
     * @return true if the frame exists, otherwise false
     */
    public boolean hasNextFrame()
    {
        int nextFrame = mCurrentFrame + 1;
        return nextFrame >= 0 && nextFrame < audioFrames.size();
    }

    /**
     * Seek to the previous frame
     */
    public void prevFrame()
    {
        mCurrentFrame -= 1;
    }

    /**
     * Seek to the next frame
     */
    public void nextFrame()
    {
        mCurrentFrame += 1;
    }

    /**
     * Restart from beginning
     * Must call nextFrame() to get first frame
     */
    public void restart()
    {
        mCurrentFrame = -1;
    }

    /**
     * Seek by the specified time
     * @param duration_ms time in milliseconds, positive or negative
     */
    public void seek(int duration_ms)
    {
        int step = duration_ms < 0 ? -1 : 1;
        int actual_ms = 0;
        while(FastMath.abs(actual_ms) < FastMath.abs(duration_ms))
        {
            mCurrentFrame += step;
            actual_ms += getCurrentFrameDuration();
        }
    }
}
