/*
 * *****************************************************************************
 * Copyright (C) 2024-2025 Benjamin VERNOUX
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
package io.github.dsheirer.source.tuner.hydrasdr;

import io.github.dsheirer.buffer.AbstractNativeBuffer;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.Iterator;

/**
 * Native buffer for HydraSDR float32 IQ samples received from libhydrasdr.
 *
 * Stores pre-split I and Q arrays. The de-interleaving is performed in the
 * native JNI layer using an unrolled C loop, which is significantly faster
 * than doing it in Java. This means the iterator() call delivers
 * ComplexSamples with zero additional copies.
 */
public class HydraSdrNativeBuffer extends AbstractNativeBuffer
{
	private float[] mISamples;
	private float[] mQSamples;

	/**
	 * Constructs an instance with pre-split I/Q arrays.
	 * @param iSamples in-phase float samples
	 * @param qSamples quadrature float samples
	 * @param timestamp for the first sample
	 * @param samplesPerMillisecond for sub-buffer fragment timestamp calculation
	 */
	public HydraSdrNativeBuffer(float[] iSamples, float[] qSamples, long timestamp,
		float samplesPerMillisecond)
	{
		super(timestamp, samplesPerMillisecond);
		mISamples = iSamples;
		mQSamples = qSamples;
	}

	@Override
	public int sampleCount()
	{
		return mISamples.length * 2;
	}

	@Override
	public Iterator<ComplexSamples> iterator()
	{
		return new SampleIterator();
	}

	@Override
	public Iterator<InterleavedComplexSamples> iteratorInterleaved()
	{
		return new InterleavedSampleIterator();
	}

	/**
	 * Iterator providing non-interleaved complex sample buffers.
	 * Zero-copy: returns the pre-split I/Q arrays directly.
	 */
	private class SampleIterator implements Iterator<ComplexSamples>
	{
		private boolean mHasNext = true;

		@Override
		public boolean hasNext()
		{
			return mHasNext;
		}

		@Override
		public ComplexSamples next()
		{
			mHasNext = false;
			return new ComplexSamples(mISamples, mQSamples, getTimestamp());
		}
	}

	/**
	 * Iterator providing interleaved complex sample buffers.
	 * Re-interleaves from the stored I/Q arrays.
	 */
	private class InterleavedSampleIterator implements Iterator<InterleavedComplexSamples>
	{
		private boolean mHasNext = true;

		@Override
		public boolean hasNext()
		{
			return mHasNext;
		}

		@Override
		public InterleavedComplexSamples next()
		{
			mHasNext = false;
			float[] interleaved = new float[mISamples.length * 2];
			for(int x = 0; x < mISamples.length; x++)
			{
				interleaved[x * 2] = mISamples[x];
				interleaved[x * 2 + 1] = mQSamples[x];
			}
			return new InterleavedComplexSamples(interleaved, getTimestamp());
		}
	}
}
