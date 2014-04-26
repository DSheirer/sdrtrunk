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
package util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeStamp
{
	private static SimpleDateFormat mSDFDate = new SimpleDateFormat( "yyyyMMdd" );
	private static SimpleDateFormat mSDFTime = new SimpleDateFormat( "HHmmss" );

	/**
	 * Returns the current system date formatted as yyyy-MM-dd
	 */
	public static String getFormattedDate()
	{
		return getFormattedDate( System.currentTimeMillis() );
	}

	/**
	 * Returns the timestamp formatted as a date of yyyy-MM-dd
	 */
	public static String getFormattedDate( long timestamp )
	{
		return mSDFDate.format( new Date( timestamp ) );
	}
	
	/**
	 * Returns the current system time formatted as HH:mm:ss
	 */
	public static String getFormattedTime()
	{
		return getFormattedTime( System.currentTimeMillis() );
	}

	/**
	 * Returns the timestamp formatted as a time of HH:mm:ss
	 */
	public static String getFormattedTime( long timestamp )
	{
		return mSDFTime.format( new Date( timestamp ) );
	}

	/**
	 * Returns current system time formatted as yyyy-MM-dd*HH:mm:ss
	 * with the * representing the separator attribute
	 */
	public static String getTimeStamp( String separator )
	{
		return getTimeStamp( System.currentTimeMillis(), separator );
	}
	
	/**
	 * Returns timestamp formatted as yyyy-MM-dd*HH:mm:ss
	 * with the * representing the separator attribute
	 */
	public static String getTimeStamp( long timestamp, String separator )
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getFormattedDate( timestamp ) );
		sb.append( separator );
		sb.append( getFormattedTime( timestamp ) );

		return sb.toString();
	}
}

