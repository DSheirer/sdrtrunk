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

package io.github.dsheirer.audio.broadcast.thinline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.controller.channel.map.ChannelRange;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.ltrnet.DecodeConfigLTRNet;
import io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.mpt1327.Sync;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.Encoding;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.Modulation;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import javafx.application.Platform;

/**
 * Reads and writes the SDRTrunk playlist for the Thinline Radio admin page.
 * Channel, alias, and channel-map edits are applied on the JavaFX thread and saved with the playlist.
 */
class ThinlineRadioPlaylist
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int ALIAS_PAGE = 200;
    private static final Set<String> CHANNEL_READ_ONLY = Set.of("channelID", "trafficChannel", "frequencyList",
        "decoderType", "sourceType");

    private final ChannelModel mChannelModel;
    private final ChannelProcessingManager mChannelProcessingManager;
    private final TunerManager mTunerManager;
    private final AliasModel mAliasModel;
    private final ChannelMapModel mChannelMapModel;
    private final Consumer<ObjectNode> mSender;

    ThinlineRadioPlaylist(ChannelModel channelModel, ChannelProcessingManager channelProcessingManager,
                          TunerManager tunerManager, AliasModel aliasModel, ChannelMapModel channelMapModel,
                          Consumer<ObjectNode> sender)
    {
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mTunerManager = tunerManager;
        mAliasModel = aliasModel;
        mChannelMapModel = channelMapModel;
        mSender = sender;
    }

    ThinlineRadioControl.Result publish()
    {
        mSender.accept(playlistMessage());
        return new ThinlineRadioControl.Result(true, "");
    }

    ThinlineRadioControl.Result aliases(JsonNode command)
    {
        mSender.accept(aliasMessage(command));
        return new ThinlineRadioControl.Result(true, "");
    }

    ThinlineRadioControl.Result channelSettings(JsonNode command)
    {
        Channel channel = findChannel(command.path("channelId").asInt(0));
        if(channel == null)
        {
            return new ThinlineRadioControl.Result(false, "Channel not found");
        }
        mSender.accept(channelSettingsMessage(channel));
        return new ThinlineRadioControl.Result(true, "");
    }

    ThinlineRadioControl.Result saveChannelSettings(JsonNode command)
    {
        int channelId = command.path("channelId").asInt(0);
        ThinlineRadioControl.Result result = onFx(() -> {
            Channel channel = findChannel(channelId);
            if(channel == null)
            {
                return new ThinlineRadioControl.Result(false, "Channel not found");
            }
            boolean running = mChannelProcessingManager.getProcessingChannels().contains(channel);
            try
            {
                if(running)
                {
                    mChannelProcessingManager.stop(channel);
                }
                ThinlineRadioSettings.apply(channel, command.path("values"), CHANNEL_READ_ONLY, Set.of());
                String name = channel.getName();
                channel.setName(" ");
                channel.setName(name);
                if(running)
                {
                    mChannelProcessingManager.start(channel);
                }
                return new ThinlineRadioControl.Result(true, "");
            }
            catch(Exception e)
            {
                return new ThinlineRadioControl.Result(false, e.getMessage() == null ? "Channel change failed" : e.getMessage());
            }
        });
        Channel channel = findChannel(channelId);
        if(channel != null)
        {
            mSender.accept(channelSettingsMessage(channel));
        }
        if(result.ok())
        {
            mSender.accept(playlistMessage());
        }
        return result;
    }

    ThinlineRadioControl.Result setAliasStream(JsonNode command)
    {
        String stream = text(command, "stream");
        boolean enabled = command.path("enabled").asBoolean();
        ThinlineRadioControl.Result result = onFx(() -> {
            Alias alias = findAlias(command.path("id").asText(""));
            if(alias == null)
            {
                return new ThinlineRadioControl.Result(false, "Alias not found");
            }
            setStreamed(alias, stream, enabled);
            return new ThinlineRadioControl.Result(true, "");
        });
        if(result.ok())
        {
            mSender.accept(aliasMessage(command));
        }
        return result;
    }

    ThinlineRadioControl.Result setAliasStreamAll(JsonNode command)
    {
        String stream = text(command, "stream");
        boolean enabled = command.path("enabled").asBoolean();
        ThinlineRadioControl.Result result = onFx(() -> {
            for(Alias alias: matchingAliases(command.path("list").asText(""), command.path("query").asText("")))
            {
                setStreamed(alias, stream, enabled);
            }
            return new ThinlineRadioControl.Result(true, "");
        });
        if(result.ok())
        {
            mSender.accept(aliasMessage(command));
        }
        return result;
    }

    private void setStreamed(Alias alias, String stream, boolean enabled)
    {
        if(stream.isBlank())
        {
            return;
        }
        BroadcastChannel existing = null;
        for(AliasID identifier: alias.getAliasIdentifiers())
        {
            if(identifier instanceof BroadcastChannel channel && stream.equals(channel.getChannelName()))
            {
                existing = channel;
                break;
            }
        }
        if(enabled && existing == null)
        {
            alias.addAliasID(new BroadcastChannel(stream));
        }
        else if(!enabled && existing != null)
        {
            alias.removeAliasID(existing);
        }
    }

    private ObjectNode channelSettingsMessage(Channel channel)
    {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "settings");
        body.put("scope", "channel");
        body.put("id", Integer.toString(channel.getChannelID()));
        body.put("title", text(channel.getName()));
        body.set("fields", ThinlineRadioSettings.describe(channel, CHANNEL_READ_ONLY, Set.of(), false));
        return body;
    }

    ThinlineRadioControl.Result saveChannel(JsonNode command, boolean create)
    {
        ThinlineRadioControl.Result result = onFx(() -> create ? addChannel(command) : updateChannel(command));
        if(result.ok())
        {
            mSender.accept(playlistMessage());
        }
        return result;
    }

    ThinlineRadioControl.Result deleteChannel(JsonNode command)
    {
        ThinlineRadioControl.Result result = onFx(() -> removeChannel(command.path("channelId").asInt(0)));
        if(result.ok())
        {
            mSender.accept(playlistMessage());
        }
        return result;
    }

    ThinlineRadioControl.Result cloneChannel(JsonNode command)
    {
        ThinlineRadioControl.Result result = onFx(() -> copyChannel(command.path("channelId").asInt(0)));
        if(result.ok())
        {
            mSender.accept(playlistMessage());
        }
        return result;
    }

    ThinlineRadioControl.Result saveAlias(JsonNode command)
    {
        ThinlineRadioControl.Result result = onFx(() -> writeAlias(command));
        if(result.ok())
        {
            mSender.accept(playlistMessage());
            mSender.accept(aliasMessage(command));
        }
        return result;
    }

    ThinlineRadioControl.Result deleteAlias(JsonNode command)
    {
        String list = command.path("list").asText("");
        ThinlineRadioControl.Result result = onFx(() -> removeAlias(command.path("id").asText("")));
        if(result.ok())
        {
            mSender.accept(playlistMessage());
            mSender.accept(aliasMessage(command));
        }
        return result;
    }

    ThinlineRadioControl.Result saveChannelMap(JsonNode command)
    {
        ThinlineRadioControl.Result result = onFx(() -> writeChannelMap(command));
        if(result.ok())
        {
            mSender.accept(playlistMessage());
        }
        return result;
    }

    ThinlineRadioControl.Result deleteChannelMap(JsonNode command)
    {
        ThinlineRadioControl.Result result = onFx(() -> removeChannelMap(command.path("name").asText("")));
        if(result.ok())
        {
            mSender.accept(playlistMessage());
        }
        return result;
    }

    private ThinlineRadioControl.Result addChannel(JsonNode command)
    {
        DecoderType decoder = decoder(command.path("decoder").asText(""));
        if(decoder == null)
        {
            return new ThinlineRadioControl.Result(false, "Choose a decoder");
        }
        Channel channel = new Channel();
        channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(decoder));
        ThinlineRadioControl.Result applied = applyChannel(channel, command, false);
        if(!applied.ok())
        {
            return applied;
        }
        mChannelModel.addChannel(channel);
        return new ThinlineRadioControl.Result(true, "");
    }

    private ThinlineRadioControl.Result updateChannel(JsonNode command)
    {
        Channel channel = findChannel(command.path("channelId").asInt(0));
        if(channel == null)
        {
            return new ThinlineRadioControl.Result(false, "Channel not found");
        }
        boolean running = mChannelProcessingManager.getProcessingChannels().contains(channel);
        if(running)
        {
            try
            {
                mChannelProcessingManager.stop(channel);
            }
            catch(Exception e)
            {
                return new ThinlineRadioControl.Result(false, e.getMessage() == null ? "Could not stop the channel" : e.getMessage());
            }
        }
        ThinlineRadioControl.Result applied = applyChannel(channel, command, true);
        if(running)
        {
            try
            {
                mChannelProcessingManager.start(channel);
            }
            catch(Exception e)
            {
                String message = e.getMessage() == null ? "Channel was saved but did not restart" : e.getMessage();
                if(!applied.ok())
                {
                    return applied;
                }
                return new ThinlineRadioControl.Result(false, message);
            }
        }
        return applied;
    }

    private ThinlineRadioControl.Result applyChannel(Channel channel, JsonNode command, boolean touchName)
    {
        DecoderType decoder = decoder(command.path("decoder").asText(""));
        if(decoder == null)
        {
            return new ThinlineRadioControl.Result(false, "Choose a decoder");
        }
        try
        {
            if(channel.getDecodeConfiguration() == null || channel.getDecodeConfiguration().getDecoderType() != decoder)
            {
                channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(decoder));
            }
            applyDecoder(channel.getDecodeConfiguration(), command);
            applySource(channel, command);
            channel.setSystem(text(command, "system"));
            channel.setSite(text(command, "site"));
            channel.setAliasListName(blankToNull(text(command, "aliasList")));
            channel.setAutoStart(command.path("autoStart").asBoolean(false));
            int order = command.path("order").asInt(0);
            channel.setAutoStartOrder(order < 1 ? null : order);
            String name = text(command, "name");
            if(touchName)
            {
                channel.setName(" ");
            }
            channel.setName(name);
            return new ThinlineRadioControl.Result(true, "");
        }
        catch(IllegalArgumentException e)
        {
            return new ThinlineRadioControl.Result(false, e.getMessage() == null ? "Invalid channel" : e.getMessage());
        }
    }

    private void applyDecoder(DecodeConfiguration configuration, JsonNode command)
    {
        if(configuration instanceof DecodeConfigP25Phase1 phase1 && !text(command, "modulation").isBlank())
        {
            phase1.setModulation(Modulation.valueOf(text(command, "modulation")));
        }
        if(configuration instanceof DecodeConfigP25 p25)
        {
            p25.setIgnoreDataCalls(command.path("ignoreDataCalls").asBoolean(false));
            int pool = command.path("trafficChannelPoolSize").asInt(0);
            if(pool > 0)
            {
                p25.setTrafficChannelPoolSize(pool);
            }
        }
        if(configuration instanceof DecodeConfigP25Phase2 phase2)
        {
            phase2.setAutoDetectScrambleParameters(command.path("autoDetectScramble").asBoolean(true));
        }
        if(configuration instanceof DecodeConfigAnalog analog && !text(command, "bandwidth").isBlank())
        {
            analog.setBandwidth(DecodeConfigAnalog.Bandwidth.valueOf(text(command, "bandwidth")));
        }
        if(configuration instanceof DecodeConfigAnalog analog && command.has("talkgroup"))
        {
            int talkgroup = command.path("talkgroup").asInt(1);
            if(talkgroup > 0)
            {
                analog.setTalkgroup(talkgroup);
            }
        }
        if(configuration instanceof DecodeConfigAM am)
        {
            am.setSquelchThreshold(command.path("squelchThreshold").asInt(am.getSquelchThreshold()));
            am.setSquelchAutoTrack(command.path("squelchAutoTrack").asBoolean(false));
        }
        if(configuration instanceof DecodeConfigNBFM nbfm)
        {
            nbfm.setAudioFilter(command.path("audioFilter").asBoolean(true));
        }
        if(configuration instanceof DecodeConfigDMR dmr)
        {
            dmr.setIgnoreDataCalls(command.path("ignoreDataCalls").asBoolean(false));
            int pool = command.path("trafficChannelPoolSize").asInt(0);
            if(pool > 0)
            {
                dmr.setTrafficChannelPoolSize(pool);
            }
            if(command.has("timeslots"))
            {
                List<TimeslotFrequency> timeslots = new ArrayList<>();
                for(JsonNode item: command.path("timeslots"))
                {
                    TimeslotFrequency frequency = new TimeslotFrequency();
                    frequency.setNumber(item.path("number").asInt(1));
                    frequency.setDownlinkFrequency(item.path("downlinkHz").asLong(0));
                    frequency.setUplinkFrequency(item.path("uplinkHz").asLong(0));
                    timeslots.add(frequency);
                }
                dmr.setTimeslotMap(timeslots);
            }
        }
        if(configuration instanceof DecodeConfigNXDN nxdn)
        {
            if(!text(command, "transmissionMode").isBlank())
            {
                nxdn.setTransmissionMode(TransmissionMode.valueOf(text(command, "transmissionMode")));
            }
            int pool = command.path("trafficChannelPoolSize").asInt(0);
            if(pool > 0)
            {
                nxdn.setTrafficChannelPoolSize(pool);
            }
            if(command.has("nxdnChannels"))
            {
                List<ChannelFrequency> channels = new ArrayList<>();
                for(JsonNode item: command.path("nxdnChannels"))
                {
                    channels.add(new ChannelFrequency(item.path("channel").asInt(1), item.path("downlinkHz").asLong(0),
                        item.path("uplinkHz").asLong(0)));
                }
                nxdn.setChannelMap(channels);
            }
        }
        if(configuration instanceof DecodeConfigMPT1327 mpt)
        {
            mpt.setChannelMapName(blankToNull(text(command, "channelMap")));
            if(!text(command, "sync").isBlank())
            {
                mpt.setSync(Sync.valueOf(text(command, "sync")));
            }
            int timeout = command.path("callTimeout").asInt(0);
            if(timeout > 0)
            {
                mpt.setCallTimeoutSeconds(timeout);
            }
            int pool = command.path("trafficChannelPoolSize").asInt(0);
            if(pool > 0)
            {
                mpt.setTrafficChannelPoolSize(pool);
            }
        }
        if(configuration instanceof DecodeConfigLTRStandard ltr && !text(command, "messageDirection").isBlank())
        {
            ltr.setMessageDirection(MessageDirection.valueOf(text(command, "messageDirection")));
        }
        if(configuration instanceof DecodeConfigLTRNet ltrNet && !text(command, "messageDirection").isBlank())
        {
            ltrNet.setMessageDirection(MessageDirection.valueOf(text(command, "messageDirection")));
        }
    }

    private void applySource(Channel channel, JsonNode command)
    {
        List<Long> frequencies = new ArrayList<>();
        for(JsonNode item: command.path("frequencies"))
        {
            long frequency = item.asLong(0);
            if(frequency > 0)
            {
                frequencies.add(frequency);
            }
        }
        String preferred = blankToNull(text(command, "preferredTuner"));
        if(frequencies.size() <= 1)
        {
            SourceConfigTuner source = channel.getSourceConfiguration() instanceof SourceConfigTuner tuner ? tuner : new SourceConfigTuner();
            source.setFrequency(frequencies.isEmpty() ? 0 : frequencies.get(0));
            source.setPreferredTuner(preferred);
            channel.setSourceConfiguration(source);
            return;
        }

        SourceConfigTunerMultipleFrequency source = channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency multiple ?
            multiple : new SourceConfigTunerMultipleFrequency();
        source.setFrequencies(frequencies);
        source.setPreferredTuner(preferred);
        if(command.has("rotationDelay"))
        {
            source.setFrequencyRotationDelay(command.path("rotationDelay").asInt(500));
        }
        channel.setSourceConfiguration(source);
    }

    private ThinlineRadioControl.Result removeChannel(int channelId)
    {
        Channel channel = findChannel(channelId);
        if(channel == null)
        {
            return new ThinlineRadioControl.Result(false, "Channel not found");
        }
        if(mChannelProcessingManager.getProcessingChannels().contains(channel))
        {
            try
            {
                mChannelProcessingManager.stop(channel);
            }
            catch(Exception e)
            {
                return new ThinlineRadioControl.Result(false, e.getMessage() == null ? "Could not stop the channel" : e.getMessage());
            }
        }
        mChannelModel.removeChannel(channel);
        return new ThinlineRadioControl.Result(true, "");
    }

    private ThinlineRadioControl.Result copyChannel(int channelId)
    {
        Channel channel = findChannel(channelId);
        if(channel == null)
        {
            return new ThinlineRadioControl.Result(false, "Channel not found");
        }
        Channel copy = channel.copyOf();
        copy.setName(uniqueName(copy.getSystem(), copy.getSite(), copy.getName()));
        copy.setAutoStart(false);
        mChannelModel.addChannel(copy);
        return new ThinlineRadioControl.Result(true, "");
    }

    private String uniqueName(String system, String site, String name)
    {
        String base = (name == null || name.isBlank() ? "Channel" : name) + " Copy";
        String candidate = base;
        int suffix = 2;
        while(findNamed(system, site, candidate) != null)
        {
            candidate = base + " " + suffix++;
        }
        return candidate;
    }

    private ThinlineRadioControl.Result writeAlias(JsonNode command)
    {
        String list = text(command, "list");
        String name = text(command, "name");
        if(list.isBlank() || AliasModel.NO_ALIAS_LIST.equals(list))
        {
            return new ThinlineRadioControl.Result(false, "Choose an alias list name");
        }
        if(name.isBlank())
        {
            return new ThinlineRadioControl.Result(false, "Alias needs a name");
        }

        String id = command.path("id").asText("");
        Alias alias = id.isBlank() ? null : findAlias(id);
        if(!id.isBlank() && alias == null)
        {
            return new ThinlineRadioControl.Result(false, "Alias not found");
        }
        if(alias == null)
        {
            alias = new Alias(name);
            alias.setAliasListName(list);
            applyAlias(alias, command);
            mAliasModel.addAlias(alias);
            return new ThinlineRadioControl.Result(true, "");
        }

        String previous = alias.getAliasListName();
        if(previous != null && !previous.equals(list))
        {
            mAliasModel.getAliasList(previous).removeAlias(alias);
        }
        alias.setName(name);
        alias.setAliasListName(list);
        applyAlias(alias, command);
        mAliasModel.addAliasList(list);
        mAliasModel.getAliasList(list).addAlias(alias);
        return new ThinlineRadioControl.Result(true, "");
    }

    private void applyAlias(Alias alias, JsonNode command)
    {
        alias.setGroup(text(command, "group"));
        alias.setRecordable(command.path("record").asBoolean(true));
        if(command.has("priority"))
        {
            alias.setCallPriority(command.path("priority").asInt(100));
        }
        if(command.has("streams"))
        {
            List<String> wanted = new ArrayList<>();
            command.path("streams").forEach(stream -> wanted.add(stream.asText("")));
            List<AliasID> current = new ArrayList<>(alias.getAliasIdentifiers());
            for(AliasID identifier: current)
            {
                if(identifier instanceof BroadcastChannel channel && !wanted.contains(channel.getChannelName()))
                {
                    alias.removeAliasID(identifier);
                }
            }
            for(String stream: wanted)
            {
                setStreamed(alias, stream, true);
            }
        }
        if(!command.has("talkgroups"))
        {
            return;
        }

        List<AliasID> remove = new ArrayList<>();
        for(AliasID identifier: alias.getAliasIdentifiers())
        {
            if(identifier instanceof Talkgroup || identifier instanceof TalkgroupRange)
            {
                remove.add(identifier);
            }
        }
        for(AliasID identifier: remove)
        {
            alias.removeAliasID(identifier);
        }
        for(JsonNode item: command.path("talkgroups"))
        {
            Protocol protocol = Protocol.valueOf(item.path("protocol").asText("APCO25"));
            if("range".equals(item.path("kind").asText("talkgroup")))
            {
                alias.addAliasID(new TalkgroupRange(protocol, item.path("min").asInt(0), item.path("max").asInt(0)));
            }
            else
            {
                alias.addAliasID(new Talkgroup(protocol, item.path("value").asInt(0)));
            }
        }
    }

    private ThinlineRadioControl.Result removeAlias(String id)
    {
        Alias alias = findAlias(id);
        if(alias == null)
        {
            return new ThinlineRadioControl.Result(false, "Alias not found");
        }
        mAliasModel.removeAlias(alias);
        return new ThinlineRadioControl.Result(true, "");
    }

    private ThinlineRadioControl.Result writeChannelMap(JsonNode command)
    {
        String name = text(command, "name");
        String original = text(command, "originalName");
        if(name.isBlank())
        {
            return new ThinlineRadioControl.Result(false, "Channel map needs a name");
        }

        ChannelMap map = original.isBlank() ? null : findChannelMap(original);
        if(map == null && !original.isBlank())
        {
            return new ThinlineRadioControl.Result(false, "Channel map not found");
        }
        if(map == null)
        {
            if(findChannelMap(name) != null)
            {
                return new ThinlineRadioControl.Result(false, "A channel map with that name already exists");
            }
            map = new ChannelMap(name);
            applyRanges(map, command.path("ranges"));
            mChannelMapModel.addChannelMap(map);
            return new ThinlineRadioControl.Result(true, "");
        }

        if(!name.equals(original))
        {
            if(findChannelMap(name) != null)
            {
                return new ThinlineRadioControl.Result(false, "A channel map with that name already exists");
            }
            map.setName(name);
            for(Channel channel: mChannelModel.getChannels())
            {
                if(channel.getDecodeConfiguration() instanceof DecodeConfigMPT1327 mpt && original.equals(mpt.getChannelMapName()))
                {
                    mpt.setChannelMapName(name);
                    String channelName = channel.getName();
                    channel.setName(" ");
                    channel.setName(channelName);
                }
            }
        }
        applyRanges(map, command.path("ranges"));
        return new ThinlineRadioControl.Result(true, "");
    }

    private void applyRanges(ChannelMap map, JsonNode ranges)
    {
        List<ChannelRange> next = new ArrayList<>();
        if(ranges != null && ranges.isArray())
        {
            for(JsonNode item: ranges)
            {
                next.add(new ChannelRange(item.path("first").asInt(1), item.path("last").asInt(1),
                    item.path("baseHz").asInt(0), item.path("stepHz").asInt(12500)));
            }
        }
        map.setRanges(next);
    }

    private ThinlineRadioControl.Result removeChannelMap(String name)
    {
        ChannelMap map = findChannelMap(name);
        if(map == null)
        {
            return new ThinlineRadioControl.Result(false, "Channel map not found");
        }
        for(Channel channel: mChannelModel.getChannels())
        {
            if(channel.getDecodeConfiguration() instanceof DecodeConfigMPT1327 mpt && name.equals(mpt.getChannelMapName()))
            {
                return new ThinlineRadioControl.Result(false, "A channel still uses this map");
            }
        }
        mChannelMapModel.removeChannelMap(map);
        return new ThinlineRadioControl.Result(true, "");
    }

    private ObjectNode playlistMessage()
    {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "playlist");
        body.set("channels", channelNodes());
        body.set("aliasLists", aliasListNodes());
        body.set("channelMaps", channelMapNodes());
        body.set("tuners", tunerNames());
        body.set("decoders", named(DecoderType.PRIMARY_DECODERS, DecoderType::name, DecoderType::getDisplayString));
        body.set("protocols", named(Protocol.TALKGROUP_PROTOCOLS, Protocol::name, Protocol::toString));
        body.set("modulations", named(List.of(Modulation.values()), Modulation::name, Modulation::getLabel));
        body.set("bandwidths", bandwidths());
        body.set("nxdnModes", named(List.of(TransmissionMode.values()), TransmissionMode::name, TransmissionMode::toString));
        body.set("directions", named(List.of(MessageDirection.values()), MessageDirection::name, MessageDirection::toString));
        body.set("mptSync", named(List.of(Sync.values()), Sync::name, Sync::toString));
        body.set("nxdnEncodings", named(List.of(Encoding.values()), Encoding::name, Encoding::toString));
        return body;
    }

    private ArrayNode channelNodes()
    {
        ArrayNode channels = MAPPER.createArrayNode();
        for(Channel channel: snapshotChannels())
        {
            if(channel == null || channel.isTrafficChannel())
            {
                continue;
            }
            ObjectNode node = channels.addObject();
            node.put("id", channel.getChannelID());
            node.put("system", text(channel.getSystem()));
            node.put("site", text(channel.getSite()));
            node.put("name", text(channel.getName()));
            node.put("aliasList", text(channel.getAliasListName()));
            node.put("autoStart", channel.isAutoStart());
            node.put("order", channel.getAutoStartOrder() == null ? 0 : channel.getAutoStartOrder());
            node.put("processing", mChannelProcessingManager.getProcessingChannels().contains(channel));
            DecodeConfiguration configuration = channel.getDecodeConfiguration();
            node.put("decoder", configuration == null ? "" : configuration.getDecoderType().name());
            node.put("decoderLabel", configuration == null ? "" : configuration.getDecoderType().getDisplayString());
            writeDecoder(node, configuration);
            writeSource(node, channel);
        }
        return channels;
    }

    private void writeDecoder(ObjectNode node, DecodeConfiguration configuration)
    {
        node.put("modulation", "");
        node.put("ignoreDataCalls", false);
        node.put("trafficChannelPoolSize", DecodeConfiguration.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
        node.put("autoDetectScramble", true);
        node.put("bandwidth", "");
        node.put("talkgroup", 1);
        node.put("squelchThreshold", 0);
        node.put("squelchAutoTrack", false);
        node.put("audioFilter", true);
        node.put("channelMap", "");
        node.put("messageDirection", MessageDirection.OSW.name());
        node.put("sync", Sync.NORMAL.name());
        node.put("callTimeout", 10);
        node.put("transmissionMode", "");
        node.putArray("timeslots");
        node.putArray("nxdnChannels");
        if(configuration == null)
        {
            return;
        }
        if(configuration instanceof DecodeConfigP25Phase1 phase1 && phase1.getModulation() != null)
        {
            node.put("modulation", phase1.getModulation().name());
        }
        if(configuration instanceof DecodeConfigP25 p25)
        {
            node.put("ignoreDataCalls", p25.getIgnoreDataCalls());
            node.put("trafficChannelPoolSize", p25.getTrafficChannelPoolSize());
        }
        if(configuration instanceof DecodeConfigP25Phase2 phase2)
        {
            node.put("autoDetectScramble", phase2.isAutoDetectScrambleParameters());
        }
        if(configuration instanceof DecodeConfigAnalog analog)
        {
            if(analog.getBandwidth() != null)
            {
                node.put("bandwidth", analog.getBandwidth().name());
            }
            node.put("talkgroup", analog.getTalkgroup());
        }
        if(configuration instanceof DecodeConfigAM am)
        {
            node.put("squelchThreshold", am.getSquelchThreshold());
            node.put("squelchAutoTrack", am.isSquelchAutoTrack());
        }
        if(configuration instanceof DecodeConfigNBFM nbfm)
        {
            node.put("audioFilter", nbfm.isAudioFilter());
        }
        if(configuration instanceof DecodeConfigDMR dmr)
        {
            node.put("ignoreDataCalls", dmr.getIgnoreDataCalls());
            node.put("trafficChannelPoolSize", dmr.getTrafficChannelPoolSize());
            ArrayNode timeslots = node.putArray("timeslots");
            if(dmr.getTimeslotMap() != null)
            {
                for(TimeslotFrequency frequency: dmr.getTimeslotMap())
                {
                    ObjectNode item = timeslots.addObject();
                    item.put("number", frequency.getNumber());
                    item.put("downlinkHz", frequency.getDownlinkFrequency());
                    item.put("uplinkHz", frequency.getUplinkFrequency());
                }
            }
        }
        if(configuration instanceof DecodeConfigNXDN nxdn)
        {
            if(nxdn.getTransmissionMode() != null)
            {
                node.put("transmissionMode", nxdn.getTransmissionMode().name());
            }
            node.put("trafficChannelPoolSize", nxdn.getTrafficChannelPoolSize());
            ArrayNode mapped = node.putArray("nxdnChannels");
            if(nxdn.getChannelMap() != null)
            {
                for(ChannelFrequency frequency: nxdn.getChannelMap())
                {
                    ObjectNode item = mapped.addObject();
                    item.put("channel", frequency.getChannel());
                    item.put("downlinkHz", frequency.getDownlink());
                    item.put("uplinkHz", frequency.getUplink());
                }
            }
        }
        if(configuration instanceof DecodeConfigMPT1327 mpt)
        {
            node.put("channelMap", text(mpt.getChannelMapName()));
            if(mpt.getSync() != null)
            {
                node.put("sync", mpt.getSync().name());
            }
            node.put("callTimeout", mpt.getCallTimeoutSeconds());
            node.put("trafficChannelPoolSize", mpt.getTrafficChannelPoolSize());
        }
        if(configuration instanceof DecodeConfigLTRStandard ltr && ltr.getMessageDirection() != null)
        {
            node.put("messageDirection", ltr.getMessageDirection().name());
        }
        if(configuration instanceof DecodeConfigLTRNet ltrNet && ltrNet.getMessageDirection() != null)
        {
            node.put("messageDirection", ltrNet.getMessageDirection().name());
        }
    }

    private void writeSource(ObjectNode node, Channel channel)
    {
        ArrayNode frequencies = node.putArray("frequencies");
        node.put("preferredTuner", "");
        node.put("rotationDelay", 500);
        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency multiple)
        {
            if(multiple.getFrequencies() != null)
            {
                for(Long frequency: multiple.getFrequencies())
                {
                    if(frequency != null && frequency > 0)
                    {
                        frequencies.add(frequency);
                    }
                }
            }
            node.put("preferredTuner", text(multiple.getPreferredTuner()));
            node.put("rotationDelay", multiple.getFrequencyRotationDelay());
        }
        else if(channel.getSourceConfiguration() instanceof SourceConfigTuner tuner)
        {
            if(tuner.getFrequency() > 0)
            {
                frequencies.add(tuner.getFrequency());
            }
            node.put("preferredTuner", text(tuner.getPreferredTuner()));
        }
    }

    private ArrayNode aliasListNodes()
    {
        ArrayNode lists = MAPPER.createArrayNode();
        for(String name: mAliasModel.getListNames())
        {
            if(name == null || name.isBlank() || AliasModel.NO_ALIAS_LIST.equals(name))
            {
                continue;
            }
            int count = 0;
            for(Alias alias: mAliasModel.aliasList())
            {
                if(name.equals(alias.getAliasListName()))
                {
                    count++;
                }
            }
            ObjectNode node = lists.addObject();
            node.put("name", name);
            node.put("count", count);
        }
        return lists;
    }

    private ObjectNode aliasMessage(JsonNode command)
    {
        String list = command.path("list").asText("");
        String query = command.path("query").asText("");
        int offset = command.path("offset").asInt(0);
        List<Alias> matched = matchingAliases(list, query);
        int start = Math.max(0, offset);
        if(start > matched.size())
        {
            start = 0;
        }
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "aliases");
        body.put("purpose", command.path("purpose").asText(""));
        body.put("list", list);
        body.put("query", query == null ? "" : query);
        body.put("offset", start);
        body.put("total", matched.size());
        ArrayNode aliases = body.putArray("aliases");
        for(int index = start; index < matched.size() && aliases.size() < ALIAS_PAGE; index++)
        {
            aliases.add(aliasNode(matched.get(index)));
        }
        return body;
    }

    private List<Alias> matchingAliases(String list, String query)
    {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Alias> matched = new ArrayList<>();
        for(Alias alias: mAliasModel.aliasList())
        {
            if(alias == null || (!list.isBlank() && !list.equals(text(alias.getAliasListName()))))
            {
                continue;
            }
            if(!needle.isEmpty() && !aliasText(alias).contains(needle))
            {
                continue;
            }
            matched.add(alias);
        }
        return matched;
    }

    private ObjectNode aliasNode(Alias alias)
    {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", Integer.toString(System.identityHashCode(alias)));
        node.put("name", text(alias.getName()));
        node.put("group", text(alias.getGroup()));
        node.put("list", text(alias.getAliasListName()));
        node.put("record", alias.isRecordable());
        node.put("priority", alias.priorityProperty().get());
        ArrayNode talkgroups = node.putArray("talkgroups");
        ArrayNode streams = node.putArray("streams");
        int extras = 0;
        for(AliasID identifier: alias.getAliasIdentifiers())
        {
            if(identifier instanceof Talkgroup talkgroup)
            {
                ObjectNode item = talkgroups.addObject();
                item.put("kind", "talkgroup");
                item.put("protocol", talkgroup.getProtocol() == null ? Protocol.APCO25.name() : talkgroup.getProtocol().name());
                item.put("value", talkgroup.getValue());
                item.put("min", talkgroup.getValue());
                item.put("max", talkgroup.getValue());
            }
            else if(identifier instanceof TalkgroupRange range)
            {
                ObjectNode item = talkgroups.addObject();
                item.put("kind", "range");
                item.put("protocol", range.getProtocol() == null ? Protocol.APCO25.name() : range.getProtocol().name());
                item.put("value", range.getMinTalkgroup());
                item.put("min", range.getMinTalkgroup());
                item.put("max", range.getMaxTalkgroup());
            }
            else if(identifier instanceof BroadcastChannel channel)
            {
                streams.add(text(channel.getChannelName()));
            }
            else
            {
                extras++;
            }
        }
        node.put("extras", extras);
        return node;
    }

    private String aliasText(Alias alias)
    {
        StringBuilder text = new StringBuilder();
        text.append(text(alias.getName()).toLowerCase(Locale.ROOT)).append(' ');
        text.append(text(alias.getGroup()).toLowerCase(Locale.ROOT)).append(' ');
        for(AliasID identifier: alias.getAliasIdentifiers())
        {
            if(identifier instanceof Talkgroup talkgroup)
            {
                text.append(talkgroup.getValue()).append(' ');
            }
            else if(identifier instanceof TalkgroupRange range)
            {
                text.append(range.getMinTalkgroup()).append(' ').append(range.getMaxTalkgroup()).append(' ');
            }
        }
        return text.toString();
    }

    private ArrayNode channelMapNodes()
    {
        ArrayNode maps = MAPPER.createArrayNode();
        for(ChannelMap map: mChannelMapModel.getChannelMaps())
        {
            ObjectNode node = maps.addObject();
            node.put("name", text(map.getName()));
            ArrayNode ranges = node.putArray("ranges");
            for(ChannelRange range: map.getRanges())
            {
                ObjectNode item = ranges.addObject();
                item.put("first", range.getFirstChannelNumber());
                item.put("last", range.getLastChannelNumber());
                item.put("baseHz", range.getBaseFrequency());
                item.put("stepHz", range.getStepSize());
            }
        }
        return maps;
    }

    private ArrayNode tunerNames()
    {
        ArrayNode names = MAPPER.createArrayNode();
        try
        {
            for(DiscoveredTuner discovered: mTunerManager.getDiscoveredTuners())
            {
                if(discovered != null && discovered.hasTuner())
                {
                    Tuner tuner = discovered.getTuner();
                    if(tuner != null && tuner.getPreferredName() != null && !tuner.getPreferredName().isBlank())
                    {
                        names.add(tuner.getPreferredName());
                    }
                }
            }
        }
        catch(Exception ignored)
        {
        }
        return names;
    }

    private ObjectNode bandwidths()
    {
        ObjectNode node = MAPPER.createObjectNode();
        node.set("AM", named(DecodeConfigAnalog.Bandwidth.AM_BANDWIDTHS, DecodeConfigAnalog.Bandwidth::name,
            DecodeConfigAnalog.Bandwidth::toString));
        node.set("NBFM", named(DecodeConfigAnalog.Bandwidth.FM_BANDWIDTHS, DecodeConfigAnalog.Bandwidth::name,
            DecodeConfigAnalog.Bandwidth::toString));
        return node;
    }

    private <T> ArrayNode named(Iterable<T> values, java.util.function.Function<T,String> name,
                                java.util.function.Function<T,String> label)
    {
        ArrayNode array = MAPPER.createArrayNode();
        for(T value: values)
        {
            ObjectNode node = array.addObject();
            node.put("name", name.apply(value));
            node.put("label", label.apply(value));
        }
        return array;
    }

    private Channel findChannel(int channelId)
    {
        for(Channel channel: snapshotChannels())
        {
            if(channel != null && channel.getChannelID() == channelId)
            {
                return channel;
            }
        }
        return null;
    }

    private Channel findNamed(String system, String site, String name)
    {
        for(Channel channel: snapshotChannels())
        {
            if(channel != null && text(channel.getSystem()).equals(text(system)) && text(channel.getSite()).equals(text(site)) &&
                text(channel.getName()).equals(text(name)))
            {
                return channel;
            }
        }
        return null;
    }

    private Alias findAlias(String id)
    {
        for(Alias alias: mAliasModel.aliasList())
        {
            if(Integer.toString(System.identityHashCode(alias)).equals(id))
            {
                return alias;
            }
        }
        return null;
    }

    private ChannelMap findChannelMap(String name)
    {
        for(ChannelMap map: mChannelMapModel.getChannelMaps())
        {
            if(text(map.getName()).equals(name))
            {
                return map;
            }
        }
        return null;
    }

    private List<Channel> snapshotChannels()
    {
        try
        {
            return new ArrayList<>(mChannelModel.getChannels());
        }
        catch(Exception e)
        {
            return List.of();
        }
    }

    private DecoderType decoder(String name)
    {
        if(name == null || name.isBlank())
        {
            return null;
        }
        try
        {
            DecoderType decoder = DecoderType.valueOf(name);
            return DecoderType.PRIMARY_DECODERS.contains(decoder) ? decoder : null;
        }
        catch(IllegalArgumentException e)
        {
            return null;
        }
    }

    static ThinlineRadioControl.Result onFx(Supplier<ThinlineRadioControl.Result> work)
    {
        if(Platform.isFxApplicationThread())
        {
            try
            {
                return work.get();
            }
            catch(Exception e)
            {
                return new ThinlineRadioControl.Result(false, e.getMessage() == null ? "Playlist change failed" : e.getMessage());
            }
        }

        AtomicReference<ThinlineRadioControl.Result> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try
            {
                result.set(work.get());
            }
            catch(Exception e)
            {
                result.set(new ThinlineRadioControl.Result(false, e.getMessage() == null ? "Playlist change failed" : e.getMessage()));
            }
            finally
            {
                done.countDown();
            }
        });
        try
        {
            if(!done.await(8, TimeUnit.SECONDS))
            {
                return new ThinlineRadioControl.Result(false, "SDRTrunk did not finish the playlist change");
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return new ThinlineRadioControl.Result(false, "Interrupted");
        }
        return result.get() == null ? new ThinlineRadioControl.Result(false, "Playlist change failed") : result.get();
    }

    private static String text(JsonNode command, String field)
    {
        JsonNode node = command.get(field);
        return node == null || node.isNull() ? "" : node.asText("");
    }

    private static String text(String value)
    {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value)
    {
        return value == null || value.isBlank() ? null : value;
    }
}
