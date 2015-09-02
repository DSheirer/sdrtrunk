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
package dsp.fm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.real.RateCounter_RB;
import sample.real.RealBuffer;

public class FMDemodulator_CB extends FMDemodulator implements Listener<ComplexBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FMDemodulator_CB.class );

	private Listener<RealBuffer> mListener;
	
	private RateCounter_RB mRateCounter = new RateCounter_RB( "FM DEMOD OUTPUT" );
	
	public FMDemodulator_CB( float gain )
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
				
				demodulated[ x ] = demodulate( samples[ index ], 
											   samples[ index + 1 ] );
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

	@Override
	public void dispose()
	{
		mListener = null;
	}
}
