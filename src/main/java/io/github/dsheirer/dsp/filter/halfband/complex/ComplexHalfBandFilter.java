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
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;

public class ComplexHalfBandFilter
{
    private ReusableComplexBufferQueue mBufferQueue = new ReusableComplexBufferQueue("ComplexHalfBandFilter");

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

    public ReusableComplexBuffer filter(ReusableComplexBuffer originalBuffer)
    {
        mSamples = originalBuffer.getSamples();
        mSamplesPointer = 0;

        mOutputBufferLength = (mHasResidual ? (mSamples.length + 2) : mSamples.length) / 4 * 2;

        ReusableComplexBuffer filteredBuffer = mBufferQueue.getBuffer(mOutputBufferLength);
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

        originalBuffer.decrementUserCount();
        return filteredBuffer;
    }

    public static void main(String[] args)
    {
        Oscillator oscillator = new Oscillator(1, 16);
        ComplexHalfBandFilter filter = new ComplexHalfBandFilter(Filters.HALF_BAND_FILTER_27T.getCoefficients(), 1.0f);

        ReusableComplexBufferQueue bufferQueue = new ReusableComplexBufferQueue("Test");

        ReusableComplexBuffer complexBuffer = bufferQueue.getBuffer(8);
        oscillator.generateComplex(complexBuffer);
        ReusableComplexBuffer filteredBuffer = filter.filter(complexBuffer);
        filteredBuffer.decrementUserCount();

        ReusableComplexBuffer complexBuffer2 = bufferQueue.getBuffer(10);
        oscillator.generateComplex(complexBuffer2);
        ReusableComplexBuffer filteredBuffer2 = filter.filter(complexBuffer2);
        filteredBuffer2.decrementUserCount();

        ReusableComplexBuffer complexBuffer3 = bufferQueue.getBuffer(8);
        oscillator.generateComplex(complexBuffer3);
        ReusableComplexBuffer filteredBuffer3 = filter.filter(complexBuffer3);
        filteredBuffer3.decrementUserCount();





    }
}
