/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.iir;

import org.apache.commons.math3.util.FastMath;

public class DeemphasisFilter
{
	private static final float MAX_SAMPLE_VALUE = 0.95f;
	private float mAlpha;
	private float mGain;
	private float mPrevious = 0.0f;
	
	public DeemphasisFilter( float sampleRate, float cutoff, float gain )
	{
		mAlpha = (float)FastMath.exp( -2.0 * FastMath.PI * cutoff * ( 1.0 / sampleRate ) );
		mGain = gain;
	}
	
	public float filter( float sample )
	{
		mPrevious = sample + ( mAlpha * mPrevious );
		
		return declip( mPrevious * mGain );
	}
	
	private float declip( float value )
	{
		if( value > MAX_SAMPLE_VALUE )
		{
			return MAX_SAMPLE_VALUE;
		}
		else return FastMath.max(value, -MAX_SAMPLE_VALUE);
	}
	
	public float[] filter( float[] samples )
	{
		for( int x = 0; x < samples.length; x++ )
		{
			samples[ x ] = filter( samples[ x ] );
		}
		
		return samples;
	}
}
