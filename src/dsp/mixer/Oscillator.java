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
package dsp.mixer;

import java.util.Arrays;

import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dsp.filter.Filters;
import dsp.filter.hilbert.HilbertTransform;
import sample.complex.Complex;

public class Oscillator
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( Oscillator.class );

	private double mFrequency;
	private double mSampleRate;

	private Complex mAnglePerSample;
	private Complex mCurrentAngle = new Complex( 0.0f, -1.0f );

	/**
	 * Oscillator produces complex or float samples corresponding to a sine wave 
	 * oscillating at the specified frequency and sample rate
	 * 
	 * @param frequency - positive or negative frequency in hertz
	 * @param sampleRate - in hertz
	 */
	public Oscillator( long frequency, int sampleRate )
	{
		mSampleRate = (double)sampleRate;
		mFrequency = (double)frequency;
		
		update();
	}
	
	/**
	 * Updates the internal values after a frequency or sample rate change
	 */
	private void update()
	{
		float anglePerSample = 
				(float)( 2.0d * Math.PI * mFrequency / mSampleRate );

		mAnglePerSample = Complex.fromAngle( anglePerSample );
	}

	/**
	 * Sets or changes the frequency of this oscillator
	 */
	public void setFrequency( long frequency )
	{
		mFrequency = (double)frequency;
		update();
	}

	/**
	 * Sets or changes the sample rate of this oscillator
	 */
	public void setSampleRate( int sampleRate )
	{
		mSampleRate = (double)sampleRate;
		update();
	}

	/**
	 * Steps the current angle by the angle per sample amount
	 */
	public void rotate()
	{
		mCurrentAngle.multiply( mAnglePerSample );
		mCurrentAngle.fastNormalize();
	}

	public float inphase()
	{
		return mCurrentAngle.inphase();
	}
	
	public float quadrature()
	{
		return mCurrentAngle.quadrature();
	}
	
	/**
	 * Get next complex sample
	 */
	public Complex getComplex()
	{
		return mCurrentAngle.copy();
	}

	/**
	 * Get the next float sample
	 */
	public float getFloat()
	{
		return mCurrentAngle.real();
	}
	
	public static void main( String[] args )
	{
		Oscillator o = new Oscillator( 1536, 4096 );

		o.rotate();
		
		float[] samples = new float[ 8192 ];
		
		for( int x = 0; x < 8192; x++ )
		{
			samples[ x ] = o.inphase();
			
			o.rotate();
		}
		
		logLargest( "Real: ", samples, false );
		
		HilbertTransform ht = new HilbertTransform();
		
		float[] transformed = ht.filter( samples );

		logLargest( "Hilbert: ", transformed, true );
		
		FloatFFT_1D fft = new FloatFFT_1D( 4096 );

		fft.complexForward( transformed );

		logLargest( "DFT:", transformed, true );
	}
	
	private static void logLargest( String label, float[] samples, boolean complex )
	{
		float largestI = 0.0f;
		int indexLargestI = 0;
		float largestQ = 0.0f;
		int indexLargestQ = 0;

		float smallestI = 0.0f;
		int indexsmallestI = 0;
		float smallestQ = 0.0f;
		int indexsmallestQ = 0;

		for( int x = 0; x < samples.length; x++ )
		{
			if( complex )
			{
				if( x % 2 == 0 )
				{
					if( samples[ x ] > largestI )
					{
						largestI = samples[ x ];
						indexLargestI = x;
					}
					if( samples[ x ] < smallestI )
					{
						smallestI = samples[ x ];
						indexsmallestI = x;
					}
				}
				else
				{
					if( samples[ x ] > largestQ )
					{
						largestQ = samples[ x ];
						indexLargestQ = x;
					}
					if( samples[ x ] < smallestQ )
					{
						smallestQ = samples[ x ];
						indexsmallestQ = x;
					}
				}
			}
			else
			{
				if( samples[ x ] > largestI )
				{
					largestI = samples[ x ];
					indexLargestI = x;
				}
				if( samples[ x ] < smallestI )
				{
					smallestI = samples[ x ];
					indexsmallestI = x;
				}
			}
		}


		if( complex )
		{
			mLog.debug( label + 
					" largestI (" + indexLargestI + "): " + largestI + 
					" smallestI (" + indexsmallestI + "): " + smallestI +
					" largestQ (" + indexLargestQ + "): " + largestQ + 
					" smallestQ (" + indexsmallestQ + "): " + smallestQ );
		}
		else
		{
			mLog.debug( label + 
					" largest (" + indexLargestI + "): " + largestI + 
					" smallest (" + indexsmallestI + "): " + smallestI );
		}
	}
}
