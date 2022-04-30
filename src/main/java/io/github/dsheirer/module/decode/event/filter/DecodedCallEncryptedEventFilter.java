package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.Arrays;

public class DecodedCallEncryptedEventFilter extends EventFilter
{
    public DecodedCallEncryptedEventFilter()
    {
        super("Voice Calls - Encrypted", Arrays.asList(
                DecodeEventType.CALL_ENCRYPTED,
                DecodeEventType.CALL_GROUP_ENCRYPTED,
                DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED,
                DecodeEventType.CALL_INTERCONNECT_ENCRYPTED,
                DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED
        ));
    }
}
