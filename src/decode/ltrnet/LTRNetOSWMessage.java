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

import java.util.ArrayList;
import java.util.Date;

import map.Plottable;
import message.MessageDirection;
import message.MessageType;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;

public class LTRNetOSWMessage extends LTRNetMessage
{
	private static final int sCHANNEL_FREQUENCY_MESSAGE_TYPE_BIT = 20;
	private static final int sCHANNEL_MAP_MESSAGE_TYPE_BIT = 17;

	private static final int sCHIU_UNIQUE_ID = 17;
	private static final int sCHIU_SITE_ID = 18;
	private static final int sCHIU_TRANSMIT_FREQUENCY = 24;
	private static final int sCHIU_RECEIVE_FREQUENCY = 25;
	private static final int sCHIU_NEIGHBOR = 26;
	private static final int sCHIU_MAP = 28;
	private static final int sCHIU_CALL_END = 31;

	private static final int sHOME_DIRECTED_GROUP_CALL = 29;
	private static final int sHOME_CHANNEL_IN_USE_MUTE_AUDIO = 30;
	
	private static final int sGROUP_DO_NOTHING = 253;
	private static final int sGROUP_CWID = 254;
	private static final int sGROUP_CHANNEL_IDLE = 255;

	private static final int sFREE_ALL_CHANNELS_BUSY = 0;
	private static final int sFREE_LTR_GROUP_CALL = 30;
	private static final int sFREE_LTRNET_GROUP_CALL = 31;
	
	private LTRNetOSWMessage mAuxMessage;
	
    public LTRNetOSWMessage( BitSetBuffer message, AliasList list )
    {
    	super( message, MessageDirection.OSW, list );
        mMessageType = getType();
    }
    
    public void setAuxiliaryMessage( LTRNetOSWMessage message )
    {
    	if( message != null )
    	{
        	mAuxMessage = message;
    	}
    }

    public String getRadioUniqueIDText()
    {
		StringBuilder sb = new StringBuilder();

		int radioId = getRadioUniqueID();
		
		if( radioId != sINT_NULL_VALUE )
		{
			sb.append( " RADIO UNIQUE ID: " );
			sb.append( radioId );
		}

		return sb.toString();
    }
    
	@Override
    public String toString()
    {
		StringBuilder sb = new StringBuilder();
    	sb.append( mDatestampFormatter.format( 
    			new Date( System.currentTimeMillis() ) ) );
		sb.append( " LTRNet OSW [" );
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
			case MA_CHNL:
				sb.append( "CHAN MAP LOW LCN [" );
				sb.append( getChannelMapAsString( getChannelMapLow() ) );
				sb.append( "]" );
				break;
			case MA_CHNH:
				sb.append( "CHAN MAP HIGH LCN [" );
				sb.append( getChannelMapAsString( getChannelMapHigh() ) );
				sb.append( "]" );
				break;
			case FQ_RXLO:
				sb.append( "CHAN RX LOW  LCN:" );
				sb.append( format( getHomeRepeater(), 2 ) );
				sb.append( " = " );
				double rxfreqhi = getFrequency();
				if( rxfreqhi != sDOUBLE_NULL_VALUE )
				{
					sb.append( mDecimalFormatter.format( rxfreqhi ) );
				}
				else
				{
					sb.append( "000.00000" );
				}
				sb.append( " MHz" );
				break;
			case FQ_RXHI:
				sb.append( "CHAN RX HIGH LCN:" );
				sb.append( format( getHomeRepeater(), 2 ) );
				sb.append( " MSG 1 OF 2" );
				break;
			case FQ_TXHI:
				sb.append( "CHAN TX LOW  LCN:" );
				sb.append( format( getHomeRepeater(), 2 ) );
				sb.append( " = " );
				double txfreq = getFrequency();
				if( txfreq != sDOUBLE_NULL_VALUE )
				{
					sb.append( mDecimalFormatter.format( txfreq ) );
				}
				else
				{
					sb.append( "000.00000" );
				}
				sb.append( " MHz" );
				break;
			case FQ_TXLO:
				sb.append( "CHAN TX HIGH LCN:" );
				sb.append( format( getHomeRepeater(), 2 ) );
				sb.append( " MSG 1 OF 2" );
				break;
			case ID_SITE:
				int siteId = getSiteID();
				if( siteId != sINT_NULL_VALUE )
				{
					sb.append( "SITE ID: " );
					sb.append( siteId );
					sb.append( " " );
					sb.append( getSiteIDAlias() );
				}
				break;
			case ID_NBOR:
				int neighborRank = getNeighborRank();
				int neighborId = getNeighborID();
				
				if( neighborRank != sINT_NULL_VALUE && 
					neighborId != sINT_NULL_VALUE )
				{
					sb.append( "NEIGHBOR SITE " );
					sb.append( neighborRank );
					sb.append( ": " );
					sb.append( neighborId );
					sb.append( " " );
					sb.append( getNeighborIDAlias() );
				}
				break;
			case ID_UNIQ:
				sb.append( "REGISTER RADIO UNIQUE ID:" );
				sb.append( getRadioUniqueID() );
				break;
			default:
				sb.append( "UNKNOWN " );
				sb.append( getArea() );
				sb.append( " " + format( getChannel(), 2 ) );
				sb.append( " " + format( getHomeRepeater(), 2 ) );
				sb.append( " " + format( getGroup(), 3 ) );
				sb.append( " " + format( getFree(), 2 ) );
				sb.append( "  A:" );
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
		if( mAliasList != null )
		{
			return mAliasList.getTalkgroupAlias( getTalkgroupID() );
		}
		
		return null;
	}
	
