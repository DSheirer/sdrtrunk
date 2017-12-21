package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public abstract class GroupChannelGrant extends ChannelGrant
{
    public static final int[] GROUP_ADDRESS = { 104,105,106,107,108,109,110,111,
        112,113,114,115,116,117,118,119 };
    
    public static final int[] SOURCE_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };

    public GroupChannelGrant( BinaryMessage message, 
                              DataUnitID duid,
                              AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    public String getGroupAddress()
    {
        return mMessage.getHex( GROUP_ADDRESS, 4 );
    }
    
    public String getSourceAddress()
    {
        return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        if( isEmergency() )
        {
            sb.append( " EMERGENCY" );
        }
        
        sb.append( " SOURCE UNIT:" );
        sb.append( getSourceAddress() );
        
        sb.append( " GROUP:" );
        sb.append( getGroupAddress() );
        
        sb.append( " CHAN:" );
        sb.append( getChannelIdentifier() + "-" + getChannelNumber() );
        
        sb.append( " DN:" + getDownlinkFrequency() );
        
        sb.append( " UP:" + getUplinkFrequency() );

        return sb.toString();
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
        return getGroupAddress();
    }

    @Override
    public Alias getToIDAlias()
    {
        if( mAliasList != null )
        {
            return mAliasList.getTalkgroupAlias( getGroupAddress() );
        }
        
        return null;
    }
}
