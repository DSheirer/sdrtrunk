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
package io.github.dsheirer.dsp.am;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the AM demodulator squelch transitions
 */
class SquelchingAMDemodulatorTest
{
    private static final int SAMPLE_RATE = 25000;
    private static final int BUFFER = 1024;
    private static final double TONE = 1000.0;

    /**
     * Demodulates silence, then a transmission, then silence.
     */
    private static float[] transmission(float carrier)
    {
        SquelchingAMDemodulator demodulator = new SquelchingAMDemodulator(-78.0f, false, 6000.0);
        demodulator.setSampleRate(SAMPLE_RATE);
        int buffers = 150;
        float[] audio = new float[BUFFER * buffers];

        for(int b = 0; b < buffers; b++)
        {
            float[] i = new float[BUFFER];
            float[] q = new float[BUFFER];

            if(b >= 30 && b < 100)
            {
                for(int x = 0; x < BUFFER; x++)
                {
                    int sample = b * BUFFER + x;
                    i[x] = carrier * (1.0f + 0.8f * (float)Math.cos(2.0 * Math.PI * TONE * sample / SAMPLE_RATE));
                }
            }

            System.arraycopy(demodulator.demodulate(i, q), 0, audio, b * BUFFER, BUFFER);
        }

        return audio;
    }

    @Test
    void transmissionHasNoClicks()
    {
        for(float carrier: new float[]{0.001f, 0.01f, 0.1f})
        {
            float[] audio = transmission(carrier);
            float peak = 0.0f;
            float step = 0.0f;

            for(int x = 1; x < audio.length; x++)
            {
                peak = Math.max(peak, Math.abs(audio[x]));
                step = Math.max(step, Math.abs(audio[x] - audio[x - 1]));
            }

            //Expected 80% modulated peak is 0.6 and the largest step of the 1 kHz tone is 0.15
            assertTrue(peak < 0.7f, "Peak " + peak + " for carrier " + carrier);
            assertTrue(step < 0.2f, "Step " + step + " for carrier " + carrier);
        }
    }

    @Test
    void audioEndsWithTheTransmission()
    {
        float[] audio = transmission(0.01f);
        int last = 0;

        for(int x = 0; x < audio.length; x++)
        {
            if(audio[x] != 0.0f)
            {
                last = x;
            }
        }

        //Audio is delayed by 32 ms, so it can't run past the end of the transmission plus the delay.
        int end = 100 * BUFFER;
        assertTrue(last <= end + SAMPLE_RATE * 0.032, "Last audio sample " + last + " for end at " + end);
        assertEquals(0.0f, audio[audio.length - 1]);
    }
}
