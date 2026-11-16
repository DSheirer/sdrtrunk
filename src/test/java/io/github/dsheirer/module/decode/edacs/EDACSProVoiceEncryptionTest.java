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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceMessage;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Tests ProVoice encryption detection with encryption IV field values from recordings of two Extended Addressing
 * systems: a clear system and a system whose calls are encrypted.
 */
class EDACSProVoiceEncryptionTest
{
    /** Clear voice: exact, and the most corrupted values seen on weak signal (9 and 15 bits from the clear value) */
    private static final long[] CLEAR = {0x6969696969696969L, 0x5959F969E9695969L, 0x696969F569ADAD34L};

    /** Encrypted voice: the values closest to the clear value (22 and 24 bits) and a typical value (32 bits) */
    private static final long[] ENCRYPTED = {0xE76830C9CBE8892CL, 0xAB4EC8EBCB157529L, 0x3C22D9F7ED84F6EDL};

    /**
     * Last two frames of a clear call, which carry the voice marker but a non-clear IV field (30 and 34 bits).
     */
    private static final long[] CLEAR_CALL_LAST_FRAMES = {0xE2EE946939BE7982L, 0xE2CFD65CF7E32898L};

    private static EDACSProVoiceMessage frame(long iv)
    {
        byte[] imbe = new byte[18];
        return new EDACSProVoiceMessage(imbe, imbe, imbe, imbe, new int[4][7][24], new boolean[0], 0, 0x2175,
            "SYNC3_INVERTED", 0, Collections.emptyList(), 0x2175, iv);
    }

    @Test
    void framesAreClassifiedByTheirIvField()
    {
        for(long iv : CLEAR)
        {
            assertFalse(frame(iv).isEncrypted(), String.format("clear IV %016X", iv));
        }

        for(long iv : ENCRYPTED)
        {
            assertTrue(frame(iv).isEncrypted(), String.format("encrypted IV %016X", iv));
        }
    }

    @Test
    void encryptedCallIsDetectedOnTheThirdFrame()
    {
        EDACSProVoiceEncryptionTracker tracker = new EDACSProVoiceEncryptionTracker();
        assertFalse(tracker.process(frame(ENCRYPTED[0])));
        assertFalse(tracker.process(frame(ENCRYPTED[1])));
        assertTrue(tracker.process(frame(ENCRYPTED[2])));
        assertTrue(tracker.process(frame(CLEAR[0])), "the call stays encrypted until reset");
        tracker.reset();
        assertFalse(tracker.isEncrypted());
    }

    @Test
    void clearCallIsNotEncryptedByItsLastFrames()
    {
        EDACSProVoiceEncryptionTracker tracker = new EDACSProVoiceEncryptionTracker();

        for(long iv : CLEAR)
        {
            assertFalse(tracker.process(frame(iv)));
        }

        for(long iv : CLEAR_CALL_LAST_FRAMES)
        {
            assertFalse(tracker.process(frame(iv)));
        }
    }

    @Test
    void encryptedFramesMustBeConsecutive()
    {
        EDACSProVoiceEncryptionTracker tracker = new EDACSProVoiceEncryptionTracker();
        assertFalse(tracker.process(frame(ENCRYPTED[0])));
        assertFalse(tracker.process(frame(ENCRYPTED[1])));
        assertFalse(tracker.process(frame(CLEAR[0])));
        assertFalse(tracker.process(frame(ENCRYPTED[0])));
        assertFalse(tracker.process(frame(ENCRYPTED[1])));
    }
}
