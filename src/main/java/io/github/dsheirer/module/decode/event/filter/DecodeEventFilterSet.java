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

        // This filter must be last. Its goal is to handle decode events that:
        // - do not have type set, or
        // - their type is not handled by filters above
        addFilter(new EventFilter("Everything else", List.of()) {
            @Override
            public boolean passes(IDecodeEvent iDecodeEvent) {
                return isEnabled();
            }
        });
    }
}
