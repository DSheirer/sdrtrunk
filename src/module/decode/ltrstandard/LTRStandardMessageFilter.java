package module.decode.ltrstandard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import message.Message;
import message.MessageType;
import filter.Filter;
import filter.FilterElement;

public class LTRStandardMessageFilter extends Filter<Message>
{
	private HashMap<MessageType, FilterElement<MessageType>> mElements = 
			new HashMap<MessageType, FilterElement<MessageType>>();
	
	public LTRStandardMessageFilter()
	{
		super( "LTR Message Filter" );

		mElements.put( MessageType.CA_STRT, 
				new FilterElement<MessageType>( MessageType.CA_STRT ) );
		mElements.put( MessageType.CA_ENDD, 
				new FilterElement<MessageType>( MessageType.CA_ENDD ) );
		mElements.put( MessageType.SY_IDLE, 
				new FilterElement<MessageType>( MessageType.SY_IDLE ) );
	}
	
	@Override
    public boolean passes( Message message )
    {
		if( mEnabled && canProcess( message ) )
		{
			LTRStandardMessage ltr = (LTRStandardMessage)message;
			
			if( mElements.containsKey( ltr.getMessageType() ) )
			{
				return mElements.get( ltr.getMessageType() ).isEnabled();
			}
		}
		
	    return false;
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof LTRStandardMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
	    return new ArrayList<FilterElement<?>>( mElements.values() );
    }
}
