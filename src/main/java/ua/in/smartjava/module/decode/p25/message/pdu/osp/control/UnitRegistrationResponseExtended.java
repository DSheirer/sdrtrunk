package ua.in.smartjava.module.decode.p25.message.pdu.osp.control;

import ua.in.smartjava.module.decode.p25.message.pdu.PDUMessage;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.module.decode.p25.reference.Response;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;
import ua.in.smartjava.edac.CRCP25;

public class UnitRegistrationResponseExtended extends PDUMessage 
{
	public static final int[] ASSIGNED_SOURCE_ADDRESS = { 88,89,90,91,92,
		93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111 };
	public static final int[] WACN = { 128,129,130,131,132,133,134,135,
		136,137,138,139,140,141,142,143,160,161,162,163 };
	public static final int[] SYSTEM_ID = { 164,165,166,167,168,169,170,
		171,172,173,174,175	};
	public static final int[] SOURCE_ID = { 176,177,178,179,180,181,182,183,184,
		185,186,187,188,189,190,191,192,193,194,195,196,197,198,199 };
	public static final int[] SOURCE_ADDRESS = { 200,201,202,203,204,205,206,
		207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223 };
	public static final int[] UNIT_REGISTRATION_RESPONSE_VALUE = { 230,231 }; 
	public static final int[] MULTIPLE_BLOCK_CRC = { 320,321,322,323,324,325,
		326,327,328,329,330,331,332,333,334,335,336,337,338,339,340,341,342,
		343,344,345,346,347,348,349,350,351 };
	
	public UnitRegistrationResponseExtended( BinaryMessage message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
	    
	    /* Header block is already error detected/corrected - perform error
	     * detection correction on the intermediate and final data blocks */
	    mMessage = CRCP25.correctPDU1( mMessage );
	    mCRC[ 1 ] = mMessage.getCRC();
    }

    @Override
    public String getEventType()
    {
        return Opcode.UNIT_REGISTRATION_RESPONSE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( "REGISTRATION:" );
        sb.append( getResponse().name() );
        
        sb.append( " SRC ID:" );
        sb.append( getSourceID() );

        sb.append( " ADDR:" );
        sb.append( getSourceAddress() );
        
        sb.append( " WACN:" );
        sb.append( getWACN() );
        
        sb.append( " SYS:" );
        sb.append( getSystemID() );
        
        return sb.toString();
    }
    
    public String getAssignedSourceAddress()
    {
    	return mMessage.getHex( ASSIGNED_SOURCE_ADDRESS, 6 );
    }
    
    public String getWACN()
    {
    	return mMessage.getHex( WACN, 5 );
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

    public Response getResponse()
    {
    	return Response.fromValue( 
    			mMessage.getInt( UNIT_REGISTRATION_RESPONSE_VALUE ) );
    }
}
