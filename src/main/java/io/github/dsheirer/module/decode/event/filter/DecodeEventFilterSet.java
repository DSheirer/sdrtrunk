package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.module.decode.event.IDecodeEvent;

import java.util.List;

public class DecodeEventFilterSet extends FilterSet<IDecodeEvent> {
    public DecodeEventFilterSet() {
        super ("All Messages");

        addFilter(new DecodedCallEventFilter());
        addFilter(new DecodedCallEncryptedEventFilter());
        addFilter(new DecodedDataEventFilter());
        addFilter(new DecodedCommandEventFilter());
        addFilter(new DecodedRegistrationEventFilter());
        addFilter(new EventFilter("Everything else", List.of()) {
            @Override
            public boolean passes(IDecodeEvent iDecodeEvent) {
                return isEnabled();
            }
        });
    }
}
