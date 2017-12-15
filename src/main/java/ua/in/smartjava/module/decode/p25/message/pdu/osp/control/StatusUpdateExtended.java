package ua.in.smartjava.module.decode.p25.message.pdu.osp.control;

import ua.in.smartjava.module.decode.p25.message.pdu.PDUMessage;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;
import ua.in.smartjava.edac.CRCP25;

public class StatusUpdateExtended extends PDUMessage 
{
	public static final int[] TARGET_ADDRESS = { 88,89,90,91,92,93,94,95,96,97,
		98,99,100,101,102,103,104,105,106,107,108,109,110,111 };
	public static final int[] SOURCE_WACN = { 128,129,130,131,132,133,134,135,136,137,
		138,139,140,141,142,143,160,161,162,163 };
	public static final int[] SOURCE_SYSTEM_ID = { 164,165,166,167,168,169,170,171,172,
		173,174,175	};
	public static final int[] SOURCE_ID = { 176,177,178,179,180,181,182,183,184,
		185,186,187,188,189,190,191,192,193,194,195,196,197,198,199 };
	public static final int[] UNIT_STATUS = { 200,201,202,203,204,205,206,207 };
	public static final int[] USER_STATUS = { 208,209,210,211,212,213,214,215 };
	public static final int[] MULTIPLE_BLOCK_CRC = { 224,225,226,227,228,229,
		230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,
		248,249,250,251,252,253,254,255 };
	
	public StatusUpdateExtended( BinaryMessage message,
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
        return Opcode.STATUS_UPDATE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " TO:" );
        sb.append( getTargetAddress() );
        
        sb.append( " FROM:" );
        sb.append( getSourceID() );
        
        sb.append( " WACN:" );
        sb.append( getSourceWACN() );
        
        sb.append( " SYS:" );
        sb.append( getSourceSystemID() );

        sb.append( " STATUS USER:" + getUserStatus() );
        sb.append( " UNIT:" + getUnitStatus() );
  
        return sb.toString();
    }
    
    public String getTargetAddress()
    {
    	return mMessage.getHex( TARGET_ADDRESS, 6 );
    }
    
    public String getSourceWACN()
    {
    	return mMessage.getHex( SOURCE_WACN, 5 );
    }
    
    public String getSourceSystemID()
    {
    	return mMessage.getHex( SOURCE_SYSTEM_ID, 3 );
    }
    
    public String getSourceID()
    {
    	return mMessage.getHex( SOURCE_ID, 6 );
    }
    
    public String getUserStatus()
    {
    	return mMessage.getHex( USER_STATUS, 2 );
    }
    
    public String getUnitStatus()
    {
    	return mMessage.getHex( UNIT_STATUS, 2 );
    }
    
}
