/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package dsp.filter.polyphase;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polyphase interpolating FIR filter with an internal circular buffer as 
 * described by the dspGuru at:
 * 
 * http://www.dspguru.com/dsp/faqs/multirate/interpolation  
 * See sections 3.4.2 and 3.4.3 )
 */
public class PolyphaseFIRInterpolatingFilter
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( PolyphaseFIRInterpolatingFilter.class );

	private double[] mBuffer;
	private int mBufferPointer = 0;
	private double[] mTaps;
	private Phase[] mPhases;

	/**
	 * Constructs the interpolating filter.  Coefficients length must be an 
	 * integer multiple of the interpolation value.  Each sample processed by
	 * the filter will generate a quantity of samples equal to the interpolation
	 * value.
	 * 
	 * @param coefficients - low pass filter coefficients sized for the desired
	 * interpolated sample rate with a cutoff frequency of no more than half of 
	 * the original sample rate.
	 * 
	 * @param interpolation - integral interpolation quantity
	 */
	public PolyphaseFIRInterpolatingFilter( double[] coefficients, int interpolation )
	{
		/* Ensure coefficients are an integral multiple of the interpolation factor */
		Validate.isTrue(coefficients.length % interpolation == 0);
		
		mTaps = coefficients;

		mPhases = new Phase[ interpolation ];

		int phaseSize = coefficients.length / interpolation;
		
		mBuffer = new double[ phaseSize ];
		
		for( int x = 0; x < interpolation; x++ )
		{
			int coefficientIndex = 0;
			
			int[] indexes = new int[ phaseSize ];
			
			for( int y = x; y < coefficients.length; y += interpolation )
			{
				indexes[ coefficientIndex++ ] = y;
			}

			/* Create a phase using the tap indexes and the interpolation value
			 * for the gain value, to offset the loss induced in upsampling from
			 * one sample to (interpolation) samples. */
			mPhases[ interpolation - x - 1 ] = new Phase( indexes, interpolation );
		}
	}
	
	/**
	 * Inserts the sample into the filter and returns (interpolation) number
	 * of interpolated samples in the return array.
	 * 
	 * @param sample - float sample
	 * @return - array of interpolated samples, size = interpolation quantity
	 */
	public float[] interpolate( float sample )
	{
		mBuffer[ mBufferPointer ] = (double)sample;
		
		mBufferPointer++;
		mBufferPointer %= mBuffer.length;
		
		float[] samples = new float[ mPhases.length ];
		
		for( int x = 0; x < mPhases.length; x++ )
		{
			samples[ x ] = mPhases[ x ].filter();
		}
		
		return samples;
	}

	/**
	 * Upsamples and interpolates the array of samples, returning an array that
	 * is upsampled and low-pass filtered by the interpolation quantity.
	 * 
	 * @param samples - array of float samples
	 * @return - array of interpolated samples, size = interpolation * samples size
	 */
	public float[] interpolate( float[] samples )
	{
		int pointer = 0;
		float[] interpolated = new float[ samples.length * mPhases.length ];
		
		for( float sample: samples )
		{
			mBuffer[ mBufferPointer ] = (double)sample;
			
			mBufferPointer++;
			mBufferPointer %= mBuffer.length;
			
			for( int x = 0; x < mPhases.length; x++ )
			{
				interpolated[ pointer ] = mPhases[ x ].filter();
				
				pointer++;
			}
		}
		
		return interpolated;
	}
	
	/**
	 * Single phase element of a polyphase interpolating FIR filter.  Each phase 
	 * element shares a common buffer.  The tap indexes argument defines the 
	 * filter taps used by this phase element to generate an interpolated value
	 * from the shared circular buffer of samples.
	 */
	public class Phase
	{
		private int[] mIndexes;
		private double mGain;
		
		public Phase( int[] tapIndexes, int gain )
		{
			mIndexes = tapIndexes;
			mGain = gain;
		}
		
		public float filter()
		{
			double accumulator = 0;
			
			for( int x = 0; x < mIndexes.length; x++ )
			{
				accumulator += mTaps[ mIndexes[ x ] ] *  
						mBuffer[ ( x + mBufferPointer ) % mIndexes.length ];
			}
			
			return (float)( accumulator * mGain );
		}
	}
	
//	public static void main( String[] args )
//	{
//		int interpolation = 6;
//		
//		PolyphaseFIRInterpolatingFilter p = 
//			new PolyphaseFIRInterpolatingFilter( IMBESynthesizer.INTERPOLATION_TAPS, interpolation );
//		
//		Oscillator o = new Oscillator( 500, 8000 );
//
//		int iterations = 40;
//		
//		float[] in = new float[ iterations ];
//		float[] out = new float[ iterations * interpolation ];
//
//		int inIndex = 0;
//		int outIndex = 0;
//		
//		for( int x = 0; x < iterations; x++ )
//		{
//			float next = o.nextFloat();
//
//			in[ inIndex++ ] = next;
//			
//			float[] samples = p.interpolate( next );
//			
//			System.arraycopy( samples, 0, out, outIndex, samples.length );
//			
//			outIndex += interpolation;
//		}
//
//		mLog.debug( "In: " + Arrays.toString( in ) );
//		mLog.debug( "Out: " + Arrays.toString( out ) );
//	}
}
