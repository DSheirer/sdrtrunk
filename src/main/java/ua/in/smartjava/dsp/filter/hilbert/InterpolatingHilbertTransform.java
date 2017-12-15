package ua.in.smartjava.dsp.filter.hilbert;

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
 * 
 * ----------------------------------------------------------------------------
 *  Half-band ua.in.smartjava.filter coefficients retrieved October 2015 from:
 *  https://github.com/airspy/host/libairspy/src/filters.h
 *  
 *  Copyright (C) 2014, Youssef Touil <youssef@airspy.com>
 *  
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 ******************************************************************************/

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.ComplexBuffer;
import ua.in.smartjava.sample.real.RealBuffer;

public class InterpolatingHilbertTransform implements Listener<RealBuffer>
{
	private static final Logger mLog = LoggerFactory.getLogger( InterpolatingHilbertTransform.class );

	private static final float[] HALF_BAND_FILTER = 
	{
		-0.000998606272947510f, 0.0f,  0.001695637278417295f, 0.0f,
		-0.003054430179754289f, 0.0f,  0.005055504379767936f, 0.0f,
		-0.007901319195893647f, 0.0f,  0.011873357051047719f, 0.0f,
		-0.017411159379930066f, 0.0f,  0.025304817427568772f, 0.0f,
		-0.037225225204559217f, 0.0f,  0.057533286997004301f, 0.0f,
		-0.102327462004259350f, 0.0f,  0.317034472508947400f, 0.5f,
		 0.317034472508947400f, 0.0f, -0.102327462004259350f, 0.0f,
		 0.057533286997004301f, 0.0f, -0.037225225204559217f, 0.0f,
		 0.025304817427568772f, 0.0f, -0.017411159379930066f, 0.0f,
		 0.011873357051047719f, 0.0f, -0.007901319195893647f, 0.0f,
		 0.005055504379767936f, 0.0f, -0.003054430179754289f, 0.0f,
		 0.001695637278417295f, 0.0f, -0.000998606272947510f 
	};
	
	private static final float GAIN = 2.0f;
	
	private Listener<ComplexBuffer> mListener;

	private float[] mBuffer;
    private int mBufferSize;
	private int mBufferPointer;
	private int mGroupDelayedInphaseTapPointIndex;
	
