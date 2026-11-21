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

package io.github.dsheirer.module.decode.smartnet;

import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import io.github.dsheirer.module.demodulate.fm.FMDemodulatorModule;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The voice channel's noise squelch passes a received FM signal and drops the static that
 * follows when the transmitter drops.
 */
public class SmartNetVoiceSquelchTest
{
    private static final double SAMPLE_RATE = 50000.0;

    /**
     * Test: 2 s of a 1 kHz tone at 3 kHz deviation gives about 2 s of 8 kHz audio; 2 s of noise that follows gives
     * almost none.
     */
    @Test
    void testTailRemoved()
    {
        FMDemodulatorModule module = new FMDemodulatorModule(12500.0, new NoiseSquelch(
            NoiseSquelch.DEFAULT_NOISE_OPEN_THRESHOLD, NoiseSquelch.DEFAULT_NOISE_CLOSE_THRESHOLD,
            NoiseSquelch.DEFAULT_HYSTERESIS_OPEN_THRESHOLD, NoiseSquelch.DEFAULT_HYSTERESIS_CLOSE_THRESHOLD));
        module.getSourceEventListener().receive(SourceEvent.sampleRateChange(SAMPLE_RATE));
        int[] audio = new int[1];
        module.setBufferListener(buffer -> audio[0] += buffer.length);

        Random random = new Random(7);
        double phase = 0;
        int block = 2500;
        long sample = 0;

        //Carrier with a 1 kHz tone, 3 kHz deviation, 30 dB SNR
        for(int b = 0; b < 40; b++)
        {
            float[] i = new float[block], q = new float[block];
            for(int x = 0; x < block; x++, sample++)
            {
                double deviation = 3000.0 * Math.sin(2 * Math.PI * 1000.0 * sample / SAMPLE_RATE);
                phase += 2 * Math.PI * deviation / SAMPLE_RATE;
                i[x] = (float)(Math.cos(phase) + random.nextGaussian() * 0.03);
                q[x] = (float)(Math.sin(phase) + random.nextGaussian() * 0.03);
            }
            module.receive(new ComplexSamples(i, q, 0));
        }

        int signalAudio = audio[0];
        assertTrue(signalAudio > 14000, "signal passed: " + signalAudio);

        //Transmitter drops: noise only
        for(int b = 0; b < 40; b++)
        {
            float[] i = new float[block], q = new float[block];
            for(int x = 0; x < block; x++)
            {
                i[x] = (float)(random.nextGaussian() * 0.1);
                q[x] = (float)(random.nextGaussian() * 0.1);
            }
            module.receive(new ComplexSamples(i, q, 0));
        }

        int noiseAudio = audio[0] - signalAudio;
        assertTrue(noiseAudio < 1600, "noise dropped (< 0.2 s): " + noiseAudio);
    }
}
