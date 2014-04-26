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
package decode.mpt1327;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import map.Plottable;
import message.Message;
import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import crc.CRC;
import crc.CRCFleetsync;

public class MPT1327Message extends Message
{
	private static DecimalFormat mDecimalFormatter = new DecimalFormat("#.#####");
	private static SimpleDateFormat mSDF = new SimpleDateFormat( "yyyyMMdd HHmmss" );
	
	//Calendar to use in calculating time hacks
	Calendar mCalendar = new GregorianCalendar();
	
	//Message parts are identified in big-endian order for correct translation
	private static int BLOCK_1_START = 20;
	private static int BLOCK_2_START = 84;
	private static int BLOCK_3_START = 148;
	private static int BLOCK_4_START = 212;
	private static int BLOCK_5_START = 276;
	private static int BLOCK_5_END = 340;
	private static int[] REVS = { 0,1,2,3 };
	private static int[] SYNC = { 4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19 };

	/* Block 1 Fields */
	private static int[] B1_PREFIX = { 21,22,23,24,25,26,27 };
	private static int[] B1_IDENT1 = { 28,29,30,31,32,33,34,35,36,37,38,39,40 };
	private static int[] B1_IDENT2 = { 53,54,55,56,57,58,59,60,61,62,63,64,65 };
	private static int[] B1_GTC_CHAN = { 43,44,45,46,47,48,49,50,51,52 }; 
	private static int[] B1_MESSAGE_TYPE = { 41,42,43,44,45,46,47,48,49 };
	private static int[] B1_STATUS_MESSAGE = { 66,67,68,69,70 };
	private static int[] B1_SYSTEM_ID = { 26,27,28,29,30,31,32,33,34,35,36,37,38,39,40 };
	private static int[] B1_SYSDEF = { 21,22,23,24,25 };
	private static int[] B1_CHANNEL = { 35,36,37,38,39,40,41,42,43,44 };
	private static int[] B1_TRAFFIC_CHANNEL = { 21,22,23,24,25,26,27,28,29,30 };
	private static int[] B1_CONTROL_CHANNEL = { 31,32,33,34,35,36,37,38,39,40 };
	private static int[] B1_ADJSITE = { 49,50,51,52 };
	
	/* Block 2 Fields */
	private static int[] B2_SYSTEM_ID = { 85,86,87,88,89,90,91,92,93,94,95,96,97,98,99 };
	private static int[] B2_PREFIX = { 112,113,114,115,116,117,118 };
	private static int[] B2_IDENT2 = { 119,120,121,122,123,124,125,126,127,128,129,130,131 };
	
	private BitSetBuffer mMessage;
    private CRC[] mCRC = new CRC[ 5 ];
    private AliasList mAliasList;
    private MPTMessageType mMessageType;
    
    public MPT1327Message( BitSetBuffer message, AliasList list )
    {
        mMessage = message;
        mAliasList = list;

        checkParity( 0, BLOCK_1_START, BLOCK_2_START );
        
        if( isValid() )
        {
            mMessageType = getMessageType();
            
            switch( mMessageType )
            {
            	/* 1 data block messages */
            	case CLEAR:
            	case MAINT:
            	case MOVE:
            		break;
            	/* 2 data block messages */
            	case AHYQ:
            	case ALH:
                    checkParity( 1, BLOCK_2_START, BLOCK_3_START );
                    break;
            	/* 3 data block messages */
            	/* 4 data block messages */
            	case ACKT:
                    checkParity( 1, BLOCK_2_START, BLOCK_3_START );
                    checkParity( 2, BLOCK_3_START, BLOCK_4_START );
                    checkParity( 3, BLOCK_4_START, BLOCK_5_START );
                    break;
            	/* 5 data block messages */
            	case AHYC:
                    checkParity( 1, BLOCK_2_START, BLOCK_3_START );
                    checkParity( 2, BLOCK_3_START, BLOCK_4_START );
                    checkParity( 3, BLOCK_4_START, BLOCK_5_START );
                    checkParity( 3, BLOCK_5_START, BLOCK_5_END );
                    break;
            	case ACK:
            	case ACKB:
            	case ACKE:
            	case ACKI:
            	case ACKQ:
            	case ACKV:
            	case ACKX:
            	case AHOY:
            	case AHYP:
            	case AHYX:
            	case ALHD:
            	case ALHE:
            	case ALHF:
            	case ALHR:
            	case ALHS:
            	case ALHX:
            	case BCAST:
            	case GTC:
            	case MARK:
            		break;
        		default:
            		break;
            }
        }
        else
        {
        	mMessageType = MPTMessageType.UNKN;
        }
    }
    
