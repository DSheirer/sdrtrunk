package module.decode.p25.message.filter;

import java.util.Collections;
import java.util.List;

import message.Message;
import module.decode.p25.message.hdu.HDUMessage;
import filter.Filter;
import filter.FilterElement;

public class HDUMessageFilter extends Filter<Message>
{
	public HDUMessageFilter()
	{
		super( "Header Data Unit" );
	}
	
	@Override
    public boolean passes( Message message )
    {
		return mEnabled && canProcess( message );
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof HDUMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
		return Collections.EMPTY_LIST;
    }
}
