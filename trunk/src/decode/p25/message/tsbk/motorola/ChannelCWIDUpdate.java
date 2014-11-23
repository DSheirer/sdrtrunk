package decode.p25.message.tsbk.motorola;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import decode.p25.message.tsbk.osp.control.IdentifierUpdateReceiver;
import decode.p25.reference.DataUnitID;

public class ChannelCWIDUpdate extends MotorolaTSBKMessage 
						implements IdentifierUpdateReceiver
{
    public static final int[] CHARACTER_1 = { 80,81,82,83,84,85 };
    public static final int[] CHARACTER_2 = { 86,87,88,89,90,91 };
    public static final int[] CHARACTER_3 = { 92,93,94,95,96,97 };
    public static final int[] CHARACTER_4 = { 98,99,100,101,102,103 };
    public static final int[] CHARACTER_5 = { 104,105,106,107,108,109 };
    public static final int[] CHARACTER_6 = { 110,111,112,113,114,115 };
    public static final int[] CHARACTER_7 = { 116,117,118,119,120,121 };
    public static final int[] CHARACTER_8 = { 122,123,124,125,126,127 };
    
    public static final int[] IDENTIFIER = { 128,129,130,131 };
    public static final int[] CHANNEL = { 132,133,134,135,136,137,138,139,140,
    	141,142,143 };
    
    private IdentifierUpdate mIdentifierUpdate;
    
    public ChannelCWIDUpdate( BitSetBuffer message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
    	super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return MotorolaOpcode.CHANNEL_CWID_UPDATE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " CHAN:" + getIdentifier() + "-" + getChannel() );
        
        sb.append( " CWID:" + getCWID() );
        
        sb.append( " DN:" + getDownlinkFrequency() );
        
        sb.append( " UP:" + getUplinkFrequency() );
        
        return sb.toString();
    }
    
    
    public String getCWID()
    {
    	StringBuilder sb = new StringBuilder();
    	
    	int char1 = mMessage.getInt( CHARACTER_1 );
    	
    	if( char1 != 0 )
    	{
    		sb.append( (char)( char1 + 46 ) );
    	}
    	else
    	{
    		return null;
    	}
    	
    	int char2 = mMessage.getInt( CHARACTER_2 );
    	
    	if( char2 != 0 )
    	{
    		sb.append( (char)( char2 + 46 ) );
    	}
    	else
    	{
    		return sb.toString();
    	}

    	int char3 = mMessage.getInt( CHARACTER_3 );
    	
    	if( char3 != 0 )
    	{
    		sb.append( (char)( char3 + 46 ) );
    	}
    	else
    	{
    		return sb.toString();
    	}

    	int char4 = mMessage.getInt( CHARACTER_4 );
    	
    	if( char4 != 0 )
    	{
    		sb.append( (char)( char4 + 46 ) );
    	}
    	else
    	{
    		return sb.toString();
    	}

    	int char5 = mMessage.getInt( CHARACTER_5 );
    	
    	if( char5 != 0 )
    	{
    		sb.append( (char)( char5 + 46 ) );
    	}
    	else
    	{
    		return sb.toString();
    	}

    	int char6 = mMessage.getInt( CHARACTER_6 );
    	
    	if( char6 != 0 )
    	{
    		sb.append( (char)( char6 + 46 ) );
    	}
    	else
    	{
    		return sb.toString();
    	}

    	int char7 = mMessage.getInt( CHARACTER_7 );
    	
    	if( char7 != 0 )
    	{
    		sb.append( (char)( char7 + 46 ) );
    	}
    	else
    	{
    		return sb.toString();
    	}

    	int char8 = mMessage.getInt( CHARACTER_8 );
    	
    	if( char8 != 0 )
    	{
    		sb.append( (char)( char8 + 46 ) );
    	}
    	
		return sb.toString();
    }
    
    public int getIdentifier()
    {
    	return mMessage.getInt( IDENTIFIER );
    }
    
    public int getChannel()
    {
    	return mMessage.getInt( CHANNEL );
    }
    
    public long getUplinkFrequency()
    {
    	return calculateUplink( mIdentifierUpdate, getChannel() );
    }
    
    public long getDownlinkFrequency()
    {
    	return calculateDownlink( mIdentifierUpdate, getChannel() );
    }

	@Override
    public void setIdentifierMessage( int identifier, IdentifierUpdate message )
    {
		mIdentifierUpdate = message;
    }

	@Override
    public int[] getIdentifiers()
    {
		int[] identifiers = new int[ 1 ];
		
		identifiers[ 0 ] = getIdentifier();
		
	    return identifiers;
    }
}
