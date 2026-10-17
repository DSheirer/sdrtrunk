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
import com.google.gson.JsonParser;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.DeemphasisMode;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.controller.channel.Channel;
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

    @Test
    void appliesWebTrafficChannelSettings()
    {
        Channel channel = new Channel("NXDN Test");
        DecodeConfigNXDN config = new DecodeConfigNXDN();
        channel.setDecodeConfiguration(config);
        JsonObject request = new JsonObject();
        request.addProperty("trafficChannelPoolSize", 12);
        request.addProperty("ignoreDataCalls", true);

        SdrTrunkWebServer.applyTrafficSettings(channel, request);

        assertEquals(12, config.getTrafficChannelPoolSize());
        assertTrue(config.isIgnoreDataCalls());
    }

    @Test
    void validatesWebListenerSettings()
    {
        SdrTrunkWebServer.validateNetworkSettings("127.0.0.1", 10000);
        SdrTrunkWebServer.validateNetworkSettings("0.0.0.0", 65535);
        assertThrows(IllegalArgumentException.class,
            () -> SdrTrunkWebServer.validateNetworkSettings("http://0.0.0.0", 10000));
        assertThrows(IllegalArgumentException.class,
            () -> SdrTrunkWebServer.validateNetworkSettings("0.0.0.0", 0));
    }

    @Test
    void hashesWebUserPasswords()
    {
        byte[] salt = new byte[16];
        for(int x = 0; x < salt.length; x++) { salt[x] = (byte)(x + 1); }
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String hash = SdrTrunkWebServer.hashPassword("correct horse battery staple", salt);

        assertTrue(SdrTrunkWebServer.passwordHashMatches(hash, encodedSalt,
            "correct horse battery staple"));
        assertFalse(SdrTrunkWebServer.passwordHashMatches(hash, encodedSalt, "incorrect"));
    }

    @Test
    void reportsTheActiveWebListener() throws Exception
    {
        int port;
        try(ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        String previousBind = System.getProperty("sdrtrunk.web.bind");
        String previousPort = System.getProperty("sdrtrunk.web.port");
        System.setProperty("sdrtrunk.web.bind", "127.0.0.1");
        System.setProperty("sdrtrunk.web.port", Integer.toString(port));
        SdrTrunkWebServer server = new SdrTrunkWebServer(null, null, null, null, "test-admin-password");
        try
        {
            server.start();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port +
                    "/api/v1/web-settings"))
                .header("Authorization", "Bearer test-admin-password").GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());
            JsonObject settings = JsonParser.parseString(response.body()).getAsJsonObject();

            assertEquals(200, response.statusCode());
            assertEquals("127.0.0.1", settings.get("actualBind").getAsString());
            assertEquals(port, settings.get("actualPort").getAsInt());
            assertTrue(settings.get("running").getAsBoolean());

            HttpRequest qualityRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port +
                    "/api/v1/quality"))
                .header("Authorization", "Bearer test-admin-password").GET().build();
            HttpResponse<String> qualityResponse = HttpClient.newHttpClient().send(qualityRequest,
                HttpResponse.BodyHandlers.ofString());
            JsonObject quality = JsonParser.parseString(qualityResponse.body()).getAsJsonObject();
            assertEquals(200, qualityResponse.statusCode());
            JsonObject qualityPage = quality.getAsJsonObject("data");
            assertTrue(qualityPage.getAsJsonArray("rows").isEmpty());
            assertEquals(100, qualityPage.get("limit").getAsInt());
            assertEquals(0, qualityPage.get("offset").getAsInt());
            assertEquals(0, qualityPage.get("total_count").getAsInt());
            assertFalse(qualityPage.get("has_more").getAsBoolean());
            assertFalse(qualityPage.has("next_offset"));

            HttpRequest preferencesRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port +
                    "/api/v1/me/preferences"))
                .header("Authorization", "Bearer test-admin-password").GET().build();
            HttpResponse<String> preferencesResponse = HttpClient.newHttpClient().send(preferencesRequest,
                HttpResponse.BodyHandlers.ofString());
            JsonObject preferenceProfile = JsonParser.parseString(preferencesResponse.body()).getAsJsonObject();
            assertEquals(200, preferencesResponse.statusCode());
            assertEquals(12, preferenceProfile.getAsJsonObject("preferences").get("version").getAsInt());
            assertTrue(preferenceProfile.get("revision").getAsLong() >= 1);

            HttpRequest dashboardRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port +
                    "/api/v1/dashboard"))
                .header("Authorization", "Bearer test-admin-password").GET().build();
            HttpResponse<String> dashboardResponse = HttpClient.newHttpClient().send(dashboardRequest,
                HttpResponse.BodyHandlers.ofString());
            JsonObject dashboard = JsonParser.parseString(dashboardResponse.body()).getAsJsonObject();
            assertEquals(200, dashboardResponse.statusCode());
            assertTrue(dashboard.getAsJsonObject("data").has("call_activity"));
        }
        finally
        {
            server.stop();
            restoreProperty("sdrtrunk.web.bind", previousBind);
            restoreProperty("sdrtrunk.web.port", previousPort);
        }
    }

    @Test
    void suppliesRetainedDashboardAndRadioActivity() throws Exception
    {
        int port;
        try(ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        String previousBind = System.getProperty("sdrtrunk.web.bind");
        String previousPort = System.getProperty("sdrtrunk.web.port");
        System.setProperty("sdrtrunk.web.bind", "127.0.0.1");
        System.setProperty("sdrtrunk.web.port", Integer.toString(port));
        SdrTrunkWebServer server = new SdrTrunkWebServer(null, null, null, null, "test-admin-password");
        try
        {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("time", System.currentTimeMillis());
            item.put("end", System.currentTimeMillis());
            item.put("duration", 1200);
            item.put("protocol", "APCO-25");
            item.put("type", "Group Call");
            item.put("talkgroup", "1001");
            item.put("talkgroupId", 1001);
            item.put("destinationKind", "talkgroup");
            item.put("alias", "Dispatch");
            item.put("channel", "County Control");
            item.put("system", "County Radio");
            item.put("source", "2001");
            item.put("sourceId", 2001);
            item.put("sourceAlias", "Unit 12");
            item.put("channelKind", "TRUNKED");
            item.put("recorded", true);
            var recordActivity = SdrTrunkWebServer.class.getDeclaredMethod("recordActivity", Map.class);
            recordActivity.setAccessible(true);
            recordActivity.invoke(server, item);
            server.start();

            JsonObject dashboard = authenticatedGet(port, "/api/v1/dashboard").getAsJsonObject("data");
            assertEquals(1, dashboard.getAsJsonObject("call_activity").getAsJsonObject("totals")
                .get("logical_call_count").getAsInt());
            assertEquals(1, dashboard.getAsJsonObject("call_activity").getAsJsonArray("series").size());
            assertEquals("Dispatch", dashboard.getAsJsonArray("top_destinations").get(0).getAsJsonObject()
                .get("alias_name").getAsString());
            assertEquals("Unit 12", dashboard.getAsJsonArray("top_sources").get(0).getAsJsonObject()
                .get("alias_name").getAsString());

            JsonObject actions = authenticatedGet(port, "/api/v1/activity/actions?range=24h")
                .getAsJsonObject("data");
            assertEquals("ACTIVE", actions.getAsJsonArray("rows").get(0).getAsJsonObject()
                .get("action").getAsString());
            JsonObject radios = authenticatedGet(port, "/api/v1/activity/radios?range=24h&action=ACTIVE")
                .getAsJsonObject("data");
            assertEquals(1, radios.get("total_count").getAsInt());
            assertEquals("2001", radios.getAsJsonArray("rows").get(0).getAsJsonObject()
                .get("radio_id").getAsString());
        }
        finally
        {
            server.stop();
            restoreProperty("sdrtrunk.web.bind", previousBind);
            restoreProperty("sdrtrunk.web.port", previousPort);
        }
    }

    private static JsonObject authenticatedGet(int port, String path) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Authorization", "Bearer test-admin-password").GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static void restoreProperty(String name, String value)
    {
        if(value == null) { System.clearProperty(name); }
        else { System.setProperty(name, value); }
    }
}
