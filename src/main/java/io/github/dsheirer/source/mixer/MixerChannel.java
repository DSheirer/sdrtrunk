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
package io.github.dsheirer.source.mixer;

import java.util.EnumSet;

public enum MixerChannel
{
	LEFT( "LEFT" ),
	RIGHT( "RIGHT" ),
	MONO( "MONO" ),
	STEREO( "STEREO" );
	
	private String mLabel;
	
	private MixerChannel( String label )
	{
		mLabel = label;
	}
	
	public String getLabel()
	{
		return mLabel;
	}

	/**
	 * Mixer Target Data Line (ie sample source) channel options
	 */
	public static EnumSet<MixerChannel> getTargetChannels()
	{
		return EnumSet.of( LEFT,RIGHT,MONO );
	}

	/**
	 * Mixer Source Data Line (ie sound card output) channel options
	 */
	public static EnumSet<MixerChannel> getSourceChannels()
	{
		return EnumSet.of( MONO,STEREO );
	}
}
