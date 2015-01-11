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
package decode.tait;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import map.Plottable;
import message.Message;

import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import edac.CRC;

public class Tait1200GPSMessage extends Message
{
	private final static Logger mLog = LoggerFactory.getLogger( Tait1200GPSMessage.class );

	public static int[] REVS_1 = { 0,1,2,3 };
	public static int[] SYNC = { 4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19 };
	public static int[] SIZE = { 20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35 };
	public static int[] FROM_DIGIT_1 = { 36,37,38,39,40,41,42,43 };
	public static int[] FROM_DIGIT_2 = { 44,45,46,47,48,49,50,51 };
	public static int[] FROM_DIGIT_3 = { 52,53,54,55,56,57,58,59 };
	public static int[] FROM_DIGIT_4 = { 60,61,62,63,64,65,66,67 };
	public static int[] FROM_DIGIT_5 = { 68,69,70,71,72,73,74,75 };
	public static int[] FROM_DIGIT_6 = { 76,77,78,79,80,81,82,83 };
	public static int[] FROM_DIGIT_7 = { 84,85,86,87,88,89,90,91 };
	public static int[] FROM_DIGIT_8 = { 92,93,94,95,96,97,98,99 };
	public static int[] CHECKSUM_1 = { 100,101,102,103,104,105,106,107,108,109,
		110,111,112,113,114,115 };

