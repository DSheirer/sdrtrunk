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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Native buffer factory for HydraSDR tuners.
 *
 * Receives pre-split I/Q float arrays from the JNI layer and repackages them
 * into power-of-2 length buffers required by downstream SIMD processing.
 *
 * Optimized fast path: when libhydrasdr delivers power-of-2 buffers (common
 * case: 65536 samples), the arrays are passed through with zero copies.
 */
public class HydraSdrNativeBufferFactory
{
	private float[] mIResidual = new float[0];
	private float[] mQResidual = new float[0];
	private long mResidualTimestamp = System.currentTimeMillis();
	private double mFractionalMsAccumulator = 0.0;
	private int mIncomingBufferLength = 0;
	private int mOptimalBufferLength = 128;
	private float mSamplesPerMillisecond;

	/**
	 * Constructs an instance.
	 * @param sampleRate in Hz
	 */
	public HydraSdrNativeBufferFactory(int sampleRate)
	{
		setSampleRate(sampleRate);
	}

	/**
	 * Updates the sample rate.
	 * @param sampleRate in Hz
	 */
	public synchronized void setSampleRate(int sampleRate)
	{
		mSamplesPerMillisecond = sampleRate / 1000.0f;
	}

	/**
	 * Repackages pre-split I/Q sample arrays into optimal-length native buffers.
	 * @param iSamples in-phase float samples
	 * @param qSamples quadrature float samples
	 * @param sampleCount number of complex samples
	 * @param timestamp of the sample block
	 * @return zero or more repackaged native buffers
	 */
	public synchronized List<HydraSdrNativeBuffer> get(float[] iSamples, float[] qSamples,
		int sampleCount, long timestamp)
	{
		updateBufferLength(sampleCount);

		/*
		 * Fast path: no residual and incoming buffer is exactly the optimal size.
		 * Copy I/Q arrays since the JNI layer reuses the underlying array objects
		 * (double-buffered). Arrays.copyOf compiles to a fast memcpy.
		 */
		if(mIResidual.length == 0 && sampleCount == mOptimalBufferLength)
		{
			mResidualTimestamp = timestamp;
			return Collections.singletonList(new HydraSdrNativeBuffer(
				Arrays.copyOf(iSamples, sampleCount),
				Arrays.copyOf(qSamples, sampleCount),
				timestamp, mSamplesPerMillisecond));
		}

		/* Slow path: combine residual with incoming and split */
		float[] iCombined = new float[mIResidual.length + sampleCount];
		System.arraycopy(mIResidual, 0, iCombined, 0, mIResidual.length);
		System.arraycopy(iSamples, 0, iCombined, mIResidual.length, sampleCount);

		float[] qCombined = new float[mQResidual.length + sampleCount];
		System.arraycopy(mQResidual, 0, qCombined, 0, mQResidual.length);
		System.arraycopy(qSamples, 0, qCombined, mQResidual.length, sampleCount);

		if(iCombined.length < mOptimalBufferLength)
		{
			mIResidual = iCombined;
			mQResidual = qCombined;
			return Collections.emptyList();
		}

		List<HydraSdrNativeBuffer> buffers = new ArrayList<>();

		while(iCombined.length >= mOptimalBufferLength)
		{
			float[] iOpt = Arrays.copyOf(iCombined, mOptimalBufferLength);
			float[] qOpt = Arrays.copyOf(qCombined, mOptimalBufferLength);
			iCombined = Arrays.copyOfRange(iCombined, mOptimalBufferLength, iCombined.length);
			qCombined = Arrays.copyOfRange(qCombined, mOptimalBufferLength, qCombined.length);

			buffers.add(new HydraSdrNativeBuffer(iOpt, qOpt,
				mResidualTimestamp, mSamplesPerMillisecond));
			/* Accumulate fractional milliseconds to avoid timestamp drift.
			 * e.g. 65536 samples @ 10 MSps = 6.5536 ms; truncating each buffer
			 * would lose ~0.55 ms per buffer (~84 ms/sec drift). */
			mFractionalMsAccumulator += (mOptimalBufferLength / (double)mSamplesPerMillisecond);
			long whole = (long)mFractionalMsAccumulator;
			mResidualTimestamp += whole;
			mFractionalMsAccumulator -= whole;
		}

		mIResidual = iCombined;
		mQResidual = qCombined;
		mResidualTimestamp = timestamp;

		return buffers;
	}

	/**
	 * Updates the optimal buffer length.
	 * Target is the nearest power-of-2 >= sample count.
	 */
	private void updateBufferLength(int sampleCount)
	{
		if(mIncomingBufferLength != sampleCount)
		{
			int optimal = 128;
			while(optimal < sampleCount)
			{
				optimal *= 2;
			}
			mOptimalBufferLength = optimal;
			mIncomingBufferLength = sampleCount;
		}
	}
}
