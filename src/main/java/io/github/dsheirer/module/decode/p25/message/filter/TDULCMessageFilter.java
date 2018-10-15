package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.TDULinkControlMessage;

import java.util.Collections;
import java.util.List;

public class TDULCMessageFilter extends Filter<IMessage>
{
    public TDULCMessageFilter()
    {
        super("TDU Terminator Data Unit with Link Control");
    }

    @Override
    public boolean passes(IMessage message)
    {
        return mEnabled && canProcess(message);
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof TDULinkControlMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return Collections.EMPTY_LIST;
    }
}
