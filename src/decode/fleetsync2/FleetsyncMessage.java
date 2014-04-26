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
package decode.fleetsync2;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import map.Plottable;
import message.Message;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import crc.CRCFleetsync;

public class FleetsyncMessage extends Message
{
	private static DecimalFormat mDecimalFormatter = new DecimalFormat("#.#####");
	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );
	
	//Calendar to use in calculating time hacks
	Calendar mCalendar = new GregorianCalendar();
	
	//Message parts are identified in big-endian order for correct translation

	//Message Header
	private static int[] sRevs = { 4,3,2,1,0 };
	private static int[] sSync = { 20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5 };

	//Message Block 1
	private static int[] sStatusMessage = { 27,26,25,24,23,22,21 };
	private static int[] sMessageType = { 33,32,31,30,29 };

	private static int sEmergencyFlag = 22;
	private static int sLoneWorkerFlag = 24;
	private static int sPagingFlag = 26;
	private static int sEndOfTransmissionFlag = 27;
	private static int sManualFlag = 28;
	private static int sANIFlag = 29;
	private static int sStatusFlag = 30;
	private static int sAcknowledgeFlag = 31;
	//32 - unknown - always 0
	//33 - unknown - always 0
	//34 - unknown - set for ACKNOWLEDGE
	private static int sGPSExtensionFlag = 35;
	private static int sFleetExtensionFlag = 36;
	private static int[] sFleetFrom = { 44,43,42,41,40,39,38,37 };
	private static int[] sIdentFrom = { 56,55,54,53,52,51,50,49,48,47,46,45 };
	private static int[] sIdentTo = { 68,67,66,65,64,63,62,61,60,59,58,57 };
	private static int[] sCRC1 = { 84,83,82,81,80,79,78,77,76,75,74,73,72,71,70,69 };

	//Message Block 2
	private static int[] sFleetTo = { 92,91,90,89,88,87,86,85 };
	private static int[] sCRC2 = { 148,147,146,145,144,143,142,141,140,139,138,137,136,135,134,132 };

	//Message Block 3
	private static int[] sGPSHours = { 176,175,174,173,172 };
	private static int[] sGPSMinutes = { 182,181,180,179,178,177 };
	private static int[] sGPSSeconds = { 188,187,186,185,184,183 };
	private static int[] sCRC3 = { 212,211,210,209,208,207,206,205,204,203,202,201,200,199,198,197 };

	//Message Block 4
	private static int[] sGPSChecksum = { 220,219,218,217,216,215,214,213 };
	private static int[] sLatitudeDegreesMinutes = { 236,235,234,233,232,231,230,229,228,227,226,225,224,223,222,221 };
	private static int[] sLatitudeDecimalMinutes = { 251,250,249,248,247,246,245,244,243,242,241,240,239,238 };
	private static int[] sSpeed = { 276,275,274,273,272,271,270,269,268,267,266,265,264,263,262,261,260,259,258,257,256,255,254,253,252 };
	private static int[] sCRC4 = { 276,275,274,273,272,271,270,269,268,267,266,265,264,263,262,261 };

	//Message Block 5
    private static int[] sGPSCentury = { 284,283,282,281,280,279,278,277 };
	private static int[] sGPSYear = { 291,290,289,288,287,286,285 };
	private static int[] sGPSMonth = { 295,294,293,292 };
	private static int[] sGPSDay = { 300,299,298,297,296 };
	private static int[] sLongitudeDegreesMinutes = { 316,315,314,313,312,311,310,309,308,307,306,305,304,303,302,301 };
	private static int[] sLongitudeDecimalMinutes = { 331,330,329,328,327,326,325,324,323,322,321,320,319,318 };
	private static int[] sCRC5 = { 340,339,338,337,336,335,334,333,332,331,330,329,328,327,326,325 };

	//Message Block 6
	private static int[] sGPSUnknown1 = { 352,351,350,349 };
	private static int[] sGPSHeading = { 365,363,362,361,360,359,358,357,356,355,354,353 };
	private static int[] sCRC6 = { 404,403,402,401,400,399,398,397,396,395,394,393,392,391,390,389 };

	//Message Block 7
	private static int[] sBLK7WhatIsIt = { 444,443,442,441,440,439,438,437,436,435,434,433,432,431,430,429 };
	private static int[] sCRC7 = { 468,467,466,465,464,463,462,461,460,459,458,457,456,455,454,453 };

	//Message Block 8
	private static int[] sGPSSpeed = { 491,490,489,488,487,486,485,484 };
	private static int[] sGPSSpeedFractional = { 499,498,497,496,495,494,493,492 };
	private static int[] sCRC8 = { 532,531,530,529,528,527,526,525,524,523,522,521,520,519,518,517 };
	
    private BitSetBuffer mMessage;
    private CRC[] mCRC = new CRC[ 8 ];
    private AliasList mAliasList;
    
    public FleetsyncMessage( BitSetBuffer message, AliasList list )
    {
        mMessage = message;
        mAliasList = list;
        checkParity();
    }
    
    private void checkParity()
    {
    	//Check message block 1
    	mCRC[ 0 ] = detectAndCorrect( 21, 85 );

    	//Only check subsequent blocks if we know block 1 is correct
    	if( mCRC[ 0 ] == CRC.PASSED || mCRC[ 0 ] == CRC.CORRECTED )
    	{
    		if( hasFleetExtensionFlag() )
    		{
    			//Check message block 2
    			mCRC[ 1 ] = detectAndCorrect( 85, 149 );
    		}
    		
    		if( hasGPSFlag() )
    		{
    	    	//Check message block 3
    			mCRC[ 2 ] = detectAndCorrect( 149, 213 );
    	    	//Check message block 4
    			mCRC[ 3 ] = detectAndCorrect( 213, 277 );
    	    	//Check message block 5
    			mCRC[ 4 ] = detectAndCorrect( 277, 341 );
    	    	//Check message block 6
    			mCRC[ 5 ] = detectAndCorrect( 341, 405 );
    	    	//Check message block 7
    			mCRC[ 6 ] = detectAndCorrect( 405, 469 );
    	    	//Check message block 8
    			mCRC[ 7 ] = detectAndCorrect( 469, 533 );
    		}
    	}
    }
    
    private CRC detectAndCorrect( int start, int end )
    {
    	BitSet original = mMessage.get( start, end );
    	
    	CRC retVal = CRCFleetsync.check( original );
    	
    	//Attempt to correct single-bit errors
    	if( retVal == CRC.FAILED_PARITY )
    	{
    		int[] errorBitPositions = CRCFleetsync.findBitErrors( original );
    		
    		if( errorBitPositions != null )
    		{
    			for( int errorBitPosition: errorBitPositions )
    			{
    				mMessage.flip( start + errorBitPosition );
    			}
    			
    			retVal = CRC.CORRECTED;
    		}
    	}
    	
    	return retVal;
    }
    
    public boolean isValid()
    {
    	boolean valid = true;
    	
    	for( int x = 0; x < mCRC.length; x++ )
    	{
    		CRC crc = mCRC[ x ];
    		
    		if( crc != null )
    		{
    			if( crc == CRC.FAILED_CRC ||
    				crc == CRC.FAILED_PARITY || 
    				crc == CRC.UNKNOWN )
    			{
    				valid = false;
    			}
    		}
    	}
    	
        return valid;
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( mSDF.format( new Date( System.currentTimeMillis() ) ) );

    	sb.append( " FSync2 " + getParity() );

    	sb.append( getMessage() );
    	
    	sb.append( " [" + mMessage.toString() + "]" );

    	return sb.toString();
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
    
    public FleetsyncMessageType getMessageType()
    {
    	if( hasStatusFlag() )
    	{
        	if( hasAcknowledgeFlag() )
        	{
        		return FleetsyncMessageType.ACKNOWLEDGE;
        	}

    		if( hasGPSFlag() )
    		{
        		return FleetsyncMessageType.GPS;
    		}
    		else
    		{
        		return FleetsyncMessageType.STATUS;
    		}
    	}
    	else
    	{
        	if( hasAcknowledgeFlag() )
        	{
        		return FleetsyncMessageType.ACKNOWLEDGE;
        	}

        	if( hasANIFlag() )
        	{
        		return FleetsyncMessageType.ANI;
        	}

        	if( hasGPSFlag() )
        	{
        		return FleetsyncMessageType.GPS;
        	}
        	
        	if( hasPagingFlag() )
        	{
    			return FleetsyncMessageType.PAGING;
        	}
        	
        	if( hasEmergencyFlag() )
        	{
        		if( hasLoneWorkerFlag() )
        		{
        			return FleetsyncMessageType.LONE_WORKER_EMERGENCY;
        		}
        		else
        			return FleetsyncMessageType.EMERGENCY;
        	}
    	}
    	
		return FleetsyncMessageType.UNKNOWN;
    }
    
    public int getStatus()
    {
		return getStatusNumber() + 9;
    }
    
    /**
     * Returns the RAW status number.  The actual status number should be
     * accessed via the getStatus() method.
     * @return
     */
    public int getStatusNumber()
    {
    	return getInt( sStatusMessage );
    }

    /**
     * @return
     */
    public Alias getStatusAlias()
    {
    	if( mAliasList != null )
    	{
    		return mAliasList.getStatus( getStatus() );
    	}
    	
    	return null;
    }
    
    public int getFleetFrom()
    {
    	return getInt( sFleetFrom ) + 99;
    }
    
    public int getIdentifierFrom()
    {
    	return getInt( sIdentFrom ) + 999;
    }

    /**
     * Inverted Flags - 0 = flag is true
     * @return
     */
    public boolean hasEndOfTransmissionFlag()
    {
    	return !mMessage.get( sEndOfTransmissionFlag );
    }
    
    public boolean hasEmergencyFlag()
    {
    	return !mMessage.get( sEmergencyFlag );
    }
    
    public boolean hasLoneWorkerFlag()
    {
    	return !mMessage.get( sLoneWorkerFlag );
    }
    
    public boolean hasPagingFlag()
    {
    	return !mMessage.get( sPagingFlag );
    }
    

    /**
     * Normal Flags - 1 = flag is true
     * @return
     */
    public boolean hasANIFlag()
    {
    	return mMessage.get( sANIFlag );
    }

    public boolean hasAcknowledgeFlag()
    {
    	return mMessage.get( sAcknowledgeFlag );
    }

    public boolean hasFleetExtensionFlag()
    {
    	return mMessage.get( sFleetExtensionFlag );
    }
    
    public boolean hasGPSFlag()
    {
    	return mMessage.get( sGPSExtensionFlag );
    }
    
    public boolean hasStatusFlag()
    {
    	return mMessage.get( sStatusFlag );
    }
    
    
    public int getFleetTo()
    {
    	if( hasFleetExtensionFlag() )
    	{
        	return getInt( sFleetTo ) + 99;
    	}
    	else
    	{
        	return getInt( sFleetFrom ) + 99;
    	}
    }
    
    public int getIdentifierTo()
    {
    	return getInt( sIdentTo ) + 999;
    }
    
    /**
     * GPS Heading 
     * @return
     */
    public double getHeading()
    {
    	double retVal = 0.0;
    	
    	int heading = getInt( sGPSHeading );
    	
    	if( heading != 4095 )
    	{
    		retVal = (double)(heading / 10.0 );
    	}
    	
    	return retVal;
    }
    
    public int getBlock7WhatIsIt()
    {
    	int value = getInt( sBLK7WhatIsIt );
    	
    	if( value == 65535 )
    	{
    		value = -1;
    	}
    	
    	return value;
    }
    
    /**
     * Speed in Kph - whole numbers and three decimal digits of precision
     * @return
     */
    public double getSpeed()
    {
    	double retVal = 0.0;
    	
    	int temp = getInt( sSpeed );
    	
    	if( temp != 0 )
    	{
    		retVal = (double)temp / 1000.0D;
    	}
    	return retVal;
    }
    
    public double getLatitude()
    {
    	//TODO: determine the correct hemisphere indicator and replace this
    	//hardcoded "0" with the correct hemisphere value

    	return convertDDMToDD( 0, 
    						   getInt( sLatitudeDegreesMinutes ), 
    						   getInt( sLatitudeDecimalMinutes ) );
    }
    
    public double getLongitude()
    {
    	//TODO: determine the correct hemisphere indicator and replace this
    	//hardcoded "1" with the correct hemisphere value

    	return convertDDMToDD( 1, 
				   getInt( sLongitudeDegreesMinutes ), 
				   getInt( sLongitudeDecimalMinutes ) );
    }

    /**
     * Converts Degrees Decimal Minutes to Decimal Degrees
     * 
     * Latitude and Longitude values are represented by:
     * 
     * @param hemisphere - 0=North & East, 1=South & West
     * @param degreesMinutes - an integer value with the first 2-3 digits representing
     * the degrees and the last two digits representing the minutes
     * 
     * @param decimalDegrees - an integer value representing the fractional
     * minutes
     * 
     * @return - decimal degrees formatted value
     */
    public double convertDDMToDD( int hemisphere, int degreesMinutes, int decimalDegrees )
    {
    	double retVal = 0.0;

    	if( degreesMinutes != 0 )
    	{
        	//Degrees - divide value by 100 and retain the whole number value (ie degrees)
        	retVal += (double)( degreesMinutes / 100 );
        	
        	//Minutes - modulus by 100 to get the whole minutes value
        	int wholeMinutes = degreesMinutes % 100;

        	if( wholeMinutes != 0 )
        	{
        		retVal += (double)( wholeMinutes / 60.0D );
        	}
    	}

    	if( decimalDegrees != 0 )
    	{
        	//Fractional Minutes - divide by 10,000 to get the decimal place correct
        	//then divide by 60 (minutes) to get the decimal value
    		//10,000 * 60 = 600,000
    		retVal += (double)( decimalDegrees / 600000.0D );
    	}

    	//Adjust the value +/- for the hemisphere
    	if( hemisphere == 1 ) //South and West values
    	{
    		retVal = -retVal;
    	}
    	
    	return retVal;
    }
    
    public String getGPSTime()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append( format( getInt( sGPSHours ), 2 ) );
    	sb.append( ":" );
    	sb.append( format( getInt( sGPSMinutes ), 2 ) );
    	sb.append( ":" );
    	sb.append( format( getInt( sGPSSeconds ), 2 ) );
    	sb.append( "z" );
    	
    	return sb.toString();
    }
    

    /**
     * 7 bit checksum that is part of the GPGGA message, I think
     * @return
     */
    public int getGPSChecksum()
    {
    	return getInt( sGPSChecksum );
    }
    
    public String getGPSChecksumString()
    {
    	String sum = Integer.toHexString( getGPSChecksum() );

    	if( sum.length() == 1 )
    	{
    		return "*0" + sum;
    	}
    	else
    	{
    		return "*" + sum;
    	}
    }
    
    /**
     * Returns 1-based calendar day of month
     * 1 = 1st day of month
     */
    public int getGPSDay()
    {
    	return getInt( sGPSDay ) + 1;
    }

    /**
     * Returns 0-based GPS Month
     * 0 = January
     * 
     * Note: actual day of month value is 1-based, so we subtract 1 to get
     * the actual day of month value
     */
    public int getGPSMonth()
    {
    	return getInt( sGPSMonth );
    }
    
    /**
     * Returns year, which is a combination of the century bit field + 1, times
     * 100, plus the year bit field
     */
    public int getGPSYear()
    {
//    	return ( ( getInt( sGPSCentury ) + 1 ) * 100 ) + getInt( sGPSYear );
    	return ( 2000 ) + getInt( sGPSYear );
    }
    
    public String getGPSLocation()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append( pad( String.valueOf( mDecimalFormatter.format( getLatitude() ) ), 8, "0" ) + " " );
    	sb.append( pad( String.valueOf( mDecimalFormatter.format( getLongitude() ) ), 10, "0" ) );

    	return sb.toString();
    }
    
    
    public String getGPSDate()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( getGPSYear() + "-" );
    	
    	int month = getGPSMonth();
    	
    	if( month < 10 )
    	{
    		sb.append( "0" );
    	}
    	
    	sb.append( month + "-" );

    	int day = getGPSDay();
    	
    	if( day < 10 )
    	{
    		sb.append( "0" );
    	}
    	
    	sb.append( day );

    	return sb.toString();
    }
    
    private int getInt( int[] bits )
    {
    	int retVal = 0;
    	
    	for( int x = 0; x < bits.length; x++ )
    	{
    		if( mMessage.get( bits[ x ] ) )
    		{
    			retVal += 1<<x;
    		}
    	}
    	
    	return retVal;
    }

	@Override
    public String getBinaryMessage()
    {
		return mMessage.toString();
	}

	/**
	 * String representing results of the parity check
	 * 
	 *   [P] = passes parity check
	 *   [f] = fails parity check
	 *   [C] = corrected message
	 *   [-] = message section not present
	 */
	public String getParity()
	{
		return "[" + CRC.format( mCRC ) + "]";
	}

	@Override
    public String getProtocol()
    {
	    return "Fleetsync II";
    }

	@Override
    public String getEventType()
    {
	    return getMessageType().toString();
    }

	@Override
    public String getFromID()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( getFleetFrom() );
		sb.append( "-" );
		sb.append( getIdentifierFrom() );
		
		return sb.toString();
    }

	@Override
    public Alias getFromIDAlias()
    {
		if( mAliasList != null )
		{
			return mAliasList.getFleetsyncAlias( getFromID() ); 
		}
		
		return null;
    }

	@Override
	public String getToID()
	{
		StringBuilder sb = new StringBuilder();
		
    	sb.append( getFleetTo() );
    	sb.append( "-" );
    	sb.append( getIdentifierTo() );
		
	    return sb.toString();
	}

	@Override
    public Alias getToIDAlias()
    {
		if( mAliasList != null )
		{
			return mAliasList.getFleetsyncAlias( getToID() ); 
		}
		
		return null;
    }
	
	private String getFromTo( boolean includeFrom, boolean includeTo )
	{
		StringBuilder sb = new StringBuilder();

		if( includeFrom )
		{
			sb.append( "FM:" );
			sb.append( getFromID() );
			
			Alias from = getFromIDAlias();
			
			if( from != null )
			{
				sb.append( " " );
				sb.append( from.getName() );
			}
		}
		
		if( includeFrom && includeTo )
		{
			sb.append( " " );
		}
		
		if( includeTo )
		{
			sb.append( "TO:" );
			sb.append( getToID() );
			
			Alias to = getToIDAlias();
			
			if( to != null )
			{
				sb.append( " " );
				sb.append( to.getName() );
			}
		}
		
		return sb.toString();
	}

	@Override
    public String getMessage()
    {
    	StringBuilder sb = new StringBuilder();

    	FleetsyncMessageType type = getMessageType();

    	switch( type )
    	{
	    	case ACKNOWLEDGE:
	        	sb.append( "ACKNOWLEDGE " );
	        	sb.append( getFromTo( true, true ) );
	    		break;
	    	case ANI:
	        	sb.append( "ANI " );
	        	sb.append( getFromTo( true, false ) );
	    		break;
	    	case EMERGENCY:
	        	sb.append( "**EMERGENCY** " );
	        	sb.append( getFromTo( true, true ) );
	    		break;
	    	case GPS:
	        	sb.append( "GPS [" );
	        	sb.append( getGPSLocation() );
	        	sb.append( "] " );

	        	sb.append( getFromTo( true, true ) );
	        	
	        	sb.append( " HDG:" );
	        	sb.append( pad( String.valueOf( getHeading() ), 5, " " ) );

	        	sb.append( " GPSDate[" );
	        	sb.append( getGPSDate() );
	        	sb.append( " " );
	        	sb.append( getGPSTime() );
	        	sb.append( "]" );
	    		break;
	    	case LONE_WORKER_EMERGENCY:
	        	sb.append( "**LONE WORKER EMERGENCY** " );
	        	sb.append( getFromTo( true, true ) );
	    		break;
	    	case PAGING:
	        	sb.append( "PAGING " );
	        	sb.append( getFromTo( true, true ) );
	    		break;
	    	case STATUS:
	        	sb.append( "STATUS: " + format( getStatus(), 2 ) );
	        	
	        	Alias status = getStatusAlias();
	        	if( status != null )
	        	{
		        	sb.append( "/" );
		        	sb.append( status.getName() );
	        	}
	        	
	        	sb.append( getFromTo( true, true ) );
	    		break;
	    	case UNKNOWN:
			default:
	    		sb.append( "*** UNKNOWN *** " );
	        	sb.append( getFromTo( true, true ) );
	    		break;
    	}

    	return sb.toString();
    }

	@Override
    public String getErrorStatus()
    {
	    return CRC.format( mCRC );
    }

	@Override
    public Plottable getPlottable()
    {
		if( isValid() && getMessageType() == FleetsyncMessageType.GPS )
		{
			GeoPosition position = 
					new GeoPosition( getLatitude(), getLongitude() );

			Alias alias = mAliasList.getFleetsyncAlias( getFromID() ); 
			
			return new Plottable( mTimeReceived, position, getFromID(), alias );
		}
		else
		{
		    return null;
		}
    }
}
