package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class ProtectionParameterUpdate extends TSBKMessage
{
    public static final int[] ALGORITHM_ID = { 96,97,98,99,100,101,102,103 };
    public static final int[] KEY_ID = { 104,105,106,107,108,109,110,111,112,
        113,114,115,116,117,118,119 };
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public ProtectionParameterUpdate( BitSetBuffer message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.PROTECTION_PARAMETER_UPDATE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( super.getMessage() );

        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        sb.append( " ALGORITHM:" + getAlgorithmID() );
        
        sb.append( " KEY:" + getKeyID() );
        
        return sb.toString();
    }
    
    public String getAlgorithmID()
    {
        return mMessage.getHex( ALGORITHM_ID, 2 );
    }
    
    public String getKeyID()
    {
        return mMessage.getHex( KEY_ID, 4 );
    }
    
    public String getTargetAddress()
    {
        return mMessage.getHex( TARGET_ADDRESS, 6 );
    }

    @Override
    public String getToID()
    {
        return getTargetAddress();
    }
}
