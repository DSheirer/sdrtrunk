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

package io.github.dsheirer.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.alias.AliasList;
import jmbe.frame.CorrectedAMBEFrame;
import jmbe.frame.CorrectedIMBEFrame;
import org.junit.jupiter.api.Test;

public class VoiceQualityTest
{
    private static void flip(byte[] frame, int bit)
    {
        frame[bit / 8] ^= (byte)(0x80 >>> (bit % 8));
    }

    private static int imbeC0Errors(byte[] frame)
    {
        return CorrectedIMBEFrame.correct(frame).getErrors()[0];
    }

    private static int ambeC0Errors(byte[] frame)
    {
        return CorrectedAMBEFrame.correct(frame).getErrors()[0];
    }

    @Test
    public void cleanFramesHaveNoFirstBlockErrors()
    {
        assertEquals(0, imbeC0Errors(new byte[18]));
        assertEquals(0, ambeC0Errors(new byte[9]));
    }

    @Test
    public void imbeCountsFlippedFirstBlockBits()
    {
        byte[] frame = new byte[18];
        flip(frame, 0);
        assertEquals(1, imbeC0Errors(frame));
        flip(frame, 7);
        assertEquals(2, imbeC0Errors(frame));
    }

    @Test
    public void imbeIgnoresBitsOutsideFirstBlock()
    {
        byte[] frame = new byte[18];
        flip(frame, 1);
        assertEquals(0, imbeC0Errors(frame));
    }

    @Test
    public void ambeCountsFlippedFirstBlockBits()
    {
        byte[] frame = new byte[9];
        flip(frame, 0);
        assertEquals(1, ambeC0Errors(frame));
        flip(frame, 4);
        flip(frame, 8);
        assertEquals(3, ambeC0Errors(frame));
    }

    @Test
    public void ambeIgnoresBitsOutsideFirstBlock()
    {
        byte[] frame = new byte[9];
        flip(frame, 2);
        assertEquals(0, ambeC0Errors(frame));
    }

    @Test
    public void voiceQualityIsThePercentOfFramesWithAtMostOneError()
    {
        AudioSegment segment = new AudioSegment(new AliasList("test"), 0);
        assertEquals(-1, segment.getVoiceQuality());

        segment.addVoiceFrame(0);
        segment.addVoiceFrame(1);
        segment.addVoiceFrame(2);
        segment.addVoiceFrame(4);

        assertEquals(4, segment.getVoiceFrameCount());
        assertEquals(50, segment.getVoiceQuality());
    }
}
