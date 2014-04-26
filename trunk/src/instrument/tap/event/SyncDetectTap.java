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
package instrument.tap.event;

import instrument.tap.Tap;
import instrument.tap.TapListener;
import instrument.tap.TapType;
import dsp.SyncDetectListener;
import dsp.SyncDetectProvider;

public class SyncDetectTap extends Tap 
						   implements SyncDetectListener,
									  SyncDetectProvider
{
	private SyncDetectListener mListener;

	/**
	 * Sync detect event tap.  Indicates when message sync word has been 
	 * detected within a binary stream of data.
	 */
	public SyncDetectTap()
    {
	    super( TapType.EVENT_SYNC_DETECT, "Sync Detected", 0 );
    }

	@Override
    public void syncDetected()
    {
		if( mListener != null )
		{
			mListener.syncDetected();
		}
		
		for( TapListener listener: mListeners )
		{
			listener.receive( getName() );
		}
    }

	@Override
    public void setSyncDetectListener( SyncDetectListener listener )
    {
		mListener = listener;
    }
}
