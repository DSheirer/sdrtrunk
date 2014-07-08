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
package decode.passport;


import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import map.Plottable;
import message.Message;
import message.MessageType;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import crc.CRCPassport;
import decode.DecoderType;

public class PassportMessage extends Message
{
	private static final String sUNKNOWN = "**UNKNOWN**";
	private SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );
	private DecimalFormat mDecimalFormatter = new DecimalFormat("0.00000");
	
	@SuppressWarnings( "unused" )
    private static final int[] sSYNC = { 8,7,6,5,4,3,2,1,0 };
	private static final int[] sDCC = { 10,9 }; //Digital Color Code
	private static final int[] sLCN = { 21,20,19,18,17,16,15,14,13,12,11 };
	private static final int[] sSITE = { 28,27,26,25,24,23,22 };
	private static final int[] sGROUP = { 44,43,42,41,40,39,38,37,36,35,34,33,32,31,30,29 };
	private static final int[] sRADIO_ID = { 44,43,42,41,40,39,38,37,36,35,34,33,32,31,30,29,28,27,26,25,24,23,22 };
//    private static final int[] sNEIGHBOR_OFFSET = { 32,31,30 };
    private static final int[] sNEIGHBOR_BAND = { 36,35,34,33 };
    private static final int[] sNEIGHBOR_REGISTRATION = { 29 };
    private static final int[] sSITE_OFFSET = { 40,39,38 };
    private static final int[] sSITE_BAND = { 44,43,42,41 };
    private static final int[] sSITE_REGISTRATION = { 37 };
	private static final int[] sTYPE = { 48,47,46,45 };
	private static final int[] sFREE = { 59,58,57,56,55,54,53,52,51,50,49 };
	private static final int[] sCHECKSUM = { 67,66,65,64,63,62,61,60 };
    private BitSetBuffer mMessage;
    private CRC mCRC;
	private AliasList mAliasList;
	private PassportMessage mIdleMessage;
	
    public PassportMessage( BitSetBuffer message, 
    						PassportMessage idleMessage,
    						AliasList list )
    {
        mMessage = CRCPassport.correct( message );
        mIdleMessage = idleMessage;
        mCRC = CRCPassport.check( mMessage );
        mAliasList = list;
    }
    
    public PassportMessage( BitSetBuffer message, AliasList list )
    {
    	this( message, null, list );
    }
    
    public BitSetBuffer getBitSetBuffer()
    {
    	return mMessage;
    }
    
    public boolean isValid()
    {
    	return mCRC != CRC.FAILED_CRC &&
    		   mCRC != CRC.FAILED_PARITY &&
    		   mCRC != CRC.UNKNOWN;
    }
    
    public CRC getCRC()
    {
    	return mCRC;
    }

    public MessageType getMessageType()
    {
        MessageType retVal = MessageType.UN_KNWN;
        
        int type = getMessageTypeNumber();
        int lcn = getLCN();
        
        switch( type )
        {
            case 0: //Group Call
                retVal = MessageType.CA_STRT;
                    break;
            case 1:
            	if( getFree() == 2042 )
            	{
                    retVal = MessageType.ID_TGAS;
            	}
            	else if( lcn < 1792 )
                {
                	retVal = MessageType.CA_STRT;
                }
            	else if( lcn == 1792 || lcn == 1793 )
                {
                    retVal = MessageType.SY_IDLE;
                }
                else if( lcn == 2047 )
                {
                    retVal = MessageType.CA_ENDD;
                }
                break;
            case 2:
                retVal = MessageType.CA_STRT;
                break;
            case 5:
            	retVal = MessageType.CA_PAGE;
            	break;
            case 6:
            	retVal = MessageType.ID_RDIO;
            	break;
            case 9:
            	retVal = MessageType.DA_STRT;
            	break;
            case 11:
            	retVal = MessageType.RA_REGI;
            	break;
            default:
                break;
        }
        
        return retVal;
    }
    
    
    public int getDCC()
    {
    	return getInt( sDCC );
    }
    
    public int getSite()
    {
    	return getInt( sSITE );
    }
    
    public int getMessageTypeNumber()
    {
    	return getInt( sTYPE );
    }
    
    public int getTalkgroupID()
    {
        if( getMessageType() == MessageType.SY_IDLE )
        {
            return 0;
        }
        else
        {
            return getInt( sGROUP );
        }
    }
    
    public Alias getTalkgroupIDAlias()
    {
    	int tg = getTalkgroupID();

    	if( mAliasList != null )
    	{
        	return mAliasList.getTalkgroupAlias( String.valueOf( tg ) );
    	}

		return null;
    }
    
    public int getLCN()
    {
        return getInt( sLCN );
    }

    public long getLCNFrequency()
    {
    	return getSiteFrequency( getLCN() );
    }
    
    public String getLCNFrequencyFormatted()
    {
    	return mDecimalFormatter.format( (double)getLCNFrequency() / 1000000.0d );   
    }
    
    public PassportBand getSiteBand()
    {
        return PassportBand.lookup( getInt( sSITE_BAND ) );
    }

    public PassportBand getNeighborBand()
    {
        return PassportBand.lookup( getInt( sNEIGHBOR_BAND ) );
    }

    public long getFrequency( int base, int channel )
    {
    	return ( base + ( channel * 12500 ) );
    }
    
    public int getFree()
    {
        return getInt( sFREE );
    }
    
    public long getFreeFrequency()
    {
    	return getSiteFrequency( getFree() );
    }
    
    public String getFreeFrequencyFormatted()
    {
    	return mDecimalFormatter.format( (double)getFreeFrequency() / 1000000.0d );   
    }
    
    public long getNeighborFrequency()
    {
    	if( getMessageType() == MessageType.SY_IDLE )
    	{
    		PassportBand band = getNeighborBand();
    		
    		return getFrequency( band.getBase(), getFree() );
    	}
    	
    	return 0;
    }
    
    /**
     * Returns the radio id in hex format, or null if not the correct message
     * type
     * 
     * @return - radio id or null
     */
    public String getMobileID()
    {
    	if( getMessageType() == MessageType.ID_RDIO )
    	{
        	int radioId = getInt( sRADIO_ID );
        	
        	return String.format("%06X", radioId & 0xFFFFFF );
    	}
    	else
    	{
    		return null;
    	}
    }
    
    public Alias getMobileIDAlias()
    {
    	String min = getMobileID();
    	
    	if( mAliasList != null && min != null )
    	{
    		return mAliasList.getMobileIDNumberAlias( min );
    	}
    	
    	return null;
    }
    
	/**
	 * Appends spaces to the end of the stringbuilder to make it length long
	 */
	private void pad( StringBuilder sb, int length )
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
	private static String getHex( int value, int digits )
	{
		return String.format( "%0" + digits + "X", value );
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

	@Override
    public String getProtocol()
    {
	    return DecoderType.PASSPORT.getDisplayString();
    }

	@Override
    public String getEventType()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public String getFromID()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public Alias getFromIDAlias()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public String getToID()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public Alias getToIDAlias()
    {
	    // TODO Auto-generated method stub
	    return null;
    }
	
	public long getSiteFrequency( int channel )
	{
		if( mIdleMessage != null && 0 < channel && channel < 1792 )
		{
			PassportBand band = mIdleMessage.getSiteBand();
			
			if( band != PassportBand.BAND_UNKNOWN )
			{
				int base = band.getBase();
				
				return getFrequency( base, channel );
			}
		}

		return 0;
	}

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		switch( getMessageType() )
		{
			case SY_IDLE:
		        sb.append( "IDLE SITE:" );
		        sb.append( format( getSite(), 3 ) );
		        sb.append( " NEIGHBOR:" );
		        sb.append( format( getFree(), 3 ) );
	        	sb.append( "/" );
	        	sb.append( getFreeFrequencyFormatted() );
				break;
			case CA_PAGE:
			    sb.append( "PAGING TG:" );
                sb.append( format( getTalkgroupID(), 5 ) );
                sb.append( "/" );
                Alias pageAlias = getTalkgroupIDAlias();
                if( pageAlias != null )
                {
                    sb.append( pageAlias.getName() );
                }
                else
                {
                    sb.append( sUNKNOWN );
                }
                sb.append( " SITE:" );
                sb.append( format( getSite(), 3 ) );
                sb.append( " CHAN:" );
                sb.append( format( getLCN(), 4 ) );
                sb.append( "/" );
	        	sb.append( getLCNFrequencyFormatted() );
                sb.append( " FREE:" );
                sb.append( format( getFree(), 3 ) );
                sb.append( "/" );
	        	sb.append( getFreeFrequencyFormatted() );
                break;
			case CA_STRT:
		        sb.append( "CALL TG:" );
				sb.append( format( getTalkgroupID(), 5 ) );
				sb.append( "/" );
				Alias startAlias = getTalkgroupIDAlias();
				if( startAlias != null )
				{
					sb.append( startAlias.getName() );
				}
				else
				{
					sb.append( sUNKNOWN );
				}
				sb.append( " SITE:" );
		        sb.append( format( getSite(), 3 ) );
				sb.append( " CHAN:" );
		        sb.append( format( getLCN(), 4 ) );
	        	sb.append( "/" );
	        	sb.append( getLCNFrequencyFormatted() );
		        sb.append( " FREE:" );
		        sb.append( format( getFree(), 3 ) );
	        	sb.append( "/" );
	        	sb.append( getFreeFrequencyFormatted() );
		        break;
			case DA_STRT:
		        sb.append( "** DATA TG:" );
				sb.append( format( getTalkgroupID(), 5 ) );
				sb.append( "/" );
				Alias dataStartAlias = getTalkgroupIDAlias();
				if( dataStartAlias != null )
				{
					sb.append( dataStartAlias.getName() );
				}
				else
				{
					sb.append( sUNKNOWN );
				}
				sb.append( " SITE:" );
		        sb.append( format( getSite(), 3 ) );
				sb.append( " CHAN:" );
		        sb.append( format( getLCN(), 4 ) );
	        	sb.append( "/" );
	        	sb.append( getLCNFrequencyFormatted() );
		        sb.append( " FREE:" );
		        sb.append( format( getFree(), 3 ) );
	        	sb.append( "/" );
	        	sb.append( getFreeFrequencyFormatted() );
		        break;
			case CA_ENDD:
				sb.append( "END TG:" );
				sb.append( format( getTalkgroupID(), 5 ) );
				sb.append( "/" );
				Alias endAlias = getTalkgroupIDAlias();
				if( endAlias != null )
				{
					sb.append( endAlias.getName() );
				}
				else
				{
					sb.append( sUNKNOWN );
				}
				sb.append( " SITE:" );
		        sb.append( format( getSite(), 3 ) );
				sb.append( " CHAN:" );
		        sb.append( format( getLCN(), 4 ) );
	        	sb.append( "/" );
	        	sb.append( getLCNFrequencyFormatted() );
		        sb.append( " FREE:" );
		        sb.append( format( getFree(), 3 ) );
	        	sb.append( "/" );
	        	sb.append( getFreeFrequencyFormatted() );
		        break;
			case ID_RDIO:
				sb.append( "MOBILE ID MIN:" );
				sb.append( getMobileID() );
				sb.append( "/" );
				Alias mobileIDAlias = getMobileIDAlias();
				
				if( mobileIDAlias != null )
				{
					sb.append( mobileIDAlias.getName() );
				}
				else
				{
					sb.append( "UNKNOWN" );
				}
		        sb.append( " FREE:" );
		        sb.append( format( getFree(), 3 ) );
	        	sb.append( "/" );
	        	sb.append( getFreeFrequencyFormatted() );
				break;
			case ID_TGAS:
		        sb.append( "ASSIGN TALKGROUP:" );
				sb.append( format( getTalkgroupID(), 5 ) );
				sb.append( "/" );
				Alias assignAlias = getTalkgroupIDAlias();
				if( assignAlias != null )
				{
					sb.append( assignAlias.getName() );
				}
				else
				{
					sb.append( sUNKNOWN );
				}
				sb.append( " SITE:" );
		        sb.append( format( getSite(), 3 ) );
				sb.append( " CHAN:" );
		        sb.append( format( getLCN(), 4 ) );
	        	sb.append( "/" );
	        	sb.append( getLCNFrequencyFormatted() );
		        break;
			case RA_REGI:
				sb.append( "RADIO REGISTER TG: " );
				sb.append( format( getTalkgroupID(), 5 ) );
				sb.append( "/" );
				Alias regAlias = getTalkgroupIDAlias();
				if( regAlias != null )
				{
					sb.append( regAlias.getName() );
				}
				else
				{
					sb.append( sUNKNOWN );
				}
		        break;
			default:
		        sb.append( "UNKNOWN SITE:" );
		        sb.append( format( getSite(), 3 ) );
				sb.append( " CHAN:" );
		        sb.append( format( getLCN(), 4 ) );
	        	sb.append( "/" );
	        	sb.append( getLCNFrequencyFormatted() );
		        sb.append( " FREE:" );
		        int free = getFree();
		        sb.append( format( free, 3 ) );
		        if( free > 0 && free < 896 )
		        {
		        	sb.append( "/" );
		        	sb.append( getFreeFrequencyFormatted() );
		        }
				sb.append( " TYP:" );
				sb.append( format( getMessageTypeNumber(), 2 ) );
				sb.append( " TG:" );
				sb.append( format( getTalkgroupID(), 5 ) );
				break;
		}
		
		return  sb.toString();
    }

	@Override
    public String toString()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( mSDF.format( mTimeReceived ) );
		sb.append( " " );
		sb.append( getMessage() );
		pad( sb, 100 );
		sb.append( mCRC.toString() );
		pad( sb, 110 );
		sb.append( " [" );
		sb.append( mMessage );
		sb.append( "]" );

		return sb.toString();
    }

	@Override
    public String getErrorStatus()
    {
	    return mCRC.getDisplayText();
    }
	
	public boolean matches( PassportMessage otherMessage )
	{
		return this.getBitSetBuffer().equals( otherMessage.getBitSetBuffer() );
	}

	@Override
    public Plottable getPlottable()
    {
	    // TODO Auto-generated method stub
	    return null;
    }
	
}
