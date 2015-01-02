package decode.p25.message.ldu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.ldu.LDU1Message;
import decode.p25.reference.LinkControlOpcode;

public class CallTermination extends LDU1Message
{
	private final static Logger mLog = LoggerFactory.getLogger( CallTermination.class );

	public static final int[] SOURCE_ADDRESS = { 720,721,722,723,724,725,730,
		731,732,733,734,735,740,741,742,743,744,745,750,751,752,753,754,755 };
	
	public CallTermination( LDU1Message message )
	{
		super( message );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.CALL_TERMINATION_OR_CANCELLATION.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " FROM:" + getSourceAddress() );
		
		return sb.toString();
	}

    public String getSourceAddress()
    {
    	return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
}
