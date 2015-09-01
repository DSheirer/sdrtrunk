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
package dsp.filter.dc;

import sample.Listener;
import sample.real.RealBuffer;

public class AveragingDCRemovalFilter_RB extends DCRemovalFilter_RB 
				implements Listener<RealBuffer>
{
	private float mAverage;
	private float mRatio;

	public AveragingDCRemovalFilter_RB( float ratio )
	{
		mRatio = ratio;
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public void reset()
	{
		mAverage = 0.0f;
	}

	public float filter( float sample )
	{
		mAverage += mRatio * ( sample - mAverage );
	
		return sample - mAverage;
	}

	@Override
	public void receive( RealBuffer buffer )
	{
		if( mListener != null )
		{
			float[] samples = buffer.getSamples();
			
			for( int x = 0; x < samples.length; x++ )
			{
				samples[ x ] = filter( samples[ x ] );
			}
			
			mListener.receive( buffer );
		}
	}
}
