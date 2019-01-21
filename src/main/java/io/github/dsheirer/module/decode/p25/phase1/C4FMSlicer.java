/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.RealSampleListener;

/**
 * C4FM slicer to convert the output stream of the C4FMSymbolFilter into a 
 * stream of C4FM symbols.  
 * 
 * Supports registering listener(s) to receive normal and/or inverted symbol
 * output streams.
 */
public class C4FMSlicer implements RealSampleListener
{
	private static final float THRESHOLD = 2.0f;

	private Broadcaster<Dibit> mBroadcaster = new Broadcaster<Dibit>();
	
	public void dispose()
	{
		mBroadcaster.dispose();
		mBroadcaster = null;
	}
	
	/**
	 * Primary method for receiving output from the C4FMSymbolFilter.  Slices
	 * (converts) the filtered sample value into a C4FMSymbol decision.
	 */
	@Override
    public void receive( float sample )
    {
		if( sample > 0.0 )
		{
			if( sample >= THRESHOLD )
			{
				dispatch( Dibit.D01_PLUS_3 );
			}
			else
			{
				dispatch( Dibit.D00_PLUS_1 );
				
			}
		}
		else
		{
			if( sample > -THRESHOLD )
			{
				dispatch( Dibit.D10_MINUS_1 );
			}
			else
			{
				dispatch( Dibit.D11_MINUS_3 );
			}
		}
    }

	/**
	 * Dispatches the symbol decision to any registered listeners
	 */
	private void dispatch( Dibit symbol )
	{
		mBroadcaster.receive( symbol );
	}

	/**
	 * Registers the listener to receive the normal (non-inverted) C4FM symbol
	 * stream.
	 */
    public void addListener( Listener<Dibit> listener )
    {
		mBroadcaster.addListener( listener );
    }

	/**
	 * Removes the listener
	 */
    public void removeListener( Listener<Dibit> listener )
    {
    	mBroadcaster.removeListener( listener );
    }
}
