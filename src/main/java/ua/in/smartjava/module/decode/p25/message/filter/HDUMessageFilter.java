package ua.in.smartjava.module.decode.p25.message.filter;

import java.util.Collections;
import java.util.List;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.p25.message.hdu.HDUMessage;
import ua.in.smartjava.filter.Filter;
import ua.in.smartjava.filter.FilterElement;

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
