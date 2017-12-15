package module.decode.p25.message.ldu.lc;

import module.decode.p25.message.ldu.LDU1Message;
import module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUpdate extends LDU1Message
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( StatusUpdate.class );
	public static final int[] USER_STATUS = { 364,365,366,367,372,373,374,375 };
	public static final int[] UNIT_STATUS = { 376,377,382,383,384,385,386,387 };
	public static final int[] TARGET_ADDRESS = { 536,537,538,539,540,541,546,
		547,548,549,550,551,556,557,558,559,560,561,566,567,568,569,570,571 };
	public static final int[] SOURCE_ADDRESS = { 720,721,722,723,724,725,730,
		731,732,733,734,735,740,741,742,743,744,745,750,751,752,753,754,755 };

	public StatusUpdate( LDU1Message message )
	{
		super( message );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.STATUS_UPDATE.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
        sb.append( " STATUS USER:" + getUserStatus() );
        sb.append( " UNIT:" + getUnitStatus() );
		sb.append( " FROM:" + getSourceAddress() );
		sb.append( " TO:" + getTargetAddress() );
		
		return sb.toString();
	}

    public String getUserStatus()
    {
    	return mMessage.getHex( USER_STATUS, 2 );
    }
    
    public String getUnitStatus()
    {
    	return mMessage.getHex( UNIT_STATUS, 2 );
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
