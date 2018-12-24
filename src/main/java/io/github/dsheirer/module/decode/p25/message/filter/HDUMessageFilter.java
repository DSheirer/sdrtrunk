package io.github.dsheirer.module.decode.p25.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.message.hdu.HDUMessage;

import java.util.Collections;
import java.util.List;

public class HDUMessageFilter extends Filter<IMessage>
{
    public HDUMessageFilter()
    {
        super("Header Data Unit");
    }

    @Override
    public boolean passes(IMessage message)
    {
        return mEnabled && canProcess(message);
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof HDUMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return Collections.EMPTY_LIST;
    }
}
