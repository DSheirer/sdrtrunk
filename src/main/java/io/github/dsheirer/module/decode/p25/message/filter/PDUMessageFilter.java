package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk2.Opcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PDUMessageFilter extends Filter<IMessage>
{
    private Map<Opcode, FilterElement<Opcode>> mOpcodeFilterElements = new HashMap<Opcode, FilterElement<Opcode>>();

    public PDUMessageFilter()
    {
        super("PDU - Packet Data Unit");

        for(Opcode opcode : Opcode.values())
        {
            mOpcodeFilterElements.put(opcode, new FilterElement<Opcode>(opcode));
        }
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && message instanceof PDUMessage)
        {
            PDUMessage pdu = (PDUMessage) message;

            Opcode opcode = pdu.getOpcode();

            return mOpcodeFilterElements.get(opcode).isEnabled();
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof PDUMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mOpcodeFilterElements.values());
    }
}
