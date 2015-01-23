package decode.p25.message.pdu.confirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.reference.PDUType;
import decode.p25.reference.SNDCPActivationRejectReason;

public class SNDCPDeactivateTDSContext extends PDUConfirmedMessage
{
	public final static Logger mLog = 
			LoggerFactory.getLogger( SNDCPDeactivateTDSContext.class );

	public static final int[] NSAPI = { 180,181,182,183 };
	public static final int[] DEACTIVATION_TYPE = { 184,185,186,187,188,189,
		190,191 };
	
	public SNDCPDeactivateTDSContext( PDUConfirmedMessage message )
    {
	    super( message );
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " LLID:" );
		sb.append( getLogicalLinkID() );
		sb.append( " REQUEST SNDCP PACKET DATA DEACTIVATE " );
		sb.append( getDeactivationType() );
		sb.append( " CRC[" );
		sb.append( getErrorStatus() );
		sb.append( "] " );
		
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
	
	public String getDeactivationType()
	{
		if( getPDUType() == PDUType.SN_DEACTIVATE_TDS_CONTEXT_REQUEST )
		{
			return mMessage.getInt( DEACTIVATION_TYPE ) == 0 ?
					"ALL NSAPIS" : "THIS NSAPI";
		}
		
		return null;
	}
}
