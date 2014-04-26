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
package decode.ltrnet;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import message.Message;
import message.MessageDirection;
import message.MessageType;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import crc.CRCLTR;
import decode.DecoderType;

public abstract class LTRNetMessage extends Message
{
	protected static final String sUNKNOWN = "**UNKNOWN**";
	protected static final int sINT_NULL_VALUE = -1;
	protected static final double sDOUBLE_NULL_VALUE = -1.0D;
	protected SimpleDateFormat mDatestampFormatter = 
			new SimpleDateFormat( "yyyyMMdd HHmmss" );
	protected DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");
	
    protected static final int[] sSYNC = { 8,7,6,5,4,3,2,1,0 };
	protected static final int[] sAREA = { 9 };
	protected static final int[] sCHANNEL = { 14,13,12,11,10 };
	protected static final int[] sHOME_REPEATER = { 19,18,17,16,15 };
	protected static final int[] sGROUP = { 27,26,25,24,23,22,21,20 };
	protected static final int[] sFREE = { 32,31,30,29,28 };
	protected static final int[] sCRC = { 39,38,37,36,35,34,33 };
	protected static final int[] sSIXTEEN_BITS = { 32,31,30,29,28,27,26,25,24,23,22,21,20,19,18,17 };
	protected static final int[] sMANUFACTURER_ID = { 23,22,21,20,19,18,17 };
	
	protected BitSetBuffer mMessage;
	protected MessageType mMessageType;
	protected CRC mCRC;
	protected AliasList mAliasList;
	
    public LTRNetMessage( BitSetBuffer message, 
    					  MessageDirection direction,
    					  AliasList list )
    {
        mMessage = message;
        mCRC = CRCLTR.check( message, direction );
        mAliasList = list;
    }
    
    public MessageType getMessageType()
    {
    	return mMessageType;
    }
    
    public abstract void setAuxiliaryMessage( LTRNetOSWMessage message );
    
    public boolean isValid()
    {
    	return mCRC != CRC.FAILED_CRC && mCRC != CRC.FAILED_PARITY;
    }
    
	/**
	 * Appends spaces to the end of the stringbuilder to make it length long
	 */
	protected void pad( StringBuilder sb, int length )
	{
		while( sb.length() < length )
		{
			sb.append( " " );
		}
	}

	@Override
    public String getBinaryMessage()
    {
		return mMessage.toString();
    }

	/**
	 * Returns a value formatted in hex notation.
	 * 
	 * @param value - integer value
	 * @return - hex digits returned
	 */
	protected static String getHex( int value, int digits )
	{
		return String.format( "%0" + digits + "X", value );
	}
	
    protected int getInt( int[] bits )
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
    
    public String format( String val, int places )
    {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( val );

    	while( sb.length() < places )
    	{
    		sb.append( " " );
    	}
    	
    	return sb.toString();
    }

    public CRC getCRC()
    {
    	return mCRC;
    }
    
	public int getArea()
	{
		return getInt( sAREA );
	}

	public int getChannel()
	{
		return getInt( sCHANNEL );
	}
	
	public int getHomeRepeater()
	{
		return getInt( sHOME_REPEATER );
	}
	
	public int getGroup()
	{
		return getInt( sGROUP );
	}

	public String getTalkgroupID()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getArea() );
		sb.append( "-" );
		sb.append( format( getHomeRepeater(), 2 ) );
		sb.append( "-" );
		sb.append( format( getGroup(), 3 ) );
		
		return sb.toString();
	}
	
	public Alias getTalkgroupIDAlias()
	{
		return mAliasList.getTalkgroupAlias( getTalkgroupID() );
	}
	
	public int getFree()
	{
		return getInt( sFREE );
	}
	
	public int getCRCChecksum()
	{
		return getInt( sCRC );
	}
	
	@Override
    public String getProtocol()
    {
	    return DecoderType.LTR_NET.getDisplayString();
    }

	@Override
    public String getEventType()
    {
		if( mMessageType != null )
		{
		    return mMessageType.getDisplayText();
		}
		else
		{
			return MessageType.UN_KNWN.getDisplayText();
		}
    }

	@Override
    public String getToID()
    {
	    return null;
    }

	@Override
    public Alias getToIDAlias()
    {
	    return null;
    }

	@Override
    public String getErrorStatus()
    {
	    return mCRC.getDisplayText();
    }
}
