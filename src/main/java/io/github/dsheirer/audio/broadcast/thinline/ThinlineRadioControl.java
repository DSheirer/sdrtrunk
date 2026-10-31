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
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastFactory;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.thinline.ThinlineRadioPreference;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live control surface for the Thinline Radio admin page: tuner, channel, and spectrum commands.
 */
class ThinlineRadioControl
{
    private static final Logger mLog = LoggerFactory.getLogger(ThinlineRadioControl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SPECTRUM_BINS = 1024;
    private static final long SPECTRUM_INTERVAL_MS = 100;
    private static final Set<String> TUNER_READ_ONLY = Set.of("uniqueID", "minimumFrequency", "maximumFrequency");
    private static final Set<String> STREAM_READ_ONLY = Set.of("id", "name", "broadcastServerType", "broadcastFormat");
    private static final Set<String> PREFERENCE_HIDDEN = Set.of("password");

    private final ChannelModel mChannelModel;
    private final ChannelProcessingManager mChannelProcessingManager;
    private final TunerManager mTunerManager;
    private final Consumer<ObjectNode> mSender;
    private final Supplier<List<ConfiguredBroadcast>> mBroadcasts;
    private final ThinlineRadioPlaylist mPlaylist;
    private final UserPreferences mUserPreferences;
    private final BroadcastModel mBroadcastModel;
    private final Supplier<ObjectNode> mPermissions;

    private volatile boolean mWatching;
    private ScheduledFuture<?> mWatchFuture;
    private ComplexDftProcessor<INativeBuffer> mSpectrum;
    private ComplexDecibelConverter mSpectrumConverter;
    private DiscoveredTuner mSpectrumTuner;
    private final AtomicLong mLastSpectrumAt = new AtomicLong();

    record Result(boolean ok, String error) {}

    ThinlineRadioControl(UserPreferences userPreferences, BroadcastModel broadcastModel, ChannelModel channelModel,
                         ChannelProcessingManager channelProcessingManager, TunerManager tunerManager,
                         AliasModel aliasModel, ChannelMapModel channelMapModel, Consumer<ObjectNode> sender,
                         Supplier<List<ConfiguredBroadcast>> broadcasts, Supplier<ObjectNode> permissions)
    {
        mUserPreferences = userPreferences;
        mBroadcastModel = broadcastModel;
        mPermissions = permissions;
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mTunerManager = tunerManager;
        mSender = sender;
        mBroadcasts = broadcasts;
        mPlaylist = new ThinlineRadioPlaylist(channelModel, channelProcessingManager, tunerManager, aliasModel,
            channelMapModel, sender);
    }

    void setWatching(boolean watching)
    {
        mWatching = watching;
        if(!watching)
        {
            stopSpectrum();
            if(mWatchFuture != null)
            {
                mWatchFuture.cancel(false);
                mWatchFuture = null;
            }
            return;
        }

        if(mWatchFuture == null || mWatchFuture.isCancelled())
        {
            mWatchFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::publishState, 0, 1, TimeUnit.SECONDS);
        }
        mPlaylist.publish();
    }

    void stop()
    {
        setWatching(false);
    }

