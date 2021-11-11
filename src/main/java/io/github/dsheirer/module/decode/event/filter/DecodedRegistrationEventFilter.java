package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEventType;

import java.util.Arrays;

public class DecodedRegistrationEventFilter extends EventFilter
{
    public DecodedRegistrationEventFilter()
    {
        super("Registrations", Arrays.asList(
                DecodeEventType.AFFILIATE,
                DecodeEventType.AUTOMATIC_REGISTRATION_SERVICE,
                DecodeEventType.REGISTER,
                DecodeEventType.REGISTER_ESN,
                DecodeEventType.DEREGISTER,
                DecodeEventType.REQUEST,
                DecodeEventType.RESPONSE,
                DecodeEventType.RESPONSE_PACKET
        ));
    }
}
