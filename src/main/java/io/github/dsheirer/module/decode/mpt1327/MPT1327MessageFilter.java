package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.mpt1327.MPT1327Message.MPTMessageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MPT1327MessageFilter extends Filter<IMessage>
{
    private Map<MPTMessageType, FilterElement<MPTMessageType>> mFilterElements = new HashMap<MPTMessageType, FilterElement<MPTMessageType>>();

    public MPT1327MessageFilter()
    {
        super("MPT1327 Message Type Filter");

        for(MPTMessageType type : MPT1327Message.MPTMessageType.values())
        {
            if(type != MPTMessageType.UNKNOWN)
            {
                mFilterElements.put(type,
                        new FilterElement<MPTMessageType>(type));
            }
        }
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            MPT1327Message mpt = (MPT1327Message) message;

            FilterElement<MPTMessageType> element =
                    mFilterElements.get(mpt.getMessageType());

            if(element != null)
            {
                return element.isEnabled();
            }
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof MPT1327Message;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mFilterElements.values());
    }
}

