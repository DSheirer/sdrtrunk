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

package io.github.dsheirer.web.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.channel.metadata.ChannelAndMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadataField;
import io.github.dsheirer.channel.metadata.ChannelMetadataModel;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.gui.calls.CallAliasResolver;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.web.WebSources;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.api.ChannelsController;
import io.github.dsheirer.web.api.RecordFilters;
import io.github.dsheirer.web.api.RecordViews.CallView;
import io.github.dsheirer.web.api.RecordViews.EventView;
import io.github.dsheirer.web.api.RuntimeViews;
import io.github.dsheirer.web.api.RuntimeViews.ChannelStatusView;
import io.github.dsheirer.web.api.RuntimeViews.TunerView;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.playlist.AliasRegistry;
import io.github.dsheirer.web.playlist.PlaylistConfig;
import javafx.collections.ListChangeListener;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers live changes from sdrtrunk to the WebSocket subscriptions of every connected client.
 *
 * Protocol, one JSON text message each way:
 * <pre>
 * client: {"op": "subscribe", "sub": 7, "topic": "calls", "params": {"to": "1201"}}
 *         {"op": "update", "sub": 7, "params": {...}}
 *         {"op": "unsubscribe", "sub": 7}
 *         {"op": "ping"}
 * server: {"op": "subscribed", "sub": 7, "topic": "calls"}
 *         {"op": "data", "sub": 7, "topic": "calls", "seq": 0, "data": {...}}
 *         {"op": "error", "sub": 7, "status": 403, "title": "...", "detail": "..."}
 *         {"op": "pong"}
 * </pre>
 * Status topics (channels.status, tuners.status) and the calls topic send a snapshot as their first data message.
 * Changes are pushed by the sdrtrunk components through listeners; nothing is polled.  Each change is serialized
 * once and shared by every subscription it matches.
 */
