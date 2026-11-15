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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.edacs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests the EDACS decode configuration's ProVoice voice marker and logical channel number (LCN) frequency list.
 */
class DecodeConfigEDACSTest
{
    @Test
    void voiceModeDefaultsToAuto()
    {
        //Auto follows the grant.  Analog as the default would play ProVoice calls through the analog decoder.
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        assertEquals(DecodeConfigEDACS.VoiceMode.AUTO, config.getVoiceMode());
        assertEquals(DecodeConfigEDACS.VoiceMode.PROVOICE, config.resolveVoiceMode(true));
        assertEquals(DecodeConfigEDACS.VoiceMode.ANALOG, config.resolveVoiceMode(false));

        config.setVoiceMode(null);
        assertEquals(DecodeConfigEDACS.VoiceMode.AUTO, config.getVoiceMode());
    }

    @Test
    void voiceMarkerDefaultsToTheKnownValue()
    {
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        assertEquals("2175", config.getProVoiceVoiceMarker());
        assertEquals(0x2175, config.getProVoiceVoiceMarkerValue());
    }

    @Test
    void emptyVoiceMarkerDisablesTheCheck()
    {
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        config.setProVoiceVoiceMarker("");
        assertEquals("", config.getProVoiceVoiceMarker());
        assertEquals(DecodeConfigEDACS.PROVOICE_VOICE_MARKER_NONE, config.getProVoiceVoiceMarkerValue());
    }

    @Test
    void voiceMarkerIsNormalizedHexadecimal()
    {
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        config.setProVoiceVoiceMarker(" a5f ");
        assertEquals("A5F", config.getProVoiceVoiceMarker());
        assertEquals(0xA5F, config.getProVoiceVoiceMarkerValue());
    }

    @Test
    void invalidVoiceMarkerDisablesTheCheck()
    {
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        config.setProVoiceVoiceMarker("12345");
        assertEquals("", config.getProVoiceVoiceMarker());
        config.setProVoiceVoiceMarker("zz");
        assertEquals("", config.getProVoiceVoiceMarker());
    }

    @Test
    void nullVoiceMarkerKeepsTheDefault()
    {
        //A playlist saved before the setting existed has no value
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        config.setProVoiceVoiceMarker(null);
        assertEquals("2175", config.getProVoiceVoiceMarker());
    }

    @Test
    void frequencyIsLookedUpByLogicalChannelNumber()
    {
        DecodeConfigEDACS config = new DecodeConfigEDACS();
        config.setLcnFrequencies("851175000,851225000,,853900000");
        assertEquals(851175000L, config.getFrequency(1));
        assertEquals(851225000L, config.getFrequency(2));
        assertEquals(0L, config.getFrequency(3), "an LCN with no frequency");
        assertEquals(853900000L, config.getFrequency(4));
        assertEquals(0L, config.getFrequency(5), "an LCN beyond the list");
        assertEquals(0L, config.getFrequency(0), "LCNs start at 1");
    }
}
