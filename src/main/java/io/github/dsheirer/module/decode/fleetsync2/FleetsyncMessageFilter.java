package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FleetsyncMessageFilter extends Filter<IMessage>
{
    private HashMap<FleetsyncMessageType, FilterElement<FleetsyncMessageType>> mElements =
            new HashMap<FleetsyncMessageType, FilterElement<FleetsyncMessageType>>();

    public FleetsyncMessageFilter()
    {
        super("Fleetsync Message Filter");

        for(FleetsyncMessageType type : FleetsyncMessageType.values())
        {
            if(type != FleetsyncMessageType.UNKNOWN)
            {
                mElements.put(type, new FilterElement<FleetsyncMessageType>(type));
            }
        }
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            FleetsyncMessage fleet = (FleetsyncMessage) message;

            if(mElements.containsKey(fleet.getMessageType()))
            {
                return mElements.get(fleet.getMessageType()).isEnabled();
            }
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof FleetsyncMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mElements.values());
    }
}
