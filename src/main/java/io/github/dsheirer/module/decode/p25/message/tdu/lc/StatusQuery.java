package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusQuery extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( StatusQuery.class );
	public static final int[] TARGET_ADDRESS = { 112,113,114,115,116,117,118,
		119,120,121,122,123,136,137,138,139,140,141,142,143,144,145,146,147 };
	public static final int[] SOURCE_ADDRESS = { 160,161,162,163,164,165,166,
		167,168,169,170,171,184,185,186,187,188,189,190,191,192,193,194,195 };
	
	public StatusQuery( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.STATUS_QUERY.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " FROM:" + getSourceAddress() );
		
		sb.append( " TO:" + getTargetAddress() );
		
		return sb.toString();
	}

    public String getTargetAddress()
    {
    	return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
    
    public String getSourceAddress()
    {
    	return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
}
