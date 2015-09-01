package module.decode.p25.message.ldu.lc;

import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.IdentifierReceiver;
import module.decode.p25.message.ldu.LDU1Message;
import module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupVoiceChannelUpdate extends LDU1Message
									 implements IdentifierReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( GroupVoiceChannelUpdate.class );

	public static final int[] IDENTIFIER_A = { 364,365,366,367 };
	public static final int[] CHANNEL_A = { 372,373,374,375,376,377,382,383,384,
		385,386,387 };
	public static final int[] GROUP_ADDRESS_A = { 536,537,538,539,540,541,546,
		547,548,549,550,551,556,557,558,559 };
	public static final int[] IDENTIFIER_B = { 560,561,566,567 };
	public static final int[] CHANNEL_B = { 568,569,570,571,720,721,722,723,724,
		725,730,731 };
	public static final int[] GROUP_ADDRESS_B = { 732,733,734,735,740,741,742,
		743,744,745,750,751,752,753,754,755 };
	
	private IBandIdentifier mIdentifierUpdateA;
	private IBandIdentifier mIdentifierUpdateB;
	
	public GroupVoiceChannelUpdate( LDU1Message message )
	{
		super( message );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.GROUP_VOICE_CHANNEL_UPDATE.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " CHAN A:" + getChannelA() );
		
		sb.append( " GRP A:" + getGroupAddressA() );
		
		if( hasChannelB() )
		{
			sb.append( " CHAN B:" + getChannelA() );
			
			sb.append( " GRP B:" + getGroupAddressA() );
		}
		
		return sb.toString();
	}

	public int getChannelIdentifierA()
	{
		return mMessage.getInt( IDENTIFIER_A );
	}
	
    public int getChannelNumberA()
    {
    	return mMessage.getInt( CHANNEL_A );
    }
    
    public String getChannelA()
    {
    	return getChannelIdentifierA() + "-" + getChannelNumberA();
    }
    
	public int getChannelIdentifierB()
	{
		return mMessage.getInt( IDENTIFIER_B );
	}
	
    public int getChannelNumberB()
    {
    	return mMessage.getInt( CHANNEL_B );
    }
    
    public String getChannelB()
    {
    	return getChannelIdentifierB() + "-" + getChannelNumberB();
    }
    
    public boolean hasChannelB()
    {
    	return getChannelNumberB() != 0;
    }
    
    public String getGroupAddressA()
    {
    	return mMessage.getHex( GROUP_ADDRESS_A, 4 );
    }

    public String getGroupAddressB()
    {
    	return mMessage.getHex( GROUP_ADDRESS_B, 4 );
    }

	@Override
    public void setIdentifierMessage( int identifier, IBandIdentifier message )
    {
		if( identifier == getChannelIdentifierA() )
		{
			mIdentifierUpdateA = message;
		}
		if( identifier == getChannelIdentifierB() )
		{
			mIdentifierUpdateB = message;
		}
    }

	@Override
    public int[] getIdentifiers()
    {
		if( hasChannelB() )
		{
			int[] identifiers = new int[ 2 ];
			identifiers[ 0 ] = getChannelIdentifierA();
			identifiers[ 1 ] = getChannelIdentifierB();
			
			return identifiers;
		}
		else
		{
			int[] identifiers = new int[ 1 ];
			identifiers[ 0 ] = getChannelIdentifierA();
			
			return identifiers;
		}
    }

    public long getDownlinkFrequencyA()
    {
    	return calculateDownlink( mIdentifierUpdateA, getChannelNumberA() );
    }
    
    public long getUplinkFrequencyA()
    {
    	return calculateUplink( mIdentifierUpdateA, getChannelNumberA() );
    }

    public long getDownlinkFrequencyB()
    {
    	return calculateDownlink( mIdentifierUpdateB, getChannelNumberA() );
    }
    
    public long getUplinkFrequencyB()
    {
    	return calculateUplink( mIdentifierUpdateB, getChannelNumberA() );
    }
}
