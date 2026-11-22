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

/**
 * 3600 baud binary FSK demodulator for the Motorola Type II control channel.  Same design as the EDACS control
 * channel demodulator, which decodes a 9600 baud control channel.
 *
 * FM discriminator, a boxcar low-pass of about half a symbol, slow DC removal, then Gardner symbol timing recovery
 * with linear interpolation.  Bits are sliced at zero; polarity is resolved by the framer.
 */
public class SmartNetDemodulator
{
    public static final double SYMBOL_RATE = 3600.0;
    private static final double TIMING_GAIN = 0.05;
    private static final double DC_ALPHA = 0.0005;

    public interface BitListener
    {
        void bit(boolean bit);
    }

    private final BitListener mListener;
    private final double mSamplesPerSymbol;
    private final float[] mBoxcar;
    private int mBoxcarIndex;
    private float mBoxcarSum;
    private float mPreviousI = 1, mPreviousQ = 0;
    private double mDC;
    private double mAmplitude = 1;

    //Timing: history of filtered samples for interpolation
    private final float[] mHistory = new float[64];
    private long mSampleIndex;
    private double mNextStrobe;
    private float mPreviousSymbol;

    public SmartNetDemodulator(double sampleRate, BitListener listener)
    {
        mListener = listener;
        mSamplesPerSymbol = sampleRate / SYMBOL_RATE;
        mBoxcar = new float[Math.max(1, (int)Math.round(mSamplesPerSymbol / 2))];
        mNextStrobe = mSamplesPerSymbol;
    }

    public double getSamplesPerSymbol()
    {
        return mSamplesPerSymbol;
    }

    /**
     * Processes complex samples.
     */
    public void process(float[] i, float[] q, int length)
    {
        for(int x = 0; x < length; x++)
        {
            //FM discriminator: angle of s[n] * conj(s[n-1])
            float re = i[x] * mPreviousI + q[x] * mPreviousQ;
            float im = q[x] * mPreviousI - i[x] * mPreviousQ;
            mPreviousI = i[x];
            mPreviousQ = q[x];
            float demod = (float)Math.atan2(im, re);

            //Boxcar low-pass
            mBoxcarSum += demod - mBoxcar[mBoxcarIndex];
            mBoxcar[mBoxcarIndex] = demod;
            mBoxcarIndex = (mBoxcarIndex + 1) % mBoxcar.length;
            float filtered = mBoxcarSum / mBoxcar.length;

            //DC removal
            mDC += (filtered - mDC) * DC_ALPHA;
            float sample = (float)(filtered - mDC);

            mHistory[(int)(mSampleIndex & 63)] = sample;
            mSampleIndex++;

            while(mNextStrobe <= mSampleIndex - 2)
            {
                float symbol = interpolate(mNextStrobe);
                float middle = interpolate(mNextStrobe - mSamplesPerSymbol / 2.0);

                //Gardner timing error, normalized by the signal amplitude
                mAmplitude += (Math.abs(symbol) - mAmplitude) * 0.01;
                double error = (symbol - mPreviousSymbol) * middle / Math.max(1e-6, mAmplitude * mAmplitude);
                error = Math.max(-1, Math.min(1, error));
                mPreviousSymbol = symbol;

                mListener.bit(symbol > 0);
                mNextStrobe += mSamplesPerSymbol - TIMING_GAIN * error;
            }
        }
    }

    /**
     * Linear interpolation of the filtered sample history at a fractional sample position.
     */
    private float interpolate(double position)
    {
        long base = (long)Math.floor(position);
        double mu = position - base;

        if(base < mSampleIndex - 63 || base + 1 >= mSampleIndex)
        {
            return mHistory[(int)((mSampleIndex - 1) & 63)];
        }

        float a = mHistory[(int)(base & 63)];
        float b = mHistory[(int)((base + 1) & 63)];
        return (float)(a + (b - a) * mu);
    }
}
