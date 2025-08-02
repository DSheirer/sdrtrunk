/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.demod;

import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;

/**
 * Differential demodulator base class
 */
public abstract class DifferentialDemodulator implements IDemodulator
{
    private int mSymbolRate;
    private float mSampleRate;
    private float mSamplesPerSymbol;
    protected float[] mIBuffer = new float[20]; //Initial size 20 for array copy, but gets resized on first buffer
    protected float[] mQBuffer = new float[20];
    protected float mMu;
    protected int mBufferOverlap;
    protected int mInterpolationOffset;
    protected final Interpolator mInterpolator = InterpolatorFactory.getInterpolator();
    private DifferentialDemodulatorScalar mScalar;

   /**
     * Constructor
     * @param sampleRate in Hertz
     * @param symbolRate symbols per second
     */
    public DifferentialDemodulator(double sampleRate, int symbolRate)
    {
        mSampleRate = (float) sampleRate;
        mSymbolRate = symbolRate;
        mSamplesPerSymbol = mSampleRate / mSymbolRate;
        mMu = mSamplesPerSymbol % 1; //Fractional part of the samples per symbol rate
        mInterpolationOffset = (int) Math.floor(mSamplesPerSymbol) - 4; //Interpolate at the middle of 8x samples
        mBufferOverlap = (int) Math.floor(mSamplesPerSymbol) + 4;

        while(mInterpolationOffset < 0)
        {
            mInterpolationOffset++;
            mBufferOverlap++;
        }
    }

    /**
     * Scalar implementation for fallback when the sample buffer size is not compatible with the vector implementation.
     * @return scalar implementation lazy instantiated when needed.
     */
    protected DifferentialDemodulatorScalar getScalarImplementation()
    {
        if(mScalar == null)
        {
            mScalar = new DifferentialDemodulatorScalar(mSampleRate, mSymbolRate);
        }

        return mScalar;
    }

    /**
     * Calculated samples per symbol.
     * @return samples per symbol.
     */
    public float getSamplesPerSymbol()
    {
        return mSamplesPerSymbol;
    }
}
