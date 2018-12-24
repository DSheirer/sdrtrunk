/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.reference;

public enum IPHeaderCompression
{
	NONE( 0, "NONE" ),
	RFC1144_COMPRESSED( 1, "RFC-1144 COMPRESSED" ),
	RFC1144_UNCOMPRESSED( 2, "RFC-1144 UNCOMPRESSED" ),
	UNKNOWN( -1, "UNKNOWN" );
	
	private int mValue;
	private String mLabel;
	
	IPHeaderCompression( int value, String label )
	{
		mValue = value;
		mLabel = label;
	}
	
	public int getValue()
	{
		return mValue;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public static IPHeaderCompression fromValue( int value )
	{
		if( 0 <= value && value <= 2 )
		{
			return values()[ value ];
		}
		
		return IPHeaderCompression.UNKNOWN;
	}
}
