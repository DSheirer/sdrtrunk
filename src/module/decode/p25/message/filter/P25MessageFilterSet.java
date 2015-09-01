package module.decode.p25.message.filter;

import message.Message;
import module.decode.p25.message.P25Message;
import filter.FilterSet;

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
