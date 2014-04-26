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

import java.util.Date;

import map.Plottable;
import message.MessageDirection;
import message.MessageType;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import crc.CRCLTR;

public class LTRNetISWMessage extends LTRNetMessage
{
	private static final int sCHIU_UNIQUE_ID = 24;
	private static final int sCHIU_ESN_LOW = 27;
	private static final int sCHIU_ESN_HIGH = 29;
	
	private static final int sFREE_CALL = 21;
	private static final int sFREE_END_CALL = 23;
	private static final int sFREE_REQUEST_ACCESS = 31;
	

	private LTRNetISWMessage mAuxMessage;
	
    public LTRNetISWMessage( BitSetBuffer message, AliasList list )
    {
    	super( message, MessageDirection.ISW, list );
    	
    	/**
    	 * If the CRC fails, test for a transmitted CRC of 127 and then check
    	 * the Free field for special messages
    	 */
    	if( mCRC == CRC.FAILED_CRC && CRCLTR.getTransmittedChecksum( mMessage ) == 127 )
    	{
    		if( getFree() == sFREE_REQUEST_ACCESS ||
    			getFree() == sFREE_END_CALL )
    		{
    			mCRC = CRC.PASSED;
    		}
    	}
    	
        mMessageType = getMessageType();
    }
    
    public void setAuxiliaryMessage( LTRNetISWMessage message )
    {
    	if( message != null )
    	{
        	mAuxMessage = message;
    	}
    }
    
    public String getESN()
    {
    	if( mMessageType == MessageType.ID_ESNH )
    	{
    		if( mAuxMessage != null && 
        			mAuxMessage.getMessageType() == MessageType.ID_ESNL )
    		{
    			return getESNHigh() + mAuxMessage.getESNLow();
    		}
    		else
    		{
        		return getESNHigh() + "xxxx";
    		}
    	}
    	else
    	{
    		if( mAuxMessage != null && 
    			mAuxMessage.getMessageType() == MessageType.ID_ESNH )
    		{
    			return mAuxMessage.getESNHigh() + getESNLow();
    		}
    		else
    		{
    			return "xxxx" + getESNLow();
    		}
    	}
    }
    
    public Alias getESNAlias()
    {
    	if( mAliasList != null )
    	{
    		return mAliasList.getESNAlias( getESN() );
    	}

		return null;
    }

    public String getESNHigh()
    {
		int esnHigh = getInt( sSIXTEEN_BITS );
		
		return String.format("%04X", esnHigh & 0xFFFF );
    }

    public String getESNLow()
    {
		int esnLow = getInt( sSIXTEEN_BITS );
		
		return String.format("%04X", esnLow & 0xFFFF );
    }
    
	@Override
    public String toString()
    {
		StringBuilder sb = new StringBuilder();
    	sb.append( mDatestampFormatter.format( new Date( System.currentTimeMillis() ) ) );
		sb.append( " LTRNet ISW [" );
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
			case RQ_ACCE:
				sb.append( "REQUEST ACCESS LCN:" );
				sb.append( format( getChannel(), 2 ) );
				sb.append( " TG [ " );
				sb.append( getTalkgroupID() );
				sb.append( "/" );
				sb.append( getTalkgroupIDAlias() );
				sb.append( " ] FREE:" );
				sb.append( format( getFree(), 2 ) );
				break;
			case ID_ESNL:
				sb.append( "ESN LOW  " );
				sb.append( getESN() );
				sb.append( "/" );
				sb.append( getESNAlias() );
				break;
			case ID_ESNH:
				sb.append( "ESN HIGH " );
				sb.append( getESN() );
				sb.append( "/" );
				sb.append( getESNAlias() );
				break;
			case ID_UNIQ:
				sb.append( "RADIO UNIQUE ID " );
				sb.append( getRadioUniqueID() );
				
				if( getRadioUniqueIDAlias() != null )
				{
					sb.append( "/" );
					sb.append( getRadioUniqueIDAlias().getName() );
				}
				break;
			default:
				sb.append( "UNKNOWN " );
				sb.append( getArea() );
				sb.append( " " );
				sb.append( format( getChannel(), 2 ) );
				sb.append( " " );
				sb.append( format( getHomeRepeater(), 2 ) );
				sb.append( " " );
				sb.append( format( getGroup(), 3 ) );
				sb.append( " " );
				sb.append( format( getFree(), 2 ) );
				sb.append( " CRC:" );
				sb.append( CRCLTR.getTransmittedChecksum( mMessage ) );
				sb.append( " CALC:" );
				sb.append( CRCLTR.getCalculatedChecksum( mMessage ) );
		}

	    return sb.toString();
    }
	
	public int getRadioUniqueID()
	{
		int retVal = sINT_NULL_VALUE;
		
		if( mMessageType == MessageType.ID_UNIQ )
		{
			retVal = getInt( sSIXTEEN_BITS );
		}
		
		return retVal;
	}
	
	public Alias getRadioUniqueIDAlias()
	{
		if( mAliasList != null )
		{
			return mAliasList.getUniqueID( getRadioUniqueID() );
		}

		return null;
	}
	
	public MessageType getMessageType()
	{
		MessageType retVal = MessageType.UN_KNWN; 
		
		int channel = getChannel();
		
		if( channel == 31 )
		{
			retVal = MessageType.CA_ENDD;
		}
		else if( channel > 20 )
		{
			switch( channel )
			{
				case sCHIU_UNIQUE_ID:
					retVal = MessageType.ID_UNIQ;
					break;
				case sCHIU_ESN_LOW:
					retVal = MessageType.ID_ESNH;
					break;
				case sCHIU_ESN_HIGH:
					retVal = MessageType.ID_ESNL;
					break;
			}
		}
		else if( channel > 0 )
		{
			int free = getFree();

			switch( free )
			{
				case sFREE_CALL:
					retVal = MessageType.CA_STRT;
					break;
				case sFREE_END_CALL:
					retVal = MessageType.CA_ENDD;
					break;
				case sFREE_REQUEST_ACCESS:
					retVal = MessageType.RQ_ACCE;
					break;
			}
		}
		
		return retVal;
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
    public void setAuxiliaryMessage( LTRNetOSWMessage message )
    {
	    // TODO Auto-generated method stub
	    
    }

	@Override
    public Plottable getPlottable()
    {
	    // TODO Auto-generated method stub
	    return null;
    }
}
