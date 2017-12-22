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
package io.github.dsheirer.sample.complex;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts received float samples to multiple listeners
 */
public class ComplexSampleBroadcaster implements ComplexSampleListener
{
	private CopyOnWriteArrayList<ComplexSampleListener> mListeners = 
			new CopyOnWriteArrayList<ComplexSampleListener>();

	@Override
    public void receive( float i, float q )
    {
		broadcast( i, q );
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
	
	public void addListener( ComplexSampleListener listener )
	{
		mListeners.add( listener );
	}
	
	public void removeListener( ComplexSampleListener listener )
	{
		mListeners.remove( listener );
	}
	
	public void clear()
	{
		mListeners.clear();
	}

    public void broadcast( float i, float q )
    {
    	for( ComplexSampleListener listener: mListeners )
    	{
    		listener.receive( i, q );
    	}
    }
}
