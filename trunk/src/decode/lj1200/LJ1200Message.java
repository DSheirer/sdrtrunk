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

	public static int[] VRC = { 16,17,18,19,20,21,22,23 };
	
	public static int[] LRC = { 24,25,26,27,28,29,30,31 };

	public static int[] FUNCTION = { 32,33,34,35 };

	/* Message 8 Site ID */
	public static int[] SITE_ID_PREFIX = { 36,37,38,39,40,41,42,43,44,45,46,47 };
	public static int[] NETWORK_ID = { 48,49,50,51,52,53,54,55 };
	public static int[] SITE_ID = { 56,57,58,59,60,61,62,63 };
	
	public static int[] ADDRESS = { 36,37,38,39,40,41,42,43,44,45,46,47,48,49,
		50,51,52,53,54,55,56,57,58,59,60,61,62,63 };

	public static int[] REPLY_1 = { 39,38,37,36,43 };
	public static int[] REPLY_2 = { 42,41,40,47,46 };
	public static int[] REPLY_3 = { 45,44,51,50,49 };
	public static int[] REPLY_4 = { 48,55,54,53,52 };
	public static int[] REPLY_5 = { 59,58,57,56,63 };

	public static int[] MESSAGE_CRC = { 64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79 };
	
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
    	return Function.fromValue( mMessage.getInt( FUNCTION ) );
    }
    
    public String getAddress()
    {
    	return mMessage.getHex( ADDRESS, 7 );
    }
    
    public String getNetwork()
    {
    	return mMessage.getHex( NETWORK_ID, 2 );
    }
    
    public String getSite()
    {
    	return mMessage.getHex( SITE_ID, 2 );
    }
    
    public String getSiteID()
    {
    	return getNetwork() + "-" + getSite();
    }
    
    public Alias getSiteIDAlias()
    {
    	if( mAliasList != null && getFunction() == Function.F8_SITE_ID )
    	{
    		return mAliasList.getSiteID( getSiteID() );
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
	    	case FE_TRACK_PULSE:
	    	case FF_TRACK_PULSE:
	    		sb.append( " REPLY CODE [" );
	    		sb.append( getReplyCode() );
	    		break;
	    	case F8_SITE_ID:
	    		sb.append( " SITE [" );
	    		sb.append( getSiteID() );

	    		Alias site = getSiteIDAlias();
	    		
	    		if( site != null )
	    		{
	    			sb.append( "/" );
	    			sb.append( site.getName() );
	    		}
	    		
	    		sb.append( "]" );
	        	sb.append( " ADDRESS [" );
	        	sb.append( getAddress() );
	    		break;
    		default:
    	    	sb.append( " ADDRESS [" );
    	    	sb.append( getAddress() );
    	    	break;
    	}
    	
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
	    return "TOWER";
    }

	@Override
    public String getFromID()
    {
		return null;
    }

	@Override
    public Alias getFromIDAlias()
    {
		return null;
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
		F0(             0x0, "0-UNKNOWN" ),
		F1(             0x1, "1-UNKNOWN" ),
		F2_ACTIVATION(  0x2, "2-ACTIVATION" ),
		F3_SPEED_UP(    0x3, "3-SPEED-UP" ),
		F4_TEST(        0x4, "4-TEST" ),
		F5(             0x5, "5-UNKNOWN" ),
		F6_SET_RATE(    0x6, "6-UNKNOWN" ), //Delete
		F7(             0x7, "7-UNKNOWN" ),
		F8_SITE_ID(     0x8, "8-SITE ID" ),
		F9(             0x9, "9-UNKNOWN" ),
		FA(             0xA, "A-UNKNOWN" ),
		FB(             0xB, "B-UNKNOWN" ),
		FC_DEACTIVATE(  0xC, "C-DEACTIVATE" ),
		FD(             0xD, "D-UNKNOWN" ),
		FE_TRACK_PULSE( 0xE, "E-UNKNOWN" ),
		FF_TRACK_PULSE( 0xF, "F-TRACK PULSE" ),
		UNKNOWN(         -1, "UNKNOWN" );
		
		private int mValue;
		private String mLabel;
		
		private Function( int value, String label )
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
		
		public String toString()
		{
			return getLabel();
		}
		
		public static Function fromValue( int value )
		{
			if( 0 <= value && value <= 15 )
			{
				return Function.values()[ value ];
			}
			
			return UNKNOWN;
		}
	}
	
	/**
	 * Provides a listing of aliases contained in the message.  
	 */
	public List<Alias> getAliases()
	{
		List<Alias> aliases = new ArrayList<Alias>();
		
		Alias from = getFromIDAlias();
		
		if( from != null )
		{
			aliases.add( from );
		}

		Alias to = getToIDAlias();
		
		if( to != null )
		{
			aliases.add( to );
		}
		
		Alias site = getSiteIDAlias();
		
		if( site != null )
		{
			aliases.add( site );
		}

		return aliases;
	}
}
