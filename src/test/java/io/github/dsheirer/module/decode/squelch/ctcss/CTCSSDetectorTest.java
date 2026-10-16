/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.squelch.ctcss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.dsheirer.module.decode.squelch.SquelchCodeState;
import java.util.List;
import org.junit.jupiter.api.Test;

class CTCSSDetectorTest
{
    @Test
    void toneSearchAcceptsAnUnknownConfiguredTone()
    {
        CTCSSDetector detector = new CTCSSDetector(List.of());
        CTCSSCode tone = CTCSSCode.TONE_1928;
        float[] samples = sineWave(tone.getFrequency(), 512);
        CTCSSMessage message = null;

        for(int x = 0; x < 3; x++)
        {
            message = detector.process(samples);
        }

        assertFalse(message.getMutedStatus());
        assertEquals(tone, message.getCTCSSCode());
        assertEquals(SquelchCodeState.ACCEPTED, message.getCodeState());
    }

    private static float[] sineWave(float frequency, int length)
    {
        float[] samples = new float[length];
        for(int x = 0; x < length; x++)
        {
            samples[x] = (float)Math.sin(2.0 * Math.PI * frequency * x / 8000.0);
        }
        return samples;
    }
}
