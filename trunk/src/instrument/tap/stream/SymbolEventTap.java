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
import dsp.fsk.SymbolEvent;

public class SymbolEventTap extends StreamTap implements Listener<SymbolEvent>
{
	private Listener<SymbolEvent> mListener;
	
	public SymbolEventTap( String name, int delay, float sampleRate )
    {
	    super( TapType.STREAM_SYMBOL, name, delay, sampleRate );
    }

	@Override
    public void receive( SymbolEvent symbolEvent )
    {
		if( mListener != null )
		{
			mListener.receive( symbolEvent );
		}
		
		for( TapListener listener: mListeners )
		{
			listener.receive( symbolEvent );
		}
    }
	
    public void setListener( Listener<SymbolEvent> listener )
    {
		mListener = listener;
    }

    public void removeListener( Listener<SymbolEvent> listener )
    {
		mListener = null;
    }
	
}
