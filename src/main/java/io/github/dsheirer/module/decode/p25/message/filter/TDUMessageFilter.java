package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.message.tdu.TDUMessage;

import java.util.Collections;
import java.util.List;

public class TDUMessageFilter extends Filter<IMessage>
{
    public TDUMessageFilter()
    {
        super("TDU Terminator Data Unit");
    }

    @Override
    public boolean passes(IMessage message)
    {
        return mEnabled && canProcess(message);
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof TDUMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return Collections.EMPTY_LIST;
    }
}