    Result handle(JsonNode command)
    {
        String name = command.path("command").asText("");
        return switch(name)
        {
            case "watch" ->
            {
                setWatching(command.path("enabled").asBoolean());
                yield new Result(true, "");
            }
            case "setTuner" -> setTunerEnabled(command.path("tunerId").asText(""), command.path("enabled").asBoolean());
            case "setPpm" -> setPpm(command.path("tunerId").asText(""), command.path("ppm").asDouble());
            case "setFrequency" -> setFrequency(command.path("tunerId").asText(""), command.path("frequencyHz").asLong());
            case "setChannel" -> setChannel(command.path("channelId").asInt(0), command.path("system").asText(""),
                command.path("site").asText(""), command.path("name").asText(""), command.path("enabled").asBoolean());
            case "spectrum" -> spectrum(command.path("tunerId").asText(""), command.path("enabled").asBoolean());
            case "playlist" -> mPlaylist.publish();
            case "aliases" -> mPlaylist.aliases(command);
            case "saveChannel" -> mPlaylist.saveChannel(command, false);
            case "addChannel" -> mPlaylist.saveChannel(command, true);
            case "deleteChannel" -> mPlaylist.deleteChannel(command);
            case "cloneChannel" -> mPlaylist.cloneChannel(command);
            case "saveAlias" -> mPlaylist.saveAlias(command);
            case "deleteAlias" -> mPlaylist.deleteAlias(command);
            case "saveChannelMap" -> mPlaylist.saveChannelMap(command);
            case "deleteChannelMap" -> mPlaylist.deleteChannelMap(command);
            case "channelSettings" -> mPlaylist.channelSettings(command);
            case "saveChannelSettings" -> mPlaylist.saveChannelSettings(command);
            case "setAliasStream" -> mPlaylist.setAliasStream(command);
            case "setAliasStreamAll" -> mPlaylist.setAliasStreamAll(command);
            case "tunerSettings" -> tunerSettings(command.path("tunerId").asText(""));
            case "saveTunerSettings" -> saveTunerSettings(command.path("tunerId").asText(""), command.path("values"));
            case "streamSettings" -> streamSettings(command.path("name").asText(""));
            case "saveStreamSettings" -> saveStreamSettings(command.path("name").asText(""), command.path("values"));
            case "addStream" -> addStream(command.path("streamType").asText(""), command.path("name").asText(""));
            case "deleteStream" -> deleteStream(command.path("name").asText(""));
            case "preferences" -> preferences();
            case "savePreferences" -> savePreferences(command.path("section").asText(""), command.path("values"));
            default -> new Result(false, "Unknown command");
        };
    }

    private void publishState()
    {
        if(!mWatching)
        {
            return;
        }

        try
        {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("type", "state");
            body.set("tuners", tunerState());
            body.set("channels", channelState());
            body.set("streams", streamState());
            body.set("streamTypes", streamTypes());
            body.set("permissions", mPermissions.get());
            mSender.accept(body);
        }
        catch(Exception e)
        {
            mLog.debug("Unable to publish SDRTrunk state", e);
        }
    }

    private ArrayNode tunerState()
    {
        ArrayNode tuners = MAPPER.createArrayNode();
        for(DiscoveredTuner discovered: snapshotTuners())
        {
            TunerStatus status = discovered.getTunerStatus();
            ObjectNode node = tuners.addObject();
            node.put("id", discovered.getId() == null ? "" : discovered.getId());
            node.put("enabled", discovered.isEnabled());
            node.put("status", status == null ? "" : status.name());
            node.put("statusLabel", status == null ? "" : status.toString());
            node.put("error", discovered.getErrorMessage() == null ? "" : discovered.getErrorMessage());
            if(discovered.hasTuner())
            {
                Tuner tuner = discovered.getTuner();
                node.put("name", tuner.getPreferredName());
                node.put("frequencyHz", tuner.getTunerController().getFrequency());
                node.put("sampleRate", tuner.getTunerController().getSampleRate());
                node.put("bandwidthHz", tuner.getTunerController().getBandwidth());
                node.put("ppm", tuner.getTunerController().getFrequencyCorrection());
                node.put("measuredPpm", tuner.getTunerController().getPPMFrequencyError());
                node.put("channelCount", tuner.getChannelSourceManager().getTunerChannelCount());
                node.put("type", tuner.getTunerType() == null ? "" : tuner.getTunerType().toString());
            }
            else
            {
                node.put("name", discovered.getId() == null ? "" : discovered.getId());
                node.put("frequencyHz", 0);
                node.put("sampleRate", 0);
                node.put("bandwidthHz", 0);
                node.put("ppm", 0);
                node.put("measuredPpm", 0);
                node.put("channelCount", 0);
            }
        }
        return tuners;
    }

