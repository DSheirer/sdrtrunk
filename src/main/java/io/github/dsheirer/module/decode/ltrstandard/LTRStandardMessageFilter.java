package io.github.dsheirer.module.decode.ltrstandard;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageType;
import io.github.dsheirer.module.decode.ltrstandard.message.LTRStandardMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LTRStandardMessageFilter extends Filter<IMessage>
{
    private Map<MessageType, FilterElement<MessageType>> mElements = new HashMap<MessageType, FilterElement<MessageType>>();

    public LTRStandardMessageFilter()
    {
        super("LTR Message Filter");

        mElements.put(MessageType.CA_STRT,
                new FilterElement<MessageType>(MessageType.CA_STRT));
        mElements.put(MessageType.CA_ENDD,
                new FilterElement<MessageType>(MessageType.CA_ENDD));
        mElements.put(MessageType.SY_IDLE,
                new FilterElement<MessageType>(MessageType.SY_IDLE));
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            LTRStandardMessage ltr = (LTRStandardMessage) message;

            if(mElements.containsKey(ltr.getMessageType()))
            {
                return mElements.get(ltr.getMessageType()).isEnabled();
            }
        }

        return false;
    }

    public boolean canProcess(IMessage message)
    {
        return message instanceof LTRStandardMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mElements.values());
    }
}
