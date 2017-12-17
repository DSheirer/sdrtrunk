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
package io.github.dsheirer.dsp.am;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.real.RealBuffer;

public class AMDemodulator_CB extends AMDemodulator implements Listener<ComplexBuffer>
{
	private Listener<RealBuffer> mListener;
	
	public AMDemodulator_CB( float gain )
	{
		super( gain );
	}
	
	@Override
	public void receive( ComplexBuffer buffer )
	{
		if( mListener != null )
		{
			float[] samples = buffer.getSamples();
			
			int half = samples.length / 2;
			
			float[] demodulated = new float[ half ];

			for( int x = 0; x < half; x++ )
			{
				int index = x * 2;
				
				demodulated[ x ] = demodulate( samples[ index ], samples[ index + 1 ] );
			}
			
			mListener.receive( new RealBuffer( demodulated ) );
		}
	}
	
	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
	}

	public void removeListener()
	{
		mListener = null;
	}

	public void dispose()
	{
		mListener = null;
	}
}
