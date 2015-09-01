package module.decode.p25.message.tsbk.osp.data;

import module.decode.p25.message.tsbk.ServiceMessage;
import module.decode.p25.reference.DataUnitID;
import alias.AliasList;
import bits.BinaryMessage;

public abstract class SNDCPData extends ServiceMessage
{
	public static final int[] NETWORK_SERVICE_ACCESS_POINT_ID = { 84,85,86,87 };
	
    public static final int[] TARGET_ADDRESS = { 96,97,98,99,100,101,102,103,
        104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119 };
    
    public static final int[] SOURCE_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public SNDCPData( BinaryMessage message, 
                                   DataUnitID duid,
                                   AliasList aliasList ) 
    {
    	super( message, duid, aliasList );
    }
    
    public String getNSAPI()
    {
    	return mMessage.getHex( NETWORK_SERVICE_ACCESS_POINT_ID, 1 );
    }
}
