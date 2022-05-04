package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.module.decode.event.IDecodeEvent;

public class DecodeEventFilterSet extends FilterSet<IDecodeEvent> {
    public DecodeEventFilterSet() {
        super ("All Messages");

        addFilter(new DecodedCallEventFilter());
        addFilter(new DecodedDataEventFilter());
        addFilter(new DecodedCommandEventFilter());
        addFilter(new DecodedRegistrationEventFilter());
    }
}
