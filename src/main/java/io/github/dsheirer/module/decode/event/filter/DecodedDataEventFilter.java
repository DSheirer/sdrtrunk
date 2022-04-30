package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.Arrays;

public class DecodedDataEventFilter extends EventFilter
{
    public DecodedDataEventFilter()
    {
        super("Data Calls", Arrays.asList(
                DecodeEventType.DATA_CALL,
                DecodeEventType.DATA_CALL_ENCRYPTED,
                DecodeEventType.DATA_PACKET,
                DecodeEventType.GPS,
                DecodeEventType.IP_PACKET,
                DecodeEventType.UDP_PACKET,
                DecodeEventType.SDM,
                DecodeEventType.ID_ANI,
                DecodeEventType.ID_UNIQUE
        ));
    }
}
