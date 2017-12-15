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
package ua.in.smartjava.instrument.tap.stream;

import ua.in.smartjava.instrument.tap.TapListener;
import ua.in.smartjava.instrument.tap.TapType;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.dsp.symbol.Dibit;

public class DibitTap extends StreamTap implements Listener<Dibit>
												
{
	private Listener<Dibit> mListener;
	
	public DibitTap( String name, 
						   int delay, 
						   float sampleRateRatio )
    {
	    super( TapType.STREAM_DIBIT, name, delay, sampleRateRatio );
    }

	@Override
    public void receive( Dibit dibit )
    {
		if( mListener != null )
		{
			mListener.receive( dibit );
		}
		
		for( TapListener listener: mListeners )
		{
			listener.receive( dibit );
		}
    }

    public void setListener( Listener<Dibit> listener )
    {
		mListener = listener;
    }

    public void removeListener( Listener<Dibit> listener )
    {
		mListener = null;
    }
}
