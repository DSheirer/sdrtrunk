package module.decode.p25.message.pdu.osp.control;

import module.decode.p25.message.pdu.PDUMessage;
import module.decode.p25.reference.DataUnitID;
import module.decode.p25.reference.Encryption;
import module.decode.p25.reference.Opcode;
import alias.AliasList;
import bits.BinaryMessage;
import edac.CRCP25;

public class ProtectionParameterBroadcast extends PDUMessage 
{
	public static final int[] TARGET_ADDRESS = { 88,89,90,91,92,93,94,95,96,97,
		98,99,100,101,102,103,104,105,106,107,108,109,110,111 };
	public static final int[] ENCRYPTION_TYPE = { 128,129,130,131,132,133,134,
		135 };
	public static final int[] ALGORITHM_ID = { 136,137,138,139,140,141,142,143 };
	public static final int[] KEY_ID = { 160,161,162,163,164,165,166,167,168,
		169,170,171,172,173,174,175 };
	public static final int[] INBOUND_INITIALIZATION_VECTOR_A = { 176,177,178,
		179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,
		197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,
		215 };
	public static final int[] INBOUND_INITIALIZATION_VECTOR_B = { 216,217,218,
		219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,
		237,238,239,240,241,242,243,244,245,246,247 };
	public static final int[] OUTBOUND_INITIALIZATION_VECTOR_A = { 248,249,250,
		251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,
		269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,
		287 };
	public static final int[] OUTBOUND_INITIALIZATION_VECTOR_B = { 288,289,290,
		291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,
		309,310,311,312,313,314,315,316,317,318,319 };
	public static final int[] MULTIPLE_BLOCK_CRC = { 320,321,322,323,324,325,
		326,327,328,329,330,331,332,333,334,335,336,337,338,339,340,341,342,
		343,344,345,346,347,348,349,350,351 };
	
	public ProtectionParameterBroadcast( BinaryMessage message,
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
        return Opcode.PROTECTION_PARAMETER_BROADCAST.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " TO:" );
        sb.append( getTargetAddress() );
        
        sb.append( " ENCRYPTION:" );
        sb.append( getEncryptionType().name() );
        
        sb.append( " ALGORITHM ID:" );
        sb.append( getAlgorithmID() );
        
        sb.append( " KEY ID:" );
        sb.append( getKeyID() );
        
        sb.append( " INBOUND IV:" );
        sb.append( getInboundInitializationVector() );
        
        sb.append( " OUTBOUND IV:" );
        sb.append( getOutboundInitializationVector() );

        return sb.toString();
    }
    
    public String getTargetAddress()
    {
    	return mMessage.getHex( TARGET_ADDRESS, 6 );
    }

    public Encryption getEncryptionType()
    {
    	return Encryption.fromValue( mMessage.getInt( ENCRYPTION_TYPE ) );
    }
    
    public String getAlgorithmID()
    {
    	return mMessage.getHex( ALGORITHM_ID, 2 );
    }
    
    public String getKeyID()
    {
    	return mMessage.getHex( KEY_ID, 4 );
    }
    
    public String getInboundInitializationVector()
    {
    	return mMessage.getHex( INBOUND_INITIALIZATION_VECTOR_A, 10 ) +
    		   mMessage.getHex( INBOUND_INITIALIZATION_VECTOR_B, 8 );
    }
    
    public String getOutboundInitializationVector()
    {
    	return mMessage.getHex( OUTBOUND_INITIALIZATION_VECTOR_A, 10 ) +
    		   mMessage.getHex( OUTBOUND_INITIALIZATION_VECTOR_B, 8 );
    }
}
