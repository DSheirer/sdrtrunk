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

package io.github.dsheirer.web.playlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.playlist.PlaylistConfig.AliasConfig;
import io.github.dsheirer.web.playlist.PlaylistConfig.ChannelConfig;
import io.github.dsheirer.web.playlist.PlaylistService.Versioned;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Playlist editing through the web API service: JSON of the playlist configuration classes, versions (ETags) and
 * the If-Match rules, and that edits made directly on the model (as the desktop editors do) change the version.
 */
public class PlaylistServiceTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private AliasModel mAliasModel;
    private ChannelModel mChannelModel;
    private PlaylistService mService;

    @BeforeEach
    void setUp()
    {
        mAliasModel = new AliasModel();
        mChannelModel = new ChannelModel(mAliasModel);
        mService = new PlaylistService(mChannelModel, mAliasModel, null, new AliasRegistry());
    }

    private static ChannelConfig nbfm(String name, long frequency)
    {
        ObjectNode source = MAPPER.createObjectNode();
        source.put("type", "sourceConfigTuner");
        source.put("frequency", frequency);
        source.put("source_type", "TUNER");
        return new ChannelConfig(null, name, "County", "Fire", "Fire Aliases", true, 2, false, source,
            PlaylistJson.toTree(new DecodeConfigNBFM()), null, null, null);
    }

    @Test
    void decoderDefaultsRoundTrip()
    {
        for(DecoderType type: DecoderType.PRIMARY_DECODERS)
        {
            DecodeConfiguration defaults = DecoderFactory.getDecodeConfiguration(type);
            JsonNode json = PlaylistJson.toTree(defaults);
            assertTrue(json.has("type"), type + " has a type discriminator");
            DecodeConfiguration parsed = PlaylistJson.fromTree(json, DecodeConfiguration.class, "decode");
            assertEquals(defaults.getClass(), parsed.getClass());
            assertEquals(json, PlaylistJson.toTree(parsed), type + " round trip");
        }
    }

    @Test
    void createReadUpdateDeleteChannel()
    {
        Versioned<ChannelConfig> created = mService.createChannel(nbfm("Dispatch", 453_850_000));
        int id = created.value().id();
        assertEquals("Dispatch", created.value().name());
        assertEquals(453_850_000, created.value().source().get("frequency").asLong());
        assertEquals("decodeConfigNBFM", created.value().decode().get("type").asText());
        assertTrue(mAliasModel.getListNames().contains("Fire Aliases"), "the alias list is created");
        assertEquals(created.version(), mService.getChannel(id).version(), "the version is stable");

        PlaylistService.ChannelUpdate updated = mService.updateChannel(id, created.version(),
            nbfm("Dispatch 2", 453_900_000));
        assertEquals("Dispatch 2", mChannelModel.getChannels().getFirst().getName());
        assertNotEquals(created.version(), updated.channel().version());

        //The old version is stale now
        ApiException stale = assertThrows(ApiException.class, () ->
            mService.updateChannel(id, created.version(), nbfm("Dispatch 3", 453_900_000)));
        assertEquals(412, stale.getStatus());
        assertEquals(428, assertThrows(ApiException.class, () ->
            mService.updateChannel(id, null, nbfm("Dispatch 3", 453_900_000))).getStatus());
        assertEquals(412, assertThrows(ApiException.class, () ->
            mService.deleteChannel(id, created.version())).getStatus());

        mService.deleteChannel(id, "W/" + updated.channel().version());
        assertTrue(mChannelModel.getChannels().isEmpty());
        assertEquals(404, assertThrows(ApiException.class, () -> mService.getChannel(id)).getStatus());
    }

    @Test
    void readConfigurationCanBeWrittenBack()
    {
        //A channel as the playlist loads it, with every configuration section
        Channel channel = new Channel("Loaded");
        io.github.dsheirer.source.config.SourceConfigTuner source = new io.github.dsheirer.source.config.SourceConfigTuner();
        source.setFrequency(453_850_000);
        channel.setSourceConfiguration(source);
        channel.setDecodeConfiguration(new DecodeConfigNBFM());
        mChannelModel.addChannel(channel);

        Versioned<ChannelConfig> read = mService.getChannel(channel.getChannelID());
        assertEquals("sourceConfigTuner", read.value().source().get("type").asText());

        //What a client reads it can send back unchanged
        PlaylistService.ChannelUpdate update = mService.updateChannel(channel.getChannelID(), read.version(),
            read.value());
        assertEquals(read.version(), update.channel().version(), "an unchanged write keeps the version");

        for(DecoderType type: DecoderType.PRIMARY_DECODERS)
        {
            JsonNode json = PlaylistJson.toTree(DecoderFactory.getDecodeConfiguration(type));
            assertFalse(json.get("type").isNull(), type + " type discriminator");
        }
    }

    @Test
    void desktopEditsChangeTheVersion()
    {
        AtomicInteger changes = new AtomicInteger();
        mChannelModel.addListener(event -> {
            if(event.getEvent() == ChannelEvent.Event.NOTIFICATION_CONFIGURATION_CHANGE)
            {
                changes.incrementAndGet();
            }
        });

        Versioned<ChannelConfig> created = mService.createChannel(nbfm("Dispatch", 453_850_000));
        Channel channel = mChannelModel.getChannels().getFirst();

        //A desktop editor changes a decoder setting directly on the channel
        ((DecodeConfigNBFM)channel.getDecodeConfiguration()).setSquelchNoiseOpenThreshold(0.3f);
        assertEquals(412, assertThrows(ApiException.class, () ->
            mService.updateChannel(created.value().id(), created.version(), nbfm("Web", 1))).getStatus());

        //The web update notifies the channel listeners, as the desktop save does
        int before = changes.get();
        mService.updateChannel(created.value().id(), "*", nbfm("Web", 453_850_000));
        assertTrue(changes.get() > before);
    }

    @Test
    void invalidChannelsListEveryError()
    {
        ObjectNode decode = MAPPER.createObjectNode();
        decode.put("type", "decodeConfigNoSuchDecoder");
        ChannelConfig bad = new ChannelConfig(null, " ", null, null, null, null, -1, null, null, decode, null, null,
            null);
        ApiException e = assertThrows(ApiException.class, () -> mService.createChannel(bad));
        assertEquals(400, e.getStatus());
        List<String> fields = e.getErrors().stream().map(ApiException.FieldError::field).toList();
        assertTrue(fields.containsAll(List.of("name", "autoStartOrder", "source", "decode")), fields.toString());
        assertTrue(mChannelModel.getChannels().isEmpty());
    }

    @Test
    void startNeedsChannelProcessing()
    {
        int id = mService.createChannel(nbfm("Dispatch", 453_850_000)).value().id();
        assertEquals(503, assertThrows(ApiException.class, () -> mService.start(id)).getStatus());
    }

    @Test
    void aliases() throws Exception
    {
        JsonNode ids = MAPPER.readTree("[{\"type\": \"talkgroup\", \"value\": 1201, \"protocol\": \"APCO25\"}," +
            "{\"type\": \"priority\", \"priority\": 2}, {\"type\": \"record\"}]");
        Versioned<AliasConfig> created = mService.createAlias(new AliasConfig(null, "Engine 4", "Fire", "Engines",
            "#ff0000", null, null, ids, null));
        long id = created.value().id();
        assertEquals("#ff0000", created.value().color());
        assertEquals(3, created.value().ids().size());

        Alias alias = mAliasModel.getAliasList("Fire").getTalkgroupAlias(Protocol.APCO25, 1201, null);
        assertNotNull(alias, "decoders can look the alias up");
        assertEquals(2, alias.getPlaybackPriority());
        assertTrue(alias.isRecordable());

        //Move the alias to another talkgroup
        JsonNode moved = MAPPER.readTree("[{\"type\": \"talkgroup\", \"value\": 1301, \"protocol\": \"APCO25\"}]");
        Versioned<AliasConfig> updated = mService.updateAlias(id, created.version(), new AliasConfig(null,
            "Engine 4", "Fire", "Engines", "#00ff00", null, null, moved, null));
        assertNotEquals(created.version(), updated.version());
        assertNull(mAliasModel.getAliasList("Fire").getTalkgroupAlias(Protocol.APCO25, 1201, null));
        assertNotNull(mAliasModel.getAliasList("Fire").getTalkgroupAlias(Protocol.APCO25, 1301, null));

        //A bad identifier leaves the alias unchanged
        JsonNode bad = MAPPER.readTree("[{\"type\": \"noSuchIdentifier\"}]");
        assertEquals(400, assertThrows(ApiException.class, () -> mService.updateAlias(id, "*",
            new AliasConfig(null, "X", "Fire", null, null, null, null, bad, null))).getStatus());
        assertEquals("Engine 4", mService.getAlias(id).value().name());

        assertEquals(1, mService.getAliasLists().get("Fire"));
        assertEquals(1, mService.listAliases("fire", 0, 10).size());

        mService.deleteAlias(id, updated.version());
        assertTrue(mAliasModel.getAliases().isEmpty());
        assertEquals(404, assertThrows(ApiException.class, () -> mService.getAlias(id)).getStatus());
    }

    @Test
    void unknownFieldsAreRefused() throws Exception
    {
        ObjectNode decode = (ObjectNode)PlaylistJson.toTree(new DecodeConfigNBFM());
        decode.put("squelchTypo", 1);
        ChannelConfig config = new ChannelConfig(null, "X", null, null, null, null, null, null,
            nbfm("X", 1).source(), decode, null, null, null);
        ApiException e = assertThrows(ApiException.class, () -> mService.createChannel(config));
        assertInstanceOf(ApiException.class, e);
        assertEquals("decode", e.getErrors().getFirst().field());
    }
}
