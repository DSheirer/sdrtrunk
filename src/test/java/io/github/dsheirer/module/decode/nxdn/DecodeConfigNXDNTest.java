/*
 * *****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 * ****************************************************************************
 */
package io.github.dsheirer.module.decode.nxdn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class DecodeConfigNXDNTest
{
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    @Test
    void defaultsLegacyConfigurationToTrunked() throws Exception
    {
        DecodeConfigNXDN configuration = new DecodeConfigNXDN();
        ObjectNode legacyJson = mObjectMapper.valueToTree(configuration);
        legacyJson.remove("channelMode");

        DecodeConfigNXDN restored = mObjectMapper.treeToValue(legacyJson, DecodeConfigNXDN.class);

        assertEquals(NXDNChannelMode.TRUNKED, restored.getChannelMode());
        assertTrue(restored.isTrunked());
        assertFalse(restored.isConventional());
    }

    @Test
    void explicitConventionalModeRoundTrips() throws Exception
    {
        DecodeConfigNXDN configuration = new DecodeConfigNXDN();
        configuration.setChannelMode(NXDNChannelMode.CONVENTIONAL);

        String json = mObjectMapper.writeValueAsString(configuration);
        DecodeConfigNXDN restored = mObjectMapper.readValue(json, DecodeConfigNXDN.class);

        assertEquals(NXDNChannelMode.CONVENTIONAL, restored.getChannelMode());
        assertTrue(restored.isConventional());
        assertFalse(restored.isTrunked());
    }
}
