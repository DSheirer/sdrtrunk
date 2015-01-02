package decode.p25.message.tdu.lc;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decode.p25.reference.LinkControlOpcode;
import decode.p25.reference.Service;

public class SystemServiceBroadcast extends TDULinkControlMessage
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( SystemServiceBroadcast.class );
	public static final int[] REQUEST_PRIORITY_LEVEL = { 96,97,98,99 };
	public static final int[] AVAILABLE_SERVICES = { 112,113,114,115,116,117,118,
		119,120,121,122,123,136,137,138,139,140,141,142,143,144,145,146,147 };
	public static final int[] SUPPORTED_SERVICES = { 160,161,162,163,164,165,
		166,167,168,169,170,171,184,185,186,187,188,189,190,191,192,193,194,195 };
	
	public SystemServiceBroadcast( TDULinkControlMessage source )
	{
		super( source );
	}
	
    @Override
    public String getEventType()
    {
        return LinkControlOpcode.SYSTEM_SERVICE_BROADCAST.getDescription();
    }

	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
        sb.append( " SERVICES AVAILABLE " );
        
        sb.append( getAvailableServices() );

        sb.append( " SUPPORTED " );
        
        sb.append( getSupportedServices() );
		
		return sb.toString();
	}

    public List<Service> getAvailableServices()
    {
    	long bitmap = mMessage.getLong( AVAILABLE_SERVICES );
    	
    	return Service.getServices( bitmap );
    }
    
    public List<Service> getSupportedServices()
    {
    	long bitmap = mMessage.getLong( SUPPORTED_SERVICES );
    	
    	return Service.getServices( bitmap );
    }
}
