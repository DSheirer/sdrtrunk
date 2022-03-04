/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.playback;

import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.ScalarRealOscillator;

/**
 * Utility for generating tones for audio clips
 */
public class ToneUtil
{
    //Maximum tone volume: 0.0 <> 1.0
    private static final double MAX_TONE_VOLUME = 0.5f;

    /**
     * Generates a tone using the specified parameters
     * @param toneFrequency of the tone
     * @param toneVolume of the tone 1-10, to a maximum 90% gain
     * @param sampleCount number of 8 kHz samples to generate
     * @return buffer of audio
     */
    public static float[] getTone(ToneFrequency toneFrequency, ToneVolume toneVolume, int sampleCount)
    {
        IRealOscillator oscillator = new ScalarRealOscillator(toneFrequency.getValue(), 8000.0);

        float[] samples = oscillator.generate(sampleCount);

        double gain = MAX_TONE_VOLUME * ((double)toneVolume.getValue() / 10.0);

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] *= gain;
        }

        //Attenuate beginning and end samples
        if(sampleCount > 10)
        {
            for(int x = 0; x < 10; x++)
            {
                samples[x] *= (float)x / 10.0f;
            }

            for(int x = 0; x < 10; x++)
            {
                samples[samples.length - 1 - x] *= (float)x / 10.0f;
            }
        }

        return samples;
    }
}
