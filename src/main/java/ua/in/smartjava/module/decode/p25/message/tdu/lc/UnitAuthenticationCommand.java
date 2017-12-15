package ua.in.smartjava.module.decode.p25.message.tdu.lc;

import ua.in.smartjava.module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitAuthenticationCommand extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( UnitAuthenticationCommand.class );
	public static final int[] NETWORK_ID = { 72,73,74,75,88,89,90,91,92,93,94,
		95,96,97,98,99,112,113,114,115 };
	public static final int[] SYSTEM_ID = { 116,117,118,119,120,121,122,123,
		136,137,138,139 };
	/* ICD says this is a ua.in.smartjava.source id, but this is a command messages, so this
	 * must be a target id */
	public static final int[] TARGET_ID = { 140,141,142,143,144,145,146,147,
		160,161,162,163,164,165,166,167,168,169,170,171,184,185,186,187 };
	
	public UnitAuthenticationCommand( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.UNIT_AUTHENTICATION_COMMAND.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );

		sb.append( " ADDRESS:" + getCompleteTargetAddress() );
		
		return sb.toString();
	}
	
	public String getCompleteTargetAddress()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getNetworkID() );
		sb.append( ":" );
		sb.append( getSystemID() );
		sb.append( ":" );
		sb.append( getTargetID() );
		
		return sb.toString();
	}
	
	public String getNetworkID()
	{
		return mMessage.getHex( NETWORK_ID, 5 );
	}
	
	public String getSystemID()
	{
		return mMessage.getHex( SYSTEM_ID, 3 );
	}

    public String getTargetID()
    {
    	return mMessage.getHex( TARGET_ID, 6 );
    }
}
