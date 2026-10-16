/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.nxdn.audio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.Test;

class NXDNAudioModuleTest
{
    private static final IntField CALL_TYPE = IntField.length3(16);
    private static final IntField SOURCE_RADIO = IntField.length16(24);
    private static final IntField DESTINATION_RADIO = IntField.length16(40);

    @Test
    void boundsPreHeaderAudioCache()
    {
        assertEquals(12, NXDNAudioModule.MAX_CACHED_AUDIO_MESSAGES);
    }

    @Test
    void identifiesInboundSimplexCallWhenEitherRadioIsUnknown()
    {
        AliasList aliasList = new AliasList("Simplex");
        VoiceCall call = individualCall(LICH.RDCH_INBOUND_SINGLE_FACCH1_FACCH1);

        assertTrue(NXDNAudioModule.isUnknownSimplexCall(call, aliasList));

        addRadioAlias(aliasList, "Known source", 1001);
        assertTrue(NXDNAudioModule.isUnknownSimplexCall(call, aliasList));

        addRadioAlias(aliasList, "Known destination", 2002);
        assertFalse(NXDNAudioModule.isUnknownSimplexCall(call, aliasList));
    }

    @Test
    void doesNotTreatRepeaterOutboundCallAsUnknownSimplex()
    {
        AliasList aliasList = new AliasList("Repeater");
        VoiceCall call = individualCall(LICH.RDCH_OUTBOUND_SINGLE_FACCH1_FACCH1);

        assertFalse(NXDNAudioModule.isUnknownSimplexCall(call, aliasList));
    }

    private static VoiceCall individualCall(LICH lich)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(80);
        message.setInt(CallType.INDIVIDUAL.getValue(), CALL_TYPE);
        message.setInt(1001, SOURCE_RADIO);
        message.setInt(2002, DESTINATION_RADIO);
        NXDNMessageType type = lich.isOutbound() ? NXDNMessageType.TRAFFIC_OUT_01_CC_VOICE_CALL :
            NXDNMessageType.TRAFFIC_IN_01_CC_VOICE_CALL;
        return new VoiceCall(message, 1_000L, type, 1, lich);
    }

    private static void addRadioAlias(AliasList aliasList, String name, int radioId)
    {
        Alias alias = new Alias(name);
        alias.addAliasID(new Radio(Protocol.NXDN, radioId));
        aliasList.addAlias(alias);
    }
}
