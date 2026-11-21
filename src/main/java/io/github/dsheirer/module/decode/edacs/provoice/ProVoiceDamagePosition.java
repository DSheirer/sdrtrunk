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

import java.util.Arrays;

/**
 * Where the damaged voice frames of one ProVoice transmission fall: in its first frames, its last frames or the
 * middle (measured on recorded calls; kept here so that it can be tested).
 *
 * A ProVoice frame is 80 ms and carries four voice frames.  The first EDGE_FRAMES frames count as the start.  The last
 * EDGE_FRAMES frames that are not part of the start count as the end, so a short transmission is never counted twice.
 * Measurement only - nothing here changes the audio.
 */
final class ProVoiceDamagePosition
{
    /** Frames that count as the start and as the end of a transmission: 6 x 80 ms = 0.48 s each */
    static final int EDGE_FRAMES = 6;
    static final int VOICE_FRAMES_PER_FRAME = 4;

    private long mFrames;
    private long mDamaged;
    private long mDamagedStart;
    private final int[] mRecent = new int[EDGE_FRAMES];

    /**
     * Adds the next frame of the transmission.
     * @param damagedVoiceFrames in this frame, 0-4
     */
    void add(int damagedVoiceFrames)
    {
        if(mFrames < EDGE_FRAMES)
        {
            mDamagedStart += damagedVoiceFrames;
        }
        else
        {
            mRecent[(int)((mFrames - EDGE_FRAMES) % EDGE_FRAMES)] = damagedVoiceFrames;
        }

        mFrames++;
        mDamaged += damagedVoiceFrames;
    }

    /** Frames counted as the start */
    long getStartFrames()
    {
        return Math.min(EDGE_FRAMES, mFrames);
    }

    /** Frames counted as the end */
    long getEndFrames()
    {
        return Math.min(EDGE_FRAMES, mFrames - getStartFrames());
    }

    /** Frames counted as the middle */
    long getMiddleFrames()
    {
        return mFrames - getStartFrames() - getEndFrames();
    }

    /** Damaged voice frames in the start */
    long getDamagedStart()
    {
        return mDamagedStart;
    }

    /** Damaged voice frames in the end */
    long getDamagedEnd()
    {
        long endFrames = getEndFrames();
        long total = 0;

        //While fewer than EDGE_FRAMES frames follow the start, they occupy the first slots of the ring.  After that
        //every slot holds one of the last EDGE_FRAMES frames.
        for(int x = 0; x < endFrames; x++)
        {
            total += mRecent[x];
        }

        return total;
    }

    /** Damaged voice frames in the middle */
    long getDamagedMiddle()
    {
        return mDamaged - mDamagedStart - getDamagedEnd();
    }

    /** Starts a new transmission */
    void reset()
    {
        mFrames = 0;
        mDamaged = 0;
        mDamagedStart = 0;
        Arrays.fill(mRecent, 0);
    }

    /**
     * @return the DEBUG log text for the transmission
     */
    String describe()
    {
        return String.format("ProVoice damage by position: first %.2f s %d of %d, middle %d of %d, last %.2f s %d of %d " +
            "voice frames", getStartFrames() * 0.08, getDamagedStart(), VOICE_FRAMES_PER_FRAME * getStartFrames(),
            getDamagedMiddle(), VOICE_FRAMES_PER_FRAME * getMiddleFrames(), getEndFrames() * 0.08, getDamagedEnd(),
            VOICE_FRAMES_PER_FRAME * getEndFrames());
    }
}
