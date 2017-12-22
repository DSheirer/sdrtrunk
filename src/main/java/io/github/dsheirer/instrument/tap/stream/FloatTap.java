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
package io.github.dsheirer.instrument.tap.stream;

import io.github.dsheirer.instrument.tap.TapListener;
import io.github.dsheirer.instrument.tap.TapType;
import io.github.dsheirer.sample.real.RealSampleListener;

public class FloatTap extends StreamTap implements RealSampleListener
{
	private RealSampleListener mFloatTapListener;
	
	public FloatTap( String name, 
						   int delay, 
						   float sampleRateRatio )
    {
	    super( TapType.STREAM_FLOAT, name, delay, sampleRateRatio );
    }

	@Override
    public void receive( float sample )
    {
		if( mFloatTapListener != null )
		{
			mFloatTapListener.receive( sample );
		}
		
		for( TapListener listener: mListeners )
		{
			listener.receive( sample );
		}
    }

	public void setListener( RealSampleListener listener )
    {
		mFloatTapListener = listener;
    }

    public void removeListener( RealSampleListener listener )
    {
		mFloatTapListener = null;
    }
}
