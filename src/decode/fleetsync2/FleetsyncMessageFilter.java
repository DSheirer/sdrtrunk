package decode.fleetsync2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import message.Message;
import filter.Filter;
import filter.FilterElement;

public class FleetsyncMessageFilter extends Filter<Message>
{
	private HashMap<FleetsyncMessageType, FilterElement<FleetsyncMessageType>> mElements = 
			new HashMap<FleetsyncMessageType, FilterElement<FleetsyncMessageType>>();
	
	public FleetsyncMessageFilter()
	{
		super( "Fleetsync Message Filter" );

		for( FleetsyncMessageType type: FleetsyncMessageType.values() )
		{
			if( type != FleetsyncMessageType.UNKNOWN )
			{
				mElements.put( type, new FilterElement<FleetsyncMessageType>( type ) );
			}
		}
	}
	
	@Override
    public boolean passes( Message message )
    {
		if( mEnabled && canProcess( message ) )
		{
			FleetsyncMessage fleet = (FleetsyncMessage)message;
			
			if( mElements.containsKey( fleet.getMessageType() ) )
			{
				return mElements.get( fleet.getMessageType() ).isEnabled();
			}
		}
		
	    return false;
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof FleetsyncMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
	    return new ArrayList<FilterElement<?>>( mElements.values() );
    }
}
