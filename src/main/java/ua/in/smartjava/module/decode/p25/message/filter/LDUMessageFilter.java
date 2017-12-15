package ua.in.smartjava.module.decode.p25.message.filter;

import java.util.Collections;
import java.util.List;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.p25.message.ldu.LDUMessage;
import ua.in.smartjava.filter.Filter;
import ua.in.smartjava.filter.FilterElement;

public class LDUMessageFilter extends Filter<Message>
{
	public LDUMessageFilter()
	{
		super( "Link Data Unit" );
	}
	
	@Override
    public boolean passes( Message message )
    {
		return mEnabled && canProcess( message );
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof LDUMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
		return Collections.EMPTY_LIST;
    }
}
