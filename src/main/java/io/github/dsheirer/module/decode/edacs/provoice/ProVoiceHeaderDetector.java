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

package io.github.dsheirer.module.decode.edacs.provoice;

/**
 * Tells the frames at the start of a ProVoice transmission that are not voice from the voice.
 *
 * A ProVoice transmission does not start with voice.  It starts with a header frame sent three to five times over,
 * bit for bit the same, then one or two more frames of other data, and only then voice - 0.3 to 0.6 seconds in all
 * (MBTA LCN 5, recorded 2026-11-09: 26 of 27 transmissions, median 0.48 s).  Every one of those frames has the
 * voice sync in front, so until this the decoder treated them as voice.  Read as voice they are noise: most of
 * their four 20 ms voice frames fail the error check and were silenced, but about one in seven passed it by chance
 * and was played, and then repeated by the concealment.  That was the burst of digital noise at the start of calls,
 * and it is why 60% of the first 0.48 s was reported damaged at every site whatever the reception.
 *
 * Nothing here knows the layout of the header; it is recognized by how it behaves:
 * - a frame that repeats the frame before it (voice never does - two voice frames in a row share about 65% of
 *   their bits, a repeated header 100%) is header, and so is the one it repeats;
 * - once a header has been seen, the frames that follow are header until one arrives that reads as voice, which is
 *   at least {@link #VOICE_FRAMES_NEEDED} of its four voice frames passing the error check;
 * - the first frame of a transmission has nothing to be compared with, so it is played only if it reads as clean
 *   voice ({@link #FIRST_FRAME_MAX_ERRORS} errors at most) - which is the case when the channel joins a
 *   transmission that is already in voice.
 * All of this applies to the first {@link #WINDOW_FRAMES} frames of a transmission only.  After that every frame is
 * voice, as before.
 *
 * Not thread safe.
 */
public class ProVoiceHeaderDetector
{
    /** Frames from the start of a transmission in which a header is looked for (1.28 seconds) */
    public static final int WINDOW_FRAMES = 16;
    /** Share of bits equal to the previous frame at which a frame is a repeat */
    public static final double REPEAT_THRESHOLD = 0.90;
    /** Voice frames, of the four in a frame, that must pass the error check for the frame to be voice */
    public static final int VOICE_FRAMES_NEEDED = 3;
    /** Errors allowed in a voice frame of the first frame of a transmission */
    public static final int FIRST_FRAME_MAX_ERRORS = 2;

    private final int[] mPrevious = new int[ProVoiceFrameDecoder.FRAME_BITS];
    private int mIndex;
    private boolean mHeader;

    /**
     * Starts a new transmission.
     */
    public void reset()
    {
        mIndex = 0;
        mHeader = false;
    }

    /**
     * The frame was read as a header ({@link ProVoiceHeader}).  The frames that follow are header
     * until one reads as voice.  A header that arrives when none was expected starts the window again.
     * @param frame bits, without the sync
     */
    public void header(int[] frame)
    {
        if(!mHeader)
        {
            mIndex = 0;
        }

        mIndex++;
        mHeader = true;
        System.arraycopy(frame, 0, mPrevious, 0, frame.length);
    }

    /**
     * Decides if the frame is part of the header at the start of the transmission.
     * @param frame bits, without the sync
     * @param errors corrected bit errors of each of its voice frames
     * @return true if the frame is not voice and must not be played
     */
    public boolean isHeader(int[] frame, int[] errors)
    {
        int index = mIndex++;

        if(index >= WINDOW_FRAMES)
        {
            mHeader = false;
            return false;
        }

        boolean repeat = false;

        if(index > 0)
        {
            int same = 0;

            for(int x = 0; x < frame.length; x++)
            {
                if(frame[x] == mPrevious[x])
                {
                    same++;
                }
            }

            repeat = same >= REPEAT_THRESHOLD * frame.length;
        }

        System.arraycopy(frame, 0, mPrevious, 0, frame.length);

        if(repeat)
        {
            mHeader = true;
            return true;
        }

        if(mHeader)
        {
            if(count(errors, ProVoiceDecoder.MAX_ERRORS) < VOICE_FRAMES_NEEDED)
            {
                return true;
            }

            mHeader = false;
            return false;
        }

        if(index == 0 && count(errors, FIRST_FRAME_MAX_ERRORS) < VOICE_FRAMES_NEEDED)
        {
            mHeader = true;
            return true;
        }

        return false;
    }

    /**
     * Number of voice frames with no more than the maximum errors.
     */
    private static int count(int[] errors, int maximum)
    {
        int count = 0;

        for(int error : errors)
        {
            if(error <= maximum)
            {
                count++;
            }
        }

        return count;
    }
}
