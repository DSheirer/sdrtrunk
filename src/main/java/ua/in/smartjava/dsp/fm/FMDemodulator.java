/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package ua.in.smartjava.dsp.fm;

public abstract class FMDemodulator
{
	private float mPreviousI = 0.0f;
	private float mPreviousQ = 0.0f;
	protected float mGain;

	public FMDemodulator( float gain )
	{
		mGain = gain;
	}

	public float demodulate( float currentI, float currentQ )
	{
		/**
		 * Multiply the current ua.in.smartjava.sample against the complex conjugate of the
		 * previous ua.in.smartjava.sample to derive the phase delta between the two samples
		 * 
		 * Negating the previous ua.in.smartjava.sample quadrature produces the conjugate
		 */
		double inphase = ( currentI * mPreviousI ) - ( currentQ * -mPreviousQ );
		double quadrature = ( currentQ * mPreviousI ) + ( currentI * -mPreviousQ );
	
		double angle = 0.0f;
	
		//Check for divide by zero
		if( inphase != 0 )
		{
			/**
			 * Use the arc-tangent of quadrature divided by inphase to
			 * get the phase angle (+/-) which was directly manipulated by the
			 * original ua.in.smartjava.message waveform during the modulation.  This value now
			 * serves as the instantaneous amplitude of the demodulated signal
			 */
			double denominator = 1.0d / inphase;
			angle = Math.atan( (double)quadrature * denominator );
		}
	
		/**
		 * Store the current ua.in.smartjava.sample to use during the next iteration
		 */
		mPreviousI = currentI;
		mPreviousQ = currentQ;
	
		return (float)( angle * mGain );
	}

	public abstract void dispose();

	public void reset()
	{
		mPreviousI = 0.0f;
		mPreviousQ = 0.0f;
	}

	public void setGain( float gain )
	{
		mGain = gain;
	}
}