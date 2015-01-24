package decode.p25.message.tsbk;

import alias.AliasList;
import bits.BinaryMessage;
import decode.p25.message.IdentifierProvider;
import decode.p25.message.IdentifierReceiver;
import decode.p25.reference.DataUnitID;

public abstract class ChannelGrant extends ServiceMessage 
								   implements IdentifierReceiver
{
    public static final int[] PRIORITY = { 85,86,87 };
    public static final int[] CHANNEL_IDENTIFIER = { 88,89,90,91 };
    public static final int[] CHANNEL_NUMBER = { 92,93,94,95,96,97,98,99,100,
        101,102,103 };
    
    private IdentifierProvider mIdentifierUpdate;
    
    public ChannelGrant( BinaryMessage message, 
    					 DataUnitID duid,
    					 AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    /**
     * 1 = Lowest, 4 = Default, 7 = Highest
     */
    public int getPriority()
    {
        return mMessage.getInt( PRIORITY );
    }
    
    public int getChannelIdentifier()
    {
        return mMessage.getInt( CHANNEL_IDENTIFIER );
    }
    
    public int getChannelNumber()
    {
        return mMessage.getInt( CHANNEL_NUMBER );
    }

    public String getChannel()
    {
    	return getChannelIdentifier() + "-" + getChannelNumber();
    }

	@Override
    public void setIdentifierMessage( int identifier, IdentifierProvider message )
    {
		mIdentifierUpdate = message;
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers = new int[ 1 ];
		identifiers[ 0 ] = getChannelIdentifier();
		
		return identifiers;
    }
    
    public long getDownlinkFrequency()
    {
    	return calculateDownlink( mIdentifierUpdate, getChannelNumber() );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mIdentifierUpdate, getChannelNumber() );
    }

}
