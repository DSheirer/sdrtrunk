package io.github.dsheirer.module.decode.mdc1200;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MDCMessageFilter extends Filter<Message>
{
	private HashMap<MDCMessageType, FilterElement<MDCMessageType>> mElements = 
			new HashMap<MDCMessageType, FilterElement<MDCMessageType>>();
	
	public MDCMessageFilter()
	{
		super( "MDC-1200 Message Filter" );

		for( MDCMessageType type: MDCMessageType.values() )
		{
			if( type != MDCMessageType.UNKNOWN )
			{
				mElements.put( type, new FilterElement<MDCMessageType>( type ) );
			}
		}
	}
	
	@Override
    public boolean passes( Message message )
    {
		if( mEnabled && canProcess( message ) )
		{
			MDCMessage mdc = (MDCMessage)message;
			
			if( mElements.containsKey( mdc.getMessageType() ) )
			{
				return mElements.get( mdc.getMessageType() ).isEnabled();
			}
		}
		
	    return false;
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof MDCMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
	    return new ArrayList<FilterElement<?>>( mElements.values() );
    }
}
