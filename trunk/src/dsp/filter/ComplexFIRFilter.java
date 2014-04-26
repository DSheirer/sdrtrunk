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

import sample.complex.ComplexSample;

public class ComplexFIRFilter extends ComplexFilter
{
	private ArrayList<ComplexSample> mBuffer;
    private int mBufferSize = 1; //Temporary initial value
	private int mBufferPointer = 0;
	private double[] mCoefficients;
	private int[][] mIndexMap;
	private int mCenterCoefficient;
	private int mCenterCoefficientMapIndex;
	private double mGain;

	public ComplexFIRFilter( double[] coefficients, double gain )
	{
		mCoefficients = coefficients;
		mBuffer = new ArrayList<ComplexSample>();
		mBufferSize = mCoefficients.length;
		mGain = gain;
		
		//Fill the buffer with zero valued samples, so we don't have to check for null
		for( int x = 0; x < mCoefficients.length; x++ )
		{
			mBuffer.add( new ComplexSample( (float)0.0, (float)0.0 ) );
		}
		
		generateIndexMap( mCoefficients.length );
	}
	
	public void dispose()
	{
		mBuffer.clear();
		mBuffer = null;
		
		mCoefficients = null;
		mIndexMap = null;
	}

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
		for( int x = 0; x < mCenterCoefficient; x++ )
		{
			leftAccumulator += mCoefficients[ x ] *
				( mBuffer.get( mIndexMap[ mBufferPointer ][ x ] ).left() + 
				  mBuffer.get( mIndexMap[ mBufferPointer ][ x + mCenterCoefficient ] ).left() );
			
			rightAccumulator += mCoefficients[ x ] *
					( mBuffer.get( mIndexMap[ mBufferPointer ][ x ] ).right() + 
					  mBuffer.get( mIndexMap[ mBufferPointer ][ x + mCenterCoefficient ] ).right() );
		}

		//We're almost finished ... apply gain, cast the doubles to floats and
		//send it on it's merry way
		send( new ComplexSample( (float)( leftAccumulator * mGain ),
								 (float)( rightAccumulator * mGain ) ) );
	}

	/**
	 * @param odd-sized number of filter taps (ie coefficients) and buffer
	 */
	private void generateIndexMap( int size )
	{
		//Ensure we have an odd size
		assert( size % 2 == 1 );
		
		mIndexMap = new int[ size ][ size ];
		
		//Last column will be the center coefficient index value for each row
		mCenterCoefficientMapIndex = size - 1;
		mCenterCoefficient = (int)( size / 2 );

		//Setup the first row.  Offset is the first row's center coefficient value
		//and will become the index value we place in the last column
		mIndexMap[ 0 ][ mCenterCoefficientMapIndex ] = mCenterCoefficient;

		//Indexes 0 to 1/2 are their index value, and indexes 1/2 to end are
		//an offset of the first half of the values
		for( int x = 0; x < mCenterCoefficient; x++ )
		{
			mIndexMap[ 0 ][ x ] = x;
			mIndexMap[ 0 ][ x + mCenterCoefficient ] = size - 1 - x;
		}
		
		//For each subsequent map row, increment the value of the preceding row
		//same column by 1, wrapping to zero when we exceed the size value
		for( int x = 1; x < size; x++ )
		{
			for( int y = 0; y < size; y++ )
			{
				mIndexMap[ x ][ y ] = mIndexMap[ x - 1 ][ y ] + 1;
				
				if( mIndexMap[ x ][ y ] >= size )
				{
					mIndexMap[ x ][ y ] = 0;
				}
			}
		}
	}

	public String printIndexMap()
	{
		StringBuilder sb = new StringBuilder();
		
		for( int x = 0; x < mCoefficients.length; x++ )
		{
			for( int y = 0; y < mCoefficients.length; y++ )
			{
				sb.append( mIndexMap[ x ][ y ] + "  " );
			}
			sb.append( "\n" );
		}

		return sb.toString();
	}
}
