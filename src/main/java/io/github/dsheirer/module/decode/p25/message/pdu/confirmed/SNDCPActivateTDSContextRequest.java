package io.github.dsheirer.module.decode.p25.message.pdu.confirmed;

import io.github.dsheirer.module.decode.p25.reference.MDPConfigurationOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNDCPActivateTDSContextRequest extends PDUConfirmedMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( SNDCPActivateTDSContextRequest.class );

	public static final int[] SNDCP_VERSION = { 180,181,182,183 };
	public static final int[] NSAPI = { 184,185,186,187 };
	public static final int[] NAT = { 188,189,190,191 };
	public static final int[] IP_1 = { 192,193,194,195,196,197,198,199 };
	public static final int[] IP_2 = { 200,201,202,203,204,205,206,207 };
	public static final int[] IP_3 = { 208,209,210,211,212,213,214,215 };
	public static final int[] IP_4 = { 216,217,218,219,220,221,222,223 };
	public static final int[] DSUT = { 224,225,226,227 };
	public static final int[] UDPC = { 228,229,230,231 };
	public static final int[] IPHC = { 232,233,234,235,236,237,238,239 };
	public static final int[] TCPSS = { 240,241,242,243 };
	public static final int[] UDPSS = { 244,245,246,247 };
	public static final int[] MDPCO = { 248,249,250,251,252,253,254,255 };
	
	public SNDCPActivateTDSContextRequest( PDUConfirmedMessage message )
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
		sb.append( " REQUEST SNDCP PACKET DATA ACTIVATE " );
		sb.append( getNetworkAddressType() );
		sb.append( " " );
		sb.append( getIPAddress() );
		sb.append( " NSAPI:" );
		sb.append( getNSAPI() );
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
		
	    return sb.toString();
    }
	
	/**
	 * SNDCP Version: 1 = P25 SNDCP Version 1
	 */
	public int getSNDCPVersion()
	{
		return mMessage.getInt( SNDCP_VERSION );
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
	
	public String getNetworkAddressType()
	{
		return mMessage.getInt( NAT ) == 0 ? "IPV4 STATIC" : "IPV4 DYNAMIC";
	}
	
	public String getIPAddress()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( mMessage.getInt( IP_1 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( IP_2 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( IP_3 ) );
		sb.append( "." );
		sb.append( mMessage.getInt( IP_4 ) );
		
		return sb.toString();
	}
	
	public String getDataSubscriberUnitType()
	{
		switch( mMessage.getInt( DSUT ) )
		{
			case 0:
				return "DATA ONLY MRC";
			case 1:
				return "DATA AND VOICE MRC";
		}
		
		return "UNKNOWN";
	}
	
	public boolean hasIPHeaderCompression()
	{
		return mMessage.getInt( IPHC ) == 1;
	}
	
	public boolean hasUserDataPayloadCompression()
	{
		return mMessage.getInt( UDPC ) == 1;
	}
	
	public int getTCPStateSlots()
	{
		return mMessage.getInt( TCPSS );
	}

	public int getUDPStateSlots()
	{
		return mMessage.getInt( UDPSS );
	}

	public MDPConfigurationOption getMDPConfigurationOption()
	{
		return MDPConfigurationOption.fromValue( mMessage.getInt( MDPCO ) );
	}
}
