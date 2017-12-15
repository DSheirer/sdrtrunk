package ua.in.smartjava.module.decode.p25.message.tsbk.osp.control;

import ua.in.smartjava.module.decode.p25.message.tsbk.TSBKMessage;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.module.decode.p25.reference.ExtendedFunction;
import ua.in.smartjava.module.decode.p25.reference.Opcode;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;

public class ExtendedFunctionCommand extends TSBKMessage
{
    public static final int[] EXTENDED_FUNCTION = { 80,81,82,83,84,85,86,87,
        88,89,90,91,92,93,94,95 };
    public static final int[] ARGUMENT = { 96,97,98,99,100,101,102,103,104,105,
    	106,107,108,109,110,111,112,113,114,115,116,117,118,119 };
    public static final int[] TARGET_ADDRESS = { 120,121,122,123,124,125,126,
        127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143 };
    
    public ExtendedFunctionCommand( BinaryMessage message, 
                                DataUnitID duid,
                                AliasList aliasList ) 
    {
        super( message, duid, aliasList );
    }

    @Override
    public String getEventType()
    {
        return Opcode.EXTENDED_FUNCTION_COMMAND.getDescription();
    }
    
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( getMessageStub() );

        sb.append( " EXTENDED FUNCTION:" + getExtendedFunction().getLabel() );

        sb.append( " ARGUMENT: " + getTargetAddress() );

        sb.append( " TGT ADDR: " + getTargetAddress() );
        
        return sb.toString();
    }
    
    public ExtendedFunction getExtendedFunction()
    {
        return ExtendedFunction.fromValue( mMessage.getInt( EXTENDED_FUNCTION ) );
    }
    
    public String getArgument()
    {
    	return mMessage.getHex( ARGUMENT, 6 );
    }
    
    public String getSourceAddress()
    {
    	switch( getExtendedFunction() )
    	{
			case RADIO_CHECK:
			case RADIO_CHECK_ACK:
			case RADIO_DETACH:
			case RADIO_DETACH_ACK:
			case RADIO_INHIBIT:
			case RADIO_INHIBIT_ACK:
			case RADIO_UNINHIBIT:
			case RADIO_UNINHIBIT_ACK:
				return getArgument();
			default:
				break;
    	}
    	
    	return null;
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
