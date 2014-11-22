package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class SecondaryControlChannelBroadcast extends TSBKMessage
{
    public static final int[] RFSS_ID = { 80,81,82,83,84,85,86,87 };
    public static final int[] SITE_ID = { 88,89,90,91,92,93,94,95 };
    public static final int[] CHANNEL_1 = { 96,97,98,99,100,101,102,103,104,105,
    	106,107,108,109,110,111 };
    public static final int[] SYSTEM_SERVICE_CLASS_1 = { 112,113,114,115,116,
    	117,118,119 };
    public static final int[] CHANNEL_2 = { 120,121,122,123,124,125,126,127,
    	128,129,130,131,132,133,134,135 };
    public static final int[] SYSTEM_SERVICE_CLASS_2 = { 136,137,138,139,140,
    	141,142,143 };
    
    public SecondaryControlChannelBroadcast( BitSetBuffer message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.SECONDARY_CONTROL_CHANNEL_BROADCAST.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );
        
        sb.append( " RFSS:" + getRFSS() );
        
        sb.append( " SITE:" + getSiteID() );
        
        sb.append( " CHAN 1:" + getChannel1() );
        
        sb.append( " SVC CLASS 1:" + 
        		SystemService.toString( getSystemServiceClass1() ) );

        if( hasChannel2() )
        {
            sb.append( " CHAN 2:" + getChannel2() );
            
            sb.append( " SVC CLASS 2:" + 
            		SystemService.toString( getSystemServiceClass2() ) );
        }
        else
        {
        	sb.append( " CHAN 2: empty" );
        }
        return sb.toString();
    }
    
    public String getRFSS()
    {
        return mMessage.getHex( RFSS_ID, 2 );
    }
    
    public String getSiteID()
    {
        return mMessage.getHex( SITE_ID, 2 );
    }
    
    public int getChannel1()
    {
        return mMessage.getInt( CHANNEL_1 );
    }
    
    public int getSystemServiceClass1()
    {
        return mMessage.getInt( SYSTEM_SERVICE_CLASS_1 );
    }
    
    public int getChannel2()
    {
        return mMessage.getInt( CHANNEL_2 );
    }
    
    public int getSystemServiceClass2()
    {
        return mMessage.getInt( SYSTEM_SERVICE_CLASS_2 );
    }

    public boolean hasChannel2()
    {
    	return getSystemServiceClass2() != 0;
    }
}