    private ArrayNode channelState()
    {
        ArrayNode channels = MAPPER.createArrayNode();
        List<Channel> configured;
        try
        {
            configured = new ArrayList<>(mChannelModel.getChannels());
        }
        catch(Exception e)
        {
            return channels;
        }

        for(Channel channel: configured)
        {
            if(channel == null || channel.isTrafficChannel())
            {
                continue;
            }
            ObjectNode node = channels.addObject();
            node.put("id", channel.getChannelID());
            node.put("name", text(channel.getName()));
            node.put("system", text(channel.getSystem()));
            node.put("site", text(channel.getSite()));
            node.put("decoder", channel.getDecodeConfiguration() == null ? "" :
                channel.getDecodeConfiguration().getDecoderType().getDisplayString());
            node.put("autoStart", channel.isAutoStart());
            node.put("processing", mChannelProcessingManager.getProcessingChannels().contains(channel));
            ArrayNode frequencies = node.putArray("frequencies");
            try
            {
                for(Long frequency: channel.getFrequencyList())
                {
                    if(frequency != null)
                    {
                        frequencies.add(frequency);
                    }
                }
            }
            catch(Exception ignored)
            {
            }
        }
        return channels;
    }

    private ArrayNode streamState()
    {
        ArrayNode streams = MAPPER.createArrayNode();
        for(ConfiguredBroadcast configured: mBroadcasts.get())
        {
            if(!(configured.getBroadcastConfiguration() instanceof io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration configuration))
            {
                continue;
            }
            if(!(configuration instanceof ThinlineRadioConfiguration) && !(configuration instanceof ThinlineRadioCoreConfiguration))
            {
                continue;
            }
            BroadcastState state = configured.broadcastStateProperty().get();
            ObjectNode node = streams.addObject();
            node.put("name", configuration.getName());
            node.put("system", configuration.getSystemID());
            node.put("enabled", configuration.isEnabled());
            node.put("kind", configuration instanceof ThinlineRadioCoreConfiguration ? "Core" : "Upload");
            node.put("state", state == null ? "" : state.name());
            node.put("stateLabel", state == null ? "" : state.toString());
        }
        return streams;
    }

    private Result setTunerEnabled(String tunerId, boolean enabled)
    {
        DiscoveredTuner tuner = findTuner(tunerId);
        if(tuner == null)
        {
            return new Result(false, "Tuner not found");
        }
        tuner.setEnabled(enabled);
        return new Result(true, "");
    }

    private Result setPpm(String tunerId, double ppm)
    {
        DiscoveredTuner tuner = findTuner(tunerId);
        if(tuner == null || !tuner.hasTuner())
        {
            return new Result(false, "Tuner is not running");
        }
        try
        {
            tuner.getTuner().getTunerController().setFrequencyCorrection(ppm);
            mTunerManager.getTunerConfigurationManager().updateTunerPPM(tuner);
            return new Result(true, "");
        }
        catch(SourceException e)
        {
            return new Result(false, e.getMessage() == null ? "PPM change failed" : e.getMessage());
        }
    }

    private Result setFrequency(String tunerId, long frequencyHz)
    {
        DiscoveredTuner tuner = findTuner(tunerId);
        if(tuner == null || !tuner.hasTuner())
        {
            return new Result(false, "Tuner is not running");
        }
        try
        {
            tuner.getTuner().getTunerController().setFrequency(frequencyHz);
            return new Result(true, "");
        }
        catch(SourceException e)
        {
            return new Result(false, e.getMessage() == null ? "Frequency change failed" : e.getMessage());
        }
    }

