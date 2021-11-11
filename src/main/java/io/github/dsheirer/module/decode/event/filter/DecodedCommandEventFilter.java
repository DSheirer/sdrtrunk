package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.Arrays;

public class DecodedCommandEventFilter extends EventFilter
{
    public DecodedCommandEventFilter()
    {
        super("Commands", Arrays.asList(
                DecodeEventType.ANNOUNCEMENT,
                DecodeEventType.ACKNOWLEDGE,
                DecodeEventType.PAGE,
                DecodeEventType.QUERY,
                DecodeEventType.RADIO_CHECK,
                DecodeEventType.STATUS,
                DecodeEventType.COMMAND,
                DecodeEventType.EMERGENCY,
                DecodeEventType.NOTIFICATION,
                DecodeEventType.FUNCTION
        ));
    }
}
