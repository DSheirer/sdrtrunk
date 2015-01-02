package decode.p25.message.tdu.lc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.message.IdentifierProvider;
import decode.p25.message.tsbk.osp.control.IdentifierProviderReceiver;
import decode.p25.reference.LinkControlOpcode;

public class GroupVoiceChannelUpdate extends TDULinkControlMessage
									 implements IdentifierProviderReceiver
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( GroupVoiceChannelUpdate.class );

	public static final int[] IDENTIFIER_A = { 72,73,74,75 };
	public static final int[] CHANNEL_A = { 88,89,90,91,92,93,94,95,96,97,98,99 };
	public static final int[] GROUP_ADDRESS_A = { 112,113,114,115,116,117,118,
		119,120,121,122,123,136,137,138,139 };
	public static final int[] IDENTIFIER_B = { 140,141,142,143 };
	public static final int[] CHANNEL_B = { 144,145,146,147,160,161,162,163,164,
		165,166,167 };
	public static final int[] GROUP_ADDRESS_B = { 168,169,170,171,184,185,186,
		187,188,189,190,191,192,193,194,195 };
	
	private IdentifierProvider mIdentifierUpdateA;
	private IdentifierProvider mIdentifierUpdateB;
	
	public GroupVoiceChannelUpdate( TDULinkControlMessage source )
	{
		super( source );
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
    public void setIdentifierMessage( int identifier, IdentifierProvider message )
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
