/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.halfband.complex;

import io.github.dsheirer.dsp.filter.Filters;
import io.github.dsheirer.dsp.filter.halfband.real.HalfBandFilter2;
import io.github.dsheirer.dsp.mixer.Oscillator;
import io.github.dsheirer.sample.buffer.ComplexBuffer;

public class ComplexHalfBandFilter
{
    private Object mBufferQueue = new Object();

    private HalfBandFilter2 mIFilter;
    private HalfBandFilter2 mQFilter;
    private float mResidualISample;
    private float mResidualQSample;
    private boolean mHasResidual;
    private int mOutputBufferLength;
    private float[] mSamples;
    private int mSamplesPointer;
    private float[] mFilteredSamples;
    private int mFilteredSamplesPointer;

    public ComplexHalfBandFilter(float[] coefficients, float gain)
    {
        mIFilter = new HalfBandFilter2(coefficients, gain);
        mQFilter = new HalfBandFilter2(coefficients, gain);
    }

    public ComplexBuffer filter(ComplexBuffer originalBuffer)
    {
        mSamples = originalBuffer.getSamples();
        mSamplesPointer = 0;

        mOutputBufferLength = (mHasResidual ? (mSamples.length + 2) : mSamples.length) / 4 * 2;

        ComplexBuffer buffer = new ComplexBuffer(new float[mOutputBufferLength]);
        ComplexBuffer filteredBuffer = buffer;
        mFilteredSamples = filteredBuffer.getSamples();
        mFilteredSamplesPointer = 0;

        if(mHasResidual)
        {
            mFilteredSamples[mFilteredSamplesPointer++] = mIFilter.filter(mResidualISample, mSamples[mSamplesPointer++]);
            mFilteredSamples[mFilteredSamplesPointer++] = mQFilter.filter(mResidualQSample, mSamples[mSamplesPointer++]);
            mHasResidual = false;
        }

        while(mSamplesPointer + 3 < mSamples.length)
        {
            mFilteredSamples[mFilteredSamplesPointer++] = mIFilter.filter(mSamples[mSamplesPointer], mSamples[mSamplesPointer + 2]);
            mFilteredSamples[mFilteredSamplesPointer++] = mIFilter.filter(mSamples[mSamplesPointer + 1], mSamples[mSamplesPointer + 3]);
            mSamplesPointer += 4;
        }

        if(mSamplesPointer < mSamples.length)
        {
            mHasResidual = true;
            mResidualISample = mSamples[mSamplesPointer++];
            mResidualQSample = mSamples[mSamplesPointer];
        }

        return filteredBuffer;
    }

    public static void main(String[] args)
    {
        Oscillator oscillator = new Oscillator(1, 16);
        ComplexHalfBandFilter filter = new ComplexHalfBandFilter(Filters.HALF_BAND_FILTER_27T.getCoefficients(), 1.0f);

        Object bufferQueue = new Object();

        ComplexBuffer buffer2 = new ComplexBuffer(new float[8]);
        ComplexBuffer complexBuffer = buffer2;
        oscillator.generateComplex(complexBuffer);
        ComplexBuffer filteredBuffer = filter.filter(complexBuffer);

        ComplexBuffer buffer1 = new ComplexBuffer(new float[10]);
        ComplexBuffer complexBuffer2 = buffer1;
        oscillator.generateComplex(complexBuffer2);
        ComplexBuffer filteredBuffer2 = filter.filter(complexBuffer2);

        ComplexBuffer buffer = new ComplexBuffer(new float[8]);
        ComplexBuffer complexBuffer3 = buffer;
        oscillator.generateComplex(complexBuffer3);
        ComplexBuffer filteredBuffer3 = filter.filter(complexBuffer3);

        }
}
