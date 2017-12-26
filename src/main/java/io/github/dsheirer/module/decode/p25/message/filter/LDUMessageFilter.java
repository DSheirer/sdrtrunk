package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.p25.message.ldu.LDUMessage;

import java.util.Collections;
import java.util.List;

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
