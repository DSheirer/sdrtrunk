/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package module.decode.ltrstandard.message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import message.Message;
import message.MessageDirection;
import message.MessageType;
import module.decode.DecoderType;

import org.apache.commons.lang3.StringUtils;

import alias.Alias;
import alias.AliasList;
import bits.BinaryMessage;
import edac.CRC;

public abstract class LTRStandardMessage extends Message
{
	protected SimpleDateFormat mDatestampFormatter = 
			new SimpleDateFormat( "yyyyMMdd HHmmss" );
	
    public static final int[] SYNC = { 0,1,2,3,4,5,6,7,8 };
	public static final int[] AREA = { 9 };
	public static final int[] CHANNEL = { 10,11,12,13,14 };
	public static final int[] HOME_REPEATER = { 15,16,17,18,19 };
	public static final int[] GROUP = { 20,21,22,23,24,25,26,27 };
	public static final int[] FREE = { 28,29,30,31,32 };
	public static final int[] CHECKSUM = { 33,34,35,36,37,38,39 };
	
	protected BinaryMessage mMessage;
	protected MessageDirection mMessageDirection;
	protected AliasList mAliasList;
	protected CRC mCRC;
	
    public LTRStandardMessage( BinaryMessage message, 
    						   MessageDirection direction,
    						   AliasList list,
    						   CRC crc )
    {
        mMessage = message;
        mMessageDirection = direction;
        mAliasList = list;
        mCRC = crc;
    }
    
	public abstract MessageType getMessageType();

	public boolean isValid()
    {
    	return mCRC != CRC.FAILED_CRC && mCRC != CRC.FAILED_PARITY;
    }
    
	@Override
    public String toString()
    {
		StringBuilder sb = new StringBuilder();
    	sb.append( mDatestampFormatter.format( 
    			new Date( System.currentTimeMillis() ) ) );
		sb.append( " LTR " );
		sb.append( mMessageDirection.name() );
		sb.append( " [" );
		sb.append( mCRC.getAbbreviation() );
		sb.append( "] " );
		sb.append( getMessage() );
		
	    return sb.toString();
    }
	
	@Override
    public String getBinaryMessage()
    {
		return mMessage.toString();
    }

    public CRC getCRC()
    {
    	return mCRC;
    }
    
	public int getArea()
	{
		return mMessage.getInt( AREA );
	}

	public int getChannel()
	{
		return mMessage.getInt( CHANNEL );
	}

	public String getChannelFormatted()
	{
		return StringUtils.leftPad( String.valueOf( getChannel() ), 2, "0" );
	}
	
	public int getHomeRepeater()
	{
		return mMessage.getInt( HOME_REPEATER );
	}
	
	public String getHomeRepeaterFormatted()
	{
		return StringUtils.leftPad( String.valueOf( getHomeRepeater() ), 2, "0" );
	}
	
	public int getGroup()
	{
		return mMessage.getInt( GROUP );
	}

	public String getGroupFormatted()
	{
		return StringUtils.leftPad( String.valueOf( getGroup() ), 3, "0" );
	}
	
	public int getFree()
	{
		return mMessage.getInt( FREE );
	}
	
	public String getFreeFormatted()
	{
		return StringUtils.leftPad( String.valueOf( getFree() ), 2, "0" );
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
		StringBuilder sb = new StringBuilder();
		
		sb.append( getArea() );
		sb.append( "-" );
		sb.append( getHomeRepeaterFormatted() );
		sb.append( "-" );
		sb.append( getGroupFormatted() );
		
		return sb.toString();
    }

	@Override
    public Alias getToIDAlias()
    {
		return mAliasList.getTalkgroupAlias( getToID() );
    }

	public List<Alias> getAliases()
	{
		List<Alias> aliases = new ArrayList<>();
		
		Alias talkgroupAlias = getToIDAlias();
		
		if( talkgroupAlias != null )
		{
			aliases.add( talkgroupAlias );
		}
		
		return aliases;
	}
	
	public int getCRCChecksum()
	{
		return mMessage.getInt( CHECKSUM );
	}
	
	@Override
    public String getProtocol()
    {
	    return DecoderType.LTR_STANDARD.getDisplayString();
    }

	@Override
    public String getEventType()
    {
	    return getMessageType().getDisplayText();
    }

	@Override
    public String getErrorStatus()
    {
	    return mCRC.getDisplayText();
    }
}