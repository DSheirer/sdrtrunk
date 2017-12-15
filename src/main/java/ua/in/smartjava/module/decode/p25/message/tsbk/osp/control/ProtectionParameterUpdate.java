package ua.in.smartjava.module.decode.p25.message.tsbk.osp.control;

import ua.in.smartjava.module.decode.p25.message.tsbk.TSBKMessage;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Encryption;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class ProtectionParameterUpdate extends TSBKMessage
{
    public static final int[] ALGORITHM_ID = { 96,97,98,99,100,101,102,103 };
    public static final int[] KEY_ID = { 104,105,106,107,108,109,110,111,112,
        113,114,115,116,117,118,119 };
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public ProtectionParameterUpdate( BinaryMessage message, 
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
        
        sb.append( getMessageStub() );

        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        sb.append( " ENCRYPTION ALGORITHM:" + getAlgorithm().name() );
        
        sb.append( " KEY:" + getKeyID() );
        
        return sb.toString();
    }
    
    public Encryption getAlgorithm()
    {
        return Encryption.fromValue( mMessage.getInt( ALGORITHM_ID ) );
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
