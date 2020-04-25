package io.github.dsheirer.module.decode.mdc1200;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.mpt1327.MPT1327Message;

import java.util.*;

public class MDCMessageFilter extends Filter<IMessage>
{
    private Map<MDCMessageType, FilterElement<MDCMessageType>> mElements = new EnumMap<>(MDCMessageType.class);

    public MDCMessageFilter()
    {
        super("MDC-1200 Message Filter");

        for(MDCMessageType type : MDCMessageType.values())
        {
            if(type != MDCMessageType.UNKNOWN)
            {
                mElements.put(type, new FilterElement<MDCMessageType>(type));
            }
        }
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            MDCMessage mdc = (MDCMessage) message;

            if(mElements.containsKey(mdc.getMessageType()))
            {
                return mElements.get(mdc.getMessageType()).isEnabled();
            }
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof MDCMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mElements.values());
    }
}
