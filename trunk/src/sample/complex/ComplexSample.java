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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ComplexSample for handling left/right channel audio, or inphase/quadrature 
 * samples, etc.  
 */
public class ComplexSample implements Serializable
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexSample.class );

	private static final long serialVersionUID = 1L;

    private float mLeft;
	private float mRight;
	
	public ComplexSample( float left, float right )
	{
		mLeft = left;
		mRight = right;
	}
	
	public ComplexSample()
	{
		this( 0.0f, 0.0f );
	}

	public ComplexSample copy()
	{
		return new ComplexSample( mLeft, mRight );
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "I:" );
		sb.append( mLeft );
		sb.append( " Q:" );
		sb.append( mRight );
		
		return sb.toString();
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
	
	public static ComplexSample multiply( ComplexSample sample, float scalor )
	{
		return new ComplexSample( sample.left() * scalor, 
								  sample.right() * scalor );
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
	
	/**
	 * Multiplies both samples returning a new sample with the results
	 */
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
										  Float inphase, Float quadrature )
	{
		float i = ( sample.inphase() * inphase ) - ( sample.quadrature() * quadrature );
		float q = ( sample.quadrature() * inphase ) + ( sample.inphase() * quadrature );
		
		return new ComplexSample( i, q );
	}

	/**
	 * Adds the adder sample value to this sample
	 */
	public void add( ComplexSample adder )
	{
		mLeft += adder.left();
		mRight += adder.right();
	}

	/**
	 * Adds the two complex samples returning a new complex sample with the result
	 */
	public static ComplexSample add( ComplexSample first, ComplexSample second )
	{
		return new ComplexSample( first.left() + second.left(), 
								  first.right() + second.right() );
	}

	/**
	 * Adds the two complex samples returning a new complex sample with the result
	 */
	public static ComplexSample subtract( ComplexSample first, ComplexSample second )
	{
		return new ComplexSample( first.left() - second.left(), 
								  first.right() - second.right() );
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
		return norm();
	}

	/**
	 * Norm of this sample = ( i * i ) + ( q * q )
	 */
	public float norm()
	{
		return (float)( ( inphase() * inphase() ) + 
				  ( quadrature() * quadrature() ) );
	}
	
	/**
	 * Returns the vector length to 1 (unit circle)
	 */
	public void normalize()
	{
		float magnitude = magnitude();
		
		if( magnitude != 0 )
		{
			multiply( (float)( 1.0f / magnitude() ) );
		}
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
	
	/**
	 * Absolute value of in-phase component
	 */
	public float inPhaseAbsolute()
	{
		return Math.abs( mLeft );
	}

	public float quadrature()
	{
		return mRight;
	}
	
	/**
	 * Absolute value of quadrature component
	 */
	public float quadratureAbsolute()
	{
		return Math.abs( mRight );
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

	/**
	 * Absolute value of real component
	 */
	public float realAbsolute()
	{
		return Math.abs( mLeft );
	}

	public float imaginary()
	{
		return mRight;
	}

	/**
	 * Absolute value of imaginary component
	 */
	public float imaginaryAbsolute()
	{
		return Math.abs( mRight );
	}

	/**
	 * Returns the greater absolute value between left and right values 
	 */
	public float maximumAbsolute()
	{
		if( Math.abs( mLeft ) > Math.abs( mRight ) )
		{
			return Math.abs( mLeft );
		}
		else
		{
			return Math.abs(  mRight );
		}
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

	/**
	 * Angle of this sample in radians
	 */
	public float angle()
	{
		return (float)Math.atan2( y(), x() );
	}

	/**
	 * Constrains the i and q quantities to +/- value. 
	 * 
	 * @param value - maximum absolute value
	 */
	public void clip( float value )
	{
		if( mLeft > value )
		{
			mLeft = value;
		}
		else if( mLeft < -value )
		{
			mLeft = -value;
		}
		
		if( mRight > value )
		{
			mRight = value;
		}
		else if( mRight < -value )
		{
			mRight = -value;
		}
	}

	/**
	 * Angle of this sample in degrees
	 */
	public float polarAngle()
	{
		return (float)Math.toDegrees( angle() );
	}

	/**
	 * Provides an approximate magnitude value for this sample.
	 */
	public float envelope()
	{
		float realAbsolute = realAbsolute();
		float imagAbsolute = imaginaryAbsolute();
		
		if( realAbsolute > imagAbsolute )
		{
			return realAbsolute + ( 0.4f * imagAbsolute );
		}
		else
		{
			return imagAbsolute + ( 0.4f * realAbsolute );
		}
	}
	
	public static void main( String[] args )
	{
		double angle = Math.PI;  //180 degrees
		
		ComplexSample s1 = new ComplexSample( (float)Math.sin( angle ), (float)Math.cos( angle ) );

		ComplexSample tap = new ComplexSample( 1.0f, -0.1f );

		ComplexSample convolved = ComplexSample.multiply( s1, tap );
		
		mLog.debug( "s: " + s1.toString() +
					" t: " + tap.toString() +
					" convolved: " + convolved.toString() );
		
	}
}
