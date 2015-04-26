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
package decode.lj1200;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import map.Plottable;
import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.Alias;
import alias.AliasList;
import bits.BinaryMessage;
import edac.CRC;
import edac.CRCLJ;

public class LJ1200Message extends Message
{
	private final static Logger mLog = LoggerFactory.getLogger( LJ1200Message.class );
	
	public static final String[] REPLY_CODE = { "0","1","2","3","4","5","6","7",
		"8","9","A","C","D","E","F","G","H","J","K","L","M","N","P","Q","R","S",
		"T","U","V","W","X","Y" };

	public static int[] SYNC = { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 };

	public static int[] VRC = { 23,22,21,20,19,18,17,16 };
	
	public static int[] LRC = { 31,30,29,28,27,26,25,24 };

	public static int[] FUNCTION = { 35,34,33,32 };

	public static int[] ADDRESS = { 63,62,61,60,59,58,57,56,55,54,53,52,51,50,
		49,48,47,46,45,44,43,42,41,40,39,38,37,36 };

	public static int[] REPLY_1 = { 39,38,37,36,43 };
	public static int[] REPLY_2 = { 42,41,40,47,46 };
	public static int[] REPLY_3 = { 45,44,51,50,49 };
	public static int[] REPLY_4 = { 48,55,54,53,52 };
	public static int[] REPLY_5 = { 59,58,57,56,63 };

