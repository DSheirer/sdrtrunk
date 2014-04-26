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

import sample.Listener;
import sample.complex.ComplexSample;
import buffer.ComplexTappedCircularBuffer;

public class ComplexPolyphaseDecimationFilter extends ComplexFilter
{
	private FilterPhase[] mFilterPhases;
	private int mPhasePointer = 0;
	private int mPhaseCount;

	/**
	 * Implements a decimating poly-phase filter bank for complex samples, as
	 * described in Understanding Digital Signal Processing, 3e, Lyons, in
	 * section 10.7, figure 10-14.
	 * 
	 * Constraints: 
	 * 		- Low-pass filter coefficient length must be an integer multiple 
	 * 		  of the decimation rate:
	 * 
	 * Consideration:
	 * 		- Low pass filter cutoff frequency must be less than:
	 * 		  ( input sample rate / 2 / decimation )
	 * 
	 * @param coefficients - low pass filter coefficients.
	 * @param decimationRate - integer decimation rate
	 */
	public ComplexPolyphaseDecimationFilter( double[] coefficients, 
											 int decimationRate )
	{
		/* Verify that the filter length is an integer multiple of the 
		 * decimation rate */
		assert( coefficients.length % decimationRate == 0 );

		mPhaseCount = decimationRate;
		
		/* Construct a number of filter phases equal to the decimation rate */
		mFilterPhases = new FilterPhase[ mPhaseCount ];

		for( int phaseNumber = 0; phaseNumber < mPhaseCount; phaseNumber++ )
		{
			double[] phaseCoefficients = 
				getPhaseCoefficients( coefficients, decimationRate, phaseNumber );
			
			mFilterPhases[ phaseNumber ] = new FilterPhase( phaseCoefficients );
		}
	}
	
	/**
	 * Inputs a new sample into the filter at the non-decimated rate
	 */
	@Override
    public void receive( ComplexSample sample )
    {
		mFilterPhases[ mPhasePointer++ ].receive( sample );
		
		if( mPhasePointer >= mPhaseCount )
		{
			/* Broadcast a new output sample */
			calculate();

			/* Reset the pointer */
			mPhasePointer = 0;
		}
    }

	/**
	 * Outputs a complex sample at the decimated rate, comprised of the sum of
	 * the outputs of the individual filter stages, trigger by a decimation rate
	 * counter on the input side.
	 */
	private void calculate()
	{
		if( hasListener() )
		{
			/* Accumulators */
			float left = 0.0f;
			float right = 0.0f;
		
			for( int x = 0; x < mPhaseCount; x++ )
			{
				ComplexSample phaseSample = mFilterPhases[ x ].calculate();
				
				left += phaseSample.left();
				right += phaseSample.right();
			}
		
			send( new ComplexSample( left, right ) );	
		}
	}

	/**
	 * Returns the subset of filter coefficients that belong to the filter bank 
	 * phase.
	 */
	private double[] getPhaseCoefficients( double[] coefficients, 
										  int phaseCount, 
										  int phaseNumber )
	{
		int phaseFilterLength = (int)( coefficients.length / phaseCount );
		
		double[] phaseCoefficients = new double[ phaseFilterLength ];

		for( int x = 0; x < phaseFilterLength; x++ )
		{
			phaseCoefficients[ x ] = coefficients[ x * phaseCount + phaseNumber ];
		}
		
		return phaseCoefficients;
	}

	/**
	 * Implements a single phase of a poly-phase filter bank
	 */
	public class FilterPhase implements Listener<ComplexSample>
	{
		private int mLength;
		private double[] mCoefficients;
		private ComplexTappedCircularBuffer mBuffer;

		/**
		 * @param coefficients - subset of the filter coefficients for this
		 * phase of the poly-phase filter bank
		 */
		public FilterPhase( double[] coefficients )
		{
			mCoefficients = coefficients;
			mLength = coefficients.length;
			
			/* The constructor preloads the buffer with 0-valued samples */
			mBuffer = new ComplexTappedCircularBuffer( coefficients.length );
		}
		
		@Override
        public void receive( ComplexSample sample )
        {
			mBuffer.add( sample );
        }
		
		/**
		 * Performs convolution by multiplying the buffer samples by their 
		 * corresponding filter coefficients and returns the sum
		 */
		public ComplexSample calculate()
		{
			/* Accumulators */
			double left = 0.0d;
			double right = 0.0d;
			
			for( int x = 0; x < mLength; x++ )
			{
				left += ( mBuffer.get( x ).left() * mCoefficients[ x ] );
				right += ( mBuffer.get( x ).right() * mCoefficients[ x ] );
			}
			
			return new ComplexSample( (float)left, (float)right );
		}
	}
}
