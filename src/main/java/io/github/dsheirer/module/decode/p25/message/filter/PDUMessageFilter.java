package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.reference.Opcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PDUMessageFilter extends Filter<Message>
{
	private HashMap<Opcode,FilterElement<Opcode>> mOpcodeFilterElements = 
			new HashMap<Opcode,FilterElement<Opcode>>();
	
	public PDUMessageFilter()
	{
		super( "PDU - Packet Data Unit" );
		
		for( Opcode opcode: Opcode.values() )
		{
			mOpcodeFilterElements.put( opcode, new FilterElement<Opcode>( opcode ) );
		}
	}
	
	@Override
    public boolean passes( Message message )
    {
		if( mEnabled && message instanceof PDUMessage)
		{
			PDUMessage pdu = (PDUMessage)message;
			
			Opcode opcode = pdu.getOpcode();
			
			return mOpcodeFilterElements.get( opcode ).isEnabled();
		}

	    return false;
    }

	@Override
    public boolean canProcess( Message message )
    {
	    return message instanceof PDUMessage;
    }

	@Override
    public List<FilterElement<?>> getFilterElements()
    {
		return new ArrayList<FilterElement<?>>( mOpcodeFilterElements.values() );
    }
}
