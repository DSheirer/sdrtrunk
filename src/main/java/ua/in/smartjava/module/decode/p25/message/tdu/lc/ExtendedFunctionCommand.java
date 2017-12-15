package ua.in.smartjava.module.decode.p25.message.tdu.lc;

import ua.in.smartjava.module.decode.p25.reference.ExtendedFunction;
import ua.in.smartjava.module.decode.p25.reference.LinkControlOpcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedFunctionCommand extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ExtendedFunctionCommand.class );
	public static final int[] EXTENDED_FUNCTION = { 72,73,74,75,88,89,90,91,92,
		93,94,95,96,97,98,99 };
	public static final int[] ARGUMENT = { 112,113,114,115,116,117,118,119,120,
		121,122,123,136,137,138,139,140,141,142,143,144,145,145,147 };
    public static final int[] TARGET_ADDRESS = { 160,161,162,163,164,165,166,
    	167,168,169,170,171,184,185,186,187,188,189,190,190,192,193,194,195 };
	
	public ExtendedFunctionCommand( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.EXTENDED_FUNCTION_COMMAND.getDescription();
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