	public static int[] MESSAGE_CRC = { 79,78,77,76,75,74,73,72,71,70,69,68,67,
		66,65,64 };
	
	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );

    private BinaryMessage mMessage;
    private AliasList mAliasList;
    private CRC mCRC;
    
    public LJ1200Message( BinaryMessage message, AliasList list )
    {
    	mMessage = message;
        mAliasList = list;
        
        checkCRC();

        switch( mCRC )
        {
			case CORRECTED:
	        	mLog.debug( "CORR:" + message.toString() );
				break;
			case FAILED_CRC:
	        	mLog.debug( "FAIL:" + message.toString() );
				break;
			case PASSED:
	        	mLog.debug( "PASS:" + message.toString() );
				break;
        }
    }
    
    private void checkCRC()
    {
    	mCRC = CRCLJ.checkAndCorrect( mMessage );
    }
    
    public boolean isValid()
    {
    	return mCRC == CRC.PASSED || mCRC == CRC.CORRECTED;
    }
    
    public String getVRC()
    {
    	return mMessage.getHex( VRC, 2 );
    }
    
    public String getLRC()
    {
    	return mMessage.getHex( LRC, 2 );
    }
    
    public String getCRC()
    {
    	return mMessage.getHex( MESSAGE_CRC, 4 );
    }
    
    public Function getFunction()
    {
    	return Function.fromValue( mMessage.getInt( FUNCTION ), 
    							   mMessage.getInt( REPLY_3 ) );
    }
    
    public String getAddress()
    {
    	return mMessage.getHex( ADDRESS, 7 );
    }
    
    public Alias getAddressAlias()
    {
    	if( mAliasList != null )
    	{
    		return mAliasList.getESNAlias( getAddress() );
    	}
    	
    	return null;
    }
    
    public Alias getSiteAndReplyCodeAlias()
    {
    	if( mAliasList != null && 
    		( getFunction() == Function.F1_SITE_ID || 
    		  getFunction() == Function.F1_SPEED_UP ) )
    	{
    		return mAliasList.getSiteID( getReplyCode() );
    	}
    	
    	return null;
    }

    /**
     * 5 character reply code for function E and F, transponder activation.
     * @return - reply code
     */
    public String getReplyCode()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( REPLY_CODE[ mMessage.getInt( REPLY_1 ) ] );
    	sb.append( REPLY_CODE[ mMessage.getInt( REPLY_2 ) ] );
    	sb.append( REPLY_CODE[ mMessage.getInt( REPLY_3 ) ] );
    	sb.append( REPLY_CODE[ mMessage.getInt( REPLY_4 ) ] );
    	sb.append( REPLY_CODE[ mMessage.getInt( REPLY_5 ) ] );
    	
    	return sb.toString();
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();

    	Function function = getFunction();
    	
    	sb.append( "FUNCTION: " );
    	sb.append( function.toString() );

    	switch( function )
    	{
    	case F1_SITE_ID:
    		sb.append( " SITE [" );
    		break;
    	case F1_SPEED_UP:
    	case F2_TEST:
    	case F3_DEACTIVATE:
    	case F4_ACTIVATE:
    	case FF_TRACK_PULSE:
		default:
    		sb.append( " REPLY CODE [" );
	    	break;
    	}
    	
		sb.append( getReplyCode() );

		Alias site = getSiteAndReplyCodeAlias();
		
		if( site != null )
		{
			sb.append( "/" );
			sb.append( site.getName() );
		}
    	
    	sb.append( "] ADDRESS [" );
    	sb.append( getAddress() );
    	sb.append( "] VRC [" + getVRC() );
    	sb.append( "] LRC [" + getLRC() );
    	sb.append( "] CRC [" + getCRC() );
    	sb.append( "]" );

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
    
	@Override
    public String getBinaryMessage()
    {
		return mMessage.toString();
	}

	@Override
    public String getProtocol()
    {
	    return "LJ-1200";
    }

	@Override
    public String getEventType()
    {
		if( getFunction() == Function.F1_SPEED_UP )
		{
			return "TRANSPONDER";
		}
		
	    return "TOWER";
    }

	@Override
    public String getFromID()
    {
		return getReplyCode();
    }

	@Override
    public Alias getFromIDAlias()
    {
		return getSiteAndReplyCodeAlias();
    }

	@Override
	public String getToID()
	{
		return getAddress();
	}

	@Override
    public Alias getToIDAlias()
    {
		return null;
    }
	
	@Override
    public String getMessage()
    {
		return toString();
    }
	
	@Override
    public String getErrorStatus()
    {
	    return mCRC.getDisplayText();
    }

	@Override
    public Plottable getPlottable()
    {
		return null;
    }
	
	public enum Function
	{
/* Little Endian Format */
//		F0(             0x0, "0-UNKNOWN" ),
//		F1(             0x1, "1-UNKNOWN" ),
//		F2_ACTIVATION(  0x2, "2-ACTIVATION" ),
//		F3_SPEED_UP(    0x3, "3-SPEED-UP" ),
//		F4_TEST(        0x4, "4-TEST" ),
//		F5(             0x5, "5-UNKNOWN" ),
//		F6_SET_RATE(    0x6, "6-UNKNOWN" ), //Delete
//		F7(             0x7, "7-UNKNOWN" ),
//		F8_SITE_ID(     0x8, "8-SITE ID" ),
//		F9(             0x9, "9-UNKNOWN" ),
//		FA(             0xA, "A-UNKNOWN" ),
//		FB(             0xB, "B-UNKNOWN" ),
//		FC_DEACTIVATE(  0xC, "C-DEACTIVATE" ),
//		FD(             0xD, "D-UNKNOWN" ),
//		FE_TRACK_PULSE( 0xE, "E-UNKNOWN" ),
//		FF_TRACK_PULSE( 0xF, "F-TRACK PULSE" ),

		/* Big Endian Format */
		F1_SITE_ID( "1Y-SITE ID" ),
		F1_SPEED_UP( "1-SPEED UP" ),
		F2_TEST( "2-TEST" ),
		F3_DEACTIVATE( "3-DEACTIVATE" ),
		F4_ACTIVATE( "4-ACTIVATE" ),
		F5_UNKNOWN( "5-UNKNOWN" ),
		F6_UNKNOWN( "6-UNKNOWN" ),
		F7_UNKNOWN( "7-UNKNOWN" ),
		F8_UNKNOWN( "8-UNKNOWN" ),
		F9_UNKNOWN( "9-UNKNOWN" ),
		FA_UNKNOWN( "A-UNKNOWN" ),
		FB_UNKNOWN( "B-UNKNOWN" ),
		FC_UNKNOWN( "C-UNKNOWN" ),
		FD_UNKNOWN( "D-UNKNOWN" ),
		FE_UNKNOWN( "E-UNKNOWN" ),
		FF_TRACK_PULSE( "F-TRACK PULSE" ),
		
		UNKNOWN( "UNKNOWN" );
		
		private String mLabel;
		
		private Function( String label )
		{
			mLabel = label;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return getLabel();
		}
		
		public static Function fromValue( int value, int replyCodeDigit3 )
		{
			switch( value )
			{
				case 1:
					if( replyCodeDigit3 == 31 ) /* 'Y' middle character */
					{
						return Function.F1_SITE_ID;
					}
					else
					{
						return Function.F1_SPEED_UP;
					}
				case 2:
					return Function.F2_TEST;
				case 3:
					return Function.F3_DEACTIVATE;
				case 4:
					return Function.F4_ACTIVATE;
				case 5:
					return Function.F5_UNKNOWN;
				case 6:
					return Function.F6_UNKNOWN;
				case 7:
					return Function.F7_UNKNOWN;
				case 8:
					return Function.F8_UNKNOWN;
				case 9:
					return Function.F9_UNKNOWN;
				case 10:
					return Function.FA_UNKNOWN;
				case 11:
					return Function.FB_UNKNOWN;
				case 12:
					return Function.FC_UNKNOWN;
				case 13:
					return Function.FD_UNKNOWN;
				case 14:
					return Function.FE_UNKNOWN;
				case 15:
					return Function.FF_TRACK_PULSE;
				default:
					return Function.UNKNOWN;
			}
		}
	}
	
	/**
	 * Provides a listing of aliases contained in the message.  
	 */
	public List<Alias> getAliases()
	{
		List<Alias> aliases = new ArrayList<Alias>();
		
		Alias siteAndReply = getSiteAndReplyCodeAlias();
		
		if( siteAndReply != null )
		{
			aliases.add( siteAndReply );
		}

		Alias address = getAddressAlias();
		
		if( address != null )
		{
			aliases.add( address );
		}
		
		return aliases;
	}
}
