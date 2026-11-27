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

package io.github.dsheirer.module.decode.cw;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test signal generator for Morse code (CW) audio at 8 kHz, with noise, a continuous guard tone and swept tones.
 */
public class CWSignalGenerator
{
    public static final double SAMPLE_RATE = 8000.0;
    private final Random mRandom = new Random(99);
    private final List<Float> mSamples = new ArrayList<>();
    private double mNoise = 0.02;
    private double mGuardToneAmplitude;
    private double mGuardPhase;
    private double mTonePhase;

    public CWSignalGenerator noise(double noise)
    {
        mNoise = noise;
        return this;
    }

    /**
     * Adds a continuous tone (like a tone remote control low-level guard tone at 2175 Hz) under all following audio.
     */
    public CWSignalGenerator guardTone(double amplitude)
    {
        mGuardToneAmplitude = amplitude;
        return this;
    }

    private void add(double value)
    {
        mGuardPhase += 2.0 * Math.PI * 2175.0 / SAMPLE_RATE;
        mSamples.add((float)(value + mGuardToneAmplitude * Math.sin(mGuardPhase) + mRandom.nextGaussian() * mNoise));
    }

    public CWSignalGenerator silence(double seconds)
    {
        for(int x = 0; x < (int)(seconds * SAMPLE_RATE); x++)
        {
            add(0.0);
        }

        return this;
    }

    private void tone(double frequency, double seconds, double amplitude)
    {
        int count = (int)Math.round(seconds * SAMPLE_RATE);
        int ramp = (int)(0.002 * SAMPLE_RATE);

        for(int x = 0; x < count; x++)
        {
            //Raised cosine key shaping to limit key clicks
            double shape = 1.0;

            if(x < ramp)
            {
                shape = 0.5 - 0.5 * Math.cos(Math.PI * x / ramp);
            }
            else if(x > count - ramp)
            {
                shape = 0.5 - 0.5 * Math.cos(Math.PI * (count - x) / ramp);
            }

            mTonePhase += 2.0 * Math.PI * frequency / SAMPLE_RATE;
            add(amplitude * shape * Math.sin(mTonePhase));
        }
    }

    /**
     * Adds a linear tone sweep.
     */
    public CWSignalGenerator sweep(double startFrequency, double endFrequency, double seconds)
    {
        int count = (int)(seconds * SAMPLE_RATE);

        for(int x = 0; x < count; x++)
        {
            double frequency = startFrequency + (endFrequency - startFrequency) * x / count;
            mTonePhase += 2.0 * Math.PI * frequency / SAMPLE_RATE;
            add(0.4 * Math.sin(mTonePhase));
        }

        return this;
    }

    /**
     * Adds Morse code keying of the text.
     * @param text to send
     * @param wordsPerMinute speed (unit = 1200 / WPM milliseconds)
     * @param frequency of the tone
     */
    public CWSignalGenerator morse(String text, double wordsPerMinute, double frequency)
    {
        double unit = 1.2 / wordsPerMinute;
        String[] words = text.split(" ");

        for(int w = 0; w < words.length; w++)
        {
            for(int c = 0; c < words[w].length(); c++)
            {
                String code = MorseCodeParser.getCode(words[w].charAt(c));

                for(int e = 0; e < code.length(); e++)
                {
                    tone(frequency, code.charAt(e) == '.' ? unit : 3 * unit, 0.4);

                    if(e < code.length() - 1)
                    {
                        silence(unit);
                    }
                }

                if(c < words[w].length() - 1)
                {
                    silence(3 * unit);
                }
            }

            if(w < words.length - 1)
            {
                silence(7 * unit);
            }
        }

        return this;
    }

    /**
     * Adds externally generated audio (e.g. a paging signal).
     */
    public CWSignalGenerator audio(float[] samples)
    {
        for(float sample: samples)
        {
            add(sample);
        }

        return this;
    }

    public float[] getSamples()
    {
        float[] samples = new float[mSamples.size()];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = mSamples.get(x);
        }

        return samples;
    }
}