    private Result setChannel(int channelId, String system, String site, String name, boolean enabled)
    {
        Channel located = channelId > 0 ? findChannel(channelId) : null;
        if(located == null)
        {
            located = findChannel(system, site, name);
        }
        if(located == null)
        {
            return new Result(false, "Channel not found");
        }
        Channel channel = located;
        AtomicReference<String> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try
            {
                if(enabled)
                {
                    mChannelProcessingManager.start(channel);
                }
                else
                {
                    mChannelProcessingManager.stop(channel);
                }
            }
            catch(Exception e)
            {
                failure.set(e.getMessage() == null ? "Channel change failed" : e.getMessage());
            }
            finally
            {
                done.countDown();
            }
        });
        try
        {
            done.await(3, TimeUnit.SECONDS);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return new Result(false, "Interrupted");
        }
        if(failure.get() == null)
        {
            publishState();
            return new Result(true, "");
        }
        return new Result(false, failure.get());
    }

    private Result spectrum(String tunerId, boolean enabled)
    {
        stopSpectrum();
        if(!enabled)
        {
            return new Result(true, "");
        }

        DiscoveredTuner tuner = findTuner(tunerId);
        if(tuner == null || !tuner.hasTuner())
        {
            return new Result(false, "Tuner is not running");
        }

        mSpectrum = new ComplexDftProcessor<>();
        mSpectrumConverter = new ComplexDecibelConverter();
        mSpectrum.addConverter(mSpectrumConverter);
        mSpectrumConverter.addListener(this::onSpectrum);
        mSpectrumTuner = tuner;
        tuner.getTuner().getTunerController().addBufferListener(mSpectrum);
        return new Result(true, "");
    }

    private void onSpectrum(float[] bins)
    {
        long now = System.currentTimeMillis();
        DiscoveredTuner discovered = mSpectrumTuner;
        if(now - mLastSpectrumAt.get() < SPECTRUM_INTERVAL_MS || bins == null || bins.length == 0 || discovered == null)
        {
            return;
        }
        mLastSpectrumAt.set(now);

        int width = Math.min(SPECTRUM_BINS, bins.length);
        ArrayNode packed = MAPPER.createArrayNode();
        for(int column = 0; column < width; column++)
        {
            int from = (int)((long)column * bins.length / width);
            int to = Math.max(from + 1, (int)((long)(column + 1) * bins.length / width));
            float peak = -Float.MAX_VALUE;
            for(int index = from; index < to && index < bins.length; index++)
            {
                peak = Math.max(peak, bins[index]);
            }
            packed.add(Math.round(Math.max(-160f, Math.min(0f, peak)) * 10f) / 10f);
        }

        Tuner tuner = discovered.getTuner();
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "spectrum");
        body.put("tunerId", discovered.getId() == null ? "" : discovered.getId());
        body.put("format", "db");
        if(tuner != null)
        {
            body.put("centerHz", tuner.getTunerController().getFrequency());
            body.put("bandwidthHz", tuner.getTunerController().getBandwidth());
            body.set("channels", spectrumChannels(tuner));
        }
        body.set("bins", packed);
        mSender.accept(body);
    }

    private ArrayNode spectrumChannels(Tuner tuner)
    {
        ArrayNode markers = MAPPER.createArrayNode();
        List<Channel> channels;
        try
        {
            channels = new ArrayList<>(mChannelProcessingManager.getProcessingChannels());
        }
        catch(Exception e)
        {
            channels = List.of();
        }
        try
        {
            for(TunerChannel tunerChannel: new ArrayList<>(tuner.getChannelSourceManager().getTunerChannels()))
            {
                ObjectNode marker = markers.addObject();
                marker.put("frequencyHz", tunerChannel.getFrequency());
                marker.put("bandwidthHz", tunerChannel.getBandwidth());
                String label = "";
                for(Channel channel: channels)
                {
                    if(channel != null && !channel.isTrafficChannel() && channel.getFrequencyList().contains(tunerChannel.getFrequency()))
                    {
                        label = text(channel.getName());
                        break;
                    }
                }
                marker.put("label", label.isEmpty() ? "Traffic" : label);
                marker.put("control", !label.isEmpty());
            }
        }
        catch(Exception ignored)
        {
        }
        return markers;
    }

    void closeSpectrum()
    {
        if(mSpectrumTuner != null)
        {
            stopSpectrum();
        }
    }

    private Result tunerSettings(String tunerId)
    {
        DiscoveredTuner tuner = findTuner(tunerId);
        if(tuner == null || tuner.getTunerConfiguration() == null)
        {
            return new Result(false, tuner == null ? "Tuner not found" : "This tuner has no saved settings");
        }
        mSender.accept(settingsMessage("tuner", tunerId, tuner.getTuner() == null ? tunerId : tuner.getTuner().getPreferredName(),
            ThinlineRadioSettings.describe(tuner.getTunerConfiguration(), TUNER_READ_ONLY, Set.of(), false)));
        return new Result(true, "");
    }

    private Result saveTunerSettings(String tunerId, JsonNode values)
    {
        DiscoveredTuner tuner = findTuner(tunerId);
        if(tuner == null || tuner.getTunerConfiguration() == null)
        {
            return new Result(false, tuner == null ? "Tuner not found" : "This tuner has no saved settings");
        }
        Result result = ThinlineRadioPlaylist.onFx(() -> {
            try
            {
                TunerConfiguration configuration = tuner.getTunerConfiguration();
                ThinlineRadioSettings.apply(configuration, values, TUNER_READ_ONLY, Set.of());
                tuner.setTunerConfiguration(configuration);
                mTunerManager.getTunerConfigurationManager().saveConfigurations();
                return new Result(true, "");
            }
            catch(Exception e)
            {
                return new Result(false, e.getMessage() == null ? "Tuner change failed" : e.getMessage());
            }
        });
        tunerSettings(tunerId);
        publishState();
        return result;
    }

    private Result streamSettings(String name)
    {
        BroadcastConfiguration configuration = mBroadcastModel.getBroadcastConfiguration(name);
        if(configuration == null)
        {
            return new Result(false, "Stream not found");
        }
        ObjectNode message = settingsMessage("stream", name, name,
            ThinlineRadioSettings.describe(configuration, STREAM_READ_ONLY, Set.of(), false));
        message.put("streamType", configuration.getBroadcastServerType() == null ? "" : configuration.getBroadcastServerType().toString());
        mSender.accept(message);
        return new Result(true, "");
    }

    private Result saveStreamSettings(String name, JsonNode values)
    {
        Result result = ThinlineRadioPlaylist.onFx(() -> {
            BroadcastConfiguration configuration = mBroadcastModel.getBroadcastConfiguration(name);
            if(configuration == null)
            {
                return new Result(false, "Stream not found");
            }
            try
            {
                ThinlineRadioSettings.apply(configuration, values, STREAM_READ_ONLY, Set.of());
                mBroadcastModel.process(new BroadcastEvent(configuration, BroadcastEvent.Event.CONFIGURATION_CHANGE));
                return new Result(true, "");
            }
            catch(Exception e)
            {
                return new Result(false, e.getMessage() == null ? "Stream change failed" : e.getMessage());
            }
        });
        streamSettings(name);
        publishState();
        return result;
    }

    private Result addStream(String type, String name)
    {
        String trimmed = name == null ? "" : name.trim();
        if(trimmed.isEmpty())
        {
            return new Result(false, "Stream needs a name");
        }
        BroadcastServerType serverType;
        try
        {
            serverType = BroadcastServerType.valueOf(type);
        }
        catch(Exception e)
        {
            return new Result(false, "Choose a stream type");
        }
        Result result = ThinlineRadioPlaylist.onFx(() -> {
            if(mBroadcastModel.getBroadcastConfiguration(trimmed) != null)
            {
                return new Result(false, "A stream with that name already exists");
            }
            BroadcastConfiguration configuration = BroadcastFactory.getConfiguration(serverType, BroadcastFormat.MP3);
            if(configuration == null)
            {
                return new Result(false, "That stream type cannot be created here");
            }
            configuration.setName(trimmed);
            configuration.setEnabled(false);
            mBroadcastModel.addBroadcastConfiguration(configuration);
            return new Result(true, "");
        });
        if(result.ok())
        {
            streamSettings(trimmed);
            publishState();
        }
        return result;
    }

    private Result deleteStream(String name)
    {
        Result result = ThinlineRadioPlaylist.onFx(() -> {
            BroadcastConfiguration configuration = mBroadcastModel.getBroadcastConfiguration(name);
            if(configuration == null)
            {
                return new Result(false, "Stream not found");
            }
            mBroadcastModel.removeBroadcastConfiguration(configuration);
            return new Result(true, "");
        });
        publishState();
        return result;
    }

    private ArrayNode streamTypes()
    {
        ArrayNode types = MAPPER.createArrayNode();
        for(BroadcastServerType type: BroadcastServerType.values())
        {
            if(type == BroadcastServerType.UNKNOWN)
            {
                continue;
            }
            ObjectNode node = types.addObject();
            node.put("value", type.name());
            node.put("label", type.toString());
        }
        return types;
    }

    private Result preferences()
    {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "settings");
        body.put("scope", "preferences");
        ArrayNode sections = body.putArray("sections");
        for(Map.Entry<String, Preference> entry: preferenceSections().entrySet())
        {
            try
            {
                ArrayNode fields = ThinlineRadioSettings.describe(entry.getValue(), Set.of(), PREFERENCE_HIDDEN, true);
                if(fields.isEmpty())
                {
                    continue;
                }
                ObjectNode section = sections.addObject();
                section.put("id", entry.getKey());
                section.put("title", ThinlineRadioSettings.label(entry.getKey().replace("Preference", "")));
                section.set("fields", fields);
            }
            catch(Exception e)
            {
                mLog.debug("Unable to describe preference " + entry.getKey(), e);
            }
        }
        mSender.accept(body);
        return new Result(true, "");
    }

    private Result savePreferences(String section, JsonNode values)
    {
        Preference preference = preferenceSections().get(section);
        if(preference == null)
        {
            return new Result(false, "Preference section not found");
        }
        Result result = ThinlineRadioPlaylist.onFx(() -> {
            try
            {
                ThinlineRadioSettings.apply(preference, values, Set.of(), PREFERENCE_HIDDEN);
                return new Result(true, "");
            }
            catch(Exception e)
            {
                return new Result(false, e.getMessage() == null ? "Preference change failed" : e.getMessage());
            }
        });
        preferences();
        return result;
    }

    /**
     * Preference pages the admin page may edit, keyed by class name.  Thinline Radio's own page (URL, key, and
     * remote permissions) and window layout are left out.
     */
    private Map<String, Preference> preferenceSections()
    {
        Map<String, Preference> sections = new LinkedHashMap<>();
        for(Method method: UserPreferences.class.getMethods())
        {
            if(method.getParameterCount() != 0 || !Preference.class.isAssignableFrom(method.getReturnType()))
            {
                continue;
            }
            try
            {
                Preference preference = (Preference)method.invoke(mUserPreferences);
                if(preference == null || preference instanceof ThinlineRadioPreference)
                {
                    continue;
                }
                sections.put(preference.getClass().getSimpleName(), preference);
            }
            catch(Exception ignored)
            {
            }
        }
        return sections;
    }

    private ObjectNode settingsMessage(String scope, String id, String title, ArrayNode fields)
    {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "settings");
        body.put("scope", scope);
        body.put("id", id);
        body.put("title", title == null ? id : title);
        body.set("fields", fields);
        return body;
    }

    private void stopSpectrum()
    {
        if(mSpectrumTuner != null && mSpectrumTuner.hasTuner() && mSpectrum != null)
        {
            mSpectrumTuner.getTuner().getTunerController().removeBufferListener(mSpectrum);
        }
        if(mSpectrum != null)
        {
            mSpectrum.dispose();
        }
        mSpectrum = null;
        mSpectrumConverter = null;
        mSpectrumTuner = null;
    }

    private DiscoveredTuner findTuner(String tunerId)
    {
        for(DiscoveredTuner tuner: snapshotTuners())
        {
            if(tunerId.equals(tuner.getId()))
            {
                return tuner;
            }
        }
        return null;
    }

    private Channel findChannel(int channelId)
    {
        try
        {
            for(Channel channel: mChannelModel.getChannels())
            {
                if(channel != null && channel.getChannelID() == channelId)
                {
                    return channel;
                }
            }
        }
        catch(Exception ignored)
        {
        }
        return null;
    }

    private Channel findChannel(String system, String site, String name)
    {
        try
        {
            for(Channel channel: mChannelModel.getChannels())
            {
                if(channel != null && text(channel.getSystem()).equals(system) && text(channel.getSite()).equals(site) &&
                    text(channel.getName()).equals(name))
                {
                    return channel;
                }
            }
        }
        catch(Exception ignored)
        {
        }
        return null;
    }

    private List<DiscoveredTuner> snapshotTuners()
    {
        try
        {
            return mTunerManager.getDiscoveredTuners();
        }
        catch(Exception e)
        {
            return List.of();
        }
    }

    private static String text(String value)
    {
        return value == null ? "" : value;
    }
}
