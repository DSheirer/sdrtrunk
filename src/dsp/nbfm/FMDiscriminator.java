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
package dsp.nbfm;

import sample.Listener;
import sample.complex.ComplexSample;

public class FMDiscriminator implements Listener<ComplexSample>
{
	private Listener<Float> mListener;
	private ComplexSample mPreviousSample = new ComplexSample( 0.0f, 0.0f );
	private double mGain;
	
	/**
	 * Implements a polar discriminator with ArcTangent angle estimator to 
	 * demodulate complex sampled frequency modulated signals.
	 * 
	 * @param gain - gain to be applied to the demodulated output - can be
	 * dynamically applied to continuously adjust audio output
	 */
	public FMDiscriminator( double gain )
	{
		mGain = gain;
	}
	
	public void dispose()
	{
		mListener = null;
		mPreviousSample = null;
	}

	public synchronized void setGain( double gain )
	{
		mGain = gain;
	}
	
	@Override
    public void receive( ComplexSample currentSample )
    {
		/**
		 * Multiply the current sample against the complex conjugate of the 
		 * previous sample to derive the phase delta between the two samples
		 * 
		 * Negating the previous sample quadrature produces the conjugate
		 */
		double i = ( currentSample.inphase() * mPreviousSample.inphase() ) - 
				( currentSample.quadrature() * -mPreviousSample.quadrature() );
		double q = ( currentSample.quadrature() * mPreviousSample.inphase() ) + 
				( currentSample.inphase() * -mPreviousSample.quadrature() );

		double angle;

		//Check for divide by zero
		if( i == 0 )
		{
			angle = 0.0;
		}
		else
		{
			/**
			 * Use the arcus tangent of imaginary (q) divided by real (i) to
			 * get the phase angle (+/-) which was directly manipulated by the
			 * original message waveform during the modulation.  This value now
			 * serves as the instantaneous amplitude of the demodulated signal
			 */
			double denominator = 1.0d / i;
			angle = Math.atan( (double)q * denominator );
		}

		if( mListener != null )
		{
			mListener.receive( (float)( angle * mGain ) );
		}

		/**
		 * Store the current sample to use during the next iteration
		 */
		mPreviousSample = currentSample;
    }

    public void setListener( Listener<Float> listener )
    {
		mListener = listener;
    }
}
