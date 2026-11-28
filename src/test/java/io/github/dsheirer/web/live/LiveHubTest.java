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
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.CallState;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.web.WebSources;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.RuntimeViews.ChannelStatusView;
import io.github.dsheirer.web.auth.PasswordHasher;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.Principal;
import io.github.dsheirer.web.auth.Role;
import io.github.dsheirer.web.auth.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WebSocket protocol, filtering and backpressure of the live hub, without a server: a test sink stands in for the
 * WebSocket session.
 */
public class LiveHubTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private LiveHub mHub;
    private SessionManager mSessions;

    /**
     * Sink that records sent messages.  Sends complete at once unless held.
     */
    private static class TestSink implements MessageSink
    {
        final List<JsonNode> mMessages = new ArrayList<>();
        final List<Runnable> mHeld = new ArrayList<>();
        boolean mHold;
        Integer mCloseCode;

        @Override
        public void send(String text, Runnable succeeded, Consumer<Throwable> failed)
        {
            try
            {
                mMessages.add(MAPPER.readTree(text));
            }
            catch(Exception e)
            {
                throw new IllegalStateException(e);
            }

            if(mHold)
            {
                mHeld.add(succeeded);
            }
            else
            {
                succeeded.run();
            }
        }

        final List<byte[]> mBinary = new ArrayList<>();

        @Override
        public void sendBinary(byte[] data, Runnable succeeded, Consumer<Throwable> failed)
        {
            mBinary.add(data);
            succeeded.run();
        }

        @Override
        public void close(int code, String reason)
        {
            mCloseCode = code;
        }

        void release()
        {
            mHold = false;

            while(!mHeld.isEmpty())
            {
                mHeld.removeFirst().run();
            }
        }

        JsonNode last()
        {
            return mMessages.getLast();
        }

        List<JsonNode> data()
        {
            return mMessages.stream().filter(message -> "data".equals(message.path("op").asText())).toList();
        }
    }

    @BeforeEach
    void setUp()
    {
        mSessions = new SessionManager(() -> 60, System::currentTimeMillis);
        AccountService accounts = new AccountService(() -> null, new PasswordHasher(1000), mSessions,
            System::currentTimeMillis);
        //Sources aren't needed: the tests publish changes to the hub directly and subscribe without the source check
        mHub = new LiveHub(WebSources.none(), null, accounts, MAPPER, new io.github.dsheirer.web.playlist.AliasRegistry());
    }

    private static Principal principal(Role role)
    {
        return new Principal(1, "user", null, role, role.getPermissions(), Principal.Method.TOKEN, null, 1, null,
            false);
    }

    private TestSink connect(Principal principal)
    {
        TestSink sink = new TestSink();
        mHub.connect("c" + System.nanoTime(), principal, sink);
        return sink;
    }

    private static CallRecord call(long key, long to, boolean active)
    {
        CallRecord call = new CallRecord();
        call.setCallKey(key);
        call.setStartTime(1_800_000_000_000L);
        call.setProtocol("APCO25");
        call.setSystem("County");
        call.setToId(String.valueOf(to));
        call.setToValue(to);
        call.setToForm("TALKGROUP");
        call.setCallState(active ? CallState.ACTIVE : CallState.COMPLETE);

        if(!active)
        {
            call.setEndTime(1_800_000_005_000L);
        }

        return call;
    }

    @Test
    void protocolErrors()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.ADMIN), sink);

        mHub.onMessage(connection, "not json");
        assertEquals(400, sink.last().get("status").asInt());

        mHub.onMessage(connection, "{\"op\":\"ping\"}");
        assertEquals("pong", sink.last().get("op").asText());

        mHub.onMessage(connection, "{\"op\":\"subscribe\",\"sub\":1,\"topic\":\"nope\"}");
        assertEquals(404, sink.last().get("status").asInt());

        mHub.onMessage(connection, "{\"op\":\"subscribe\",\"sub\":0,\"topic\":\"calls\"}");
        assertEquals(400, sink.last().get("status").asInt());

        //No call manager in the sources
        mHub.onMessage(connection, "{\"op\":\"subscribe\",\"sub\":1,\"topic\":\"calls\"}");
        assertEquals(503, sink.last().get("status").asInt());

        mHub.onMessage(connection, "{\"op\":\"unsubscribe\",\"sub\":9}");
        assertEquals("unsubscribed", sink.last().get("op").asText());
    }

    @Test
    void topicPermissionIsChecked()
    {
        TestSink sink = new TestSink();
        Principal mapOnly = new Principal(1, "user", null, Role.ADMIN, Set.of(Permission.MAP_READ),
            Principal.Method.TOKEN, null, 1, null, false);
        LiveConnection connection = mHub.connect("c1", mapOnly, sink);
        mHub.onMessage(connection, "{\"op\":\"subscribe\",\"sub\":1,\"topic\":\"calls\"}");
        assertEquals(403, sink.last().get("status").asInt());
        assertEquals("events.read required", sink.last().get("title").asText().equals("Forbidden") ?
            sink.last().get("detail").asText() : sink.last().get("title").asText());
    }

    @Test
    void callLifecycleWithFilter()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        Subscription subscription = subscribe(connection, 3, Topic.CALLS, new Subscription.Filter(
            io.github.dsheirer.web.api.RecordFilters.calls(name -> "to".equals(name) ? "1201" : null), false, null,
            null));

        mHub.callChanged(call(10, 1201, true));
        mHub.callChanged(call(11, 999, true));
        CallRecord updated = call(10, 1201, true);
        updated.setTalkerAlias("ENGINE 4");
        mHub.callChanged(updated);
        mHub.callSaved(withId(call(10, 1201, true), 77));
        mHub.callChanged(call(10, 1201, false));

        List<String> types = sink.data().stream().map(message -> message.path("data").path("type").asText())
            .toList();
        assertEquals(List.of("call.snapshot", "call.started", "call.updated", "call.stored", "call.ended"), types);
        JsonNode ended = sink.data().getLast();
        assertEquals("calls", ended.get("topic").asText());
        assertEquals(3, ended.get("sub").asInt());
        assertEquals(4, ended.get("seq").asInt());
        assertEquals(77, ended.path("data").path("call").path("id").asLong());
        assertEquals("COMPLETE", ended.path("data").path("call").path("state").asText());
        assertTrue(subscription.getActiveCalls().isEmpty());
    }

    @Test
    void duplicateMidCallIsRemoved()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.CALLS, new Subscription.Filter(null, false, null, null));
        mHub.callChanged(call(20, 5, true));
        CallRecord duplicate = call(20, 5, true);
        duplicate.setDuplicate(true);
        mHub.callChanged(duplicate);
        mHub.callChanged(call(20, 5, false));

        List<String> types = sink.data().stream().map(message -> message.path("data").path("type").asText())
            .toList();
        assertEquals(List.of("call.snapshot", "call.started", "call.removed", "call.ended"), types);
    }

    @Test
    void snapshotHasActiveCalls()
    {
        mHub.callChanged(call(30, 1, true));
        mHub.callChanged(call(31, 2, true));
        mHub.callChanged(call(31, 2, false));

        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.CALLS, new Subscription.Filter(null, false, null, null));
        JsonNode snapshot = sink.data().getFirst().path("data");
        assertEquals("call.snapshot", snapshot.get("type").asText());
        assertEquals(1, snapshot.get("calls").size());
        assertEquals(30, snapshot.get("calls").get(0).get("callKey").asLong());
    }

    @Test
    void eventsCreatedThenUpdated()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.EVENTS, new Subscription.Filter(null, false,
            io.github.dsheirer.web.api.RecordFilters.events(name -> "type".equals(name) ? "REGISTER" : null), null));

        EventRecord register = new EventRecord();
        register.setEventKey(1);
        register.setEventType("REGISTER");
        EventRecord group = new EventRecord();
        group.setEventKey(2);
        group.setEventType("CALL_GROUP");

        mHub.eventChanged(register);
        mHub.eventChanged(group);
        mHub.eventChanged(register);

        List<String> types = sink.data().stream().map(message -> message.path("data").path("type").asText())
            .toList();
        assertEquals(List.of("event.created", "event.updated"), types);
    }

    @Test
    void slowClientGetsLatestStatus()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.CHANNELS_STATUS, new Subscription.Filter(null, false, null, null));
        sink.mHold = true;
        int before = sink.mMessages.size();

        //First status goes out at once (and is held); the next ones wait and replace each other
        for(String state: List.of("IDLE", "CALL", "FADE", "IDLE"))
        {
            mHub.channelStatus(status(5, state));
        }

        mHub.channelStatus(status(6, "CONTROL"));
        sink.release();

        List<JsonNode> sent = sink.mMessages.subList(before, sink.mMessages.size());
        assertEquals(3, sent.size());
        assertEquals("IDLE", sent.get(0).path("data").path("status").path("state").asText());
        assertEquals("IDLE", sent.get(1).path("data").path("status").path("state").asText());
        assertEquals(6, sent.get(2).path("data").path("status").path("channelId").asInt());
        assertEquals(List.of(0L, 1L, 2L), sent.stream().map(message -> message.get("seq").asLong()).toList(),
            "no gap in the sequence for replaced messages");
    }

    @Test
    void overflowClosesTheConnection()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.EVENTS, new Subscription.Filter(null, false, null, null));
        sink.mHold = true;

        for(int x = 0; x <= LiveConnection.MAX_QUEUED + 1; x++)
        {
            EventRecord event = new EventRecord();
            event.setEventKey(1000 + x);
            event.setEventType("REGISTER");
            mHub.eventChanged(event);
        }

        assertEquals(LiveConnection.CLOSE_TOO_SLOW, sink.mCloseCode);
        assertTrue(connection.isClosed());
    }

    @Test
    void longQueueDoesNotRecurse()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.EVENTS, new Subscription.Filter(null, false, null, null));
        sink.mHold = true;

        for(int x = 0; x < LiveConnection.MAX_QUEUED - 10; x++)
        {
            EventRecord event = new EventRecord();
            event.setEventKey(5000 + x);
            event.setEventType("REGISTER");
            mHub.eventChanged(event);
        }

        //Each send now completes at once, inside the send call
        sink.release();
        assertEquals(LiveConnection.MAX_QUEUED - 10, sink.data().size());
        assertFalse(connection.isClosed());
    }

    @Test
    void signedOutSessionIsClosed()
    {
        TestSink sink = new TestSink();
        Principal session = new Principal(1, "user", null, Role.VIEWER, Role.VIEWER.getPermissions(),
            Principal.Method.SESSION, "ended-session", 0, "csrf", false);
        mHub.connect("c1", session, sink);
        mHub.refreshPrincipals();
        assertEquals(LiveConnection.CLOSE_UNAUTHORIZED, sink.mCloseCode);
        assertEquals(0, mHub.getConnectionCount());
    }

    @Test
    void playlistChangesAreAnnouncedOnce()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.PLAYLIST, new Subscription.Filter(null, false, null, null));

        io.github.dsheirer.controller.channel.Channel channel = new io.github.dsheirer.controller.channel.Channel(
            "Dispatch");
        mHub.channelEvent(new io.github.dsheirer.controller.channel.ChannelEvent(channel,
            io.github.dsheirer.controller.channel.ChannelEvent.Event.NOTIFICATION_ADD));
        //Processing changes report a configuration change that doesn't change the version
        mHub.channelEvent(new io.github.dsheirer.controller.channel.ChannelEvent(channel,
            io.github.dsheirer.controller.channel.ChannelEvent.Event.NOTIFICATION_CONFIGURATION_CHANGE));
        channel.setSite("North");
        mHub.channelEvent(new io.github.dsheirer.controller.channel.ChannelEvent(channel,
            io.github.dsheirer.controller.channel.ChannelEvent.Event.NOTIFICATION_CONFIGURATION_CHANGE));
        mHub.channelEvent(new io.github.dsheirer.controller.channel.ChannelEvent(channel,
            io.github.dsheirer.controller.channel.ChannelEvent.Event.NOTIFICATION_DELETE));

        javafx.collections.ObservableList<io.github.dsheirer.alias.Alias> aliases =
            javafx.collections.FXCollections.observableArrayList(io.github.dsheirer.alias.Alias.extractor());
        aliases.addListener(mHub::aliasesChanged);
        io.github.dsheirer.alias.Alias alias = new io.github.dsheirer.alias.Alias("Engine 4");
        alias.setAliasListName("Fire");
        aliases.add(alias);
        alias.setGroup("Engines");
        aliases.remove(alias);

        List<String> types = sink.data().stream().map(message -> message.path("data").path("type").asText())
            .toList();
        assertEquals(List.of("channel.added", "channel.changed", "channel.removed", "alias.added", "alias.changed",
            "alias.removed"), types);
        JsonNode changed = sink.data().get(1).path("data");
        assertEquals("North", changed.path("channel").path("site").asText());
        assertEquals(io.github.dsheirer.web.playlist.PlaylistConfig.version(channel),
            changed.path("version").asText());

        //A bulk change (playlist load) is one message
        List<io.github.dsheirer.alias.Alias> many = new ArrayList<>();

        for(int x = 0; x < 100; x++)
        {
            many.add(new io.github.dsheirer.alias.Alias("A" + x));
        }

        aliases.addAll(many);
        assertEquals("playlist.reloaded", sink.data().getLast().path("data").path("type").asText());
        assertEquals(7, sink.data().size());
    }

    @Test
    void liveAudioFlow()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.LISTENER), sink);
        Subscription subscription = new Subscription(1, Topic.AUDIO_LIVE, new Subscription.Filter(
            io.github.dsheirer.web.api.RecordFilters.calls(name -> "to".equals(name) ? "1201" : null), false, null,
            null));
        connection.getSubscriptions().put(1, subscription);

        //Audio before the call manager published the call
        assertFalse(mHub.audioPacket(50, 0, new byte[]{1}));

        mHub.callChanged(call(50, 1201, true));
        mHub.callChanged(call(51, 999, true));
        assertTrue(mHub.audioPacket(50, 0, new byte[]{1, 2}));
        assertTrue(mHub.audioPacket(50, 1, new byte[]{3}));
        assertTrue(mHub.audioPacket(51, 0, new byte[]{4}), "known call that doesn't match the filter");
        mHub.callChanged(call(50, 1201, false));
        //The last frames after the call ended still find it
        assertTrue(mHub.audioPacket(50, 2, new byte[]{5}));
        mHub.audioEnded(50);

        List<String> types = sink.data().stream().map(message -> message.path("data").path("type").asText())
            .toList();
        assertEquals(List.of("audio.start", "audio.end"), types);
        JsonNode start = sink.data().getFirst().path("data");
        assertEquals("opus", start.get("codec").asText());
        assertEquals(8000, start.get("sampleRate").asInt());
        assertEquals(1201, start.path("call").path("to").path("value").asInt());
        assertEquals(3, sink.data().getLast().path("data").path("frames").asInt());
        assertEquals(3, sink.mBinary.size());
        assertEquals(50, java.nio.ByteBuffer.wrap(sink.mBinary.getFirst()).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .getLong(8));
    }

    @Test
    void slowAudioClientDropsFrames()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.LISTENER), sink);
        Subscription subscription = new Subscription(1, Topic.AUDIO_LIVE, new Subscription.Filter(null, false, null,
            null));
        connection.getSubscriptions().put(1, subscription);
        mHub.callChanged(call(60, 1, true));
        sink.mHold = true;

        for(int x = 0; x < LiveHub.AUDIO_QUEUE_LIMIT + 20; x++)
        {
            mHub.audioPacket(60, x, new byte[]{1});
        }

        sink.release();
        mHub.audioPacket(60, 999, new byte[]{1});
        mHub.audioEnded(60);
        assertFalse(connection.isClosed(), "audio drops frames instead of closing");
        assertTrue(sink.data().stream().anyMatch(message ->
            "audio.dropped".equals(message.path("data").path("type").asText())));
        assertTrue(sink.data().getLast().path("data").path("droppedFrames").asInt() > 0);
    }

    @Test
    void liveAudioEncodesSegments() throws Exception
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.LISTENER), sink);
        connection.getSubscriptions().put(1, new Subscription(1, Topic.AUDIO_LIVE, new Subscription.Filter(null,
            false, null, null)));
        LiveAudio audio = new LiveAudio(() -> mHub);

        io.github.dsheirer.audio.AudioSegment segment = new io.github.dsheirer.audio.AudioSegment(null, 0);
        segment.incrementConsumerCount();
        audio.receive(segment);
        segment.addAudio(new float[400]);
        audio.process();
        assertTrue(sink.mBinary.isEmpty(), "waits until the call is published");

        mHub.callChanged(call(segment.getId(), 1, true));
        segment.addAudio(new float[400]);
        audio.process();
        assertEquals(5, sink.mBinary.size(), "800 samples are five 20 ms frames");

        segment.completeProperty().set(true);
        mHub.callChanged(call(segment.getId(), 1, false));
        audio.process();
        assertEquals(5, sink.mBinary.size(), "no partial frame left");
        assertEquals("audio.end", sink.data().getLast().path("data").path("type").asText());
        assertEquals(0, audio.getTrackedCount());
    }

    @Test
    void spectrumParameters()
    {
        LiveSpectrum spectrum = new LiveSpectrum(null);
        LiveSpectrum.Parameters parameters = spectrum.parse(name -> switch(name)
        {
            case "source" -> "tuner:abc";
            case "fftSize" -> "8192";
            case "frameRate" -> "10";
            default -> null;
        });
        assertEquals("abc", parameters.tunerKey());
        assertEquals(8192, parameters.size().getSize());
        assertEquals(10, parameters.frameRate());
        assertEquals(400, assertThrows(io.github.dsheirer.web.api.ApiException.class, () ->
            spectrum.parse(name -> "source".equals(name) ? "tuner:x" : "fftSize".equals(name) ? "1000" : null))
            .getStatus());
        assertEquals(400, assertThrows(io.github.dsheirer.web.api.ApiException.class, () ->
            spectrum.parse(name -> "source".equals(name) ? "channel:1" : null)).getStatus());
        assertEquals(503, assertThrows(io.github.dsheirer.web.api.ApiException.class, () ->
            spectrum.add(null, new Subscription(1, Topic.SPECTRUM, null), parameters)).getStatus());
    }

    @Test
    void callProgressIsPacedPerSubscription()
    {
        TestSink fast = new TestSink();
        LiveConnection fastConnection = mHub.connect("c1", principal(Role.VIEWER), fast);
        subscribe(fastConnection, 1, Topic.CALLS, new Subscription.Filter(null, false, null, null, 250));
        TestSink off = new TestSink();
        LiveConnection offConnection = mHub.connect("c2", principal(Role.VIEWER), off);
        subscribe(offConnection, 1, Topic.CALLS, new Subscription.Filter(null, false, null, null, 0));

        CallRecord call = call(70, 1201, true);
        mHub.callChanged(call);
        long t0 = call.getStartTime() + 1000;

        //First report starts the interval; reports in between are skipped
        mHub.callProgress(70, 900, 20, t0);
        mHub.callProgress(70, 980, 20, t0 + 100);
        mHub.callProgress(70, 1140, 0, t0 + 260);
        mHub.callProgress(70, 1180, 0, t0 + 300);
        mHub.callProgress(70, 1400, 300, t0 + 520);
        mHub.callProgress(99, 100, 0, t0 + 600);

        List<JsonNode> progress = fast.data().stream().map(message -> message.path("data"))
            .filter(data -> "call.progress".equals(data.path("type").asText())).toList();
        assertEquals(2, progress.size());
        assertEquals(1140, progress.get(0).get("audioMs").asLong());
        assertEquals(1260, progress.get(0).get("elapsedMs").asLong());
        assertEquals(1400, progress.get(1).get("audioMs").asLong());
        assertEquals(300, progress.get(1).get("lastAudioMs").asLong());
        assertEquals(70, progress.get(1).get("callKey").asLong());
        assertTrue(off.data().stream().noneMatch(message ->
            "call.progress".equals(message.path("data").path("type").asText())), "interval 0 turns it off");

        mHub.callChanged(call(70, 1201, false));
        JsonNode ended = fast.data().getLast().path("data");
        assertEquals("call.ended", ended.get("type").asText());
        assertEquals(1400, ended.get("audioMs").asLong(), "final audio length");
    }

    @Test
    void slowClientGetsTheLatestProgress()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.CALLS, new Subscription.Filter(null, false, null, null, 250));
        CallRecord call = call(80, 1, true);
        mHub.callChanged(call);
        long t0 = call.getStartTime();
        sink.mHold = true;
        mHub.callProgress(80, 0, 0, t0);
        mHub.callChanged(call(80, 1, true));
        int before = sink.mMessages.size();

        for(int x = 1; x <= 5; x++)
        {
            mHub.callProgress(80, x * 250, 0, t0 + x * 250);
        }

        sink.release();
        List<JsonNode> progress = sink.mMessages.subList(before, sink.mMessages.size()).stream()
            .map(message -> message.path("data")).filter(data -> "call.progress".equals(data.path("type").asText()))
            .toList();
        assertEquals(1, progress.size(), "waiting progress messages replace each other");
        assertEquals(1250, progress.getFirst().get("audioMs").asLong());
    }

    @Test
    void progressIntervalIsValidated()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.CALLS, new Subscription.Filter(null, false, null, null));
        mHub.onMessage(connection, "{\"op\":\"update\",\"sub\":1,\"params\":{\"progressIntervalMs\":100}}");
        assertEquals(400, sink.last().get("status").asInt());
        mHub.onMessage(connection, "{\"op\":\"update\",\"sub\":1,\"params\":{\"progressIntervalMs\":500}}");
        assertEquals("subscribed", sink.mMessages.get(sink.mMessages.size() - 2).get("op").asText());
        assertEquals(500, connection.getSubscriptions().get(1).getFilter().progressIntervalMs());
    }

    @Test
    void liveAudioReportsProgress()
    {
        TestSink sink = new TestSink();
        LiveConnection connection = mHub.connect("c1", principal(Role.VIEWER), sink);
        subscribe(connection, 1, Topic.CALLS, new Subscription.Filter(null, false, null, null, 250));
        LiveAudio audio = new LiveAudio(() -> mHub);

        io.github.dsheirer.audio.AudioSegment segment = new io.github.dsheirer.audio.AudioSegment(null, 0);
        segment.incrementConsumerCount();
        audio.receive(segment);
        segment.addAudio(new float[1600]);
        audio.process();
        CallRecord call = call(segment.getId(), 1, true);
        call.setStartTime(System.currentTimeMillis() - 1000);
        mHub.callChanged(call);
        audio.process();
        segment.addAudio(new float[800]);
        long deadline = System.currentTimeMillis() + 2000;

        while(System.currentTimeMillis() < deadline && sink.data().stream().noneMatch(message ->
            "call.progress".equals(message.path("data").path("type").asText())))
        {
            audio.process();
            Thread.onSpinWait();
        }

        JsonNode progress = sink.data().getLast().path("data");
        assertEquals("call.progress", progress.get("type").asText());
        assertEquals(300, progress.get("audioMs").asLong(), "2400 samples at 8 kHz");
        assertTrue(progress.get("elapsedMs").asLong() >= 1000);
    }

    private static CallRecord withId(CallRecord call, long id)
    {
        call.setId(id);
        return call;
    }

    private static ChannelStatusView status(int channelId, String state)
    {
        return new ChannelStatusView(String.valueOf(channelId), channelId, "Channel " + channelId, "STANDARD", "S",
            null, null, state, 851_012_500L, "P25_PHASE1", null, null, null);
    }

    /**
     * Subscribes without the source check, which needs the sdrtrunk components.
     */
    private Subscription subscribe(LiveConnection connection, int sub, Topic topic, Subscription.Filter filter)
    {
        Subscription subscription = new Subscription(sub, topic, filter);
        connection.getSubscriptions().put(sub, subscription);

        if(topic == Topic.CALLS)
        {
            mHub.sendSnapshotForTest(connection, subscription);
        }

        return subscription;
    }
}
