/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;

public class EyeDiagramDataTap extends StreamTap implements Listener<EyeDiagramData>
{
	private final static Logger mLog = LoggerFactory.getLogger( EyeDiagramDataTap.class );

	private Listener<EyeDiagramData> mListener;
	
	public EyeDiagramDataTap( String name, int delay, float sampleRate )
    {
	    super( TapType.STREAM_EYE_DIAGRAM, name, delay, sampleRate );
    }

	@Override
    public void receive( EyeDiagramData data )
    {
		if( mListener != null )
		{
			mListener.receive( data );
		}
		
		for( TapListener listener: mListeners )
		{
			listener.receive( data );
		}
    }
	
    public void setListener( Listener<EyeDiagramData> listener )
    {
		mListener = listener;
    }

    public void removeListener( Listener<EyeDiagramData> listener )
    {
		mListener = null;
    }
}
