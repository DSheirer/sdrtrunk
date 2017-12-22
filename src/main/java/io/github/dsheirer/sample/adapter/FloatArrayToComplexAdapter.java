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
package io.github.dsheirer.sample.adapter;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;

public class FloatArrayToComplexAdapter implements Listener<Float[]>
{
	private Listener<Complex> mListener;

	@Override
    public void receive( Float[] samples )
    {
		if( samples != null )
		{
			for( int x = 0; x < samples.length; x += 2 )
			{
				if( mListener != null &&
					samples[ x ] != null && 
					samples[ x + 1 ] != null )
				{
					mListener.receive( new Complex( samples[ x ], 
											  			  samples[ x + 1 ] ) );
				}
			}
			
			samples = null;
		}
    }

	public void setListener( Listener<Complex> listener )
	{
		mListener = listener;
	}
	
	public void removeListener( Listener<Complex> listener )
	{
		mListener = null;
	}
	
	public void dispose()
	{
		mListener = null;
	}
}
