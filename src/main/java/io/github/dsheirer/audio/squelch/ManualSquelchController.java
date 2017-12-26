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

package io.github.dsheirer.audio.squelch;


/**
 * Provides squelch control based on presense of decoded audio samples.
 * 
 * NOTE: this class is not yet fully implemented with audio sample processing
 * and currently only provides user driven squelch control
 */
public class ManualSquelchController extends SquelchController
{
	public ManualSquelchController()
	{
		mSquelchMode = SquelchMode.NONE;
	}

	@Override
	public void setSquelchMode( SquelchMode mode )
	{
		//TODO: add functionality
	}
}
