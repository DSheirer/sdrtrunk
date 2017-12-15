package ua.in.smartjava.module.decode.p25.message.filter;

import ua.in.smartjava.message.Message;
import ua.in.smartjava.module.decode.p25.message.P25Message;
import ua.in.smartjava.filter.FilterSet;

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
