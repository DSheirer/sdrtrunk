package io.github.dsheirer.module.decode.ltrnet;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.message.MessageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LTRNetMessageFilter extends Filter<IMessage>
{
    private Map<MessageType, FilterElement<MessageType>> mElements = new HashMap<MessageType, FilterElement<MessageType>>();

    public LTRNetMessageFilter()
    {
        super("LTR-Net Message Filter");

        mElements.put(MessageType.CA_STRT,
                new FilterElement<MessageType>(MessageType.CA_STRT));
        mElements.put(MessageType.CA_ENDD,
                new FilterElement<MessageType>(MessageType.CA_ENDD));
        mElements.put(MessageType.SY_IDLE,
                new FilterElement<MessageType>(MessageType.SY_IDLE));
        mElements.put(MessageType.ID_UNIQ,
                new FilterElement<MessageType>(MessageType.ID_UNIQ));
        mElements.put(MessageType.ID_ESNH,
                new FilterElement<MessageType>(MessageType.ID_ESNH));
        mElements.put(MessageType.ID_ESNL,
                new FilterElement<MessageType>(MessageType.ID_ESNL));
        mElements.put(MessageType.RQ_ACCE,
                new FilterElement<MessageType>(MessageType.RQ_ACCE));
        mElements.put(MessageType.ID_SITE,
                new FilterElement<MessageType>(MessageType.ID_SITE));
        mElements.put(MessageType.FQ_RXHI,
                new FilterElement<MessageType>(MessageType.FQ_RXHI));
        mElements.put(MessageType.FQ_RXLO,
                new FilterElement<MessageType>(MessageType.FQ_RXLO));
        mElements.put(MessageType.FQ_TXHI,
                new FilterElement<MessageType>(MessageType.FQ_TXHI));
        mElements.put(MessageType.FQ_TXLO,
                new FilterElement<MessageType>(MessageType.FQ_TXLO));
        mElements.put(MessageType.ID_NBOR,
                new FilterElement<MessageType>(MessageType.ID_NBOR));
        mElements.put(MessageType.MA_CHNH,
                new FilterElement<MessageType>(MessageType.MA_CHNH));
        mElements.put(MessageType.MA_CHNL,
                new FilterElement<MessageType>(MessageType.MA_CHNL));
    }

    @Override
    public boolean passes(IMessage message)
    {
        if(mEnabled && canProcess(message))
        {
            LTRNetMessage ltr = (LTRNetMessage) message;

            if(mElements.containsKey(ltr.getMessageType()))
            {
                return mElements.get(ltr.getMessageType()).isEnabled();
            }
        }

        return false;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof LTRNetMessage;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<FilterElement<?>>(mElements.values());
    }
}
