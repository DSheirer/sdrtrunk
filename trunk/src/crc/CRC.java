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
package crc;

public enum CRC
{
	PASSED       ( "*", "Pass" ),
	PASSED_INV   ( "#", "Pass Invert" ),
	FAILED_CRC   ( "f", "Fail CRC" ),
	FAILED_PARITY( "p", "Fail Parity" ),
	CORRECTED    ( "C", "Corrected" ),
	UNKNOWN      ( "-", "Unknown");

	private String mAbbreviation;
	private String mDisplayText;
	
	CRC( String abbreviation, String displayText )
	{
		mAbbreviation = abbreviation;
		mDisplayText = displayText;
	}
	
	public String getAbbreviation()
	{
		return mAbbreviation;
	}
	
	public String getDisplayText()
	{
		return mDisplayText;
	}

	public static String format( CRC[] checks )
	{
		StringBuilder sb = new StringBuilder();
		
		for( int x = 0; x < checks.length; x++ )
		{
			CRC check = checks[ x ];
			
			if( check != null )
			{
				sb.append( check.getAbbreviation() );
			}
			else
			{
				sb.append( CRC.UNKNOWN.getAbbreviation() );
			}
		}
		
		return sb.toString();
	}
}
