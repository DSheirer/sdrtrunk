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
package sample;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts an item to multiple listeners
 */
public class Broadcaster<T> implements Listener<T>
{
	private List<Listener<T>> mListeners = new CopyOnWriteArrayList<>();

	@Override
    public void receive( T t )
    {
		broadcast( t );
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
	
	public List<Listener<T>> getListeners()
	{
		return mListeners;
	}
	
	public void addListener( Listener<T> listener )
	{
	    if(listener == null)
        {
            throw new IllegalArgumentException("Listener cannot be null");
        }

		mListeners.add( listener );
	}
	
	public void removeListener( Listener<T> listener )
	{
		mListeners.remove( listener );
	}
	
	public void clear()
	{
		mListeners.clear();
	}

    public void broadcast( T t )
    {
    	for( Listener<T> listener: mListeners )
    	{
    		listener.receive( t );
    	}
    }
}
