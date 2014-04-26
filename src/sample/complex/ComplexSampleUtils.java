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


public class ComplexSampleUtils
{

	public static ComplexSample multiply( ComplexSample a,
										  ComplexSample b )
	{
		return new ComplexSample( 
			( a.inphase() * b.inphase() ) - ( a.quadrature() * b.quadrature() ),
			( ( a.quadrature() * b.inphase() ) + ( a.inphase() * b.quadrature() ) ) );
	}

	public static ComplexSample minus( ComplexSample a, ComplexSample b )
	{
		return new ComplexSample( a.inphase() - b.inphase(), 
				 				  a.quadrature() - b.quadrature() );
	}

	public static double magnitude( ComplexSample sample )
	{
		return Math.sqrt( magnitudeSquared( sample ) );
	}
	
	public static int magnitudeSquared( ComplexSample sample )
	{
		return (int)( ( sample.inphase() * sample.inphase() ) +
				 ( sample.quadrature() * sample.quadrature() ) );
	}
	
}