	public static int[] REVS_2 = { 116,117,118,119,120,121,122,123,124,125,126,127,
		128,129,130,131	};
	public static int[] SIZE_2 = { 188,189,190,191,192,193,194,195,196,197,198,
		199,200,201,202,203 };
	public static int[] TO_DIGIT_1 = { 204,205,206,207,208,209,210,211 };
	public static int[] TO_DIGIT_2 = { 212,213,214,215,216,217,218,219 };
	public static int[] TO_DIGIT_3 = { 220,221,222,223,224,225,226,227 };
	public static int[] TO_DIGIT_4 = { 228,229,230,231,232,233,234,235 };
	public static int[] TO_DIGIT_5 = { 236,237,238,239,240,241,242,243 };
	public static int[] TO_DIGIT_6 = { 244,245,246,247,248,249,250,251 };
	public static int[] TO_DIGIT_7 = { 252,253,254,255,256,257,258,259 };
	public static int[] TO_DIGIT_8 = { 260,261,262,263,264,265,266,267 };
	public static int[] UNKNOWN_1 = { 268,269,270,271,272,273,274,275 };
	public static int[] CHECKSUM_2 = { 276,277,278,279,280,281,282,283,284,285,
		286,287,288,289,290,291 };
	public static int DIVIDER_1 = 292;
	public static int[] HOUR_TENS = { 293,294,295 };
	public static int[] HOUR_ONES = { 296,297,298,299 };
	public static int DIVIDER_2 = 300;
	public static int[] MINUTES_TENS = { 301,302,303 };
	public static int[] MINUTES_ONES = { 304,305,306,307 };
	public static int DIVIDER_3 = 308;
	public static int[] SECONDS_TENS = { 309,310,311 };
	public static int[] SECONDS_ONES = { 312,313,314,315 };
	public static int DIVIDER_4 = 316;
	public static int[] LATITUDE_SIGN = { 317,318 };
	public static int DIVIDER_5 = 319;
	public static int[] LATITUDE_DEGREES_TENS = { 320,321,322,323 };
	public static int[] LATITUDE_DEGREES_ONES = { 324,325,326,327 };
	public static int DIVIDER_6 = 328;
	public static int[] LATITUDE_MINUTES_TENS = { 329,330,331 };
	public static int[] LATITUDE_MINUTES_ONES = { 332,333,334,335 };
	public static int[] LATITUDE_SECONDS_HUND = { 336,337,338,339 };
	public static int[] LATITUDE_SECONDS_TENS = { 340,341,342,344 };
	public static int[] LATITUDE_SECONDS_ONES = { 344,345,346,347 };
	public static int DIVIDER_7 = 348;
	public static int[] LONGITUDE_SIGN = { 349,350 };
	public static int LONGITUDE_DEGREES_HUNDREDS = 351;
	public static int[] LONGITUDE_DEGREES_TENS = { 352,353,354,355 };
	public static int[] LONGITUDE_DEGREES_ONES = { 356,357,358,359 };
	public static int DIVIDER_9 = 360;
	public static int[] LONGITUDE_MINUTES_TENS = { 361,362,363 };
	public static int[] LONGITUDE_MINUTES_ONES = { 364,365,366,367 };
	public static int[] LONGITUDE_SECONDS_HUND = { 368,369,370,371 };
	public static int[] LONGITUDE_SECONDS_TENS = { 372,373,374,375 };
	public static int[] LONGITUDE_SECONDS_ONES = { 376,377,378,379 };
	public static int DIVIDER_10 = 380;
	public static int[] UNKNOWN_2 = { 381,382 };
	public static int[] DATE_DAY = { 383,384,385,386,387 };
	public static int[] DATE_MONTH = { 388,389,390,391 };
	public static int[] HEADING = { 392,393,394,395,396,397,398,399,400 };
	public static int[] SPEED = { 401,402,403,404,405,406,407,408,409 };

	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );

    private BitSetBuffer mMessage;
    private AliasList mAliasList;
    private CRC mCRC;
    
    public Tait1200GPSMessage( BitSetBuffer message, AliasList list )
    {
    	mMessage = message;
        mAliasList = list;
        
        mLog.debug( toString() );
    }
    
    public boolean isValid()
    {
    	//TODO: Override until we figure out the CRC
    	return true;
    }
    
    public int getMessage1Size()
    {
    	return mMessage.getInt( SIZE );
    }
    
    public int getMessage2Size()
    {
    	return mMessage.getInt( SIZE_2 );
    }

    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( "GPS FROM:" );
    	sb.append( getFromID() );
    	sb.append( " TO:" );
    	sb.append( getToID() );
    	sb.append( " LOCATION:" );
    	
    	GeoPosition location = getGPSLocation();
    	
    	sb.append( location.getLatitude() );
    	sb.append( " " );
    	sb.append( location.getLongitude() );
    	
    	sb.append( " GPS TIME:" );
    	sb.append( mSDF.format( new Date( getGPSTime() ) ) );
    	
    	sb.append( " " );
    	sb.append( mMessage.toString() );

    	return sb.toString();
    }
    
    public GeoPosition getGPSLocation()
    {
    	double latitude = mMessage.getInt( LATITUDE_DEGREES_TENS ) * 10.0d;
    	latitude += mMessage.getInt( LATITUDE_DEGREES_ONES );
    	latitude += (double)mMessage.getInt( LATITUDE_MINUTES_TENS ) / 6.0d;
    	latitude += (double)mMessage.getInt( LATITUDE_MINUTES_ONES ) / 60.0d;
    	latitude += (double)mMessage.getInt( LATITUDE_SECONDS_HUND ) / 600.0d;
    	latitude += (double)mMessage.getInt( LATITUDE_SECONDS_TENS ) / 6000.0d;
    	latitude += (double)mMessage.getInt( LATITUDE_SECONDS_ONES ) / 60000.0d;

    	if( mMessage.getInt( LATITUDE_SIGN ) == 0 )
    	{
    		latitude *= -1;
    	}
    	
    	double longitude = mMessage.get( LONGITUDE_DEGREES_HUNDREDS ) ? 100.0d : 0.0d;

    	longitude += mMessage.getInt( LONGITUDE_DEGREES_TENS ) * 10.0d;
    	longitude += mMessage.getInt( LONGITUDE_DEGREES_ONES );
    	longitude += (double)mMessage.getInt( LONGITUDE_MINUTES_TENS ) / 6.0d;
    	longitude += (double)mMessage.getInt( LONGITUDE_MINUTES_ONES ) / 60.0d;
    	longitude += (double)mMessage.getInt( LONGITUDE_SECONDS_HUND ) / 600.0d;
    	longitude += (double)mMessage.getInt( LONGITUDE_SECONDS_TENS ) / 6000.0d;
    	longitude += (double)mMessage.getInt( LONGITUDE_SECONDS_ONES ) / 60000.0d;
    	
    	if( mMessage.getInt( LONGITUDE_SIGN ) == 0 )
    	{
    		longitude = -1;
    	}
    	
    	return new GeoPosition( latitude, longitude );
    }
    
    public long getGPSTime()
    {
    	Calendar cal = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
    	
    	cal.clear();
    	
    	/* Use current time to get current year */
    	cal.setTimeInMillis( System.currentTimeMillis() );
    	
    	cal.set( Calendar.MONTH, mMessage.getInt( DATE_MONTH ) );
    	cal.set( Calendar.DAY_OF_MONTH, mMessage.getInt( DATE_DAY ) );
    	cal.set( Calendar.HOUR_OF_DAY, ( mMessage.getInt( HOUR_TENS ) * 10 ) +
    			mMessage.getInt( HOUR_ONES ) );
    	cal.set( Calendar.MINUTE, ( mMessage.getInt( MINUTES_TENS ) * 10 ) +
    			mMessage.getInt( MINUTES_ONES ) );
    	cal.set( Calendar.SECOND, ( mMessage.getInt( SECONDS_TENS ) * 10 ) +
    			mMessage.getInt( SECONDS_ONES ) );
    	cal.set( Calendar.MILLISECOND, 0 );
    	
    	return cal.getTimeInMillis();
    }
    
	@Override
    public String getFromID()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( getCharacter( FROM_DIGIT_1 ) );
    	sb.append( getCharacter( FROM_DIGIT_2 ) );
    	sb.append( getCharacter( FROM_DIGIT_3 ) );
    	sb.append( getCharacter( FROM_DIGIT_4 ) );
    	sb.append( getCharacter( FROM_DIGIT_5 ) );
    	sb.append( getCharacter( FROM_DIGIT_6 ) );
    	sb.append( getCharacter( FROM_DIGIT_7 ) );
    	sb.append( getCharacter( FROM_DIGIT_8 ) );
    	
    	return sb.toString();
    }
	
	@Override
    public Alias getFromIDAlias()
    {
		if( mAliasList != null )
		{
			return mAliasList.getTalkgroupAlias( getFromID() );
		}
		
		return null;
    }
    
	@Override
    public String getToID()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( getCharacter( TO_DIGIT_1 ) );
    	sb.append( getCharacter( TO_DIGIT_2 ) );
    	sb.append( getCharacter( TO_DIGIT_3 ) );
    	sb.append( getCharacter( TO_DIGIT_4 ) );
    	sb.append( getCharacter( TO_DIGIT_5 ) );
    	sb.append( getCharacter( TO_DIGIT_6 ) );
    	sb.append( getCharacter( TO_DIGIT_7 ) );
    	sb.append( getCharacter( TO_DIGIT_8 ) );
    	
    	return sb.toString();
    }
	
	@Override
    public Alias getToIDAlias()
    {
		if( mAliasList != null )
		{
			return mAliasList.getTalkgroupAlias( getToID() );
		}
		
		return null;
    }
    
    public char getCharacter( int[] bits )
    {
    	int value = mMessage.getInt( bits );
    	
    	return (char)value;
    }

    /**
     * Pads spaces onto the end of the value to make it 'places' long
     */
    public String pad( String value, int places, String padCharacter )
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( value );
    	
    	while( sb.length() < places )
    	{
    		sb.append( padCharacter );
    	}
    	
    	return sb.toString();
    }
    
    /**
     * Pads an integer value with additional zeroes to make it decimalPlaces long
     */
    public String format( int number, int decimalPlaces )
    {
    	StringBuilder sb = new StringBuilder();

    	int paddingRequired = decimalPlaces - ( String.valueOf( number ).length() );

    	for( int x = 0; x < paddingRequired; x++)
    	{
    		sb.append( "0" );
    	}
    	
    	sb.append( number );
    	
    	return sb.toString();
    }
    
	@Override
    public String getBinaryMessage()
    {
		return mMessage.toString();
	}

	@Override
    public String getProtocol()
    {
	    return "Tait-1200";
    }

	@Override
    public String getEventType()
    {
	    return "GPS";
    }

	@Override
    public String getMessage()
    {
		return toString();
    }
	
	@Override
    public String getErrorStatus()
    {
		return "UNKNOWN";
//	    return mCRC.getDisplayText();
    }

	@Override
    public Plottable getPlottable()
    {
		return new Plottable( getGPSTime(), getGPSLocation(), getFromID(), 
				getFromIDAlias() );
    }
}
