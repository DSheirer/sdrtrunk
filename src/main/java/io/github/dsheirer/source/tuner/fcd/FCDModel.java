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
package io.github.dsheirer.source.tuner.fcd;

public enum FCDModel 
{ 
	FUNCUBE_DONGLE_PRO( "FunCube Dongle Pro" ), 
	FUNCUBE_DONGLE_PRO_PLUS( "FunCube Dongle Pro Plus"),
	FUNCUBE_UNKNOWN( "Unknown" );

	private String mLabel;

	private FCDModel( String label )
	{
		mLabel = label;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public static FCDModel getFCD( String board )
	{
		FCDModel retVal = FUNCUBE_UNKNOWN;

		if( board.equalsIgnoreCase( "Brd 1.1" ) )
		{
			retVal = FUNCUBE_DONGLE_PRO;
		}
		else if( board.equalsIgnoreCase( "Brd 2.0" ) )
		{
			retVal = FUNCUBE_DONGLE_PRO_PLUS;
		}
		
		return retVal;
	}
}
