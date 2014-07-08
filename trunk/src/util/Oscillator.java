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
package util;

import sample.complex.ComplexSample;

public class Oscillator
{
	private double mFrequency;
	private double mSampleRate;

	private ComplexSample mAnglePerSample;
	private ComplexSample mCurrentAngle = new ComplexSample( 0.0f, -1.0f );

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
	
	private void update()
	{
		float anglePerSample = 
				(float)( 2.0d * Math.PI * mFrequency / mSampleRate );

		mAnglePerSample = ComplexSample.fromAngle( anglePerSample );
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
	private void rotate()
	{
		mCurrentAngle.multiply( mAnglePerSample );
		mCurrentAngle.fastNormalize();
	}

	/**
	 * Get next complex sample
	 */
	public ComplexSample nextComplex()
	{
		rotate();
		return mCurrentAngle;
	}

	/**
	 * Get the next float sample
	 */
	public float nextFloat()
	{
		rotate();
		return mCurrentAngle.real();
	}
}
