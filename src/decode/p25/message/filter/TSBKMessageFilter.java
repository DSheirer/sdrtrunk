package decode.p25.message.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import message.Message;
import decode.p25.message.tsbk.TSBKMessage;
import decode.p25.reference.Opcode;
import filter.Filter;
import filter.FilterElement;

public class TSBKMessageFilter extends Filter<Message>
{
	private HashMap<Opcode,FilterElement<Opcode>> mFilterElements = 
			new HashMap<Opcode,FilterElement<Opcode>>();
	
	public TSBKMessageFilter()
	{
		super( "TSBK Trunking Signalling Block" );
		
		for( Opcode opcode: Opcode.values() )
		{
			mFilterElements.put( opcode, new FilterElement<Opcode>( opcode ) );
		}
	}
	
	@Override
    public boolean passes( Message message )
    {
		if( mEnabled && canProcess( message ) )
		{
			TSBKMessage tsbk = (TSBKMessage)message;
			
			Opcode opcode = tsbk.getOpcode();
			
			return mFilterElements.get( opcode ).isEnabled();
		}

	    return false;
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof TSBKMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
		return new ArrayList<FilterElement<?>>( mFilterElements.values() );
    }
}
