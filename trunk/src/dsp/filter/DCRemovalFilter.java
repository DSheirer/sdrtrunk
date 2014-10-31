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
import sample.real.RealSampleListener;

/**
 * Implements the real-time general DC removal algorithm described on page 
 * 762, figure 13-62d, in Understanding Digital Signal Processing 3e, Lyons.
 */
public class DCRemovalFilter implements RealSampleListener
{
	private double mAlpha;
	private float mPrevious;
	private RealSampleListener mListener;
	
	public DCRemovalFilter( double alpha )
	{
		mAlpha = alpha;
	}
	
	public void dispose()
	{
		mListener = null;
	}

	@Override
    public void receive( float sample )
    {
		send( (float)( sample + ( mPrevious * mAlpha ) - mPrevious ) );
		
		mPrevious = sample;
    }
	
	/**
	 * Sends the filtered sample to the listener
	 */
	private void send( float sample )
	{
		if( mListener != null )
		{
			mListener.receive( sample );
		}
	}

    public void setListener( RealSampleListener listener )
    {
		mListener = listener;
    }
}
