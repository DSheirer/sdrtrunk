package ua.in.smartjava.module.decode.p25.message.tsbk.osp.data;

import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class SNDCPDataPageRequest extends SNDCPData
{
	public static final int[] DATA_ACCESS_CONTROL = { 104,105,106,107,108,109,
		110,111,112,113,114,115,116,117,118,119 };
	
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public SNDCPDataPageRequest( BinaryMessage message, 
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

        sb.append( " TGT:" );
        sb.append( getTargetAddress() );
        
        sb.append( " DAC:" );
        sb.append( getDataAccessControl() );
        
        return sb.toString();
    }
    
    public String getDataAccessControl()
    {
    	return mMessage.getHex( DATA_ACCESS_CONTROL, 4 );
    }
    
    public String getTargetAddress()
    {
        return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
}
