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
package dsp.filter;

import sample.Listener;

public class DCRemovalFilter2 implements Listener<Float>
{
	private double mAverage;
	private double mRatio;
	private Listener<Float> mListener;
	
	public DCRemovalFilter2( double ratio )
	{
		mRatio = ratio;
	}

	@Override
    public void receive( Float sample )
    {
		mAverage += mRatio * ( (double)sample - mAverage );

		send( (float)( sample - mAverage ) );
    }
	
	/**
	 * Sends the filtered sample to all registered listeners
	 */
	private void send( float sample )
	{
		if( mListener != null )
		{
			mListener.receive( sample );
		}
	}

    public void setListener( Listener<Float> listener )
    {
		mListener = listener;
    }
}