	public ArrayList<Integer> getChannelMapLow()
	{
		ArrayList<Integer> retVal = new ArrayList<Integer>();
		
		if( mMessageType == MessageType.MA_CHNL )
		{
			for( int x = 27; x >= 18; x-- )
			{
				if( mMessage.get( x ) )
				{
					retVal.add( 28 - x );
				}
			}
		}

		return retVal;
	}
	
	public ArrayList<Integer> getChannelMapHigh()
	{
		ArrayList<Integer> retVal = new ArrayList<Integer>();
		
		if( mMessageType == MessageType.MA_CHNH )
		{
			for( int x = 27; x >= 18; x-- )
			{
				if( mMessage.get( x ) )
				{
					retVal.add( 38 - x );
				}
			}
		}

		return retVal;
	}
	
	public String getChannelMapAsString( ArrayList<Integer> channels )
	{
		StringBuilder sb = new StringBuilder();
		
		for( Integer channel: channels )
		{
			sb.append( channel );
			sb.append( " " );
		}
		
		return sb.toString();
	}
	
	public int getRadioUniqueID()
	{
		int retVal = sINT_NULL_VALUE;
		
		if( mMessageType == MessageType.ID_UNIQ )
		{
			retVal = mMessage.getInt( 17, 32 );
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
	
	public int getSiteID()
	{
		int retVal = sINT_NULL_VALUE;
		
		if( mMessageType == MessageType.ID_SITE )
		{
			retVal = mMessage.getInt( 23, 32 );
		}
		
		return retVal;
	}
	
	public Alias getSiteIDAlias()
	{
		if( mAliasList != null )
		{
			return mAliasList.getSiteID( getSiteID() );
		}
		
		return null;
	}
	
	public int getNeighborID()
	{
		int retVal = sINT_NULL_VALUE;
		
		if( mMessageType == MessageType.ID_NBOR )
		{
			retVal = mMessage.getInt( 23, 32 );
		}
		
		return retVal;
	}
	
	private Alias getNeighborIDAlias()
	{
		if( mAliasList != null )
		{
			return mAliasList.getSiteID( getNeighborID() );
		}

		return null;
	}
	
	public int getNeighborRank()
	{
		int retVal = sINT_NULL_VALUE;
		
		if( mMessageType == MessageType.ID_NBOR )
		{
			retVal = mMessage.getInt( 15, 18 ) + 1;
		}
		
		return retVal;
	}
	
	public double getFrequency()
	{
		double retVal = sDOUBLE_NULL_VALUE;

		if( mAuxMessage != null )
		{
			int high = getHighChannelUnits();
			int low = mAuxMessage.getLowChannelUnits();
			
			if( high != sINT_NULL_VALUE && low != sINT_NULL_VALUE )
			{
				retVal = ( high + low ) * .00125 + 150.0;
			}
		}
		
		return retVal;
	}
	
	/**
	 * Returns the value of the high order (15 - 12) frequency bits which are
	 * the lower 4 bits of the Free field, left shifted by 12, representing the
	 * frequency value in units of .00125 MHz
	 */
	public int getHighChannelUnits()
	{
		int retVal = sINT_NULL_VALUE;
		
		if( mAuxMessage != null && 
				( mMessageType == MessageType.FQ_RXHI ||
				  mMessageType == MessageType.FQ_TXHI ) )
		{
			retVal = mMessage.getInt( 29, 32 );

			retVal = Integer.rotateLeft( retVal, 12 );
		}
		
		return retVal;
	}
	
	/**
	 * Returns the value of the low order (11 - 0) frequency bits which are
	 * contained in the Group and Free fields representing the frequency value 
	 * units of .00125 MHz
	 */
	public int getLowChannelUnits()
	{
		int retVal = sINT_NULL_VALUE;
		
		if( mMessageType == MessageType.FQ_RXLO ||
			mMessageType == MessageType.FQ_TXLO )
		{
			retVal = mMessage.getInt( 21, 32 );
		}
		
		return retVal;
	}
	
	public MessageType getType()
	{
		MessageType retVal = MessageType.UN_KNWN; 
		
		int channel = getChannel();
		
		int home = getHomeRepeater();

		//LTR-Net messages
		if( home != 31 && ( channel > 20 || home > 20 ) )
		{
			switch( channel )
			{
				case sCHIU_UNIQUE_ID:
					retVal = MessageType.ID_UNIQ;
					break;
				case sCHIU_SITE_ID:
					retVal = MessageType.ID_SITE;
					break;
				case sCHIU_TRANSMIT_FREQUENCY:
					if( getHomeRepeater() == 24 )
					{
						retVal = MessageType.ID_UNIQ;
					}
					else
					{
						if( mMessage.get( sCHANNEL_FREQUENCY_MESSAGE_TYPE_BIT ) )
						{
							retVal = MessageType.FQ_TXHI;
						}
						else
						{
							retVal = MessageType.FQ_TXLO;
						}
					}
					break;
				case sCHIU_RECEIVE_FREQUENCY:
					if( mMessage.get( sCHANNEL_FREQUENCY_MESSAGE_TYPE_BIT ) )
					{
						retVal = MessageType.FQ_RXHI;
					}
					else
					{
						retVal = MessageType.FQ_RXLO;
					}
					break;
				case sCHIU_NEIGHBOR:
					retVal = MessageType.ID_NBOR;
					break;
				case sCHIU_MAP:
					if( mMessage.get( sCHANNEL_MAP_MESSAGE_TYPE_BIT ) )
					{
						retVal = MessageType.MA_CHNH;
					}
					else
					{
						retVal = MessageType.MA_CHNL;
					}
					break;
				case sCHIU_CALL_END:
					retVal = MessageType.CA_ENDD;
			}
		}
		else
		{
			int group = getGroup();

			if( group == sGROUP_CHANNEL_IDLE )
			{
				retVal = MessageType.SY_IDLE;
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
}
