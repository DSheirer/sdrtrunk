package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.IdentifierProvider;
import decode.p25.message.tsbk.osp.control.IdentifierProviderReceiver;
import decode.p25.reference.DataUnitID;

public abstract class GroupMultiChannelGrant extends ChannelGrant
									implements IdentifierProviderReceiver
{
    public static final int[] CHANNEL_IDENTIFIER_1 = { 80,81,82,83 };
    public static final int[] CHANNEL_NUMBER_1 = { 84,85,86,87,
        88,89,90,91,92,93,94,95 };
    public static final int[] GROUP_ADDRESS_1 = { 96,97,98,99,100,101,102,103,
    	104,105,106,107,108,109,110,111 };
    
    public static final int[] CHANNEL_IDENTIFIER_2 = { 112,113,114,115 };
    public static final int[] CHANNEL_NUMBER_2 = { 116,117,118,119,120,121,122,
    	123,124,125,126,127 };
    public static final int[] GROUP_ADDRESS_2 = { 128,129,130,131,132,133,134,
    	135,136,137,138,139,140,141,142,143 };
    
    private IdentifierProvider mIdentifierUpdate1;
    private IdentifierProvider mIdentifierUpdate2;

    public GroupMultiChannelGrant( BitSetBuffer message, 
                              DataUnitID duid,
                              AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        sb.append( " CHAN1:" );
        sb.append( getChannelIdentifier1() + "/" + getChannelNumber1() );
        
        sb.append( " GRP1:" );
        sb.append( getGroupAddress1() );
        
        if( hasChannelNumber2() )
        {
            sb.append( " CHAN2:" );
            sb.append( getChannelIdentifier2() + "/" + getChannelNumber2() );
            
            sb.append( " GRP2:" );
            sb.append( getGroupAddress2() );
        }
        
        return sb.toString();
    }
    
    public int getChannelIdentifier1()
    {
        return mMessage.getInt( CHANNEL_IDENTIFIER_1 );
    }
    
    public int getChannelNumber1()
    {
        return mMessage.getInt( CHANNEL_NUMBER_1 );
    }
    
    public String getChannel1()
    {
    	return getChannelIdentifier1() + "-" + getChannelNumber1();
    }
    
    public String getGroupAddress1()
    {
        return mMessage.getHex( GROUP_ADDRESS_1, 4 );
    }
    
    public int getChannelIdentifier2()
    {
        return mMessage.getInt( CHANNEL_IDENTIFIER_2 );
    }
    
    public int getChannelNumber2()
    {
        return mMessage.getInt( CHANNEL_NUMBER_2 );
    }
    
    public String getChannel2()
    {
    	return getChannelIdentifier2() + "-" + getChannelNumber2();
    }
    
    public String getGroupAddress2()
    {
        return mMessage.getHex( GROUP_ADDRESS_2, 4 );
    }
    
    public boolean hasChannelNumber2()
    {
    	return mMessage.getInt( CHANNEL_NUMBER_2 ) != 
    		   mMessage.getInt( CHANNEL_NUMBER_1 );
    }
    
    @Override
    public String getFromID()
    {
        return getGroupAddress1();
    }

    @Override
    public String getToID()
    {
        return getGroupAddress2();
    }
    
	@Override
    public void setIdentifierMessage( int identifier, IdentifierProvider message )
    {
		if( identifier == getChannelIdentifier1() )
		{
			mIdentifierUpdate1 = message;
		}

		if( hasChannelNumber2() && identifier == getChannelIdentifier2() )
		{
			mIdentifierUpdate2 = message;
		}
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers;
		
		if( hasChannelNumber2() )
		{
			identifiers = new int[ 2 ];
			identifiers[ 0 ] = getChannelIdentifier1();
			identifiers[ 1 ] = getChannelIdentifier2();
		}
		else
		{
			identifiers = new int[ 1 ];
			identifiers[ 0 ] = getChannelIdentifier1();
		}
		
		return identifiers;
    }
    
    public long getDownlinkFrequency1()
    {
    	return calculateDownlink( mIdentifierUpdate1, getChannelNumber() );
    }
    
    public long getUplinkFrequency1()
    {
    	return calculateUplink( mIdentifierUpdate1, getChannelNumber() );
    }
    
    public long getDownlinkFrequency2()
    {
    	return calculateDownlink( mIdentifierUpdate2, getChannelNumber() );
    }
    
    public long getUplinkFrequency2()
    {
    	return calculateUplink( mIdentifierUpdate2, getChannelNumber() );
    }
    
}
