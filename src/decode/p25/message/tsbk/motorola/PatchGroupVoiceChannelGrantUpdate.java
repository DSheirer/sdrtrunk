package decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.IBandIdentifier;
import decode.p25.message.IdentifierReceiver;
import decode.p25.reference.DataUnitID;

public class PatchGroupVoiceChannelGrantUpdate extends MotorolaTSBKMessage
												  implements IdentifierReceiver
{
	public static final int[] CHANNEL_1 = { 80,81,82,83 };
	public static final int[] IDENTIFIER_1 = { 84,85,86,87,88,89,90,91,92,93,94,
		95 };
	public static final int[] PATCH_GROUP_ADDRESS_1 = { 96,97,98,99,100,101,102,
		103,104,105,106,107,108,109,110,111 };
	public static final int[] CHANNEL_2 = { 112,113,114,115 };
	public static final int[] IDENTIFIER_2 = { 116,117,118,119,120,121,122,123,
		124,125,126,127 };
	public static final int[] PATCH_GROUP_ADDRESS_2 = { 128,129,130,131,132,133,
		134,135,136,137,138,139,140,141,142,143 };

    private IBandIdentifier mIdentifierUpdate1;
    private IBandIdentifier mIdentifierUpdate2;

    public PatchGroupVoiceChannelGrantUpdate( BinaryMessage message, 
    		DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
    }

	@Override
    public String getEventType()
    {
	    return MotorolaOpcode.PATCH_GROUP_CHANNEL_GRANT_UPDATE.getLabel();
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( getMessageStub() );

		
		sb.append( " PATCH GRP1:" );
		sb.append( getPatchGroupAddress1() );
		sb.append( " GRP2:" );
		sb.append( getPatchGroupAddress2() );

		sb.append( " CHAN 1:" );
        sb.append( getChannel1() );
        sb.append( " DN:" + getDownlinkFrequency1() );
        
		sb.append( " CHAN 2:" );
        sb.append( getChannel2() );
        sb.append( " DN:" + getDownlinkFrequency2() );
		
	    return sb.toString();
    }
	
    public String getPatchGroupAddress1()
    {
        return mMessage.getHex( PATCH_GROUP_ADDRESS_1, 4 );
    }
	
    public String getPatchGroupAddress2()
    {
        return mMessage.getHex( PATCH_GROUP_ADDRESS_2, 4 );
    }
	
    public int getChannelIdentifier1()
    {
        return mMessage.getInt( IDENTIFIER_1 );
    }
    
    public int getChannelNumber1()
    {
        return mMessage.getInt( CHANNEL_1 );
    }

    public String getChannel1()
    {
    	return getChannelIdentifier1() + "-" + getChannelNumber1();
    }

    public int getChannelIdentifier2()
    {
        return mMessage.getInt( IDENTIFIER_2 );
    }
    
    public int getChannelNumber2()
    {
        return mMessage.getInt( CHANNEL_2 );
    }

    public String getChannel2()
    {
    	return getChannelIdentifier2() + "-" + getChannelNumber2();
    }

	@Override
    public void setIdentifierMessage( int identifier, IBandIdentifier message )
    {
		if( identifier == getChannelIdentifier1() )
		{
			mIdentifierUpdate1 = message;
		}
		
		if( identifier == getChannelIdentifier2() )
		{
			mIdentifierUpdate2 = message;
		}
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers = new int[ 2 ];
		identifiers[ 0 ] = getChannelIdentifier1();
		identifiers[ 1 ] = getChannelIdentifier2();
		
		return identifiers;
    }
    
    public long getDownlinkFrequency1()
    {
    	return calculateDownlink( mIdentifierUpdate1, getChannelNumber1() );
    }
    
    public long getUplinkFrequency1()
    {
    	return calculateUplink( mIdentifierUpdate1, getChannelNumber1() );
    }
    
    public long getDownlinkFrequency2()
    {
    	return calculateDownlink( mIdentifierUpdate2, getChannelNumber2() );
    }
    
    public long getUplinkFrequency2()
    {
    	return calculateUplink( mIdentifierUpdate2, getChannelNumber2() );
    }
    
}
