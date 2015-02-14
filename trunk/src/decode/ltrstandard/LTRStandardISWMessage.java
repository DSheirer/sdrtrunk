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
package decode.ltrstandard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import map.Plottable;
import message.MessageDirection;
import message.MessageType;
import alias.Alias;
import alias.AliasList;
import bits.BinaryMessage;

public class LTRStandardISWMessage extends LTRStandardMessage
{
	private static final int sCALL_END = 31;
	private static final int sIDLE = 255;

	public LTRStandardISWMessage( BinaryMessage message, AliasList list )
    {
    	super( message, MessageDirection.OSW, list );
        mMessageType = getType();
    }

	@Override
    public String toString()
    {
		StringBuilder sb = new StringBuilder();
    	sb.append( mDatestampFormatter.format( 
    			new Date( System.currentTimeMillis() ) ) );
		sb.append( " LTR OSW [" );
		sb.append( mCRC.getAbbreviation() );
		sb.append( "] " );
		sb.append( getMessage() );
		pad(  sb, 100 );
		sb.append( mMessage.toString() );
		
	    return sb.toString();
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		switch( mMessageType )
		{
			case SY_IDLE:
				sb.append( "IDLE AREA:" );
				sb.append( getArea() );
				sb.append( " LCN:" );
				sb.append( format( getChannel(), 2 ) );
				sb.append( " HOME:" );
				sb.append( format( getHomeRepeater(), 2 ) );
				sb.append( " GRP:" );
				sb.append( format( getGroup(), 3 ) );
				sb.append( " FREE:" );
				sb.append( format( getFree(), 2 ) );
				break;
			case CA_STRT:
				sb.append( "CALL LCN:" );
				sb.append( format( getChannel(), 2 ) );
				sb.append( " TG [ " );
				sb.append( getTalkgroupID() );
				sb.append( "/" );
				sb.append( getTalkgroupIDAlias() );
				sb.append( " ] FREE:" );
				sb.append( format( getFree(), 2 ) );
				break;
			case CA_ENDD:
				sb.append( "END* AREA:" );
				sb.append( getArea() );
				sb.append( " LCN:" );
				sb.append( format( getChannel(), 2 ) );
				sb.append( " TG [" );
				sb.append( getTalkgroupID() );
				sb.append( "/" );
				sb.append( getTalkgroupIDAlias() );
				sb.append( "] FREE:" );
				sb.append( format( getFree(), 2 ) );
				break;
			default:
				sb.append( "UNKNOWN A:" );
				sb.append( getArea() );
				sb.append( " C:" );
				sb.append( format( getChannel(), 2 ) );
				sb.append( " H:" );
				sb.append( format( getHomeRepeater(), 2 ) );
				sb.append( " G:" );
				sb.append( format( getGroup(), 3 ) );
				sb.append( " F:" );
				sb.append( format( getFree(), 2 ) );
		}

	    return sb.toString();
    }
	
	public String getTalkgroupID()
	{
		return getTalkgroupID( true );
	}
	
	public String getTalkgroupID( boolean format )
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getArea() );
		
		if( format )
		{
			sb.append( "-" );
		}

		sb.append( format( getHomeRepeater(), 2 ) );
		
		if( format )
		{
			sb.append( "-" );
		}

		sb.append( format( getGroup(), 3 ) );
		
		return sb.toString();
	}
	
	public Alias getTalkgroupIDAlias()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( getArea() );
		sb.append( format( getHomeRepeater(), 2 ) );
		sb.append( format( getGroup(), 3 ) );

		return mAliasList.getTalkgroupAlias( sb.toString() );
	}
	
	public MessageType getType()
	{
		MessageType retVal = MessageType.UN_KNWN; 
		
		int group = getGroup();

		if( group == sIDLE )
		{
			retVal = MessageType.SY_IDLE;
		}
		else
		{
			int channel = getChannel();

			if( channel == sCALL_END )
			{
				retVal = MessageType.CA_ENDD;
			}
			else
			{
				retVal = MessageType.CA_STRT;
			}
		}
		
		return retVal;
	}
	
	@Override
    public String getFromID()
    {
		if( mMessageType == MessageType.CA_STRT || 
			mMessageType == MessageType.CA_ENDD )
		{
		    return getTalkgroupID();
		}
		else
		{
			return null;
		}
    }

	@Override
    public Alias getFromIDAlias()
    {
		if( mMessageType == MessageType.CA_STRT || 
			mMessageType == MessageType.CA_ENDD )
		{
		    return getTalkgroupIDAlias();
		}
		else
		{
			return null;
		}
    }

	@Override
    public Plottable getPlottable()
    {
	    // TODO Auto-generated method stub
	    return null;
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
		
		return aliases;
	}
}
