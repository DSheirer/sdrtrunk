package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.Arrays;

public class DecodedCallEventFilter extends EventFilter
{
    public DecodedCallEventFilter()
    {
        super("Voice Calls", Arrays.asList(
                DecodeEventType.CALL_GROUP,
                DecodeEventType.CALL_ENCRYPTED,
                DecodeEventType.CALL_GROUP_ENCRYPTED,
                DecodeEventType.CALL_PATCH_GROUP,
                DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED,
                DecodeEventType.CALL_ALERT,
                DecodeEventType.CALL_DETECT,
                DecodeEventType.CALL_DO_NOT_MONITOR,
                DecodeEventType.CALL_END,
                DecodeEventType.CALL_INTERCONNECT,
                DecodeEventType.CALL_INTERCONNECT_ENCRYPTED,
                DecodeEventType.CALL_UNIQUE_ID,
                DecodeEventType.CALL_UNIT_TO_UNIT,
                DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED,
                DecodeEventType.CALL_NO_TUNER,
                DecodeEventType.CALL_TIMEOUT
        ));
    }
}
