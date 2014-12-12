package decode.lj1200;

import java.util.Collections;
import java.util.List;

import message.Message;
import filter.Filter;
import filter.FilterElement;

public class LJ1200MessageFilter extends Filter<Message>
{
	
	public LJ1200MessageFilter()
	{
		super( "LJ-1200 Message Filter" );
	}
	
	@Override
    public boolean passes( Message message )
    {
		return mEnabled && canProcess( message );
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof LJ1200Message;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
	    return Collections.EMPTY_LIST;
    }
}
