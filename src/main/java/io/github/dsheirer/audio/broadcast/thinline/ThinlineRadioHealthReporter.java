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
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.thinline.ThinlineRadioPreference;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.util.ThreadPool;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JFrame;
import javafx.application.Platform;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Talks to Thinline Radio over one websocket when site health is enabled.
 *
 * SDRTrunk sends the site health report and each upload key's check-in. Thinline Radio can ask for a report
 * or turn a Thinline upload stream on or off. A trunking channel counts as control-lost after 30 seconds
 * outside the CONTROL state.
 */
public class ThinlineRadioHealthReporter
{
    private static final Logger mLog = LoggerFactory.getLogger(ThinlineRadioHealthReporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CALL_UPLOAD_PATH = "/api/call-upload";
    private static final String HEALTH_PATH = "/api/sdrtrunk-health";
    private static final String SOCKET_PATH = "/api/sdrtrunk";
    private static final long CONTROL_LOST_AFTER_MS = 30_000;
    private static final long POST_INTERVAL_MS = 30_000;
    private static final long MIN_POST_GAP_MS = 10_000;
    private static final EnumSet<DecoderType> TRUNKING_DECODERS = EnumSet.of(DecoderType.DMR, DecoderType.LTR,
        DecoderType.LTR_NET, DecoderType.MPT1327, DecoderType.NXDN, DecoderType.PASSPORT, DecoderType.P25_PHASE1,
        DecoderType.P25_PHASE2);

    private final UserPreferences mUserPreferences;
    private final BroadcastModel mBroadcastModel;
    private final ChannelModel mChannelModel;
    private final ChannelProcessingManager mChannelProcessingManager;
    private final TunerManager mTunerManager;
    private final long mStartedAt = System.currentTimeMillis();
    private final Map<String, Long> mLastControlSeen = new ConcurrentHashMap<>();
    private final Set<Integer> mReportingConfigurations = ConcurrentHashMap.newKeySet();
    private final HttpClient mHttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final ThinlineRadioEndpoint mSocket = new ThinlineRadioEndpoint(this::onCommand, null);
    private final AtomicBoolean mReportNow = new AtomicBoolean();
    private ThinlineRadioControl mControl;
    private final ThinlineRadioDesktop mDesktop;

    private ScheduledFuture<?> mFuture;
    private long mLastPostAt;
    private String mLastSignature = "";
    private boolean mSiteReporting;

    public ThinlineRadioHealthReporter(UserPreferences userPreferences, BroadcastModel broadcastModel,
                                       ChannelModel channelModel, ChannelProcessingManager channelProcessingManager,
                                       TunerManager tunerManager, AliasModel aliasModel, ChannelMapModel channelMapModel,
                                       JFrame frame)
    {
        mUserPreferences = userPreferences;
        mBroadcastModel = broadcastModel;
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mTunerManager = tunerManager;
        mControl = new ThinlineRadioControl(userPreferences, broadcastModel, channelModel, channelProcessingManager,
            tunerManager, aliasModel, channelMapModel, node -> mSocket.send(node), this::snapshotBroadcasts,
            this::remotePermissions);
        mDesktop = new ThinlineRadioDesktop(frame, mSocket::trySend, mSocket::send, mSocket::isReady);
    }

    public void start()
    {
        if(mFuture == null)
        {
            mFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::tick, 5, 5, TimeUnit.SECONDS);
        }
    }

    public void stop()
    {
        if(mFuture != null)
        {
            mFuture.cancel(false);
            mFuture = null;
        }

        for(ConfiguredBroadcast configured: snapshotBroadcasts())
        {
            if(configured.getBroadcastServerType() == BroadcastServerType.THINLINE_RADIO &&
                mReportingConfigurations.contains(configured.getBroadcastConfiguration().getId()))
            {
                postUpload(configured, false);
            }
        }

        if(mSiteReporting)
        {
            postSite(false, null, null);
        }

        mReportingConfigurations.clear();
        mSiteReporting = false;
        if(mControl != null)
        {
            mControl.stop();
        }
        mDesktop.setEnabled(false);
        mSocket.close();
    }

