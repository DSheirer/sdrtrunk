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

package io.github.dsheirer.module.decode.moto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests the analog voice de-emphasis setting and filter for Motorola Type II traffic channels.
 */
class MotorolaTypeIIAnalogAudioFilterTest
{
    private static final double SAMPLE_RATE = 8000.0;

    /**
     * Level in dB of a tone of the frequency after the filter, relative to the tone that went in.
     */
    private static double response(AnalogDeemphasis deemphasis, double frequency)
    {
        MotorolaTypeIIAnalogAudioFilter filter = new MotorolaTypeIIAnalogAudioFilter(deemphasis);
        double power = 0;
        int counted = 0;

        //Audio arrives in buffers.  The first second lets the filters settle.
        for(int buffer = 0; buffer < 20; buffer++)
        {
            float[] audio = new float[800];

            for(int x = 0; x < audio.length; x++)
            {
                audio[x] = (float)Math.sin(2.0 * Math.PI * frequency * (buffer * audio.length + x) / SAMPLE_RATE);
            }

            float[] filtered = filter.filter(audio);

            if(buffer >= 10)
            {
                for(float sample : filtered)
                {
                    power += sample * sample;
                    counted++;
                }
            }
        }

        return 10.0 * Math.log10(power / counted / 0.5);
    }

    @Test
    void lowFrequenciesPassAtTheSameLevel()
    {
        for(AnalogDeemphasis deemphasis : AnalogDeemphasis.values())
        {
            if(deemphasis != AnalogDeemphasis.NONE && deemphasis != AnalogDeemphasis.US_750)
            {
                assertEquals(0.0, response(deemphasis, 100), 1.0, deemphasis.name());
            }
        }
    }

    @Test
    void trebleIsReducedMoreByEachStrongerSetting()
    {
        double previous = 1.0;

        for(AnalogDeemphasis deemphasis : new AnalogDeemphasis[]{AnalogDeemphasis.US_75, AnalogDeemphasis.US_150,
            AnalogDeemphasis.US_200, AnalogDeemphasis.US_300, AnalogDeemphasis.US_750})
        {
            double level = response(deemphasis, 2500);
            assertTrue(level < previous - 1.0, deemphasis.name() + " at 2500 Hz: " + level + " dB");
            previous = level;
        }
    }

    @Test
    void mediumSettingMatchesASinglePoleAt150Microseconds()
    {
        //A single pole with a 150 microsecond time constant has its corner at 1,061 Hz.
        assertEquals(-3.0, response(AnalogDeemphasis.US_150, 1061), 1.0);
        assertEquals(-7.9, response(AnalogDeemphasis.US_150, 2500), 1.5);
    }

    @Test
    void audioAboveThreeKilohertzIsRemoved()
    {
        assertTrue(response(AnalogDeemphasis.US_75, 3700) < -40.0);
    }

    @Test
    void configurationDefaultsToMediumAndIgnoresNull()
    {
        DecodeConfigMotorolaTypeII config = new DecodeConfigMotorolaTypeII();
        assertEquals(AnalogDeemphasis.US_150, config.getAnalogDeemphasis());

        config.setAnalogDeemphasis(AnalogDeemphasis.NONE);
        assertEquals(AnalogDeemphasis.NONE, config.getAnalogDeemphasis());

        config.setAnalogDeemphasis(null);
        assertEquals(AnalogDeemphasis.US_150, config.getAnalogDeemphasis());
    }
}
