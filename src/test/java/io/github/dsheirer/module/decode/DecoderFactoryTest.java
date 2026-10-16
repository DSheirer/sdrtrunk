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

package io.github.dsheirer.module.decode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.DeemphasisMode;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.NXDNChannelMode;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.module.decode.squelch.SquelchDecoderConfig;
import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecoderFactoryTest
{
    @Test
    void copiesAllNbfmAudioAndSquelchSettings()
    {
        DecodeConfigNBFM original = new DecodeConfigNBFM();
        original.setBandwidth(DecodeConfigNBFM.Bandwidth.BW_25_0);
        original.setTalkgroup(1200);
        original.setAudioFilter(false);
        original.setAudioALC(true);
        original.setDeemphasis(DeemphasisMode.NBFM_300);
        original.setSquelchDecoders(new ArrayList<>(List.of(new SquelchDecoderConfig(
            SquelchDecoderConfig.SquelchType.CTCSS, CTCSSCode.TONE_1000.name()))));

        DecodeConfigNBFM copy = (DecodeConfigNBFM)DecoderFactory.copy(original);

        assertEquals(DecodeConfigNBFM.Bandwidth.BW_25_0, copy.getBandwidth());
        assertEquals(1200, copy.getTalkgroup());
        assertFalse(copy.isAudioFilter());
        assertTrue(copy.isAudioALC());
        assertEquals(DeemphasisMode.NBFM_300, copy.getDeemphasis());
        assertEquals(1, copy.getSquelchDecoders().size());
        assertEquals(SquelchDecoderConfig.SquelchType.CTCSS,
            copy.getSquelchDecoders().getFirst().getSquelchType());
        assertEquals(CTCSSCode.TONE_1000.name(), copy.getSquelchDecoders().getFirst().getValue());
        assertNotSame(original.getSquelchDecoders(), copy.getSquelchDecoders());
        assertNotSame(original.getSquelchDecoders().getFirst(), copy.getSquelchDecoders().getFirst());

        copy.getSquelchDecoders().getFirst().setValue(CTCSSCode.TONE_1035.name());
        assertEquals(CTCSSCode.TONE_1000.name(), original.getSquelchDecoders().getFirst().getValue());
    }

    @Test
    void copiesNxdnModeAndUsesItForRotationStates()
    {
        DecodeConfigNXDN conventional = new DecodeConfigNXDN();
        conventional.setChannelMode(NXDNChannelMode.CONVENTIONAL);

        DecodeConfigNXDN copy = (DecodeConfigNXDN)DecoderFactory.copy(conventional);

        assertEquals(NXDNChannelMode.CONVENTIONAL, copy.getChannelMode());
        assertEquals(List.of(State.CALL, State.ENCRYPTED, State.DATA),
            DecoderFactory.nxdnRotationActiveStates(copy));
        assertEquals(List.of(State.CONTROL), DecoderFactory.nxdnRotationActiveStates(new DecodeConfigNXDN()));
    }
}
