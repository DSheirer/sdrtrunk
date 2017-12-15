package ua.in.smartjava.module.decode.mpt1327;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.mpt1327.MPT1327Message.MPTMessageType;
import ua.in.smartjava.filter.Filter;
import ua.in.smartjava.filter.FilterElement;

public class MPT1327MessageFilter extends Filter<Message>
{
	private HashMap<MPTMessageType,FilterElement<MPTMessageType>> mFilterElements = 
			new HashMap<MPTMessageType,FilterElement<MPTMessageType>>();

	public MPT1327MessageFilter()
	{
		super( "MPT1327 Message Type Filter" );

		for( MPTMessageType type: MPT1327Message.MPTMessageType.values() )
		{
			if( type != MPTMessageType.UNKN )
			{
				mFilterElements.put( type, 
						new FilterElement<MPTMessageType>( type ) );
			}
		}
	}
	
	@Override
    public boolean passes( Message message )
    {
		if( mEnabled && canProcess( message ) )
		{
			MPT1327Message mpt = (MPT1327Message)message;

			FilterElement<MPTMessageType> element = 
					mFilterElements.get( mpt.getMessageType() );
			
			if( element != null )
			{
				return element.isEnabled();
			}
		}
		
        return false;
    }

	@Override
    public boolean canProcess( Message message )
    {
        return message instanceof MPT1327Message;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
		return new ArrayList<FilterElement<?>>( mFilterElements.values() );
    }
}

