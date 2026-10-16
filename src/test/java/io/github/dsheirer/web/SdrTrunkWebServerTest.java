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

package io.github.dsheirer.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.DeemphasisMode;
import io.github.dsheirer.module.decode.squelch.SquelchDecoderConfig;
import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;
import org.junit.jupiter.api.Test;

class SdrTrunkWebServerTest
{
    @Test
    void appliesNbfmWebSettings()
    {
        DecodeConfigNBFM config = new DecodeConfigNBFM();
        JsonObject request = new JsonObject();
        request.addProperty("nbfmBandwidth", DecodeConfigNBFM.Bandwidth.BW_25_0.name());
        request.addProperty("nbfmTalkgroup", 4321);
        request.addProperty("nbfmAudioFilter", false);
        request.addProperty("nbfmAudioALC", true);
        request.addProperty("nbfmDeemphasis", DeemphasisMode.NBFM_300.name());
        request.addProperty("nbfmSquelchType", SquelchDecoderConfig.SquelchType.CTCSS.name());
        request.addProperty("nbfmSquelchValue", CTCSSCode.TONE_1000.name());

        SdrTrunkWebServer.applyNbfmSettings(config, request);

        assertEquals(DecodeConfigNBFM.Bandwidth.BW_25_0, config.getBandwidth());
        assertEquals(4321, config.getTalkgroup());
        assertFalse(config.isAudioFilter());
        assertTrue(config.isAudioALC());
        assertEquals(DeemphasisMode.NBFM_300, config.getDeemphasis());
        assertEquals(SquelchDecoderConfig.SquelchType.CTCSS,
            config.getSquelchDecoders().getFirst().getSquelchType());
        assertEquals(CTCSSCode.TONE_1000.name(), config.getSquelchDecoders().getFirst().getValue());

        JsonObject disableTone = new JsonObject();
        disableTone.addProperty("nbfmSquelchType", SquelchDecoderConfig.SquelchType.NONE.name());
        SdrTrunkWebServer.applyNbfmSettings(config, disableTone);
        assertTrue(config.getSquelchDecoders().isEmpty());
    }

    @Test
    void rejectsInvalidNbfmSquelchCode()
    {
        DecodeConfigNBFM config = new DecodeConfigNBFM();
        JsonObject request = new JsonObject();
        request.addProperty("nbfmSquelchType", SquelchDecoderConfig.SquelchType.CTCSS.name());
        request.addProperty("nbfmSquelchValue", "NOT_A_TONE");

        assertThrows(IllegalArgumentException.class,
            () -> SdrTrunkWebServer.applyNbfmSettings(config, request));
    }

    @Test
    void appliesCtcssToneSearchWithoutMutingCarrierAudio()
    {
        DecodeConfigNBFM config = new DecodeConfigNBFM();
        JsonObject request = new JsonObject();
        request.addProperty("nbfmSquelchType", SquelchDecoderConfig.SquelchType.CTCSS_SEARCH.name());

        SdrTrunkWebServer.applyNbfmSettings(config, request);

        assertTrue(config.isCTCSSSearchEnabled());
        assertFalse(config.isSquelchDecoderEnabled());
        assertEquals(SquelchDecoderConfig.SquelchType.CTCSS_SEARCH,
            config.getSquelchDecoders().getFirst().getSquelchType());
        assertTrue(config.getSquelchDecoders().getFirst().isValid());
    }
}
