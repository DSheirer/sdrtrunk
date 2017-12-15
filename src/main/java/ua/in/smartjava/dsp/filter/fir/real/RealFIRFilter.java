/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package ua.in.smartjava.dsp.filter.fir.real;

import ua.in.smartjava.dsp.filter.fir.FIRFilter;

public class RealFIRFilter extends FIRFilter
{
	private float[] mBuffer;
    private int mBufferSize = 1;
	private int mBufferPointer = 0;
	private int[][] mIndexMap;
	
	private float[] mCoefficients;
	private float mGain;
	
	/**
	 * Float ua.in.smartjava.sample FIR ua.in.smartjava.filter base class.
	 * 
	 * @param coefficients - ua.in.smartjava.filter coefficients
	 * @param gain value to apply to the filtered output - use 1.0f for no gain
	 */
	public RealFIRFilter( float[] coefficients, float gain )
	{
		mCoefficients = coefficients;
		mGain = gain;

		mBufferSize = mCoefficients.length;

		mBuffer = new float[ mBufferSize ];
		mBufferPointer = mBufferSize - 1;

		generateIndexMap( mBufferSize );
	}
	
	public float[] getCoefficients()
	{
		return mCoefficients;
	}
	
	@Override
	public void dispose()
	{
		mCoefficients = null;
		mIndexMap = null;
		mBuffer = null;
	}

	public float filter( float sample )
	{
		mBuffer[ mBufferPointer ] = sample;
		
		float accumulator = 0.0f;

		for( int x = 0; x < mBufferSize; x++ )
		{
			accumulator += mCoefficients[ x ] * 
					mBuffer[ mIndexMap[ mBufferPointer][ x ] ];
		}

		mBufferPointer--;

		if( mBufferPointer < 0 )
		{
			mBufferPointer += mBufferSize;
		}

		/* Apply gain and return the filtered value */
		return accumulator * mGain;
	}

	/**
	 * Generates a circular ua.in.smartjava.buffer index ua.in.smartjava.map to support lookup of the translated
	 * index based on the current ua.in.smartjava.buffer pointer and the desired ua.in.smartjava.sample index.
	 */
	private void generateIndexMap( int size )
	{
		mIndexMap = new int[ size ][ size ];
		
		for( int x = 0; x < size; x++ )
		{
			for( int y = 0; y < size; y++ )
			{
				int z = x + y;
				
				mIndexMap[ x ][ y ] = z < size ? z : z - size;
			}
		}
	}
}