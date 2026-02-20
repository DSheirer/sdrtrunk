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

package io.github.dsheirer.dsp.squelch;

import io.github.dsheirer.sample.Listener;
import java.util.ArrayDeque;

/**
 * Removes squelch tail (noise burst at end of transmission) and optionally squelch head
 * (noise/tone ramp at start of transmission) from audio streams.
 *
 * Works by buffering audio with a configurable delay. When squelch closes (end of
 * transmission), the trailing buffered audio is discarded instead of being output.
 * When squelch opens (start of transmission), the first N milliseconds can be
 * discarded to remove CTCSS tone ramp-up noise.
 *
 * Thread safety: this class is NOT thread-safe. All calls should be from the same
 * processing thread.
 */
public class SquelchTailRemover
{
    public static final int DEFAULT_TAIL_REMOVAL_MS = 100;
    public static final int DEFAULT_HEAD_REMOVAL_MS = 0;
    public static final int MINIMUM_REMOVAL_MS = 0;
    public static final int MAXIMUM_TAIL_REMOVAL_MS = 300;
    public static final int MAXIMUM_HEAD_REMOVAL_MS = 150;
    private static final int AUDIO_SAMPLE_RATE = 8000;

    private final ArrayDeque<float[]> mDelayBuffer = new ArrayDeque<>();
    private Listener<float[]> mOutputListener;
    private int mTailRemovalSamples;
    private int mHeadRemovalSamples;
    private int mBufferedSampleCount = 0;
    private int mHeadSamplesRemaining = 0;
    private boolean mSquelchOpen = false;
    private boolean mFirstBufferAfterOpen = true;

    /**
     * Constructs an instance
     * @param tailRemovalMs milliseconds of audio to discard at end of transmission (0-300)
     * @param headRemovalMs milliseconds of audio to discard at start of transmission (0-150)
     */
    public SquelchTailRemover(int tailRemovalMs, int headRemovalMs)
    {
        setTailRemovalMs(tailRemovalMs);
        setHeadRemovalMs(headRemovalMs);
    }

    /**
     * Constructs with default values
     */
    public SquelchTailRemover()
    {
        this(DEFAULT_TAIL_REMOVAL_MS, DEFAULT_HEAD_REMOVAL_MS);
    }

    /**
     * Sets the tail removal duration
     * @param tailMs milliseconds to remove from end of transmission
     */
    public void setTailRemovalMs(int tailMs)
    {
        tailMs = Math.max(MINIMUM_REMOVAL_MS, Math.min(MAXIMUM_TAIL_REMOVAL_MS, tailMs));
        mTailRemovalSamples = (int)(AUDIO_SAMPLE_RATE * tailMs / 1000.0);
    }

    /**
     * Sets the head removal duration
     * @param headMs milliseconds to remove from start of transmission
     */
    public void setHeadRemovalMs(int headMs)
    {
        headMs = Math.max(MINIMUM_REMOVAL_MS, Math.min(MAXIMUM_HEAD_REMOVAL_MS, headMs));
        mHeadRemovalSamples = (int)(AUDIO_SAMPLE_RATE * headMs / 1000.0);
    }

    /**
     * Sets the output listener for processed audio
     */
    public void setOutputListener(Listener<float[]> listener)
    {
        mOutputListener = listener;
    }

    /**
     * Called when squelch opens (start of transmission).
     * Begins head removal countdown.
     */
    public void squelchOpen()
    {
        mSquelchOpen = true;
        mFirstBufferAfterOpen = true;
        mHeadSamplesRemaining = mHeadRemovalSamples;
        mDelayBuffer.clear();
        mBufferedSampleCount = 0;
    }

    /**
     * Called when squelch closes (end of transmission).
     * Discards the tail buffer contents instead of outputting them.
     */
    public void squelchClose()
    {
        mSquelchOpen = false;
        // Discard everything in the delay buffer — this IS the squelch tail
        mDelayBuffer.clear();
        mBufferedSampleCount = 0;
    }

    /**
     * Processes an audio buffer. If tail removal is configured, buffers audio and
     * outputs delayed audio. If squelch closes, buffered audio is discarded.
     *
     * @param audio buffer to process
     */
    public void process(float[] audio)
    {
        if(audio == null || audio.length == 0 || mOutputListener == null)
        {
            return;
        }

        // Handle head removal: skip initial samples after squelch open
        if(mHeadSamplesRemaining > 0)
        {
            if(audio.length <= mHeadSamplesRemaining)
            {
                mHeadSamplesRemaining -= audio.length;
                return; // Discard entire buffer
            }
            else
            {
                // Partial discard — trim the beginning
                int keep = audio.length - mHeadSamplesRemaining;
                float[] trimmed = new float[keep];
                System.arraycopy(audio, mHeadSamplesRemaining, trimmed, 0, keep);
                audio = trimmed;
                mHeadSamplesRemaining = 0;
            }
        }

        // If no tail removal configured, pass through directly
        if(mTailRemovalSamples <= 0)
        {
            mOutputListener.receive(audio);
            return;
        }

        // Buffer this audio
        mDelayBuffer.addLast(audio);
        mBufferedSampleCount += audio.length;

        // Output audio that has aged past the tail removal window
        while(mBufferedSampleCount > mTailRemovalSamples && !mDelayBuffer.isEmpty())
        {
            float[] oldest = mDelayBuffer.peekFirst();

            if(oldest == null)
            {
                break;
            }

            if(mBufferedSampleCount - oldest.length >= mTailRemovalSamples)
            {
                // This buffer has aged out — safe to output
                mDelayBuffer.pollFirst();
                mBufferedSampleCount -= oldest.length;
                mOutputListener.receive(oldest);
            }
            else
            {
                // Need to split this buffer
                int samplesToOutput = mBufferedSampleCount - mTailRemovalSamples;

                if(samplesToOutput > 0 && samplesToOutput < oldest.length)
                {
                    float[] output = new float[samplesToOutput];
                    float[] keep = new float[oldest.length - samplesToOutput];
                    System.arraycopy(oldest, 0, output, 0, samplesToOutput);
                    System.arraycopy(oldest, samplesToOutput, keep, 0, keep.length);

                    mDelayBuffer.pollFirst();
                    mDelayBuffer.addFirst(keep);
                    mBufferedSampleCount -= samplesToOutput;
                    mOutputListener.receive(output);
                }
                break;
            }
        }
    }

    /**
     * Flushes any remaining buffered audio. Call this when you want to force output
     * of all buffered audio (e.g., at stream end with known good signal).
     */
    public void flush()
    {
        while(!mDelayBuffer.isEmpty())
        {
            float[] buffer = mDelayBuffer.pollFirst();
            if(mOutputListener != null && buffer != null)
            {
                mOutputListener.receive(buffer);
            }
        }
        mBufferedSampleCount = 0;
    }

    /**
     * Resets all state
     */
    public void reset()
    {
        mDelayBuffer.clear();
        mBufferedSampleCount = 0;
        mHeadSamplesRemaining = 0;
        mSquelchOpen = false;
        mFirstBufferAfterOpen = true;
    }
}
