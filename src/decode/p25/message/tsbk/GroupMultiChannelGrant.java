package decode.p25.message.tsbk;

import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;

public abstract class GroupMultiChannelGrant extends ChannelGrant
{
    public static final int[] CHANNEL_ID_1 = { 80,81,82,83 };
    public static final int[] CHANNEL_NUMBER_1 = { 84,85,86,87,
        88,89,90,91,92,93,94,95 };
    public static final int[] GROUP_ADDRESS_1 = { 96,97,98,99,100,101,102,103 };
    
    public static final int[] CHANNEL_ID_2 = { 104,105,106,107 };
    public static final int[] CHANNEL_NUMBER_2 = { 108,109,110,111,
        112,113,114,115,116,117,118,119 };
    public static final int[] GROUP_ADDRESS_2 = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135 };

    public GroupMultiChannelGrant( BitSetBuffer message, 
                              DataUnitID duid,
                              AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( super.getMessage() );
        
        sb.append( " CHAN1:" );
        sb.append( getChannelID1() + "/" + getChannelNumber1() );
        
        sb.append( " GRP1:" );
        sb.append( getGroupAddress1() );
        
        sb.append( " CHAN2:" );
        sb.append( getChannelID2() + "/" + getChannelNumber2() );
        
        sb.append( " GRP2:" );
        sb.append( getGroupAddress2() );
        
        return sb.toString();
    }
    
    public int getChannelID1()
    {
        return mMessage.getInt( CHANNEL_ID_1 );
    }
    
    public int getChannelNumber1()
    {
        return mMessage.getInt( CHANNEL_NUMBER_1 );
    }
    
    public String getGroupAddress1()
    {
        return mMessage.getHex( GROUP_ADDRESS_1, 4 );
    }
    
    public int getChannelID2()
    {
        return mMessage.getInt( CHANNEL_ID_2 );
    }
    
    public int getChannelNumber2()
    {
        return mMessage.getInt( CHANNEL_NUMBER_2 );
    }
    
    public String getGroupAddress2()
    {
        return mMessage.getHex( GROUP_ADDRESS_2, 4 );
    }
    
    @Override
    public String getFromID()
    {
        return getGroupAddress1();
    }

    @Override
    public Alias getFromIDAlias()
    {
        if( mAliasList != null )
        {
            return mAliasList.getTalkgroupAlias( getFromID() );
        }
        
        return null;
    }

    @Override
    public String getToID()
    {
        return getGroupAddress2();
    }

    @Override
    public Alias getToIDAlias()
    {
        if( mAliasList != null )
        {
            return mAliasList.getTalkgroupAlias( getToID() );
        }
        
        return null;
    }
}
