package ua.in.smartjava.module.decode.p25.message.filter;

import java.util.Collections;
import java.util.List;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.p25.message.tdu.TDUMessage;
import ua.in.smartjava.filter.Filter;
import ua.in.smartjava.filter.FilterElement;

public class TDUMessageFilter extends Filter<Message>
{
	public TDUMessageFilter()
	{
		super( "TDU Terminator Data Unit" );
	}
	
	@Override
    public boolean passes( Message message )
    {
		return mEnabled && canProcess( message );
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof TDUMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
		return Collections.EMPTY_LIST;
    }
}
