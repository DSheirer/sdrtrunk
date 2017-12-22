package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitAuthenticationCommand extends LDU1Message
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( UnitAuthenticationCommand.class );
	public static final int[] NETWORK_ID = { 364,365,366,367,372,373,374,375,
		376,377,382,383,384,385,386,387,536,537,538,539 };
	public static final int[] SYSTEM_ID = { 540,541,546,547,548,549,550,551,556,
		557,558,559 };
	public static final int[] TARGET_ID = { 560,561,566,567,568,569,570,571,720,
		721,722,723,724,725,730,731,732,733,734,735,740,741,742,743 };
	
	public UnitAuthenticationCommand( LDU1Message message )
	{
		super( message );
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
