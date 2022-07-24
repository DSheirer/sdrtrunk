package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.Arrays;

public class DecodedCallEventFilter extends EventFilter
{
    public DecodedCallEventFilter()
    {
        super("Voice Calls", Arrays.asList(
                DecodeEventType.CALL,
                DecodeEventType.CALL_GROUP,
                DecodeEventType.CALL_PATCH_GROUP,
                DecodeEventType.CALL_ALERT,
                DecodeEventType.CALL_DETECT,
                DecodeEventType.CALL_DO_NOT_MONITOR,
                DecodeEventType.CALL_END,
                DecodeEventType.CALL_INTERCONNECT,
                DecodeEventType.CALL_UNIQUE_ID,
                DecodeEventType.CALL_UNIT_TO_UNIT,
                DecodeEventType.CALL_NO_TUNER,
                DecodeEventType.CALL_TIMEOUT
        ));
    }
}
