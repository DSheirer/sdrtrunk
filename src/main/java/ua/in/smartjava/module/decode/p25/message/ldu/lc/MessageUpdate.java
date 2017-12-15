package ua.in.smartjava.module.decode.p25.message.ldu.lc;

import ua.in.smartjava.module.decode.p25.message.ldu.LDU1Message;
import ua.in.smartjava.module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageUpdate extends LDU1Message
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( MessageUpdate.class );
	public static final int[] SHORT_DATA_MESSAGE = { 364,365,366,367,372,373,
		374,375,376,377,382,383,384,385,386,387 };
	public static final int[] TARGET_ADDRESS = { 536,537,538,539,540,541,546,
		547,548,549,550,551,556,557,558,559,560,561,566,567,568,569,570,571 };
	public static final int[] SOURCE_ADDRESS = { 720,721,722,723,724,725,730,
		731,732,733,734,735,740,741,742,743,744,745,750,751,752,753,754,755 };

	public MessageUpdate( LDU1Message message )
	{
		super( message );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.MESSAGE_UPDATE.getDescription();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " MSG:" + getShortDataMessage() );
        sb.append( " SRC ADDR: " + getSourceAddress() );
        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        return sb.toString();
    }
    
    public String getShortDataMessage()
    {
        return mMessage.getHex( SHORT_DATA_MESSAGE, 4 );
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
