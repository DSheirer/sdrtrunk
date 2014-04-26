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
package instrument.tap;

import java.util.ArrayList;

public abstract class Tap
{
	protected TapType mType;
	protected String mName;
	protected int mDelay;

	protected ArrayList<TapListener> mListeners = new ArrayList<TapListener>();

	/**
	 * Instrumentation tap.  Provides a tap into a data stream or event stream
	 * within a process, so that registered listeners can be notified every
	 * time a new data sample is availabe, or an event occurrs.
	 * 
	 * @param name - displayable name to use for the tap
	 * 
	 * @param delay - indicates the number of delay units from when the data
	 * begins the process until this event occurs.  When this data or event is
	 * plotted on a flow graph, the delay value will be used to adjust placement
	 * on the display to correctly align events.
	 */
	public Tap( TapType type, String name, int delay )
	{
		mType = type;
		mName = name;
		mDelay = delay;
	}
	
	public TapType getType()
	{
		return mType;
	}
	
	/**
	 * Display name for this tap
	 */
	public String getName()
	{
		return mName;
	}

	/**
	 * Delay for this tap
	 */
	public int getDelay()
	{
		return mDelay;
	}

	/**
	 * Registers a listener for data or events produced by this tap
	 */
	public void addListener( TapListener listener )
	{
		if( !mListeners.contains( listener ) )
		{
			mListeners.add( listener );
		}
	}

	/**
	 * Removes the listener from receiving data or events from this tap
	 */
	public void removeListener( TapListener listener )
	{
		mListeners.remove( listener );
	}

	/**
	 * Number of listeners currently registered on this tap
	 */
	public int getListenerCount()
	{
		return mListeners.size();
	}
}