public class LiveHub
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveHub.class);
    public static final int MAX_CONNECTIONS = 200;
    public static final int MAX_CONNECTIONS_PER_USER = 20;
    private static final int MAX_TRACKED = 20_000;

    private final WebSources mSources;
    private final DatabaseManager mDatabaseManager;
    private final AccountService mAccountService;
    private final ObjectMapper mMapper;
    private final CallAliasResolver mAliases;
    private final Map<String,LiveConnection> mConnections = new ConcurrentHashMap<>();
    private final Object mLock = new Object();

    //Hub lock
    private final Map<Long,CallRecord> mActiveCalls = new LinkedHashMap<>();
    private final Map<Long,Long> mCallIds = lru();
    private final Map<Long,Long> mEventIds = lru();
    private final Map<Long,Boolean> mSeenEvents = lru();
    private final Map<ChannelMetadata,ChannelStatusView> mLastStatus = new IdentityHashMap<>();

    private final Listener<CallRecord> mCallListener = this::callChanged;
    private final Listener<CallRecord> mCallSavedListener = this::callSaved;
    private final Listener<EventRecord> mEventListener = this::eventChanged;
    private final Listener<EventRecord> mEventSavedListener = this::eventSaved;
    private final ChannelMetadataModel.Listener mMetadataListener = new MetadataListener();
    private final DiscoveredTunerModel.ModelListener mTunerListener = new TunerListener();
    private final AliasRegistry mAliasRegistry;
    private final Listener<ChannelEvent> mChannelListener = this::channelEvent;
    private final ListChangeListener<Alias> mAliasListener = this::aliasesChanged;
    //Hub lock: last version sent per channel id and alias id, so changes that don't change the configuration (for
    //example a channel starting) aren't announced
    private final Map<Integer,String> mChannelVersions = new java.util.HashMap<>();
    private final Map<Long,String> mAliasVersions = new java.util.HashMap<>();
    private static final int BULK_CHANGE = 50;
    //audio.live: frames are dropped for a client whose queue holds more than about 5 seconds of one call
    static final int AUDIO_QUEUE_LIMIT = 250;
    private final LiveSpectrum mSpectrum;
    //Hub lock: calls that ended recently, so the last audio frames of a call still find it
    private final Map<Long,CallRecord> mRecentCalls = lru();
    //Hub lock: decoded audio length of active calls, from the live audio tracker
    private final Map<Long,Long> mAudioMs = new java.util.HashMap<>();

    /**
     * Constructs an instance
     * @param sources of live data; components may be null
     * @param databaseManager for the stored ids of calls and events, or null
     * @param accountService to check the callers again periodically
     * @param mapper for JSON
     */
    public LiveHub(WebSources sources, DatabaseManager databaseManager, AccountService accountService,
                   ObjectMapper mapper, AliasRegistry aliasRegistry)
    {
        mAliasRegistry = aliasRegistry;
        mSources = sources;
        mDatabaseManager = databaseManager;
        mAccountService = accountService;
        mMapper = mapper;
        mAliases = new CallAliasResolver(sources.aliasModel());
        mSpectrum = new LiveSpectrum(sources.tunerManager());
    }

    /**
     * Registers with the sdrtrunk components.
     */
    public void attach()
    {
        if(mSources.callManager() != null)
        {
            mSources.callManager().addCallListener(mCallListener);
        }

        if(mSources.eventManager() != null)
        {
            mSources.eventManager().addEventListener(mEventListener);
        }

        if(mDatabaseManager != null)
        {
            mDatabaseManager.addCallSavedListener(mCallSavedListener);
            mDatabaseManager.addEventSavedListener(mEventSavedListener);
        }

        ChannelMetadataModel metadataModel = getMetadataModel();

        if(metadataModel != null)
        {
            metadataModel.addListener(mMetadataListener);
        }

        if(mSources.tunerManager() != null)
        {
            mSources.tunerManager().getDiscoveredTunerModel().addModelListener(mTunerListener);
        }

        if(mSources.playlistManager() != null)
        {
            mSources.playlistManager().getChannelModel().addListener(mChannelListener);
            mSources.playlistManager().getAliasModel().aliasList().addListener(mAliasListener);
        }
    }

    /**
     * Deregisters from the sdrtrunk components and closes every connection.
     */
    public void detach()
    {
        if(mSources.callManager() != null)
        {
            mSources.callManager().removeCallListener(mCallListener);
        }

        if(mSources.eventManager() != null)
        {
            mSources.eventManager().removeEventListener(mEventListener);
        }

        if(mDatabaseManager != null)
        {
            mDatabaseManager.removeCallSavedListener(mCallSavedListener);
            mDatabaseManager.removeEventSavedListener(mEventSavedListener);
        }

        ChannelMetadataModel metadataModel = getMetadataModel();

        if(metadataModel != null)
        {
            metadataModel.removeListener(mMetadataListener);
        }

        if(mSources.tunerManager() != null)
        {
            mSources.tunerManager().getDiscoveredTunerModel().removeModelListener(mTunerListener);
        }

        if(mSources.playlistManager() != null)
        {
            mSources.playlistManager().getChannelModel().removeListener(mChannelListener);
            mSources.playlistManager().getAliasModel().aliasList().removeListener(mAliasListener);
        }

        mSpectrum.dispose();

        for(LiveConnection connection: List.copyOf(mConnections.values()))
        {
            connection.close(1001, "Server stopping");
        }

        mConnections.clear();
    }

    //----------------------------------------------------------------------------------------------------------------
    // Connections
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Checks whether the caller may open another connection.
     * @throws ApiException 429 when the server or the caller has too many connections
     */
    public void checkConnectionLimit(Principal principal)
    {
        if(mConnections.size() >= MAX_CONNECTIONS)
        {
            throw ApiException.tooManyRequests("The server has too many WebSocket connections");
        }

        long mine = mConnections.values().stream()
            .filter(connection -> connection.getPrincipal().userId() == principal.userId()).count();

        if(mine >= MAX_CONNECTIONS_PER_USER)
        {
            throw ApiException.tooManyRequests("Your account has too many WebSocket connections");
        }
    }

    /**
     * Registers a new client connection.
     */
    LiveConnection connect(String id, Principal principal, MessageSink sink)
    {
        LiveConnection connection = new LiveConnection(id, principal, sink, mMapper);
        mConnections.put(id, connection);
        return connection;
    }

    /**
     * Connection with the id, or null
     */
    LiveConnection getConnection(String id)
    {
        return mConnections.get(id);
    }

    /**
     * Removes a client connection that closed.
     */
    void disconnect(String id)
    {
        LiveConnection connection = mConnections.remove(id);

        if(connection != null)
        {
            connection.closed();
            connection.getSubscriptions().values().forEach(mSpectrum::remove);
        }
    }

    /**
     * Count of open connections
     */
    public int getConnectionCount()
    {
        return mConnections.size();
    }

    /**
     * Checks each connection's caller again: a signed-out session, revoked token or disabled account closes the
     * connection, and subscriptions whose permission the caller lost end with an error.
     */
    public void refreshPrincipals()
    {
        for(LiveConnection connection: List.copyOf(mConnections.values()))
        {
            Optional<Principal> principal;

            try
            {
                principal = mAccountService.refresh(connection.getPrincipal());
            }
            catch(ApiException e)
            {
                //Database unavailable: keep the connection until the next check
                continue;
            }

            if(principal.isEmpty() || principal.get().mustChangePassword())
            {
                connection.close(LiveConnection.CLOSE_UNAUTHORIZED, "Signed out");
                disconnect(connection.getId());
                continue;
            }

            connection.setPrincipal(principal.get());

            for(Subscription subscription: List.copyOf(connection.getSubscriptions().values()))
            {
                if(!principal.get().has(subscription.getTopic().getPermission()))
                {
                    synchronized(mLock)
                    {
                        connection.getSubscriptions().remove(subscription.getId());
                    }

                    mSpectrum.remove(subscription);
                    connection.sendError(subscription.getId(), ApiException.forbidden(
                        subscription.getTopic().getPermission().getKey() + " required"));
                }
            }
        }
    }

    /**
     * Handles a message from the client.
     */
    void onMessage(LiveConnection connection, String text)
    {
        int sub = 0;

        try
        {
            JsonNode message = connection.parse(text);
            sub = message.path("sub").asInt(0);
            String op = message.path("op").asText("");

            switch(op)
            {
                case "subscribe" -> subscribe(connection, sub, message.path("topic").asText(""),
                    message.path("params"));
                case "update" -> update(connection, sub, message.path("params"));
                case "unsubscribe" -> unsubscribe(connection, sub);
                case "ping" -> connection.sendControl(Map.of("op", "pong"));
                default -> throw ApiException.badRequest("Unknown op '" + op + "'");
            }
        }
        catch(ApiException e)
        {
            connection.sendError(sub, e);
        }
        catch(Exception e)
        {
            LOGGER.error("Error handling a WebSocket message", e);
            connection.sendError(sub, new ApiException(500, "Server error", "See the application log"));
        }
    }

    private void subscribe(LiveConnection connection, int sub, String topicName, JsonNode params)
    {
        if(sub <= 0)
        {
            throw ApiException.invalid("sub", "must be a positive integer chosen by the client");
        }

        Topic topic = Topic.fromName(topicName).orElseThrow(() -> new ApiException(404, "Not found",
            "No topic '" + topicName + "'.  Topics: channels.status, tuners.status, calls, events, playlist, " +
            "spectrum, audio.live"));

        if(!connection.getPrincipal().has(topic.getPermission()))
        {
            throw ApiException.forbidden(topic.getPermission().getKey() + " required");
        }

        if(connection.getSubscriptions().containsKey(sub))
        {
            throw ApiException.conflict("Subscription " + sub + " is already in use");
        }

        if(connection.getSubscriptions().size() >= LiveConnection.MAX_SUBSCRIPTIONS)
        {
            throw ApiException.conflict("A connection can have at most " + LiveConnection.MAX_SUBSCRIPTIONS +
                " subscriptions");
        }

        checkAvailable(topic);
        Subscription subscription = new Subscription(sub, topic, parseFilter(topic, params));

        if(topic == Topic.SPECTRUM)
        {
            mSpectrum.add(connection, subscription, mSpectrum.parse(parameterReader(params)));
        }

        synchronized(mLock)
        {
            connection.getSubscriptions().put(sub, subscription);
            connection.sendControl(Map.of("op", "subscribed", "sub", sub, "topic", topic.getName()));
            sendSnapshot(connection, subscription);
        }
    }

    private void update(LiveConnection connection, int sub, JsonNode params)
    {
        Subscription subscription = connection.getSubscriptions().get(sub);

        if(subscription == null)
        {
            throw ApiException.notFound("No subscription " + sub);
        }

        Subscription.Filter filter = parseFilter(subscription.getTopic(), params);

        if(subscription.getTopic() == Topic.SPECTRUM)
        {
            mSpectrum.add(connection, subscription, mSpectrum.parse(parameterReader(params)));
        }

        synchronized(mLock)
        {
            subscription.setFilter(filter);
            subscription.getActiveCalls().clear();
            subscription.getProgressDue().clear();
            connection.sendControl(Map.of("op", "subscribed", "sub", sub, "topic",
                subscription.getTopic().getName()));
            sendSnapshot(connection, subscription);
        }
    }

    private void unsubscribe(LiveConnection connection, int sub)
    {
        Subscription removed;

        synchronized(mLock)
        {
            removed = connection.getSubscriptions().remove(sub);
        }

        if(removed != null)
        {
            mSpectrum.remove(removed);
        }

        connection.sendControl(Map.of("op", "unsubscribed", "sub", sub));
    }

    private void checkAvailable(Topic topic)
    {
        boolean available = switch(topic)
        {
            case CHANNELS_STATUS -> mSources.playlistManager() != null;
            case TUNERS_STATUS -> mSources.tunerManager() != null;
            case CALLS -> mSources.callManager() != null;
            case EVENTS -> mSources.eventManager() != null;
            case PLAYLIST -> mSources.playlistManager() != null;
            case SPECTRUM -> mSources.tunerManager() != null;
            case AUDIO_LIVE -> mSources.callManager() != null;
        };

        if(!available)
        {
            throw ApiException.unavailable("The " + topic.getName() + " topic isn't available");
        }
    }

    private Subscription.Filter parseFilter(Topic topic, JsonNode params)
    {
        Function<String,String> parameter = parameterReader(params);

        return switch(topic)
        {
            case CALLS, AUDIO_LIVE -> new Subscription.Filter(RecordFilters.calls(parameter),
                RecordFilters.flag(parameter, "includeDuplicates", false), null, null,
                parseProgressInterval(parameter.apply("progressIntervalMs")));
            case EVENTS -> new Subscription.Filter(null, false, RecordFilters.events(parameter), null);
            case CHANNELS_STATUS -> {
                String channel = parameter.apply("channelId");

                try
                {
                    yield new Subscription.Filter(null, false, null, channel == null ? null :
                        Integer.parseInt(channel));
                }
                catch(NumberFormatException nfe)
                {
                    throw ApiException.invalid("channelId", "must be a channel id");
                }
            }
            case TUNERS_STATUS, PLAYLIST, SPECTRUM -> new Subscription.Filter(null, false, null, null);
        };
    }

    /**
     * calls topic progressIntervalMs: 0 (no progress messages) or 250 to 5000, default 1000.
     */
    private static int parseProgressInterval(String value)
    {
        if(value == null || value.isBlank())
        {
            return Subscription.Filter.DEFAULT_PROGRESS_INTERVAL_MS;
        }

        try
        {
            int interval = Integer.parseInt(value.trim());

            if(interval == 0 || (interval >= 250 && interval <= 5000))
            {
                return interval;
            }
        }
        catch(NumberFormatException nfe)
        {
            //Reported below
        }

        throw ApiException.invalid("progressIntervalMs", "must be 0 (none) or from 250 to 5000");
    }

    private static Function<String,String> parameterReader(JsonNode params)
    {
        return name -> {
            JsonNode value = params == null ? null : params.get(name);
            return value == null || value.isNull() ? null : value.asText();
        };
    }

    /**
     * Sends the snapshot of a subscription that a test created directly.
     */
    void sendSnapshotForTest(LiveConnection connection, Subscription subscription)
    {
        synchronized(mLock)
        {
            sendSnapshot(connection, subscription);
        }
    }

    /**
     * Sends the current state as the first data message of a subscription.  Hub lock.
     */
    private void sendSnapshot(LiveConnection connection, Subscription subscription)
    {
        Subscription.Filter filter = subscription.getFilter();
        Map<String,Object> data = new LinkedHashMap<>();

        switch(subscription.getTopic())
        {
            case CHANNELS_STATUS -> {
                data.put("type", "snapshot");
                data.put("statuses", ChannelsController.getStatus(mSources.playlistManager(),
                    mSources.talkgroupFormat()).stream().filter(status -> filter.matchesChannel(status.channelId()))
                    .toList());
            }
            case TUNERS_STATUS -> {
                data.put("type", "snapshot");
                data.put("tuners", mSources.tunerManager().getDiscoveredTunerModel().getDiscoveredTuners().stream()
                    .map(TunerView::of).toList());
            }
            case CALLS -> {
                List<CallView> calls = new ArrayList<>();

                for(CallRecord call: mActiveCalls.values())
                {
                    if(filter.matches(call))
                    {
                        subscription.getActiveCalls().add(call.getCallKey());
                        calls.add(CallView.of(call, mCallIds.get(call.getCallKey()), mAliases));
                    }
                }

                data.put("type", "call.snapshot");
                data.put("calls", calls);
            }
            case EVENTS, PLAYLIST, SPECTRUM, AUDIO_LIVE -> {
                return;
            }
        }

        connection.sendData(subscription, null, json(data));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Calls
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Live call snapshot from the call manager: started, changed or completed.
     */
    public void callChanged(CallRecord call)
    {
        synchronized(mLock)
        {
            long key = call.getCallKey();
            boolean started = !mActiveCalls.containsKey(key);
            boolean ended = !call.isActive();

            if(ended)
            {
                mActiveCalls.remove(key);
                mRecentCalls.put(key, call);
            }
            else
            {
                mActiveCalls.put(key, call);
            }

            String type = ended ? "call.ended" : (started ? "call.started" : "call.updated");
            Long audioMs = ended ? mAudioMs.remove(key) : null;
            Supplier<String> data = once(() -> {
                Map<String,Object> message = new LinkedHashMap<>();
                message.put("type", type);
                message.put("call", CallView.of(call, mCallIds.get(key), mAliases));

                if(ended)
                {
                    //Final decoded audio length, when the live audio tracker measured it
                    message.put("audioMs", audioMs);
                }

                return json(message);
            });
            Supplier<String> removed = once(() -> json(Map.of("type", "call.removed", "callKey", key)));

            forEach(Topic.CALLS, (connection, subscription) -> {
                boolean matches = subscription.getFilter().matches(call);
                boolean known = subscription.getActiveCalls().contains(key);

                if(matches)
                {
                    if(ended)
                    {
                        subscription.getActiveCalls().remove(key);
                        subscription.getProgressDue().remove(key);
                        connection.sendData(subscription, null, data.get());
                    }
                    else if(known || started)
                    {
                        subscription.getActiveCalls().add(key);
                        connection.sendData(subscription, known ? key : null, data.get());
                    }
                    else
                    {
                        //Began to match mid-call: deliver as a start for this subscription
                        subscription.getActiveCalls().add(key);
                        connection.sendData(subscription, null, json(Map.of("type", "call.started", "call",
                            CallView.of(call, mCallIds.get(key), mAliases))));
                    }
                }
                else if(known)
                {
                    //Stopped matching mid-call, for example marked as a duplicate
                    subscription.getActiveCalls().remove(key);
                    subscription.getProgressDue().remove(key);
                    connection.sendData(subscription, null, removed.get());
                }
            });
        }
    }

    /**
     * Progress of an active call from the live audio tracker, about every 40 ms.  Each calls subscription that knows
     * the call receives a call.progress message at its own interval: audioMs (decoded audio so far, what a player
     * can play now), elapsedMs (wall time since the call started, which keeps growing during an encrypted call or a
     * gap) and lastAudioMs (time since audio last arrived, so a client can show a call as quiet).  A waiting progress
     * message is replaced by a newer one.
     * @param callKey of the call (the audio segment id)
     * @param audioMs decoded audio length
     * @param lastAudioMs milliseconds since audio last arrived
     * @param now epoch milliseconds
     */
    public void callProgress(long callKey, long audioMs, long lastAudioMs, long now)
    {
        synchronized(mLock)
        {
            CallRecord call = mActiveCalls.get(callKey);

            if(call == null)
            {
                return;
            }

            mAudioMs.put(callKey, audioMs);
            Supplier<String> data = once(() -> {
                Map<String,Object> message = new LinkedHashMap<>();
                message.put("type", "call.progress");
                message.put("callKey", callKey);
                message.put("id", mCallIds.get(callKey));
                message.put("audioMs", audioMs);
                message.put("elapsedMs", Math.max(0, now - call.getStartTime()));
                message.put("lastAudioMs", Math.max(0, lastAudioMs));
                return json(message);
            });

            forEach(Topic.CALLS, (connection, subscription) -> {
                int interval = subscription.getFilter().progressIntervalMs();

                if(interval <= 0 || !subscription.getActiveCalls().contains(callKey))
                {
                    return;
                }

                Long due = subscription.getProgressDue().get(callKey);

                if(due == null)
                {
                    //First progress one interval after the subscription learned of the call
                    subscription.getProgressDue().put(callKey, now + interval);
                }
                else if(now >= due)
                {
                    subscription.getProgressDue().put(callKey, now + interval);
                    connection.sendData(subscription, "progress:" + callKey, data.get());
                }
            });
        }
    }

    /**
     * Call written to the database: tells subscribers the stored id of a call the first time it is known.
     */
    public void callSaved(CallRecord call)
    {
        if(call.getId() <= 0 || call.getCallKey() == 0)
        {
            return;
        }

        synchronized(mLock)
        {
            Long previous = mCallIds.put(call.getCallKey(), call.getId());

            if(previous != null && previous == call.getId())
            {
                return;
            }

            Supplier<String> data = once(() -> json(Map.of("type", "call.stored", "callKey", call.getCallKey(),
                "id", call.getId())));

            forEach(Topic.CALLS, (connection, subscription) -> {
                if(subscription.getFilter().matches(call))
                {
                    connection.sendData(subscription, null, data.get());
                }
            });
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Events
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Live decode event snapshot from the event manager: created or updated.
     */
    public void eventChanged(EventRecord event)
    {
        synchronized(mLock)
        {
            long key = event.getEventKey();
            boolean created = mSeenEvents.put(key, Boolean.TRUE) == null;
            String type = created ? "event.created" : "event.updated";
            Supplier<String> data = once(() -> json(Map.of("type", type, "event",
                EventView.of(event, mEventIds.get(key), mAliases))));

            forEach(Topic.EVENTS, (connection, subscription) -> {
                if(subscription.getFilter().matches(event))
                {
                    connection.sendData(subscription, created ? null : "event:" + key, data.get());
                }
            });
        }
    }

    /**
     * Event written to the database: tells subscribers the stored id of an event the first time it is known.
     */
    public void eventSaved(EventRecord event)
    {
        if(event.getId() <= 0 || event.getEventKey() == 0)
        {
            return;
        }

        synchronized(mLock)
        {
            Long previous = mEventIds.put(event.getEventKey(), event.getId());

            if(previous != null && previous == event.getId())
            {
                return;
            }

            Supplier<String> data = once(() -> json(Map.of("type", "event.stored", "eventKey", event.getEventKey(),
                "id", event.getId())));

            forEach(Topic.EVENTS, (connection, subscription) -> {
                if(subscription.getFilter().matches(event))
                {
                    connection.sendData(subscription, null, data.get());
                }
            });
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Channel status and tuners
    //----------------------------------------------------------------------------------------------------------------

    /**
     * New state of a channel timeslot.
     */
    public void channelStatus(ChannelStatusView status)
    {
        synchronized(mLock)
        {
            Supplier<String> data = once(() -> json(Map.of("type", "channel.status", "status", status)));

            forEach(Topic.CHANNELS_STATUS, (connection, subscription) -> {
                if(subscription.getFilter().matchesChannel(status.channelId()))
                {
                    connection.sendData(subscription, "channel:" + status.key(), data.get());
                }
            });
        }
    }

    /**
     * A channel timeslot stopped processing.
     */
    public void channelRemoved(String key, int channelId)
    {
        synchronized(mLock)
        {
            Supplier<String> data = once(() -> json(Map.of("type", "channel.removed", "key", key, "channelId",
                channelId)));

            forEach(Topic.CHANNELS_STATUS, (connection, subscription) -> {
                if(subscription.getFilter().matchesChannel(channelId))
                {
                    //Same coalescing key as the status, so a waiting status for the timeslot is replaced
                    connection.sendData(subscription, "channel:" + key, data.get());
                }
            });
        }
    }

    /**
     * New state of a tuner.
     */
    public void tunerChanged(TunerView tuner)
    {
        synchronized(mLock)
        {
            Supplier<String> data = once(() -> json(Map.of("type", "tuner.status", "tuner", tuner)));
            forEach(Topic.TUNERS_STATUS, (connection, subscription) ->
                connection.sendData(subscription, "tuner:" + tuner.key(), data.get()));
        }
    }

    /**
     * A tuner was removed.
     */
    public void tunerRemoved(String key)
    {
        synchronized(mLock)
        {
            Supplier<String> data = once(() -> json(Map.of("type", "tuner.removed", "key", key)));
            forEach(Topic.TUNERS_STATUS, (connection, subscription) ->
                connection.sendData(subscription, "tuner:" + key, data.get()));
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Live audio
    //----------------------------------------------------------------------------------------------------------------

    /**
     * One Opus packet of a call, from the live audio encoder.  Sent to each audio.live subscription whose filter
     * matches the call, after an audio.start message for the call.
     * @param callKey of the call (the audio segment id)
     * @param sequence of the frame within the call
     * @param packet Opus packet, 20 ms
     * @return false when the call isn't known yet (the call manager publishes it shortly), so the packet should be
     * offered again
     */
    boolean audioPacket(long callKey, int sequence, byte[] packet)
    {
        synchronized(mLock)
        {
            CallRecord call = mActiveCalls.get(callKey);

            if(call == null)
            {
                call = mRecentCalls.get(callKey);
            }

            if(call == null)
            {
                return false;
            }

            CallRecord current = call;

            forEach(Topic.AUDIO_LIVE, (connection, subscription) -> {
                Map<Long,int[]> calls = subscription.getAudioCalls();
                int[] state = calls.get(callKey);

                if(!subscription.getFilter().matches(current))
                {
                    if(state != null)
                    {
                        calls.remove(callKey);
                        connection.sendData(subscription, null, json(audioEnd(callKey, state, "FILTERED")));
                    }

                    return;
                }

                if(state == null)
                {
                    state = new int[3];
                    calls.put(callKey, state);
                    Map<String,Object> start = new LinkedHashMap<>();
                    start.put("type", "audio.start");
                    start.put("callKey", callKey);
                    start.put("codec", "opus");
                    start.put("sampleRate", io.github.dsheirer.web.media.OpusStream.SAMPLE_RATE);
                    start.put("channels", 1);
                    start.put("frameMs", io.github.dsheirer.web.media.OpusStream.FRAME_MS);
                    start.put("call", CallView.of(current, mCallIds.get(callKey), mAliases));
                    connection.sendData(subscription, null, json(start));
                }

                if(connection.getQueueSize() > AUDIO_QUEUE_LIMIT)
                {
                    state[1]++;
                    state[2]++;
                    return;
                }

                if(state[2] > 0)
                {
                    connection.sendData(subscription, null, json(Map.of("type", "audio.dropped", "callKey", callKey,
                        "frames", state[2])));
                    state[2] = 0;
                }

                connection.sendBinary(subscription, null, io.github.dsheirer.web.media.BinaryFrames.audio(
                    subscription.getId(), callKey, sequence, packet));
                state[0]++;
            });

            return true;
        }
    }

    /**
     * The call's audio ended.
     */
    void audioEnded(long callKey)
    {
        synchronized(mLock)
        {
            forEach(Topic.AUDIO_LIVE, (connection, subscription) -> {
                int[] state = subscription.getAudioCalls().remove(callKey);

                if(state != null)
                {
                    connection.sendData(subscription, null, json(audioEnd(callKey, state, "COMPLETE")));
                }
            });
        }
    }

    private static Map<String,Object> audioEnd(long callKey, int[] state, String reason)
    {
        Map<String,Object> end = new LinkedHashMap<>();
        end.put("type", "audio.end");
        end.put("callKey", callKey);
        end.put("reason", reason);
        end.put("frames", state[0]);
        end.put("droppedFrames", state[1]);
        return end;
    }

    /**
     * Count of running spectrum sources
     */
    public int getSpectrumSourceCount()
    {
        return mSpectrum.getSourceCount();
    }

    //----------------------------------------------------------------------------------------------------------------
    // Playlist
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Channel added, changed or deleted in the channel model (desktop editor, web API or playlist load).
     */
    void channelEvent(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        if(channel.getChannelType() != ChannelType.STANDARD || !hasSubscribers(Topic.PLAYLIST))
        {
            return;
        }

        String type;
        String version = null;

        switch(event.getEvent())
        {
            case NOTIFICATION_ADD -> type = "channel.added";
            case NOTIFICATION_CONFIGURATION_CHANGE -> type = "channel.changed";
            case NOTIFICATION_DELETE -> type = "channel.removed";
            default -> {
                return;
            }
        }

        if(!type.equals("channel.removed"))
        {
            version = PlaylistConfig.version(channel);
        }

        synchronized(mLock)
        {
            String previous = version != null ? mChannelVersions.put(channel.getChannelID(), version) :
                mChannelVersions.remove(channel.getChannelID());

            if(type.equals("channel.changed") && version.equals(previous))
            {
                return;
            }

            Map<String,Object> data = new LinkedHashMap<>();
            data.put("type", type);
            data.put("id", channel.getChannelID());
            data.put("version", version);

            if(version != null)
            {
                data.put("channel", RuntimeViews.ChannelView.of(channel));
            }

            publishPlaylist("channel:" + channel.getChannelID(), json(data));
        }
    }

    /**
     * Aliases added, changed or removed in the alias model.  A large change (a playlist load or an import) is
     * announced as one playlist.reloaded message, after which clients read the aliases again.
     */
    void aliasesChanged(ListChangeListener.Change<? extends Alias> change)
    {
        if(!hasSubscribers(Topic.PLAYLIST))
        {
            return;
        }

        List<Map<String,Object>> messages = new ArrayList<>();
        int count = 0;

        while(change.next())
        {
            if(change.wasPermutated())
            {
                continue;
            }

            if(change.wasRemoved())
            {
                count += change.getRemovedSize();

                for(Alias alias: change.getRemoved())
                {
                    Long id = mAliasRegistry.findId(alias);

                    //A replaced alias (set) is also reported as added below
                    if(id != null && !change.getList().contains(alias))
                    {
                        messages.add(aliasMessage("alias.removed", id, null, null));
                    }
                }
            }

            if(change.wasAdded())
            {
                count += change.getAddedSize();

                for(Alias alias: change.getAddedSubList())
                {
                    messages.add(aliasMessage("alias.added", mAliasRegistry.getId(alias), alias,
                        PlaylistConfig.version(alias)));
                }
            }

            if(change.wasUpdated())
            {
                for(int x = change.getFrom(); x < change.getTo(); x++)
                {
                    count++;
                    Alias alias = change.getList().get(x);
                    messages.add(aliasMessage("alias.changed", mAliasRegistry.getId(alias), alias,
                        PlaylistConfig.version(alias)));
                }
            }
        }

        synchronized(mLock)
        {
            if(count > BULK_CHANGE)
            {
                mAliasVersions.clear();
                publishPlaylist(null, json(Map.of("type", "playlist.reloaded")));
                return;
            }

            for(Map<String,Object> message: messages)
            {
                long id = (Long)message.get("id");
                String version = (String)message.get("version");
                String previous = version != null ? mAliasVersions.put(id, version) : mAliasVersions.remove(id);

                if("alias.changed".equals(message.get("type")) && version.equals(previous))
                {
                    continue;
                }

                publishPlaylist("alias:" + id, json(message));
            }
        }
    }

    private static Map<String,Object> aliasMessage(String type, long id, Alias alias, String version)
    {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("id", id);
        data.put("version", version);

        if(alias != null)
        {
            data.put("name", alias.getName());
            data.put("list", alias.getAliasListName());
        }

        return data;
    }

    /**
     * Hub lock.
     */
    private void publishPlaylist(Object coalesceKey, String data)
    {
        forEach(Topic.PLAYLIST, (connection, subscription) -> connection.sendData(subscription, coalesceKey, data));
    }

    //----------------------------------------------------------------------------------------------------------------
    // Support
    //----------------------------------------------------------------------------------------------------------------

    private void forEach(Topic topic, SubscriptionAction action)
    {
        for(LiveConnection connection: mConnections.values())
        {
            if(connection.isClosed())
            {
                continue;
            }

            for(Subscription subscription: connection.getSubscriptions().values())
            {
                if(subscription.getTopic() == topic)
                {
                    action.apply(connection, subscription);
                }
            }
        }
    }

    boolean hasSubscribers(Topic topic)
    {
        for(LiveConnection connection: mConnections.values())
        {
            for(Subscription subscription: connection.getSubscriptions().values())
            {
                if(subscription.getTopic() == topic)
                {
                    return true;
                }
            }
        }

        return false;
    }

    private ChannelMetadataModel getMetadataModel()
    {
        return mSources.playlistManager() != null ?
            mSources.playlistManager().getChannelProcessingManager().getChannelMetadataModel() : null;
    }

    private String json(Object value)
    {
        try
        {
            return mMapper.writeValueAsString(value);
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Unable to serialize a live message", e);
        }
    }

    /**
     * Memoizes a supplier, so a message is serialized at most once however many subscriptions receive it.
     */
    private static Supplier<String> once(Supplier<String> supplier)
    {
        return new Supplier<>()
        {
            private String mValue;

            @Override
            public String get()
            {
                if(mValue == null)
                {
                    mValue = supplier.get();
                }

                return mValue;
            }
        };
    }

    private static <K,V> Map<K,V> lru()
    {
        return new LinkedHashMap<>(256, 0.75f, false)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest)
            {
                return size() > MAX_TRACKED;
            }
        };
    }

    @FunctionalInterface
    private interface SubscriptionAction
    {
        void apply(LiveConnection connection, Subscription subscription);
    }

    /**
     * Channel metadata changes from the channel processing manager
     */
    private class MetadataListener implements ChannelMetadataModel.Listener
    {
        @Override
        public void added(ChannelAndMetadata channelAndMetadata)
        {
            for(ChannelMetadata metadata: channelAndMetadata.getChannelMetadata())
            {
                publish(metadata, channelAndMetadata.getChannel());
            }
        }

        @Override
        public void removed(ChannelMetadata metadata)
        {
            ChannelStatusView last;

            synchronized(mLock)
            {
                last = mLastStatus.remove(metadata);
            }

            if(last != null)
            {
                channelRemoved(last.key(), last.channelId());
            }
        }

        @Override
        public void updated(ChannelMetadata metadata, ChannelMetadataField field)
        {
            ChannelMetadataModel model = getMetadataModel();
            publish(metadata, model != null ? model.getChannelFromMetadata(metadata) : null);
        }

        private void publish(ChannelMetadata metadata, Channel channel)
        {
            if(channel == null)
            {
                return;
            }

            ChannelStatusView status;

            try
            {
                status = ChannelStatusView.of(metadata, channel, mSources.talkgroupFormat());
            }
            catch(Exception e)
            {
                LOGGER.debug("Unable to read channel metadata for the web API", e);
                return;
            }

            synchronized(mLock)
            {
                ChannelStatusView previous = mLastStatus.put(metadata, status);

                //Many metadata updates don't change what the status shows
                if(status.equals(previous) || !hasSubscribers(Topic.CHANNELS_STATUS))
                {
                    return;
                }
            }

            channelStatus(status);
        }
    }

    /**
     * Discovered tuner changes from the tuner model
     */
    private class TunerListener implements DiscoveredTunerModel.ModelListener
    {
        @Override
        public void tunerAdded(DiscoveredTuner discoveredTuner)
        {
            tunerUpdated(discoveredTuner);
        }

        @Override
        public void tunerRemoved(DiscoveredTuner discoveredTuner)
        {
            mSpectrum.tunerStopped(RuntimeViews.tunerKey(discoveredTuner));
            LiveHub.this.tunerRemoved(RuntimeViews.tunerKey(discoveredTuner));
        }

        @Override
        public void tunerUpdated(DiscoveredTuner discoveredTuner)
        {
            if(!discoveredTuner.isAvailable())
            {
                mSpectrum.tunerStopped(RuntimeViews.tunerKey(discoveredTuner));
            }

            if(hasSubscribers(Topic.TUNERS_STATUS))
            {
                tunerChanged(TunerView.of(discoveredTuner));
            }
        }
    }
}
