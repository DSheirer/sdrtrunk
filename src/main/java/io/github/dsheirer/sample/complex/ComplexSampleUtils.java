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
package io.github.dsheirer.sample.complex;


import org.apache.commons.math3.util.FastMath;

public class ComplexSampleUtils
{

	public static Complex multiply( Complex a,
										  Complex b )
	{
		return new Complex( 
			( a.inphase() * b.inphase() ) - ( a.quadrature() * b.quadrature() ),
			( ( a.quadrature() * b.inphase() ) + ( a.inphase() * b.quadrature() ) ) );
	}

	public static Complex minus( Complex a, Complex b )
	{
		return new Complex( a.inphase() - b.inphase(), 
				 				  a.quadrature() - b.quadrature() );
	}

	public static double magnitude( Complex sample )
	{
		return FastMath.sqrt( magnitudeSquared( sample ) );
	}
	
	public static int magnitudeSquared( Complex sample )
	{
		return (int)( ( sample.inphase() * sample.inphase() ) +
				 ( sample.quadrature() * sample.quadrature() ) );
	}
	
}