    private void checkParity( int section, int start, int end )
    {
    	mCRC[ section ] = detectAndCorrect( start, end );
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

    public boolean isValid()
    {
        return mCRC[ 0 ] == CRC.PASSED || 
        	   mCRC[ 0 ] == CRC.CORRECTED;
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( mSDF.format( new Date() ) );

    	sb.append( " MPT1327 " );
    	
    	sb.append( getParity() );

    	sb.append( " " );
    	
    	sb.append( getMessage() );
    	
    	sb.append( getFiller( sb, 100 ) );
    	
    	sb.append( " [" + mMessage.toString() + "]" );
    	
    	return sb.toString();
    }
    
    public MPTMessageType getMessageType()
    {
    	int type = mMessage.getInt( B1_MESSAGE_TYPE );

    	return MPTMessageType.fromNumber( type );
    }
    
    public int getSiteID()
    {
    	if( mMessageType == MPTMessageType.BCAST )
    	{
    		return mMessage.getInt( B1_SYSTEM_ID );
    	}
    	else if( mMessageType == MPTMessageType.ALH )
    	{
        	return mMessage.getInt( B2_SYSTEM_ID );
    	}
    	else
    	{
    		return 0;
    	}
    }

    public boolean hasSystemID()
    {
    	return getSiteID() != 0;
    }
    
    public SystemDefinition getSystemDefinition()
    {
    	int sysdef = mMessage.getInt( B1_SYSDEF );
    	
    	return SystemDefinition.fromNumber( sysdef );
    }
    
    public int getChannel()
    {
    	switch( mMessageType )
    	{
    		case CLEAR:
    			return mMessage.getInt( B1_TRAFFIC_CHANNEL );
    		case GTC:
        		return mMessage.getInt( B1_GTC_CHAN );
        	default:
            	return mMessage.getInt( B1_CHANNEL );
    	}
    }
    
    public int getReturnToChannel()
    {
    	if( mMessageType == MPTMessageType.CLEAR )
    	{
    		return mMessage.getInt( B1_CONTROL_CHANNEL );
    	}
    	else
    	{
    		return 0;
    	}
    }
    
    public int getAdjacentSiteSerialNumber()
    {
    	return mMessage.getInt( B1_ADJSITE );
    }

    public int getPrefix()
    {
    	return mMessage.getInt( B1_PREFIX );
    }
    
    public int getBlock2Prefix()
    {
    	return mMessage.getInt( B2_PREFIX );
    }
    
    public int getIdent1()
    {
    	return mMessage.getInt( B1_IDENT1 );
    }
    
    public int getIdent2()
    {
    	return mMessage.getInt( B1_IDENT2 );
    }
    
    public int getBlock2Ident2()
    {
    	return mMessage.getInt( B2_IDENT2 );
    }
    
    public String getStatusMessage()
    {
    	int status = mMessage.getInt( B1_STATUS_MESSAGE );
    	
    	switch( status )
    	{
    		case 0:
    			return "STATUS: Request Speech Call";
    		case 31:
    			return "STATUS: Cancel Request Speech Call";
			default:
				return "STATUS: " + status;
    	}
    }
    
    /**
     * Returns spaces to the fill the string builder to ensure length is >= index
     */
    public String getFiller( StringBuilder sb, int index )
    {
    	if( sb.length() < index )
    	{
        	return String.format( "%" + ( index - sb.length() ) + "s", " " );
    	}
    	else
    	{
    		return "";
    	}
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
    public String getMessage()
    {
    	StringBuilder sb = new StringBuilder();

    	sb.append( pad( mMessageType.toString(), 5, " " ) );
    	
    	switch( mMessageType )
    	{
    		case ACK:
    			sb.append( " MESSAGE ACKNOWLEDGED" );

    			if( hasFromID() )
    			{
        			sb.append( " FROM:" );
        			sb.append( getFromID() );
    			}
    			break;
    		case ACKI:
    			sb.append( " SYSTEM:" );
    			sb.append( format( getSiteID(), 5 ) );

    			sb.append( " MESSAGE ACKNOWLEDGED - MORE TO FOLLOW" );

    			if( hasFromID() )
    			{
        			sb.append( " FROM:" );
        			sb.append( getFromID() );
    			}
    			
    			if( hasToID() )
    			{
        			sb.append( " TO:" );
        			sb.append( getToID() );
    			}
    			break;
    		case ACKT:
    			sb.append( " SITE:" );
    			sb.append( format( getSiteID(), 5 ) );

    			sb.append( " LONG ACK MESSAGE" );

    			if( hasFromID() )
    			{
        			sb.append( " FROM:" );
        			sb.append( getFromID() );
    			}
    			
    			if( hasToID() )
    			{
        			sb.append( " TO:" );
        			sb.append( getToID() );
    			}
    			
    			sb.append( "**********************************" );
    			break;
    		case ACKQ:
    			sb.append( " SYSTEM:" );
    			sb.append( format( getSiteID(), 5 ) );

    			sb.append( " CALL QUEUED FROM:" );

    			if( hasFromID() )
    			{
        			sb.append( " FROM:" );
        			sb.append( getFromID() );
    			}
    			
    			if( hasToID() )
    			{
        			sb.append( " TO:" );
        			sb.append( getToID() );
    			}
    			break;
    		case ACKX:
    			sb.append( " SYSTEM:" );
    			sb.append( format( getSiteID(), 5 ) );

    			sb.append( " MESSAGE REJECTED FROM:" );

    			if( hasFromID() )
    			{
        			sb.append( " FROM:" );
        			sb.append( getFromID() );
    			}
    			
    			if( hasToID() )
    			{
        			sb.append( " TO:" );
        			sb.append( getToID() );
    			}
    			break;
    		case AHYC:
    			/* SDM */
    			sb.append( " SHORT DATA INVITATION MESSAGE ********* " );
    			break;
    		case AHYQ:
    			/* Status Message */
    			sb.append( " STATUS MESSAGE" );

    			if( hasFromID() )
    			{
        			sb.append( " FROM:" );
        			sb.append( getFromID() );
    			}
    			
    			if( hasToID() )
    			{
        			sb.append( " TO:" );
        			sb.append( getToID() );
    			}
    			sb.append( " " );
    			sb.append( getStatusMessage() );
    			break;
    		case ALH:
    		case ALHD:
    		case ALHS:
    		case ALHE:
    		case ALHR:
    		case ALHX:
    		case ALHF:
    			sb.append( " SYSTEM:" );
    			sb.append( format( getSiteID(), 5 ) );

    			if( hasToID() )
    			{
        			sb.append( " ID:" );
        			sb.append( getToID() );
    			}
    			break;
    		case BCAST:
    			SystemDefinition sysdef = getSystemDefinition();

    			sb.append( " " );
    			sb.append( sysdef.getLabel() );
    			
    			switch( sysdef )
    			{
    				case ANNOUNCE_CONTROL_CHANNEL:
    				case WITHDRAW_CONTROL_CHANNEL:
	    				sb.append( " CHAN:" );
	    				sb.append( getChannel() );
	    				break;
    				case BROADCAST_ADJACENT_SITE_CONTROL_CHANNEL_NUMBER:
	    				sb.append( " CHAN:" );
	    				sb.append( getChannel() );
	    				sb.append( " SER:" );
	    				sb.append( getAdjacentSiteSerialNumber() );
	    				//debug
	    				sb.append( " *************************" );
	    				break;
    			}
    			break;
    		case CLEAR:
    			sb.append( " TRAFFIC CHANNEL:" );
    			sb.append( getChannel() );
    			sb.append( " RETURN TO CONTROL CHANNEL:" );
    			sb.append( getReturnToChannel() );
    			break;
    		case GTC:
    			if( hasFromID() )
    			{
        			sb.append( " FROM:" );
        			sb.append( getFromID() );
    			}

    			if( hasToID() )
    			{
        			sb.append( " TO:" );
        			sb.append( getToID() );
    			}
    			
				sb.append( " CHAN:" );
				sb.append( getChannel() );
				break;
    		case MAINT:
    			if( hasToID() )
    			{
        			sb.append( " ID:" );
        			sb.append( getToID() );
    			}
    			
    			break;
    	}
    	
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
	    return "MPT-1327";
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
    	StringBuilder sb = new StringBuilder();

    	int ident2 = getIdent2();
    	
    	IdentType type = IdentType.fromIdent( ident2 );

    	/* Inter-Prefix - the from and to idents are different prefixes */
    	if( type == IdentType.IPFIXI )
    	{
        	sb.append( format( getBlock2Prefix(), 3 ) );
        	sb.append( "-" );
        	sb.append( format( getBlock2Ident2(), 4) );
    	}
    	else
    	{
        	sb.append( format( getPrefix(), 3 ) );
        	sb.append( "-" );
        	sb.append( format( ident2, 4) );
    	}
    	
    	return sb.toString();
    }
	
	public boolean hasFromID()
	{
		return getFromID() != null;
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

		int prefix = getPrefix();
    	int ident = getIdent1();

    	switch( IdentType.fromIdent( ident ) )
    	{
    		case USER:
            	sb.append( format( prefix, 3 ) );
            	sb.append( "-" );
            	sb.append( format( ident, 4) );
            	break;
    		case IPFIXI:
    			sb.append( "INTER-PREFIX" );
    			break;
    		case ALLI:
    			sb.append( "ALL RADIOS" );
    			break;
    		case PABXI:
    			sb.append( "PABX EXT" );
    			break;
    		case PSTNSI1:
    		case PSTNSI2:
    		case PSTNSI3:
    		case PSTNSI4:
    		case PSTNSI5:
    		case PSTNSI6:
    		case PSTNSI7:
    		case PSTNSI8:
    		case PSTNSI9:
    		case PSTNSI10:
    		case PSTNSI11:
    		case PSTNSI12:
    		case PSTNSI13:
    		case PSTNSI14:
    		case PSTNSI15:
    			sb.append( "PRE-DEFINED PSTN" );
    			break;
    		case PSTNGI:
    			sb.append( "PSTN GATEWAY" );
    			break;
    		case TSCI:
    			sb.append( "CONTROLLER" );
    			break;
    		case DIVERTI:
    			sb.append( "CALL DIVERT" );
    			break;
    	}

    	return sb.toString();
    }
	
	public boolean hasToID()
	{
		return getToID() != null;
	}

	@Override
    public Alias getToIDAlias()
    {
		if( hasToID() )
		{
			return mAliasList.getMPT1327Alias( getToID() );
		}
		else
		{
			return null;
		}
    }

	@Override
    public String getErrorStatus()
    {
	    return getParity();
    }

	@Override
    public Plottable getPlottable()
    {
	    return null;
    }
	
	public enum IdentType
	{
		ALLI( "All Subscribers" ),
		DIVERTI( "Divert" ),
		DNI( "Data Network Gateway" ),
		DUMMYI( "Dummy Ident" ),
		INCI( "Include in Call" ),
		IPFIXI( "Inter-Prefix" ),
		PABXI( "PABX Gateway" ),
		PSTNGI( "PSTN Gateway" ),
		PSTNSI1( "PSTN or Network 1" ),
		PSTNSI2( "PSTN or Network 2" ),
		PSTNSI3( "PSTN or Network 3" ),
		PSTNSI4( "PSTN or Network 4" ),
		PSTNSI5( "PSTN or Network 5" ),
		PSTNSI6( "PSTN or Network 6" ),
		PSTNSI7( "PSTN or Network 7" ),
		PSTNSI8( "PSTN or Network 8" ),
		PSTNSI9( "PSTN or Network 9" ),
		PSTNSI10( "PSTN or Network 10" ),
		PSTNSI11( "PSTN or Network 11" ),
		PSTNSI12( "PSTN or Network 12" ),
		PSTNSI13( "PSTN or Network 13" ),
		PSTNSI14( "PSTN or Network 14" ),
		PSTNSI15( "PSTN or Network 15" ),
		REGI( "Registration" ),
		RESERVED( "Reserved" ),
		SDMI( "Short Data Message" ),
		SPARE( "Spare" ),
		TSCI( "TSC" ),
		USER( "Individual or Group Ident" ),
		UNKNOWN( "Unknown" );
		
		private String mLabel;
		
		private IdentType( String label )
		{
			mLabel = label;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public static IdentType fromIdent( int ident )
		{
			switch( ident )
			{
				case 0:
					return DUMMYI;
				case 8101:
					return PSTNGI;
				case 8102:
					return PABXI;
				case 8103:
					return DNI;
				case 8121:
					return PSTNSI1;
				case 8122:
					return PSTNSI2;
				case 8123:
					return PSTNSI3;
				case 8124:
					return PSTNSI4;
				case 8125:
					return PSTNSI5;
				case 8126:
					return PSTNSI6;
				case 8127:
					return PSTNSI7;
				case 8128:
					return PSTNSI8;
				case 8129:
					return PSTNSI9;
				case 8130:
					return PSTNSI10;
				case 8131:
					return PSTNSI11;
				case 8132:
					return PSTNSI12;
				case 8133:
					return PSTNSI13;
				case 8134:
					return PSTNSI14;
				case 8135:
					return PSTNSI15;
				case 8181:
				case 8182:
				case 8183:
				case 8184:
					return RESERVED;
				case 8185:
					return REGI;
				case 8186:
					return INCI;
				case 8187:
					return DIVERTI;
				case 8188:
					return SDMI;
				case 8189:
					return IPFIXI;
				case 8190:
					return TSCI;
				case 8191:
					return ALLI;
				default:
					if( 1 <= ident && ident <= 8100 )
					{
						return USER;
					}
					else if( ( 8104 <= ident && ident <= 8120 ) || 
							 ( 8136 <= ident && ident <= 8180 ) )
					{
						return SPARE;
					}
					else
					{
						return UNKNOWN;
					}
			}
		}
	}
	
	public enum SystemDefinition
	{
		UNKNOWN( "UNKNOWN" ),
		ANNOUNCE_CONTROL_CHANNEL( "ANNOUNCE CONTROL CHANNEL" ),
		WITHDRAW_CONTROL_CHANNEL( "WITHDRAW CONTROL CHANNEL" ),
		SPECIFY_CALL_MAINTENANCE_PARAMETERS( "CALL MAINT PARAMETERS" ),
		SPECIFY_REGISTRATION_PARAMETERS( "REGISTRATION PARAMETERS" ),
		BROADCAST_ADJACENT_SITE_CONTROL_CHANNEL_NUMBER( "NEIGHBOR" ),
		VOTE_NOW_ADVICE( "VOTE NOW" );

		private String mLabel;
		
		private SystemDefinition( String label )
		{
			mLabel = label;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public static SystemDefinition fromNumber( int number )
		{
			switch( number )
			{
				case 0:
					return ANNOUNCE_CONTROL_CHANNEL;
				case 1:
					return WITHDRAW_CONTROL_CHANNEL;
				case 2:
					return SPECIFY_CALL_MAINTENANCE_PARAMETERS;
				case 3:
					return SPECIFY_REGISTRATION_PARAMETERS;
				case 4:
					return BROADCAST_ADJACENT_SITE_CONTROL_CHANNEL_NUMBER;
				case 5:
					return VOTE_NOW_ADVICE;
				default:
					return UNKNOWN;
			}
		}
	}
	
	public enum MPTMessageType
	{
		UNKN(  -1, "Unknown" ),
		GTC(    0, "GTC - Goto Channel" ),
		ALH(  256, "ALH - Aloha" ),
		ALHS( 257, "ALHS - Standard Data Excluded" ),
		ALHD( 258, "ALHD - Simple Calls Excluded" ),
		ALHE( 259, "ALHE - Emergency Calls Only" ),
		ALHR( 260, "ALHR - Emergency or Registration" ),
		ALHX( 261, "ALHX - Registration Excluded" ),
		ALHF( 262, "ALHF - Fallback Mode" ),
		
		ACK(  264, "ACK - Acknowledge" ),
		ACKI( 265, "ACKI - More To Follow" ),
		ACKQ( 266, "ACKQ - Call Queued" ),
		ACKX( 267, "ACKX - Message Rejected" ),
		ACKV( 268, "ACKV - Called Unit Unavailable" ),
		ACKE( 269, "ACKE - Emergency" ),
		ACKT( 270, "ACKT - Try On Given Address" ),
		ACKB( 271, "ACKB - Call Back/Negative Ack" ),
		
		AHOY( 272, "AHOY - General Availability Check" ),
		AHYX( 274, "AHYX - Cancel Alert/Waiting Status" ),
		AHYP( 277, "AHYP - Called Unit Presence Monitoring" ),
		AHYQ( 278, "AHYQ - Status Message" ),
		AHYC( 279, "AHYC - Short Data Message" ),
		
		MARK( 280, "MARK - Control Channel Marker" ),
		MAINT(281, "MAINT - Call Maintenance Message"),
		CLEAR(282, "CLEAR - Down From Allocated Channel" ),
		MOVE( 283, "MOVE - To Specified Channel" ),
		BCAST(284, "BCAST - System Parameters" );

		/* There are more to fill in here */
		
		private int mNumber;
		private String mDescription;
		
		private MPTMessageType( int number, String description )
		{
			mNumber = number;
			mDescription = description;
		}
		
		public int getMessageNumber()
		{
			return mNumber;
		}
		
		public String getDescription()
		{
			return mDescription;
		}

		public static MPTMessageType fromNumber( int number )
		{
			if( number < 256 )
			{
				return GTC;
			}
			
			switch( number )
			{
				case 256:
					return ALH;
				case 257:
					return ALHS;
				case 258:
					return ALHD;
				case 259:
					return ALHE;
				case 260:
					return ALHR;
				case 261:
					return ALHX;
				case 262:
					return ALHF;
				case 264:
					return ACK;
				case 265:
					return ACKI;
				case 266:
					return ACKQ;
				case 267:
					return ACKX;
				case 268:
					return ACKV;
				case 269:
					return ACKE;
				case 270:
					return ACKT;
				case 271:
					return ACKB;
				case 272:
					return AHOY;
				case 274:
					return AHYX;
				case 277: 
					return AHYP;
				case 278:
					return AHYQ;
				case 279:
					return AHYC;
				case 280:
					return MARK;
				case 281:
					return MAINT;
				case 282:
					return CLEAR;
				case 283:
					return MOVE;
				case 284:
					return BCAST;
				default:
					return UNKN;
			}
		}
	}
}