	private float[] mCoefficients;
	private int[][] mIndexMap;

	
	/**
	 * Hilbert transform ua.in.smartjava.filter used for converting real-valued samples into
	 * complex valued samples using frequency translation (FS/4) and a half-band
	 * ua.in.smartjava.filter.
	 * 
	 * This ua.in.smartjava.filter uses a circular ua.in.smartjava.sample ua.in.smartjava.buffer and a pre-calculated
	 * index ua.in.smartjava.map to correctly ua.in.smartjava.map the ua.in.smartjava.filter coefficients to the circular delay
	 * ua.in.smartjava.buffer contents as each new ua.in.smartjava.sample is added to the ua.in.smartjava.buffer and processed.
	 * 
	 * Half-band ua.in.smartjava.filter coefficients used in this ua.in.smartjava.filter must be of length N
	 * where (N + 1) is a multiple of 4.  This ua.in.smartjava.filter uses a pre-defined half
	 * band ua.in.smartjava.filter with 47 coefficients.
	 * 
	 * This transform process is described in Understanding Digital Signal 
	 * Processing, Lyons, 3e, 2011, sections 13.1.2 and 13.1.3 (p 674-678) and 
	 * implemented as described in Section 13.37.1 and 13.37.2 (p 802-804)
	 */
	public InterpolatingHilbertTransform()
	{
		mBufferSize = HALF_BAND_FILTER.length;
		mBuffer = new float[ mBufferSize ];
		
		createHilbertCoefficients();
		
		generateIndexMap( mBufferSize );
	}

	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( filter( buffer ) );
		}
	}
	
	/**
	 * Sets the listener to receive filtered complex buffers.
	 */
	public void setListener( Listener<ComplexBuffer> listener )
	{
		mListener = listener;
	}
	
	/**
	 * Applies Hilbert transform to the real ua.in.smartjava.sample ua.in.smartjava.buffer and returns a
	 * complex ua.in.smartjava.sample ua.in.smartjava.buffer
	 */
	public ComplexBuffer filter( RealBuffer buffer )
	{
		return new ComplexBuffer( filter( buffer.getSamples() ) );
	}

	/**
	 * Applies a hilbert transform to an array of real samples and returns an 
	 * array twice as long containing I,Q,I,Q ... complex samples
	 */
	public float[] filter( float[] samples )
	{
		float[] filtered = new float[ samples.length * 2 ];

		float accumulator;

		for( int y = 0; y < samples.length; y++ )
		{
			insert( samples[ y ] );
			
			accumulator = 0.0f;

			for( int x = 0; x < mCoefficients.length; x++ )
			{
				accumulator += mCoefficients[ x ] * 
							   mBuffer[ mIndexMap[ mBufferPointer ][ x ] ] ;
			}

			int index = y * 2;

			/* We use the buffered ua.in.smartjava.sample at the half group delay point as the
			 * Inphase value and the accumulator as the quadrature value */
			filtered[ index ] = mBuffer[ mIndexMap[ mBufferPointer ]
					[ mGroupDelayedInphaseTapPointIndex ] ];

			filtered[ index + 1 ] = accumulator;
		}
		
		return filtered;
	}
	
	/**
	 * Inserts the ua.in.smartjava.sample into the circular ua.in.smartjava.buffer, overwriting the oldest value
	 */
	private void insert( float sample )
	{
		mBuffer[ mBufferPointer ] = sample;

		mBufferPointer++;
		
		mBufferPointer = mBufferPointer % mBufferSize;
	}
	
	
	/**
	 * Creates an N x (N + 1 / 2) index ua.in.smartjava.map enabling quick access to the
	 * circular ua.in.smartjava.buffer samples to support convolution.
	 * 
	 * As the ua.in.smartjava.buffer pointer shifts right, as each ua.in.smartjava.sample is added, we use the
	 * index ua.in.smartjava.map pointed to by the ua.in.smartjava.buffer pointer, so that the ua.in.smartjava.filter coefficients
	 * are pointing to the correct circular ua.in.smartjava.buffer samples.
	 * 
	 * We add an extra index to each index ua.in.smartjava.map with a value that points to the
	 * current index of the middle ua.in.smartjava.sample in the ua.in.smartjava.buffer, so that we can retrieve
	 * that value as the half-group delayed ua.in.smartjava.sample that forms the inphase
	 * component.
	 */
	private void generateIndexMap( int size )
	{
		int mapWidth = ( ( size + 1 ) / 2 ) + 1;
		
		int center  = ( size - 1 ) / 2;

		//Set center tap index for coefficients array
		mGroupDelayedInphaseTapPointIndex = center + 1;

		mIndexMap = new int[ size ][ mapWidth ];

		//Setup the first row with ua.in.smartjava.buffer pointer index 0 as a starting point
		for( int x = 0; x < mapWidth - 1; x++ )
		{
			mIndexMap[ 0 ][ x ] = size - 1 - ( x * 2 );
		}

		//Place the group delayed in-phase ua.in.smartjava.sample tap point in last element of
		//the first row
		mIndexMap[ 0 ][ mGroupDelayedInphaseTapPointIndex ] = center;
		
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
	
	public void logIndexMap()
	{
		for( int[] indexSet: mIndexMap )
		{
			mLog.debug( "Row:" + Arrays.toString( indexSet ) );
		}
	}
	
	/**
	 * Converts the half-band ua.in.smartjava.filter coefficients into a packed set of hilbert
	 * coefficients by removing all of the odd index, zero-valued coefficients 
	 * and the center 0.5 coefficient.  Then, all coefficients left of the 
	 * original center are made negative and all coefficients right of the 
	 * original center are made positive.  These coefficient sign modifications
	 * effectively incorporate a frequency translation by FS/4 into the hilbert 
	 * coefficients.  A gain of 2.0 is applied to the coefficients to offset the
	 * gain of 0.5 induced by the ua.in.smartjava.filter.
	 */
	private void createHilbertCoefficients()
	{
		int half = ( HALF_BAND_FILTER.length + 1 ) / 2;
		int middle = half / 2;
		
		mCoefficients = new float[ half ];

		/* Pack all of the even numbered HB coefficients into our coefficient 
		 * array and pre-apply a gain of two to offset the loss in the ua.in.smartjava.filter */
		for( int x = 0; x < half; x++ )
		{
			mCoefficients[ x ] = HALF_BAND_FILTER[ x * 2 ] * GAIN;

			/* Make all of the left of center coefficients negative */
			if( x < middle && mCoefficients[ x ] > 0.0f )
			{
				mCoefficients[ x ] = -mCoefficients[ x ];
			}
			/* Make all of the right of center coefficients positive */
			else if( x >= middle && mCoefficients[ x ] < 0.0f )
			{
				mCoefficients[ x ] = -mCoefficients[ x ];
			}
		}
	}
}
