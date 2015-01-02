package decode.p25.message.filter;

import java.util.Collections;
import java.util.List;

import message.Message;
import decode.p25.message.ldu.LDUMessage;
import filter.Filter;
import filter.FilterElement;

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
