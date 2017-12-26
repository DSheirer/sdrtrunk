package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.p25.message.P25Message;

public class P25MessageFilterSet extends FilterSet<Message>
{
	public P25MessageFilterSet()
	{
		super( "P25 Message Filter" );
		
		addFilter( new HDUMessageFilter() );
		addFilter( new LDUMessageFilter() );
		addFilter( new PDUMessageFilter() );
		addFilter( new TDUMessageFilter() );
		addFilter( new TDULCMessageFilter() );
		addFilter( new TSBKMessageFilterSet() );
	}

	@Override
    public boolean canProcess( Message message )
    {
		return message instanceof P25Message;
    }
}
