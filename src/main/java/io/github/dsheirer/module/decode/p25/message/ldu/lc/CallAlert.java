package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;

public class CallAlert extends LDU1Message
{
    public static final int[] TARGET_ADDRESS = { 536,537,538,539,540,541,546,
    	547,548,549,550,551,556,557,558,559,560,561,566,567,568,569,570,571 };
    public static final int[] SOURCE_ADDRESS = { 720,721,722,723,724,725,730,
    	731,732,733,734,735,740,741,742,743,744,745,750,751,752,753,754,755 };
	
	public CallAlert( LDU1Message message )
	{
		super( message );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.CALL_ALERT.getDescription();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " SRC ADDR: " + getSourceAddress() );
        sb.append( " TGT ADDR: " + getTargetAddress() );
        
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
