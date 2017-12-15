package ua.in.smartjava.module.decode.p25.message.tsbk.osp.control;

import ua.in.smartjava.module.decode.p25.message.tsbk.TSBKMessage;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class StatusUpdate extends TSBKMessage
{
	public static final int[] USER_STATUS = { 72,73,74,75,88,89,90,91 };
	public static final int[] UNIT_STATUS = { 92,93,94,95,96,97,98,99 };
    public static final int[] TARGET_ADDRESS = { 112,113,114,115,116,117,118,
    	119,120,121,122,123,136,137,138,139,140,141,142,143,144,145,146,147 };
    public static final int[] SOURCE_ADDRESS = { 160,161,162,163,164,165,166,
    	167,168,169,170,171,184,185,186,187,188,189,190,190,192,193,194,195 };
    
    public StatusUpdate( BinaryMessage message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
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

        sb.append( " STATUS USER:" + getUserStatus() );
        sb.append( " UNIT:" + getUnitStatus() );
        sb.append( " SRC ADDR: " + getSourceAddress() );
        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        return sb.toString();
    }
    
    public String getUserStatus()
    {
    	return mMessage.getHex( USER_STATUS, 2 );
    }
    
    public String getUnitStatus()
    {
    	return mMessage.getHex( UNIT_STATUS, 2 );
    }
    
    public String getSourceAddress()
    {
        return mMessage.getHex( SOURCE_ADDRESS, 6 );
    }
    
    @Override
    public String getFromID()
    {
        return getSourceAddress();
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
