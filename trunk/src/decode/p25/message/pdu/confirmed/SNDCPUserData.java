package decode.p25.message.pdu.confirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.reference.IPHeaderCompression;
import decode.p25.reference.IPProtocol;

public class SNDCPUserData extends PDUConfirmedMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( SNDCPUserData.class );

	public static final int DATA_BLOCK_START = 176;
	
	public static final int[] NSAPI = { 180,181,182,183 };
	public static final int[] PCOMP = { 184,185,186,187 };
	public static final int[] DCOMP = { 188,189,190,191 };

	/* IP Packet Header */
	public static final int[] IP_VERSION = { 192,193,194,195 };
	public static final int[] IHL = { 196,197,198,199 };
	public static final int[] DSCP = { 200,201,202,203,204,205 };
	public static final int[] ECN = { 206,207 };
	public static final int[] TOTAL_LENGTH = { 208,209,210,211,212,213,214,215,
		216,217,218,219,220,221,222,223 };
	public static final int[] IDENTIFICATION = { 224,225,226,227,228,229,230,231,
		232,233,234,235,236,237,238,239	};
	public static final int[] FLAGS = { 240,241,242 };
	public static final int[] FRAGMENT_OFFSET = { 243,244,245,246,247,248,249,
		250,251,252,253,254,255 };
	public static final int[] TTL = { 256,257,258,259,260,261,262,263 };
	public static final int[] PROTOCOL = { 264,265,266,267,268,269,270,271 };
	public static final int[] HEADER_CHECKSUM = { 272,273,274,275,276,277,278,
		279,280,281,282,283,284,285,286,287 };
	public static final int[] SOURCE_IP_1 = { 288,289,290,291,292,293,294,295 };
	public static final int[] SOURCE_IP_2 = { 296,297,298,299,300,301,302,303 };
	public static final int[] BLOCK_SERIAL_2 = { 304,305,306,307,308,309,310 };
	public static final int[] BLOCK_CRC9_2 = { 311,312,313,314,315,316,317,318,
		319 };
	public static final int[] SOURCE_IP_3 = { 320,321,322,323,324,325,326,327 };
	public static final int[] SOURCE_IP_4 = { 328,329,330,331,332,333,334,335 };
	public static final int[] DESTINATION_IP_1 = { 336,337,338,339,340,341,342,343 };
	public static final int[] DESTINATION_IP_2 = { 344,345,346,347,348,349,350,351 };
	public static final int[] DESTINATION_IP_3 = { 352,353,354,355,356,357,358,359 };
	public static final int[] DESTINATION_IP_4 = { 360,361,362,363,364,365,366,367 };
	
	public static final int IP_DATA_START = 368;
	
	/* User Datagram Protocol */
	public static final int[] UDP_SOURCE_PORT = { 368,369,370,371,372,373,374,
		375 };
	public static final int[] UDP_DESTINATION_PORT = { 376,377,378,379,380,381,
		382,383 };
	public static final int[] UDP_LENGTH = { 384,385,386,387,388,389,390,391,
		392,393,394,395,396,397,398,399 };
	public static final int[] UDP_CHECKSUM = { 400,401,402,403,404,405,406,407,
		408,409,410,411,412,413,414,415 };
	public static final int UDP_DATA_START = 416;
	
	public SNDCPUserData( PDUConfirmedMessage message )
    {
	    super( message );
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " PDUC LLID:" );
		sb.append( getLogicalLinkID() );
		sb.append( " SNDCP NSAPI:" );
		sb.append( getNSAPI() );
		sb.append( " " );
		IPProtocol protocol = getIPProtocol();

		sb.append( protocol.getLabel() );
		sb.append( "/IP" );
		
		sb.append( " FM:" );
		sb.append( getSourceIPAddress() );
		
		if( protocol == IPProtocol.UDP )
		{
			sb.append( ":" );
			sb.append( getUDPSourcePort() );
		}
		
		sb.append( " TO:" );
		sb.append( getDestinationIPAddress() );
		
		if( protocol == IPProtocol.UDP )
		{
			sb.append( ":" );
			sb.append( getUDPDestinationPort() );
		}
		
		sb.append( " DATA:" );
		sb.append( getPayload() );
		
		sb.append( " CRC[" );
		sb.append( getErrorStatus() );
		sb.append( "]" );
		
		sb.append( " PACKET #" );
		sb.append( getPacketSequenceNumber() );
		
		if( isFinalFragment() && getFragmentSequenceNumber() == 0 )
		{
			sb.append( ".C" );
		}
		else
		{
			sb.append( "." );
			sb.append( getFragmentSequenceNumber() );
			
			if( isFinalFragment() )
			{
				sb.append( "C" );
			}
		}
		
		sb.append( " " );
		
		sb.append( mMessage.toString() );
		
	    return sb.toString();
    }

	/**
	 * Network Service Access Point Identifier - up to 14 NSAPI's can be
	 * allocated to the mobile with each NSAPI to be used for a specific 
	 * protocol layer.
	 */
	public int getNSAPI()
	{
		return mMessage.getInt( NSAPI );
	}
	
	public IPHeaderCompression getIPHeaderCompressionState()
	{
		return IPHeaderCompression.fromValue( mMessage.getInt( PCOMP ) );
	}

	/**
	 * No enum values were defined for this
	 */
	public boolean hasUserDataCompression()
	{
		return mMessage.getInt( DCOMP ) > 0;
	}

	/**
	 * IP Version - should always be 4 (IPV4)
	 */
	public int getIPVersion()
	{
		return mMessage.getInt( IP_VERSION );
	}

	/**
	 * IP packet header length in 32-bit words
	 */
	public int getInternetHeaderLength()
	{
		return mMessage.getInt( IHL );
	}

	/**
	 * IP packet DSCP
	 */
	public int getDifferentiatedServicesCodePoint()
	{
		return mMessage.getInt( DSCP );
	}

	/**
	 * IP packet ECN
	 */
	public int getExplicitCongestionNotification()
	{
		return mMessage.getInt( ECN );
	}

	/**
	 * IP packet size in bytes, including header and data
	 */
	public int getTotalLength()
	{
		return mMessage.getInt( TOTAL_LENGTH );
	}
	
	/**
	 * IP packet identification
	 */
	public int getIdentification()
	{
		return mMessage.getInt( IDENTIFICATION );
	}

	/**
	 * IP packet flags.  0-Reserved, 1-Don't Fragment, 2-More Fragments
	 */
	public int getFlags()
	{
		return mMessage.getInt( FLAGS );
	}
	
	public boolean isFragment()
	{
		return getFlags() > 0;
	}

	/**
	 * IP packet - identifies the offset value in 8-byte blocks (64-bit blocks)
	 * for this packet fragment from the beginning.
	 */
	public int getFragmentOffset()
	{
		return mMessage.getInt( FRAGMENT_OFFSET );
	}

	/**
	 * IP packet time (hops) to live
	 */
	public int getTimeToLive()
	{
		return mMessage.getInt( TTL );
	}
	
	/**
	 * Identifies the IP protocol carried by the IP packet
	 */
	public IPProtocol getIPProtocol()
	{
		return IPProtocol.fromValue( mMessage.getInt( PROTOCOL ) );
	}

	/**
	 * Source IPV4 address
	 */
	public String getSourceIPAddress()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( mMessage.getInt( SOURCE_IP_1 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( SOURCE_IP_2 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( SOURCE_IP_3 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( SOURCE_IP_4 ) );
		
		return sb.toString();
	}

	/**
	 * Destination IPV4 address
	 */
	public String getDestinationIPAddress()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( mMessage.getInt( DESTINATION_IP_1 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( DESTINATION_IP_2 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( DESTINATION_IP_3 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( DESTINATION_IP_4 ) );
		
		return sb.toString();
	}

	/**
	 * UDP source port
	 */
	public int getUDPSourcePort()
	{
		return mMessage.getInt( UDP_SOURCE_PORT );
	}

	/**
	 * UDP destination port
	 */
	public int getUDPDestinationPort()
	{
		return mMessage.getInt( UDP_DESTINATION_PORT );
	}

	/**
	 * UDP data payload size in bytes
	 */
	public int getUDPDataLength()
	{
		return mMessage.getInt( UDP_LENGTH );
	}
	/**
	 * Hex string representation of the packet data, including packet header
	 * data if present.
	 */
	public String getPayload()
	{
		StringBuilder sb = new StringBuilder();

		/* Adjust for PDU header (2 bytes) plus IP Packet header (20 bytes) */
		int start = 22;
		
		if( getIPProtocol() == IPProtocol.UDP )
		{
			/* Adjust for UDP header (8 bytes) */
			start = 30;
		}
		
		int spacingCounter = 0;

		/* Append octets inserting a space between each 32-bit value */
		int octets = getOctetCount() + getDataHeaderOffset();
		
		for( int x = start; x < octets; x++ )
		{
			sb.append( getPacketOctet( x ) );
			
			spacingCounter++;
			
			if( spacingCounter >= 4 )
			{
				sb.append( " " );
				spacingCounter = 0;
			}
		}

		return sb.toString();
	}

	/**
	 * Returns the octet identified by the 0-indexed 'octet' argument location.
	 * If there is a Data Header Offset, the 0 index points to the first octet
	 * of the data header.  Otherwise, the 0 index points to the first octet
	 * of the packet.  You must account for the data header offset when 
	 * specifying the octet.
	 */
	public String getPacketOctet( int octet )
	{
		int block = (int)( octet / 16 );
		int offset = octet % 16;

		int start = DATA_BLOCK_START + ( block * 144 ) + ( offset * 8 );
		
		return mMessage.getHex( start, start + 7, 2 );
	}
	
}
