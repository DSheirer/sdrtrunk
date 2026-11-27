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

package io.github.dsheirer.module.decode.pager;

/**
 * Audio frequency shift keying (AFSK) tone discriminator.  Converts audio containing two alternating tones into a
 * baseband signal that is positive while the upper tone is present and negative while the lower tone is present.
 *
 * Some paging systems send POCSAG over a voice channel as AFSK audio tones, instead of directly frequency shift keying
 * the carrier.  The baseband output of this discriminator can be processed like the direct FSK discriminator audio.
 *
 * Each tone's magnitude is measured with a quadrature mixer and a moving average (boxcar) filter.  A 1 millisecond
 * window places the filter nulls at multiples of 1 kHz, which rejects the other tone for the 1200/2200 Hz (Bell 202)
 * tone pair.  The output is normalized by the total tone magnitude so it doesn't depend on the audio level.
 */
public class AfskToneDiscriminator
{
    private static final float MINIMUM_MAGNITUDE = 1E-6f;
    private final ToneDetector mLowerTone;
    private final ToneDetector mUpperTone;

    /**
     * Constructs an instance
     * @param sampleRate of the audio
     * @param lowerTone frequency in Hertz
     * @param upperTone frequency in Hertz
     * @param windowSeconds duration of the tone magnitude averaging window
     */
    public AfskToneDiscriminator(double sampleRate, double lowerTone, double upperTone, double windowSeconds)
    {
        int window = Math.max(1, (int)Math.round(sampleRate * windowSeconds));
        mLowerTone = new ToneDetector(sampleRate, lowerTone, window);
        mUpperTone = new ToneDetector(sampleRate, upperTone, window);
    }

    /**
     * Processes the audio buffer
     * @param samples of audio
     * @return baseband samples in the range -1.0 (lower tone) to 1.0 (upper tone)
     */
    public float[] process(float[] samples)
    {
        float[] output = new float[samples.length];

        for(int x = 0; x < samples.length; x++)
        {
            float lower = mLowerTone.process(samples[x]);
            float upper = mUpperTone.process(samples[x]);
            output[x] = (upper - lower) / Math.max(upper + lower, MINIMUM_MAGNITUDE);
        }

        return output;
    }

    /**
     * Resets the detectors.
     */
    public void reset()
    {
        mLowerTone.reset();
        mUpperTone.reset();
    }

    /**
     * Single tone magnitude detector: quadrature mixer with moving average filtering of the I and Q products.
     */
    private static class ToneDetector
    {
        private final double mPhaseIncrement;
        private final float[] mInphase;
        private final float[] mQuadrature;
        private double mPhase;
        private float mInphaseSum;
        private float mQuadratureSum;
        private int mPointer;

        ToneDetector(double sampleRate, double frequency, int window)
        {
            mPhaseIncrement = 2.0 * Math.PI * frequency / sampleRate;
            mInphase = new float[window];
            mQuadrature = new float[window];
        }

        float process(float sample)
        {
            float i = (float)(sample * Math.cos(mPhase));
            float q = (float)(sample * -Math.sin(mPhase));

            mPhase += mPhaseIncrement;

            if(mPhase > 2.0 * Math.PI)
            {
                mPhase -= 2.0 * Math.PI;
            }

            mInphaseSum += i - mInphase[mPointer];
            mQuadratureSum += q - mQuadrature[mPointer];
            mInphase[mPointer] = i;
            mQuadrature[mPointer] = q;
            mPointer = (mPointer + 1) % mInphase.length;

            //Recalculate the running sums once per pass through the buffer to prevent floating point drift
            if(mPointer == 0)
            {
                float inphaseSum = 0.0f;
                float quadratureSum = 0.0f;

                for(int x = 0; x < mInphase.length; x++)
                {
                    inphaseSum += mInphase[x];
                    quadratureSum += mQuadrature[x];
                }

                mInphaseSum = inphaseSum;
                mQuadratureSum = quadratureSum;
            }

            return (float)Math.sqrt(mInphaseSum * mInphaseSum + mQuadratureSum * mQuadratureSum);
        }

        void reset()
        {
            java.util.Arrays.fill(mInphase, 0.0f);
            java.util.Arrays.fill(mQuadrature, 0.0f);
            mInphaseSum = 0.0f;
            mQuadratureSum = 0.0f;
            mPointer = 0;
            mPhase = 0.0;
        }
    }
}
