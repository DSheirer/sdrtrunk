package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;

public class CallTermination extends TDULinkControlMessage
{
	public static final int[] SOURCE_ADDRESS = { 160,161,162,163,164,165,166,
		167,168,169,170,171,184,185,186,187,188,189,190,191,192,193,194,195 };
	
	public CallTermination( TDULinkControlMessage source )
	{
		super( source );
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
		
		sb.append( " BY:" + getSourceAddress() );
		
		return sb.toString();
	}

    public String getSourceAddress()
    {
    	return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
}
