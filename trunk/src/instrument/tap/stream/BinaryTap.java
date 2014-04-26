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
package instrument.tap.stream;

import instrument.tap.TapListener;
import instrument.tap.TapType;
import sample.Listener;

public class BinaryTap extends StreamTap implements Listener<Boolean>
												
{
	private Listener<Boolean> mListener;
	
	public BinaryTap( String name, 
						   int delay, 
						   float sampleRateRatio )
    {
	    super( TapType.STREAM_BINARY, name, delay, sampleRateRatio );
    }

	@Override
    public void receive( Boolean bit )
    {
		if( mListener != null )
		{
			mListener.receive( bit );
		}
		
		for( TapListener listener: mListeners )
		{
			listener.receive( bit );
		}
    }

    public void setListener( Listener<Boolean> listener )
    {
		mListener = listener;
    }

    public void removeListener( Listener<Boolean> listener )
    {
		mListener = null;
    }
}
