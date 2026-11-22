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

package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceMessage;

/**
 * Decides whether a ProVoice call is encrypted from the encryption IV field of its voice frames.
 *
 * <p>A single frame's IV can be corrupted, and on a clear call the last frames of an over can carry a non-clear IV
 * field, so the call is only treated as encrypted after {@link #ENCRYPTED_FRAMES} consecutive encrypted frames.
 * Every frame of an encrypted call is encrypted, so the call is detected within 240 ms.  The decision holds until
 * {@link #reset()} at the end of the call.</p>
 */
public class EDACSProVoiceEncryptionTracker
{
    public static final int ENCRYPTED_FRAMES = 3;

    private int mConsecutiveEncryptedFrames = 0;
    private boolean mEncrypted = false;

    /**
     * Processes a voice frame.
     * @param message voice frame
     * @return true if the call is encrypted, including from this frame
     */
    public boolean process(EDACSProVoiceMessage message)
    {
        if(message.isEncrypted())
        {
            mConsecutiveEncryptedFrames++;

            if(mConsecutiveEncryptedFrames >= ENCRYPTED_FRAMES)
            {
                mEncrypted = true;
            }
        }
        else
        {
            mConsecutiveEncryptedFrames = 0;
        }

        return mEncrypted;
    }

    /**
     * Indicates if the current call has been found to be encrypted.
     */
    public boolean isEncrypted()
    {
        return mEncrypted;
    }

    /**
     * Resets for the next call.
     */
    public void reset()
    {
        mConsecutiveEncryptedFrames = 0;
        mEncrypted = false;
    }
}
