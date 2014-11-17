package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;
import decode.p25.reference.Response;

public class UnitRegistrationResponse extends TSBKMessage
{
	public static final int[] REGISTRATION_RESPONSE = { 82,83 };
	public static final int[] SYSTEM_ID = { 84,85,86,87,88,89,90,91,92,93,94,95 };
    public static final int[] SOURCE_ID = { 96,97,98,99,100,101,102,103,
        104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119 };
    public static final int[] SOURCE_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public UnitRegistrationResponse( BitSetBuffer message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.UNIT_REGISTRATION_RESPONSE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( super.getMessage() );
        
        sb.append( " REGISTRATION:" + getResponse().name() );
        sb.append( " SYSTEM:" + getSystemID() );
        sb.append( " SRC ID: " + getSourceID() );
        sb.append( " SRC ADDR: " + getSourceAddress() );
        
        return sb.toString();
    }
    
    public Response getResponse()
    {
    	return Response.fromValue( mMessage.getInt( REGISTRATION_RESPONSE ) );
    }
    
    public String getSystemID()
    {
    	return mMessage.getHex( SYSTEM_ID, 3 );
    }

    public String getSourceID()
    {
        return mMessage.getHex( SOURCE_ID, 6 );
    }
    
    public String getSourceAddress()
    {
        return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
    
    @Override
    public String getToID()
    {
        return getSourceAddress();
    }
}
