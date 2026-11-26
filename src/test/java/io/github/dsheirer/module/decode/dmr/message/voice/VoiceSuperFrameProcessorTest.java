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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the voice super frame processor's use of the EMB PI bit.  Per TS 102 361-1 9.3.2 the PI bit is the
 * pre-emption and power control indicator: when set, the burst's embedded field carries reverse channel information
 * for the other logical channel (6.4.2), so it is neither an encryption indicator nor a single burst for this channel.
 */
public class VoiceSuperFrameProcessorTest
{
    //EMB bit positions in a 288-bit voice burst, either side of the 32-bit embedded field.
    private static final int[] EMB_BITS = {132, 133, 134, 135, 136, 137, 138, 139, 172, 173, 174, 175, 176, 177, 178,
        179};
    private static final int LCSS_SINGLE_FRAGMENT = 0;
    private static final int LCSS_FIRST_FRAGMENT = 1;

    /**
     * Voice burst with a valid EMB (colour code 1) carrying the PI and LCSS values, and an all-zero embedded field.
     */
    private static VoiceEMBMessage voice(DMRSyncPattern pattern, boolean pi, int lcss)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(288);
        int word = EMB.encode((1 << 3) | ((pi ? 1 : 0) << 2) | lcss);

        for(int x = 0; x < EMB_BITS.length; x++)
        {
            if(((word >> (15 - x)) & 1) == 1)
            {
                message.set(EMB_BITS[x]);
            }
        }

        VoiceEMBMessage voice = new VoiceEMBMessage(pattern, message, null, 0L, 1);
        assertTrue(voice.getEMB().isValid(), "Test EMB should be a valid codeword");
        return voice;
    }

    @Test
    public void reverseChannelPiDoesNotMarkTheCallEncrypted()
    {
        VoiceSuperFrameProcessor processor = new VoiceSuperFrameProcessor();
        processor.process(voice(DMRSyncPattern.BS_VOICE_FRAME_B, true, LCSS_FIRST_FRAGMENT));
        assertFalse(processor.isCollecting(), "PI = 1 is not an encryption indicator");
    }

    @Test
    public void reverseChannelFrameFIsNotDecodedAsSingleBurst()
    {
        VoiceEMBMessage frameF = voice(DMRSyncPattern.BS_VOICE_FRAME_F, true, LCSS_SINGLE_FRAGMENT);
        new VoiceSuperFrameProcessor().process(frameF);
        assertFalse(frameF.hasEmbeddedParameters(), "PI = 1: frame F carries reverse channel information");
    }

    @Test
    public void frameFWithoutReverseChannelIsDecodedAsSingleBurst()
    {
        VoiceEMBMessage frameF = voice(DMRSyncPattern.BS_VOICE_FRAME_F, false, LCSS_SINGLE_FRAGMENT);
        new VoiceSuperFrameProcessor().process(frameF);
        assertTrue(frameF.hasEmbeddedParameters(), "PI = 0: frame F carries this channel's single burst");
    }
}
