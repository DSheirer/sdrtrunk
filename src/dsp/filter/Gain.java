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

public class Gain implements Listener<Float>
{
	private double mGain;
	private Listener<Float> mListener;
	
	public Gain( double gain )
	{
		mGain = gain;
	}

	@Override
    public void receive( Float sample )
    {
		if( mListener != null )
		{
			mListener.receive( (float)( sample * mGain ) );
		}
    }
	
    public void setListener( Listener<Float> listener )
    {
		mListener = listener;
    }
}