    private void tick()
    {
        try
        {
            ThinlineRadioPreference preference = mUserPreferences.getThinlineRadioPreference();
            boolean siteActive = preference.isEnabled() && !preference.getUrl().isBlank() &&
                !preference.getApiKey().isBlank();
            if(siteActive)
            {
                mSocket.ensure(socketUrl(preference.getUrl()), preference.getApiKey());
            }
            else
            {
                mSocket.close();
            }
            if(!preference.isRemoteAllowed(ThinlineRadioPreference.Remote.DESKTOP))
            {
                mDesktop.setEnabled(false);
            }
            if(!preference.isRemoteAllowed(ThinlineRadioPreference.Remote.SPECTRUM))
            {
                mControl.closeSpectrum();
            }

            List<ConfiguredBroadcast> broadcasts = snapshotBroadcasts();
            ObjectNode channels = channelReport();
            ObjectNode tuners = tunerReport();
            StringBuilder signatureBuilder = new StringBuilder();
            signatureBuilder.append(channels.path("signature").asText(""));
            signatureBuilder.append('|').append(tuners.path("signature").asText(""));
            signatureBuilder.append("|site=").append(siteActive);
            signatureBuilder.append(preference.isAlertTuner()).append(preference.isAlertControl());
            signatureBuilder.append(preference.isAlertUploader()).append(preference.isAlertOverflow());
            signatureBuilder.append(preference.isAlertOffline());
            boolean needStop = (!siteActive && mSiteReporting) || mReportNow.getAndSet(false);

            for(ConfiguredBroadcast configured: broadcasts)
            {
                if(configured.getBroadcastServerType() != BroadcastServerType.THINLINE_RADIO)
                {
                    continue;
                }

                BroadcastState state = configured.broadcastStateProperty().get();
                signatureBuilder.append('|').append(configured.getBroadcastConfiguration().getId()).append(':');
                signatureBuilder.append(state == null ? "" : state.name());
                boolean active = configured.getBroadcastConfiguration().isEnabled() &&
                    configured.getBroadcastConfiguration().isValid();

                if(!active && mReportingConfigurations.contains(configured.getBroadcastConfiguration().getId()))
                {
                    needStop = true;
                }
            }

            String signature = signatureBuilder.toString();
            long now = System.currentTimeMillis();
            boolean intervalElapsed = mLastPostAt == 0 || now - mLastPostAt >= POST_INTERVAL_MS;
            boolean changed = !signature.equals(mLastSignature);
            boolean gapElapsed = mLastPostAt == 0 || now - mLastPostAt >= MIN_POST_GAP_MS;

            if(!needStop && !intervalElapsed && !(changed && gapElapsed))
            {
                return;
            }

            boolean posted = false;

            for(ConfiguredBroadcast configured: broadcasts)
            {
                if(configured.getBroadcastServerType() != BroadcastServerType.THINLINE_RADIO)
                {
                    continue;
                }

                BroadcastConfiguration configuration = configured.getBroadcastConfiguration();
                boolean active = configuration instanceof ThinlineRadioConfiguration thinline &&
                    thinline.isEnabled() && thinline.isValid();

                if(active)
                {
                    if(postUpload(configured, true))
                    {
                        mReportingConfigurations.add(configuration.getId());
                        posted = true;
                    }
                }
                else if(mReportingConfigurations.remove(configuration.getId()))
                {
                    postUpload(configured, false);
                    posted = true;
                }
            }

            if(siteActive)
            {
                if(postSite(true, tuners.withArray("tuners"), channels.withArray("channels")))
                {
                    mSiteReporting = true;
                    posted = true;
                }
            }
            else if(mSiteReporting)
            {
                postSite(false, tuners.withArray("tuners"), channels.withArray("channels"));
                mSiteReporting = false;
                posted = true;
            }

            if(posted)
            {
                mLastSignature = signature;
            }

            if(posted || intervalElapsed || changed)
            {
                mLastPostAt = now;
            }
        }
        catch(Exception e)
        {
            mLog.warn("Thinline Radio health report failed", e);
        }
    }

