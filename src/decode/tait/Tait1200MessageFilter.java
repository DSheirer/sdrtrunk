package decode.tait;

import java.util.Collections;
import java.util.List;

import message.Message;
import filter.Filter;
import filter.FilterElement;

public class Tait1200MessageFilter extends Filter<Message>
{
	
	public Tait1200MessageFilter()
	{
		super( "Tait-1200 Message Filter" );
	}
	
	@Override
    public boolean passes( Message message )
    {
		return mEnabled && canProcess( message );
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof Tait1200GPSMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
	    return Collections.EMPTY_LIST;
    }
}
