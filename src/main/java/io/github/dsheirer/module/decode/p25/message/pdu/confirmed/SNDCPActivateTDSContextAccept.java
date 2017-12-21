package io.github.dsheirer.module.decode.p25.message.pdu.confirmed;

import io.github.dsheirer.module.decode.p25.reference.MDPConfigurationOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNDCPActivateTDSContextAccept extends PDUConfirmedMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( SNDCPActivateTDSContextAccept.class );

	/* SN-Activate TDS Context Accept */
	public static final int[] NSAPI = { 180,181,182,183 };
	public static final int[] PDUPM = { 184,185,186,187 };
	public static final int[] READY = { 188,189,190,191 };
	public static final int[] STANDBY = { 192,193,194,195 };
	public static final int[] NAT = { 196,197,198,199 };
	public static final int[] IP_1 = { 200,201,202,203,204,205,206,207 };
	public static final int[] IP_2 = { 208,209,210,211,212,213,214,215 };
	public static final int[] IP_3 = { 216,217,218,219,220,221,222,223 };
	public static final int[] IP_4 = { 224,225,226,227,228,229,230,231 };
	public static final int[] IPHC = { 232,233,234,235,236,237,238,239 };
	public static final int[] TCPSS = { 240,241,242,243 };
	public static final int[] UDPSS = { 244,245,246,247 };
	public static final int[] MTU = { 248,249,250,251 };
	public static final int[] UDPC = { 252,253,254,255 };
	public static final int[] MDPCO = { 256,257,258,259,260,261,262,263 };
	public static final int[] DATA_ACCESS_CONTROL = { 264,265,266,267,268,
		269,270,271,272,273,274,275,276,277,278,279 };
	
	public SNDCPActivateTDSContextAccept( PDUConfirmedMessage message )
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
		sb.append( " ACCEPT  SNDCP PACKET DATA ACTIVATE " );
		sb.append( getNetworkAddressType() );
		sb.append( " " );
		sb.append( getIPAddress() );
		sb.append( " NSAPI:" );
		sb.append( getNSAPI() );
		sb.append( " MTU:" );
		sb.append( getMaximumTransmissionUnit() );
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
	 * Network Service Access Point Identifier - up to 14 NSAPI's can be
	 * allocated to the mobile with each NSAPI to be used for a specific 
	 * protocol layer.
	 */
	public int getNSAPI()
	{
		return mMessage.getInt( NSAPI );
	}
	
	public int getPDUPriorityMaximum()
	{
		return mMessage.getInt( PDUPM );
	}

	public String getReadyTimer()
	{
		switch( mMessage.getInt( READY ) )
		{
			case 1:
				return "1 Second";
			case 2:
				return "2 Seconds";
			case 3:
				return "4 Seconds";
			case 4:
				return "6 Seconds";
			case 5:
				return "8 Seconds";
			case 6:
				return "10 Seconds";
			case 7:
				return "15 Seconds";
			case 8:
				return "20 Seconds";
			case 9:
				return "25 Seconds";
			case 10:
				return "30 Seconds";
			case 11:
				return "60 Seconds";
			case 12:
				return "120 Seconds";
			case 13:
				return "180 Seconds";
			case 14:
				return "300 Seconds";
			case 15:
				return "Always in Ready";
		}
		
		return "UNKNOWN";
	}
	
	
	public String getStandbyTimer()
	{
		switch( mMessage.getInt( STANDBY ) )
		{
			case 1:
				return "10 Seconds";
			case 2:
				return "30 Seconds";
			case 3:
				return "1 Minutes";
			case 4:
				return "5 Minutes";
			case 5:
				return "10 Minutes";
			case 6:
				return "30 Minutes";
			case 7:
				return "1 Hour";
			case 8:
				return "2 Hours";
			case 9:
				return "4 Hours";
			case 10:
				return "8 Hours";
			case 11:
				return "12 Hours";
			case 12:
				return "24 Hours";
			case 13:
				return "48 Hours";
			case 14:
				return "72 Hours";
			case 15:
				return "Always in Ready";
		}

		return "Unknown";
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

	public String getMaximumTransmissionUnit()
	{
		int mtu = mMessage.getInt( MTU );
		
		switch( mtu )
		{
			case 1:
				return "296 BYTES";
			case 2:
				return "510 BYTES";
			case 3:
				return "1020 BYTES";
			case 4:
				return "1500 BYTES";
			default:
				return "UNK-" + mtu;
		}
	}
	
	public MDPConfigurationOption getMDPConfigurationOption()
	{
		return MDPConfigurationOption.fromValue( mMessage.getInt( MDPCO ) );
	}

	public String getDataAccessControl()
	{
		return mMessage.getHex( DATA_ACCESS_CONTROL, 4 );
	}
}