    private boolean postUpload(ConfiguredBroadcast configured, boolean reporting)
    {
        if(!(configured.getBroadcastConfiguration() instanceof ThinlineRadioConfiguration configuration))
        {
            return false;
        }

        ObjectNode body = reportBody(configuration.getApiKey(), configuration.getSystemID(), reporting, "upload");
        body.set("uploader", uploaderReport(configured));
        body.set("alerts", uploadAlerts());

        if(socketActive())
        {
            body.put("type", "upload");
            return mSocket.isReady() && mSocket.send(body);
        }

        String url = healthUrl(configuration.getHost());
        if(url == null || configuration.getApiKey() == null || configuration.getApiKey().isBlank())
        {
            return false;
        }
        return send(url, body);
    }

    private boolean postSite(boolean reporting, ArrayNode tuners, ArrayNode channels)
    {
        ThinlineRadioPreference preference = mUserPreferences.getThinlineRadioPreference();
        String url = healthUrl(preference.getUrl());

        if(url == null || preference.getApiKey().isBlank())
        {
            return false;
        }

        ObjectNode body = reportBody(preference.getApiKey(), 0, reporting, "health");
        body.put("type", "health");
        body.set("alerts", siteAlerts());
        body.set("tuners", tuners == null ? MAPPER.createArrayNode() : tuners.deepCopy());
        body.set("channels", channels == null ? MAPPER.createArrayNode() : channels.deepCopy());
        body.set("streams", streamReport());
        return mSocket.isReady() && mSocket.send(body);
    }

    private boolean socketActive()
    {
        ThinlineRadioPreference preference = mUserPreferences.getThinlineRadioPreference();
        return preference.isEnabled() && !preference.getUrl().isBlank() && !preference.getApiKey().isBlank();
    }

    private void onCommand(JsonNode command)
    {
        String id = command.path("id").asText("");
        String name = command.path("command").asText("");
        if("uploadCall".equals(name) || "discardCall".equals(name))
        {
            return;
        }
        boolean ok = false;
        String error = "";

        ThinlineRadioPreference.Remote needed = requiredRemote(name, command);
        if(needed != null && !mUserPreferences.getThinlineRadioPreference().isRemoteAllowed(needed))
        {
            if("desktopInput".equals(name))
            {
                return;
            }
            error = "This site does not allow it: " + needed;
            name = "";
        }

        switch(name)
        {
            case "" ->
            {
            }
            case "report" ->
            {
                mReportNow.set(true);
                ok = true;
            }
            case "setStream" ->
            {
                ok = setStreamEnabled(command.path("name").asText(""), command.path("enabled").asBoolean());
                if(!ok)
                {
                    error = "Stream not found";
                }
            }
            case "desktop" ->
            {
                mDesktop.setEnabled(command.path("enabled").asBoolean());
                ok = true;
            }
            case "desktopInput" ->
            {
                mDesktop.input(command);
                return;
            }
            default ->
            {
                ThinlineRadioControl.Result handled = mControl.handle(command);
                ok = handled.ok();
                error = handled.error();
            }
        }

        ObjectNode result = MAPPER.createObjectNode();
        result.put("type", "result");
        result.put("id", id);
        result.put("command", command.path("command").asText(""));
        result.put("ok", ok);
        result.put("error", error);
        mSocket.send(result);
    }

    /**
     * Permission a remote command needs, or null when it only reads.  Turning the desktop or spectrum off is
     * always allowed.
     */
    private static ThinlineRadioPreference.Remote requiredRemote(String name, JsonNode command)
    {
        return switch(name)
        {
            case "desktop" -> command.path("enabled").asBoolean() ? ThinlineRadioPreference.Remote.DESKTOP : null;
            case "desktopInput" -> ThinlineRadioPreference.Remote.DESKTOP;
            case "spectrum" -> command.path("enabled").asBoolean() ? ThinlineRadioPreference.Remote.SPECTRUM : null;
            case "setTuner", "setPpm", "setFrequency", "saveTunerSettings" -> ThinlineRadioPreference.Remote.TUNERS;
            case "setChannel", "saveChannel", "addChannel", "deleteChannel", "cloneChannel", "saveChannelSettings" ->
                ThinlineRadioPreference.Remote.CHANNELS;
            case "saveAlias", "deleteAlias", "saveChannelMap", "deleteChannelMap" -> ThinlineRadioPreference.Remote.ALIASES;
            case "setStream", "saveStreamSettings", "addStream", "deleteStream", "setAliasStream", "setAliasStreamAll" ->
                ThinlineRadioPreference.Remote.STREAMS;
            case "savePreferences" -> ThinlineRadioPreference.Remote.PREFERENCES;
            default -> null;
        };
    }

