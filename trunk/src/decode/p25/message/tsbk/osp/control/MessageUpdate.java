package decode.p25.message.tsbk.osp.control;

import alias.AliasList;
import bits.BitSetBuffer;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.DataUnitID;
import decode.p25.reference.Opcode;

public class MessageUpdate extends TSBKMessage
{
	public static final int[] SHORT_DATA_MESSAGE = { 80,81,82,83,84,85,86,87,88,89,90,91,
		92,93,94,95 };
    public static final int[] TARGET_ADDRESS = { 96,97,98,99,100,101,102,103,
    	104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119 };
    public static final int[] SOURCE_ADDRESS = { 120,121,122,123,124,125,126,
    	127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
	      
    public MessageUpdate( BitSetBuffer message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.MESSAGE_UPDATE.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " MSG:" + getShortDataMessage() );
        sb.append( " SRC ADDR: " + getSourceAddress() );
        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        return sb.toString();
    }
    
    public String getShortDataMessage()
    {
        return mMessage.getHex( SHORT_DATA_MESSAGE, 4 );
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
