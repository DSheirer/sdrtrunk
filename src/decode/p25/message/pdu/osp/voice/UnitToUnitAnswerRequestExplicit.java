package decode.p25.message.pdu.osp.voice;

import crc.CRCP25;
import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.pdu.PDUMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class UnitToUnitAnswerRequestExplicit extends PDUMessage 
{
    /* Service Options */
    public static final int EMERGENCY_FLAG = 128;
    public static final int ENCRYPTED_CHANNEL_FLAG = 129;
    public static final int DUPLEX_MODE = 130;
    public static final int SESSION_MODE = 131;
	
	public static final int[] TARGET_ADDRESS = { 88,89,90,91,92,93,94,95,96,97,
		98,99,100,101,102,103,104,105,106,107,108,109,110,111 };
	public static final int[] WACN = { 168,169,170,171,172,173,174,175,176,177,
		178,179,180,181,182,183,184,185,186,187 };
	public static final int[] SYSTEM_ID = { 188,189,190,191,192,193,194,195,196,
		197,198,199 };
	public static final int[] SOURCE_ID = { 200,201,202,203,204,205,206,207,208,
		209,210,211,212,213,214,215,216,217,218,219,220,221,222,223 };
	public static final int[] MULTIPLE_BLOCK_CRC = { 224,225,226,227,228,229,
		230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,
		248,249,250,251,252,253,254,255 };
	
	public UnitToUnitAnswerRequestExplicit( BitSetBuffer message,
            DataUnitID duid, AliasList aliasList )
    {
	    super( message, duid, aliasList );
	    
	    /* Header block is already error detected/corrected - perform error
	     * detection correction on the intermediate and final data blocks */
	    mCRC[ 1 ] = CRCP25.correctPDU1( mMessage );
    }

    @Override
    public String getEventType()
    {
        return Opcode.UNIT_TO_UNIT_ANSWER_REQUEST.getDescription();
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
        sb.append( getSourceID() );
        
        sb.append( " TO:" );
        sb.append( getTargetAddress() );

        sb.append( " WACN:" + getWACN() );
        
        sb.append( " SYS:" + getSystemID() );
        
        return sb.toString();
    }
    
    public boolean isEmergency()
    {
        return mMessage.get( EMERGENCY_FLAG );
    }
    
    public boolean isEncryptedChannel()
    {
        return mMessage.get( ENCRYPTED_CHANNEL_FLAG );
    }
    
    public DuplexMode getDuplexMode()
    {
        return mMessage.get( DUPLEX_MODE ) ? DuplexMode.FULL : DuplexMode.HALF;
    }

    public SessionMode getSessionMode()
    {
        return mMessage.get( SESSION_MODE ) ? 
                SessionMode.CIRCUIT : SessionMode.PACKET;
    }

	public String getSystemID()
	{
		return mMessage.getHex( SYSTEM_ID, 3 );
	}
	
	public String getWACN()
	{
		return mMessage.getHex( WACN, 5 );
	}
	
    public String getSourceID()
    {
    	return mMessage.getHex( SOURCE_ID, 6 );
    }
    
    public String getTargetAddress()
    {
    	return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
}