    private ObjectNode remotePermissions()
    {
        ObjectNode permissions = MAPPER.createObjectNode();
        ThinlineRadioPreference preference = mUserPreferences.getThinlineRadioPreference();
        for(ThinlineRadioPreference.Remote remote: ThinlineRadioPreference.Remote.values())
        {
            permissions.put(remote.key(), preference.isRemoteAllowed(remote));
        }
        return permissions;
    }

    private boolean setStreamEnabled(String name, boolean enabled)
    {
        if(name == null || name.isBlank())
        {
            return false;
        }

        AtomicBoolean found = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try
            {
                for(ConfiguredBroadcast configured: snapshotBroadcasts())
                {
                    if(configured.getBroadcastConfiguration() instanceof io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration configuration &&
                        (configuration instanceof ThinlineRadioConfiguration || configuration instanceof ThinlineRadioCoreConfiguration) &&
                        name.equals(configuration.getName()))
                    {
                        configuration.setEnabled(enabled);
                        mBroadcastModel.process(new BroadcastEvent(configuration, BroadcastEvent.Event.CONFIGURATION_CHANGE));
                        found.set(true);
                        break;
                    }
                }
            }
            finally
            {
                done.countDown();
            }
        });

        try
        {
            done.await(2, TimeUnit.SECONDS);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        return found.get();
    }

    private ObjectNode reportBody(String apiKey, int system, boolean reporting, String role)
    {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("key", apiKey);
        body.put("system", system);
        body.put("reporting", reporting);
        body.put("role", role);
        body.put("version", SystemProperties.getInstance().getApplicationName());
        body.put("uptimeSeconds", Math.max(0, (System.currentTimeMillis() - mStartedAt) / 1000));
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }

    private ArrayNode uploadAlerts()
    {
        ArrayNode alerts = MAPPER.createArrayNode();

        if(mUserPreferences.getThinlineRadioPreference().isAlertUploader())
        {
            alerts.add("uploader_error");
        }

        return alerts;
    }

    private ArrayNode siteAlerts()
    {
        ThinlineRadioPreference preference = mUserPreferences.getThinlineRadioPreference();
        ArrayNode alerts = MAPPER.createArrayNode();

        if(preference.isAlertTuner())
        {
            alerts.add("tuner_error");
        }

        if(preference.isAlertControl())
        {
            alerts.add("control_lost");
        }

        if(preference.isAlertOverflow())
        {
            alerts.add("sample_overflow");
        }

        if(preference.isAlertOffline())
        {
            alerts.add("offline");
        }

        return alerts;
    }

    private ArrayNode streamReport()
    {
        ArrayNode streams = MAPPER.createArrayNode();

        for(ConfiguredBroadcast configured: snapshotBroadcasts())
        {
            if(!(configured.getBroadcastConfiguration() instanceof io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration configuration) ||
                !(configuration instanceof ThinlineRadioConfiguration || configuration instanceof ThinlineRadioCoreConfiguration))
            {
                continue;
            }

            ObjectNode node = streams.addObject();
            BroadcastState state = configured.broadcastStateProperty().get();
            node.put("name", configuration.getName());
            node.put("system", configuration.getSystemID());
            node.put("enabled", configuration.isEnabled());
            node.put("kind", configuration instanceof ThinlineRadioCoreConfiguration ? "Core" : "Upload");
            node.put("state", state == null ? "" : state.name());
            node.put("stateLabel", state == null ? "" : state.toString());
            node.put("error", state != null && isUploaderError(state, configured.getAudioBroadcaster() != null));
        }

        return streams;
    }

    private boolean send(String url, ObjectNode body)
    {
        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("User-Agent", "sdrtrunk")
                .POST(HttpRequest.BodyPublishers.ofByteArray(MAPPER.writeValueAsBytes(body)))
                .build();

            HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() < 200 || response.statusCode() >= 300)
            {
                mLog.warn("Thinline Radio health report to {} returned {} {}", url, response.statusCode(),
                    response.body());
                return false;
            }

            return true;
        }
        catch(Exception e)
        {
            mLog.warn("Thinline Radio health report to {} failed: {}", url, e.toString());
            return false;
        }
    }

    private ObjectNode uploaderReport(ConfiguredBroadcast configured)
    {
        ObjectNode node = MAPPER.createObjectNode();
        BroadcastState state = configured.broadcastStateProperty().get();

        if(state == null)
        {
            state = BroadcastState.READY;
        }

        AbstractAudioBroadcaster broadcaster = configured.getAudioBroadcaster();
        node.put("name", configured.getBroadcastConfiguration().getName());
        node.put("state", state.name());
        node.put("stateLabel", state.toString());
        node.put("error", isUploaderError(state, broadcaster != null));
        node.put("queueSize", broadcaster == null ? 0 : broadcaster.getAudioQueueSize());
        node.put("streamedCount", broadcaster == null ? 0 : broadcaster.getStreamedAudioCount());
        node.put("errorCount", broadcaster == null ? 0 : broadcaster.getAudioErrorCount());
        node.put("agedOffCount", broadcaster == null ? 0 : broadcaster.getAgedOffAudioCount());
        return node;
    }

    private boolean isUploaderError(BroadcastState state, boolean hasBroadcaster)
    {
        if(state.isErrorState() || state == BroadcastState.NO_SERVER || state == BroadcastState.NETWORK_UNAVAILABLE)
        {
            return true;
        }

        if(hasBroadcaster && state == BroadcastState.DISCONNECTED)
        {
            return true;
        }

        return state == BroadcastState.READY && System.currentTimeMillis() - mStartedAt > 60_000;
    }

    private ObjectNode tunerReport()
    {
        ObjectNode wrapper = MAPPER.createObjectNode();
        ArrayNode tuners = wrapper.putArray("tuners");
        StringBuilder signature = new StringBuilder();
        boolean error = false;

        List<DiscoveredTuner> discovered = List.of();

        try
        {
            discovered = mTunerManager.getDiscoveredTuners();
        }
        catch(Exception e)
        {
            mLog.debug("Unable to read tuners for health report", e);
        }

        for(DiscoveredTuner discoveredTuner: discovered)
        {
            TunerStatus status = discoveredTuner.getTunerStatus();
            ObjectNode node = tuners.addObject();
            node.put("id", discoveredTuner.getId() == null ? "" : discoveredTuner.getId());
            node.put("status", status == null ? "" : status.name());
            node.put("statusLabel", status == null ? "" : status.toString());
            node.put("error", discoveredTuner.getErrorMessage() == null ? "" : discoveredTuner.getErrorMessage());

            boolean tunerError = status == TunerStatus.ERROR;
            error = error || tunerError;
            signature.append(discoveredTuner.getId()).append(':').append(status).append(';');

            if(discoveredTuner.hasTuner())
            {
                Tuner tuner = discoveredTuner.getTuner();
                node.put("name", tuner.getPreferredName());
                node.put("locked", tuner.getTunerController().isLockedSampleRate());
                node.put("channelCount", tuner.getChannelSourceManager().getTunerChannelCount());
                node.put("frequencyHz", tuner.getTunerController().getFrequency());
                node.put("frequencyErrorHz", tuner.getTunerController().getMeasuredFrequencyError());
                node.put("frequencyErrorPpm", tuner.getTunerController().getPPMFrequencyError());
            }
            else
            {
                node.put("name", "");
                node.put("locked", false);
                node.put("channelCount", 0);
                node.put("frequencyHz", 0);
                node.put("frequencyErrorHz", 0);
                node.put("frequencyErrorPpm", 0);
            }
        }

        wrapper.put("error", error);
        wrapper.put("signature", signature.toString());
        return wrapper;
    }

    private ObjectNode channelReport()
    {
        ObjectNode wrapper = MAPPER.createObjectNode();
        ArrayNode channels = wrapper.putArray("channels");
        StringBuilder signature = new StringBuilder();
        Set<String> seen = new HashSet<>();
        long now = System.currentTimeMillis();
        Set<Channel> processing = new HashSet<>(mChannelProcessingManager.getProcessingChannels());
        List<Channel> configured = snapshotChannels();

        for(Channel channel: configured)
        {
            if(channel == null || channel.isTrafficChannel())
            {
                continue;
            }

            boolean isProcessing = processing.contains(channel);

            if(!isProcessing && !channel.isAutoStart())
            {
                continue;
            }

            DecoderType decoderType = channel.getDecodeConfiguration() == null ? null :
                channel.getDecodeConfiguration().getDecoderType();
            boolean trunking = decoderType != null && TRUNKING_DECODERS.contains(decoderType);
            String key = channelKey(channel);
            seen.add(key);

            ProcessingChain chain = mChannelProcessingManager.getProcessingChain(channel);
            List<State> states = List.of();
            boolean overflow = false;

            if(chain != null && chain.getChannelState() != null)
            {
                states = chain.getChannelState().getStates();
                overflow = chain.getChannelState().isOverflow();
            }

            boolean inControl = states.contains(State.CONTROL);

            if(trunking && inControl)
            {
                mLastControlSeen.put(key, now);
            }
            else if(trunking)
            {
                mLastControlSeen.putIfAbsent(key, now);
            }

            long sinceControl = 0;
            boolean controlLost = false;

            if(trunking)
            {
                long last = mLastControlSeen.getOrDefault(key, now);
                sinceControl = inControl ? 0 : Math.max(0, (now - last) / 1000);
                controlLost = !inControl && now - last >= CONTROL_LOST_AFTER_MS;
            }

            boolean noTuner = trunking && !isProcessing && now - mStartedAt > CONTROL_LOST_AFTER_MS;
            ObjectNode node = channels.addObject();
            node.put("name", text(channel.getName()));
            node.put("system", text(channel.getSystem()));
            node.put("site", text(channel.getSite()));
            node.put("decoder", decoderType == null ? "" : decoderType.getDisplayString());
            node.put("trunking", trunking);
            node.put("processing", isProcessing);
            node.put("state", primaryState(states));
            ArrayNode stateNames = node.putArray("states");

            for(State state: states)
            {
                stateNames.add(state.name());
            }

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
            catch(Exception e)
            {
                //Frequency list can change while the snapshot is read.
            }

            node.put("secondsSinceControl", sinceControl);
            node.put("controlLost", controlLost);
            node.put("noTuner", noTuner);
            node.put("overflow", overflow);

            if(controlLost || noTuner)
            {
                signature.append(key).append(":lost=").append(controlLost).append(",noTuner=").append(noTuner).append(';');
            }
        }

        mLastControlSeen.keySet().retainAll(seen);
        wrapper.put("signature", signature.toString());
        return wrapper;
    }

    private static String primaryState(List<State> states)
    {
        if(states.contains(State.CONTROL))
        {
            return State.CONTROL.name();
        }

        if(states.isEmpty())
        {
            return "";
        }

        return states.get(0).name();
    }

    private static String channelKey(Channel channel)
    {
        return text(channel.getSystem()) + "\u0000" + text(channel.getSite()) + "\u0000" + text(channel.getName());
    }

    private static String text(String value)
    {
        return value == null ? "" : value;
    }

    private static String healthUrl(String host)
    {
        if(host == null || host.isBlank())
        {
            return null;
        }

        String trimmed = host.trim();

        if(trimmed.endsWith(CALL_UPLOAD_PATH))
        {
            trimmed = trimmed.substring(0, trimmed.length() - CALL_UPLOAD_PATH.length());
        }

        while(trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed + HEALTH_PATH;
    }

    private static String socketUrl(String host)
    {
        String httpUrl = healthUrl(host);
        if(httpUrl == null)
        {
            return null;
        }

        String base = httpUrl.substring(0, httpUrl.length() - HEALTH_PATH.length());
        if(base.startsWith("https://"))
        {
            return "wss://" + base.substring("https://".length()) + SOCKET_PATH;
        }
        if(base.startsWith("http://"))
        {
            return "ws://" + base.substring("http://".length()) + SOCKET_PATH;
        }
        return null;
    }

    private List<ConfiguredBroadcast> snapshotBroadcasts()
    {
        try
        {
            return new ArrayList<>(mBroadcastModel.getConfiguredBroadcasts());
        }
        catch(Exception e)
        {
            mLog.debug("Unable to snapshot broadcast configurations", e);
            return List.of();
        }
    }

    private List<Channel> snapshotChannels()
    {
        try
        {
            return new ArrayList<>(mChannelModel.getChannels());
        }
        catch(Exception e)
        {
            mLog.debug("Unable to snapshot channels", e);
            return List.of();
        }
    }
}
