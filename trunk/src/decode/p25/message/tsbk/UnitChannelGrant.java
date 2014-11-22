package decode.p25.message.tsbk;

import alias.Alias;
import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.reference.DataUnitID;

public abstract class UnitChannelGrant extends ChannelGrant
{
    public static final int[] TARGET_ADDRESS = { 96,97,98,99,100,101,102,103,
        104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119 };
    
    public static final int[] SOURCE_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public UnitChannelGrant( BitSetBuffer message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        if( isEmergency() )
        {
            sb.append( " EMERGENCY" );
        }
        
        sb.append( " FROM:" );
        sb.append( getSourceAddress() );
        
        sb.append( " TO:" );
        sb.append( getTargetAddress() );
        
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
    
    @Override
    public String getFromID()
    {
        return getSourceAddress();
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
        return getTargetAddress();
    }
}
