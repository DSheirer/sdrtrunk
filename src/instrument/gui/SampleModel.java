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
package instrument.gui;

import instrument.tap.TapListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import log.Log;
import dsp.fsk.SymbolEvent;

public class SampleModel<T> extends Observable implements TapListener<T>
{
	private int mSampleCount = 2000;
	private int mDelay = 0;
	private ArrayList<T> mSamples = new ArrayList<T>();
	
	public SampleModel()
	{
	}
	
	public List<T> getSamples()
	{
		return Collections.unmodifiableList( mSamples );
	}

	public void clearSamples()
	{
		mSamples.clear();
		
		changed();
	}
	
	public int getSampleCount()
	{
		return mSampleCount;
	}
	
	public void setSampleCount( int count )
	{
		mSampleCount = count;
		
		changed();
	}

	public void setDelay( int delay )
	{
		mDelay = delay;
	}
	
	public int getDelay()
	{
		return mDelay;
	}
	
	@Override
    public void receive( T t )
    {
		if( mDelay > 0 )
		{
			mDelay--;
		}
		else
		{
			mSamples.add( t );
			
			while( mSamples.size() > mSampleCount )
			{
				mSamples.remove( 0 );
			}
		}
		
		changed();
    }
	
	private void changed()
	{
		setChanged();
		notifyObservers();
	}
}
