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

package audio.squelch;

public class TrunkingSquelchController extends SquelchController
{
	private boolean mAllowSquelchOverride = true;
	
	public TrunkingSquelchController()
	{
	}
	
	/**
	 * Provides control over audio stream as directed by an external trunking 
	 * channel state.
	 * 
	 * Override automatic squelch control by setting the squelch mode to none.
	 * 
	 * Disable automatic control override by setting allowManualOverride to false;
	 * 
	 * Note: this control does not respond to squelch mode manual.
	 */
	public TrunkingSquelchController( boolean allowSquelchOverride )
	{
		mAllowSquelchOverride = allowSquelchOverride;
		
		mSquelchMode = SquelchMode.AUTOMATIC;
	}

	@Override
	public void setSquelchMode( SquelchMode mode )
	{
		if( mode == SquelchMode.AUTOMATIC ||
			( mode == SquelchMode.NONE && mAllowSquelchOverride ) )
		{
			mSquelchMode = mode;
		}
	}
}
