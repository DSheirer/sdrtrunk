package ua.in.smartjava.module.decode.p25.message.filter;

import java.util.Collections;
import java.util.List;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.p25.message.tdu.lc.TDULinkControlMessage;
import ua.in.smartjava.filter.Filter;
import ua.in.smartjava.filter.FilterElement;

public class TDULCMessageFilter extends Filter<Message>
{
	public TDULCMessageFilter()
	{
		super( "TDU Terminator Data Unit with Link Control" );
	}
	
	@Override
    public boolean passes( Message message )
    {
		return mEnabled && canProcess( message );
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof TDULinkControlMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
		return Collections.EMPTY_LIST;
    }
}
