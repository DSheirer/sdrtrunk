package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.message.ldu.LDUMessage;

import java.util.Collections;
import java.util.List;

public class LDUMessageFilter extends Filter<IMessage>
{
    public LDUMessageFilter()
    {
        super("Link Data Unit");
    }

    @Override
    public boolean passes(IMessage message)
    {
        return mEnabled && canProcess(message);
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof LDUMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return Collections.EMPTY_LIST;
    }
}
