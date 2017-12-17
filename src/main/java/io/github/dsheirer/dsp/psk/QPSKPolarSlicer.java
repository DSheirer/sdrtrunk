package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.dsp.symbol.Dibit;

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
public class QPSKPolarSlicer implements Listener<Complex>
{
	private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();
	
	/**
	 * Slices a ComplexSample representing a phase shifted symbol according to
	 * the following constellation pattern:
	 * 
	 *    \ 00 /
	 *     \  /
	 *   01 \/ 10
	 *      /\
	 *     /  \
	 *    / 11 \
	 */
	public QPSKPolarSlicer()
	{
	}
	
	public void dispose()
	{
		mBroadcaster.dispose();
		mBroadcaster = null;
	}

	public void addListener( Listener<Dibit> listener )
	{
		mBroadcaster.addListener( listener );
	}

	public void removeListener( Listener<Dibit> listener )
	{
		mBroadcaster.removeListener( listener );
	}

	@Override
	public void receive( Complex complex )
	{
		mBroadcaster.broadcast( decide( complex ) );
	}
	
	public static Dibit decide( Complex complex )
	{
		if( Math.abs( complex.inphase() ) > Math.abs( complex.quadrature() ) )
		{
			if( complex.inphase() > 0 )
			{
				return Dibit.D10_MINUS_1;
			}
			else
			{
				return Dibit.D01_PLUS_3;
			}
		}
		else
		{
			if( complex.quadrature() > 0 )
			{
				return Dibit.D00_PLUS_1;
			}
			else
			{
				return Dibit.D11_MINUS_3;
			}
		}
	}
	
}
