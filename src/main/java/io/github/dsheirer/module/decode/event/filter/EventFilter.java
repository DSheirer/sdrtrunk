package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This is used as a base for {@link IDecodeEvent} types.
 * This will take a list of {@link IDecodeEvent}s and generate filters for the list.
 * This will default canProcess to TRUE. Override if other value or evaluation is needed.
 *
 */
public class EventFilter extends Filter<IDecodeEvent>
{
    private final Map<DecodeEventType, FilterElement<DecodeEventType>> mElements = new EnumMap<>(DecodeEventType.class);

    public EventFilter(String name, List<DecodeEventType> eventTypes)
    {
        super(name);

        for (DecodeEventType eventType : eventTypes) {
            mElements.put(eventType, new FilterElement<>(eventType));
        }
    }

    @Override
    public boolean canProcess(IDecodeEvent iDecodeEvent)
    {
        return mElements.containsKey(iDecodeEvent.getEventType());
    }

    @Override
    public boolean passes(IDecodeEvent iDecodeEvent)
    {
        if (mEnabled && canProcess(iDecodeEvent))
        {
            return mElements.get(iDecodeEvent.getEventType()).isEnabled();
        }
        return false;
    }

    @Override
    public List<FilterElement<?>> getFilterElements()
    {
        return new ArrayList<>(mElements.values());
    }
}
