/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.nxdn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.call.TransmissionRelease;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;
import org.junit.jupiter.api.Test;

class NXDNDecoderStateTest
{
    private static final IntField CALL_TYPE = IntField.length3(16);
    private static final IntField SOURCE_RADIO = IntField.length16(24);
    private static final IntField DESTINATION_RADIO = IntField.length16(40);

    @Test
    void inboundIndividualCallPublishesRadioIdentifiersAndEndsCleanly()
    {
        Channel channel = new Channel("NXDN Simplex");
        DecodeConfigNXDN configuration = new DecodeConfigNXDN();
        configuration.setChannelMode(NXDNChannelMode.CONVENTIONAL);
        channel.setDecodeConfiguration(configuration);
        NXDNDecoderState decoderState = new NXDNDecoderState(channel, null);

        CorrectedBinaryMessage callMessage = new CorrectedBinaryMessage(80);
        callMessage.setInt(CallType.INDIVIDUAL.getValue(), CALL_TYPE);
        callMessage.setInt(1001, SOURCE_RADIO);
        callMessage.setInt(2002, DESTINATION_RADIO);
        VoiceCall voiceCall = new VoiceCall(callMessage, 1_000L,
            NXDNMessageType.TRAFFIC_IN_01_CC_VOICE_CALL, 1, LICH.RDCH_INBOUND_SINGLE_FACCH1_FACCH1);

        decoderState.receive(voiceCall);

        Identifier source = decoderState.getIdentifierCollection()
            .getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);
        Identifier destination = decoderState.getIdentifierCollection()
            .getIdentifier(IdentifierClass.USER, Form.RADIO, Role.TO);

        assertInstanceOf(RadioIdentifier.class, source);
        assertInstanceOf(RadioIdentifier.class, destination);
        assertEquals(1001, ((RadioIdentifier)source).getValue());
        assertEquals(2002, ((RadioIdentifier)destination).getValue());

        TransmissionRelease release = new TransmissionRelease(new CorrectedBinaryMessage(80), 2_000L,
            NXDNMessageType.TRAFFIC_IN_08_CC_TRANSMISSION_RELEASE, 1,
            LICH.RDCH_INBOUND_SINGLE_FACCH1_FACCH1);
        decoderState.receive(release);

        assertNull(decoderState.getIdentifierCollection()
            .getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM));
    }
}
