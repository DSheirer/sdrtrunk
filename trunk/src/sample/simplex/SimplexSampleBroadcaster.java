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
package sample.simplex;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts a received float sample to multiple listeners
 */
public class SimplexSampleBroadcaster implements SimplexSampleListener
{
	private CopyOnWriteArrayList<SimplexSampleListener> mListeners = 
			new CopyOnWriteArrayList<SimplexSampleListener>();

	@Override
    public void receive( float sample )
    {
		broadcast( sample );
    }
	
	/**
	 * Clear listeners to prepare for garbage collection
	 */
	public void dispose()
	{
		mListeners.clear();
	}
	
	public boolean hasListeners()
	{
		return !mListeners.isEmpty();
	}
	
	public int getListenerCount()
	{
		return mListeners.size();
	}
	
	public void addListener( SimplexSampleListener listener )
	{
		mListeners.add( listener );
	}
	
	public void removeListener( SimplexSampleListener listener )
	{
		mListeners.remove( listener );
	}
	
	public void clear()
	{
		mListeners.clear();
	}

    public void broadcast( float sample )
    {
    	for( SimplexSampleListener listener: mListeners )
    	{
    		listener.receive( sample );
    	}
    }
}
