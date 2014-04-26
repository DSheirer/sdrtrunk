/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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
package dsp.filter;

import java.util.ArrayList;

import sample.Listener;
import sample.complex.ComplexSample;

public class ComplexHalfBandNoDecimateFilter extends ComplexFilter
{
	private Listener<ComplexSample> mListener;
	private ArrayList<ComplexSample> mBuffer;
    private int mBufferSize = 1; //Temporary initial value
	private int mBufferPointer = 0;
	private double mGain;
	private double[] mCoefficients;
	private int[][] mIndexMap;
	private int mCenterCoefficient;
	private int mCenterCoefficientMapIndex;
	

	/**
	 * Half-Band filter with no decimation against complex samples composed of 
	 * real valued floats.
	 * 
	 * Takes advantage of the symmetrical nature of FIR filter coefficients by
	 * adding oldest and newest sample first, then multiplying once by the 
	 * corresponding coefficient
	 * 
	 * Also, takes advantage of the 0-valued FIR half-band coefficents inherent
	 * in the half-band filter, and does not calculate those coefficients.
	 * 
	 * This reduces the workload to (tap-size - 1) / 4 + 1 calculations per sample.
	 * 
	 * @param filter - filter coefficients
	 * @param gain - gain multiplier.  Use 1.0 for unity/no gain
	 */
	public ComplexHalfBandNoDecimateFilter( Filters filter, double gain )
	{
		mCoefficients = filter.getCoefficients();
		mBuffer = new ArrayList<ComplexSample>();
		mBufferSize = mCoefficients.length;
		
		//Fill the buffer with zero valued samples
		for( int x = 0; x < mCoefficients.length; x++ )
		{
			mBuffer.add( new ComplexSample( (float)0.0, (float)0.0 ) );
		}
		
		generateIndexMap( mCoefficients.length );
		mGain = gain;
	}

	/**
	 * Calculate the filtered value by applying the coefficients against
	 * the complex samples in mBuffer
	 */
	public void receive( ComplexSample newSample )
	{
		//Add the new sample to the buffer
		mBuffer.set( mBufferPointer, newSample );

		//Increment & Adjust the buffer pointer for circular wrap around
		mBufferPointer++;

		if( mBufferPointer >= mBufferSize )
		{
			mBufferPointer = 0;
		}

		//Convolution - multiply filter coefficients by the circular buffer 
		//samples to calculate a new filtered value
		double leftAccumulator = 0;
		double rightAccumulator = 0;

		//Start with the center tap value
		leftAccumulator += mCoefficients[ mCenterCoefficient ] * 
				mBuffer.get( mIndexMap[ mBufferPointer ][ mCenterCoefficientMapIndex ] ).left();
		rightAccumulator += mCoefficients[ mCenterCoefficient ] * 
				mBuffer.get( mIndexMap[ mBufferPointer ][ mCenterCoefficientMapIndex ] ).right();
		
		//For the remaining coefficients, add the symmetric samples, oldest and newest
		//first, then multiply by the single coefficient
		for( int x = 0; x < mCenterCoefficientMapIndex; x += 2 )
		{
			leftAccumulator += mCoefficients[ x ] *
				( mBuffer.get( mIndexMap[ mBufferPointer ][ x ] ).left() + 
				  mBuffer.get( mIndexMap[ mBufferPointer ][ x + 1 ] ).left() );
			
			rightAccumulator += mCoefficients[ x ] *
					( mBuffer.get( mIndexMap[ mBufferPointer ][ x ] ).right() + 
					  mBuffer.get( mIndexMap[ mBufferPointer ][ x + 1 ] ).right() );
		}

		//We're almost finished ... apply gain, cast the doubles to floats and
		//send it on it's merry way
		send( new ComplexSample( (float)( leftAccumulator * mGain ),
								 (float)( rightAccumulator * mGain ) ) );
	}
	
	/**
	 * Creates an n X (n + 1 / 2) index map enabling quick access to the 
	 * circular buffer samples.  
	 * 
	 * As the buffer shifts right with each subsequent sample, we have to move 
	 * the index pointers with it, for efficient access of the samples.
	 * 
	 * The first array index value in the index map corresponds to the current 
	 * buffer pointer location.
	 * 
	 * The second array index value points to the samples that should be
	 * multiplied by the coefficients as follows:
	 *   
	 * 0 = center tap sample, to be multiplied by center coefficient
	 * 
	 * 0 = sample( 1 )
	 * 1 = sample( size - 1 )
	 * 
	 * Indexes 0 and 1 will be multiplied by coefficient( 0 ).
	 * 
	 * Subsequent indexes 3, 4, etc, point to the oldest and newest samples that 
	 * correspond to the matching ( 3 ) coefficient index. 
	 * 
	 * @param odd-sized number of filter taps (ie coefficients) and buffer
	 */
	private void generateIndexMap( int size )
	{
		//Ensure we have an odd size
		assert( size % 2 == 1 );

		int mapWidth = ( ( size + 1 ) / 2 ) + 1;
		
		//Set center tap index for coefficients array
		mCenterCoefficient = ( size - 1 ) / 2;
		mCenterCoefficientMapIndex = mCenterCoefficient + 1;

		mIndexMap = new int[ size ][ mapWidth ];

		//Setup the first row, buffer pointer index 0, as a starting point
		for( int x = 0; x < mapWidth - 2; x += 2 )
		{
			mIndexMap[ 0 ][ x ] = x;
			mIndexMap[ 0 ][ x + 1 ] = size - 1 - x;
		}

		//Place center tap index in last element
		mIndexMap[ 0 ][ mCenterCoefficientMapIndex ] = mCenterCoefficient;
		
		//For each subsequent row, increment the previous row's value by 1, 
		//subtracting size as needed, to keep the values between 0 and size - 1
		for( int x = 1; x < size; x++ )
		{
			for( int y = 0; y < mapWidth; y++ )
			{
				mIndexMap[ x ][ y ] = mIndexMap[ x - 1 ][ y ] + 1;
				
				if( mIndexMap[ x ][ y ] >= size )
				{
					mIndexMap[ x ][ y ] -= size;
				}
			}
		}
	}
}
