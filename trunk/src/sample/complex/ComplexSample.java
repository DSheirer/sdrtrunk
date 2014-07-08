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
package sample.complex;

import java.io.Serializable;

import log.Log;

/**
 * ComplexSample for handling left/right channel audio, or inphase/quadrature 
 * samples, etc.  
 */
public class ComplexSample implements Serializable
{
    private static final long serialVersionUID = 1L;

    private float mLeft;
	private float mRight;
	
	public ComplexSample( float left, float right )
	{
		mLeft = left;
		mRight = right;
	}

	public ComplexSample copy()
	{
		return new ComplexSample( mLeft, mRight );
	}
	
	public String toString()
	{
		return "I:" + mLeft + " Q:" + mRight;
	}
	
	/**
	 * Returns a new sample representing the conjugate of this one
	 */
	public ComplexSample conjugate()
	{
		return new ComplexSample( mLeft, -mRight );
	}

	/**
	 * Multiplies this sample by the scalor value
	 */
	public void multiply( float scalor )
	{
		mLeft *= scalor;
		mRight *= scalor;
	}
	
	/**
	 * Multiplies this sample by the multiplier sample
	 */
	public void multiply( ComplexSample multiplier )
	{
		float inphase = ( inphase() * multiplier.inphase() ) - 
						( quadrature() * multiplier.quadrature() );
		
		float quadrature = ( quadrature() * multiplier.inphase() ) + 
						   ( inphase() * multiplier.quadrature() );
		
		mLeft = inphase;
		mRight = quadrature;
	}
	
	public static ComplexSample multiply( ComplexSample sample1, 
										  ComplexSample sample2 )
	{
		float inphase = ( sample1.inphase() * sample2.inphase() ) - 
		( sample1.quadrature() * sample2.quadrature() );

		float quadrature = ( sample1.quadrature() * sample2.inphase() ) + 
		   ( sample1.inphase() * sample2.quadrature() );

		return new ComplexSample( inphase, quadrature );
	}

	public static ComplexSample multiply( ComplexSample sample, 
										  Float left, Float right )
	{
		float l = ( sample.left() * left ) - ( sample.right() * right );
		float r = ( sample.right() * left ) + ( sample.left() * right );
		
		return new ComplexSample( l, r );
	}

	/**
	 * Adds the adder sample values to this sample
	 */
	public void add( ComplexSample adder )
	{
		mLeft += adder.left();
		mRight += adder.right();
	}

	/**
	 * Magnitude of this sample
	 */
	public float magnitude()
	{
		return (float)Math.sqrt( magnitudeSquared() );
	}

	/**
	 * Magnitude squared of this sample
	 */
	public float magnitudeSquared()
	{
		return (float)( ( inphase() * inphase() ) + 
					  ( quadrature() * quadrature() ) );
	}
	
	/**
	 * Returns the vector length to 1 (unit circle)
	 */
	public void normalize()
	{
		multiply( (float)( 1.0f / magnitude() ) );
	}

	/**
	 * Returns the vector length to 1 (unit circle),
	 * avoiding square root multiplication
	 */
	public void fastNormalize()
	{
		multiply( (float)( 1.95f - magnitudeSquared() ) );
	}
	
	public float left()
	{
		return mLeft;
	}
	
	public float right()
	{
		return mRight;
	}

	public float inphase()
	{
		return mLeft;
	}
	
	public float quadrature()
	{
		return mRight;
	}
	
	public float x()
	{
		return mLeft;
	}
	
	public float y()
	{
		return mRight;
	}
	
	public float real()
	{
		return mLeft;
	}
	
	public float imaginery()
	{
		return mRight;
	}

	/**
	 * Creates a new complex sample representing the angle with unit circle
	 * magnitude
	 * @param angle in radians
	 * @return
	 */
	public static ComplexSample fromAngle( float angle )
	{
		return new ComplexSample( (float)Math.cos( angle ), 
								  (float)Math.sin( angle ) );
	}
}
