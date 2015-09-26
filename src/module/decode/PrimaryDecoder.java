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

package module.decode;

import module.decode.config.DecodeConfiguration;
import sample.Listener;
import audio.squelch.ISquelchStateProvider;
import audio.squelch.SquelchState;

/**
 * Primary decoder adds the following functionality over the basic decoder:
 * 
 * - Provides audio squelch control
 */

public abstract class PrimaryDecoder extends Decoder implements ISquelchStateProvider
{
	protected Listener<SquelchState> mSquelchStateListener;
	
	protected DecodeConfiguration mDecodeConfiguration;
	
	public PrimaryDecoder( DecodeConfiguration config )
	{
		mDecodeConfiguration = config;
	}
	
	public DecodeConfiguration getDecodeConfiguration()
	{
		return mDecodeConfiguration;
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		
		mSquelchStateListener = null;
		mDecodeConfiguration = null;
	}

	public void broadcast( SquelchState state )
	{
		if( mSquelchStateListener != null )
		{
			mSquelchStateListener.receive( state );
		}
	}
	
	@Override
	public void setSquelchStateListener( Listener<SquelchState> listener )
	{
		mSquelchStateListener = listener;
	}

	@Override
	public void removeSquelchStateListener()
	{
		mSquelchStateListener = null;
	}
}
