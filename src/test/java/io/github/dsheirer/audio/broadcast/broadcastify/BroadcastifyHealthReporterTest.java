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

package io.github.dsheirer.audio.broadcast.broadcastify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.health.ChannelHealth;
import io.github.dsheirer.health.HealthHistory;
import io.github.dsheirer.health.HealthSnapshot;
import io.github.dsheirer.health.NodeReporter;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BroadcastifyHealthReporterTest
{
    private static final long SINCE = 1_794_590_000_000L;

    private HttpServer mServer;
    private final List<JsonObject> mReceived = new ArrayList<>();
    private final AtomicInteger mStatus = new AtomicInteger(200);
    private final AtomicReference<String> mAnswer = new AtomicReference<>("{\"ok\":true,\"nextS\":300}");
    private final AtomicReference<String> mRetryAfter = new AtomicReference<>();

    @BeforeEach
    void serve() throws Exception
    {
        mServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mServer.createContext("/call-health", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized(mReceived)
            {
                mReceived.add(JsonParser.parseString(body).getAsJsonObject());
            }
            if(mRetryAfter.get() != null)
            {
                exchange.getResponseHeaders().add("Retry-After", mRetryAfter.get());
            }
            byte[] answer = mAnswer.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(mStatus.get(), answer.length);
            try(OutputStream out = exchange.getResponseBody())
            {
                out.write(answer);
            }
        });
        mServer.start();
    }

    @AfterEach
    void stopServing()
    {
        mServer.stop(0);
    }

    private BroadcastifyCallConfiguration configuration()
    {
        BroadcastifyCallConfiguration configuration = new BroadcastifyCallConfiguration();
        configuration.setName("Downtown Calls");
        configuration.setSystemID(1234);
        configuration.setApiKey("secret");
        configuration.setHost("http://127.0.0.1:" + mServer.getAddress().getPort() + "/call-upload");
        return configuration;
    }

    /**
     * Two aliases lists: one with an alias streaming to the node, one without.
     */
    private static AliasModel aliases()
    {
        AliasModel model = new AliasModel();
        Alias streams = new Alias("Fire Dispatch");
        streams.setAliasListName("Downtown");
        streams.addAliasID(new BroadcastChannel("Downtown Calls"));
        model.addAlias(streams);
        Alias elsewhere = new Alias("Water");
        elsewhere.setAliasListName("County");
        elsewhere.addAliasID(new BroadcastChannel("County Calls"));
        model.addAlias(elsewhere);
        return model;
    }

    /**
     * A site on the node's alias list and one on another, every 10 s for 20 minutes.
     */
    private static HealthHistory history()
    {
        ChannelHealth node = new ChannelHealth(new ChannelHealth.ChannelInfo(9001, "CC 1", "Metro", "Downtown",
            "Downtown", "p25"));
        ChannelHealth other = new ChannelHealth(new ChannelHealth.ChannelInfo(9002, "CC 2", "County", "North",
            "County", "p25"));
        node.started(SINCE);
        other.started(SINCE);
        node.setCallsCounted();
        other.setCallsCounted();
        HealthHistory history = new HealthHistory();

        for(long t = SINCE; t <= SINCE + 1_200_000; t += 10_000)
        {
            node.control("p25", true, t);
            other.control("p25", true, t);
            history.add(new HealthSnapshot(t, SINCE, List.of(node.totals(t), other.totals(t)), List.of()));
        }

        return history;
    }

    /**
     * The history carried on, a snapshot every 10 s, to a time.
     */
    private static HealthHistory later(HealthHistory history, long untilMs)
    {
        HealthSnapshot last = history.latest();
        ChannelHealth.Totals site = last.channel(9001);
        ChannelHealth.Totals other = last.channel(9002);

        for(long t = last.timeMs() + 10_000; t <= untilMs + 10_000; t += 10_000)
        {
            history.add(new HealthSnapshot(t, SINCE, List.of(grown(site, t), grown(other, t)), List.of()));
        }

        return history;
    }

    /**
     * A site's totals, running and locked until a time.
     */
    private static ChannelHealth.Totals grown(ChannelHealth.Totals totals, long t)
    {
        long ms = t - SINCE;
        return new ChannelHealth.Totals(totals.info(), totals.protocol(), totals.frequency(), totals.identity(),
            ms / 10_000, 0, ms, totals.calls(), ms, 0, 0, 0);
    }

    /**
     * A reporter that started with the recording (its schedule saw the first snapshot).
     */
    private BroadcastifyHealthReporter reporter(NodeReporter schedule)
    {
        schedule.isDue(new HealthSnapshot(SINCE, SINCE, List.of(), List.of()));
        return new BroadcastifyHealthReporter(configuration(), aliases(), HttpClient.newHttpClient(), schedule);
    }

    @Test
    void the_health_url_is_beside_the_upload_server()
    {
        assertEquals("https://api.broadcastify.com/call-health",
            BroadcastifyCallConfiguration.getHealthUrl("https://api.broadcastify.com/call-upload"));
        assertEquals("http://10.0.0.5:8080/call-health",
            BroadcastifyCallConfiguration.getHealthUrl("http://10.0.0.5:8080/call-upload/"));
        assertNull(BroadcastifyCallConfiguration.getHealthUrl("https://api.broadcastify.com/call-upload-dev"));
        assertNull(BroadcastifyCallConfiguration.getHealthUrl(null));
    }

    @Test
    void health_reports_are_on_by_default()
    {
        assertTrue(new BroadcastifyCallConfiguration().isHealthReports());
        assertTrue(((BroadcastifyCallConfiguration)configuration().copyOf()).isHealthReports());
    }

    @Test
    void a_node_reports_the_sites_whose_calls_can_stream_to_it()
    {
        BroadcastifyHealthReporter reporter = reporter(new NodeReporter(1));
        HealthHistory history = history();
        reporter.check(history);

        assertEquals(1, mReceived.size());
        JsonObject report = mReceived.getFirst();
        assertEquals(1234, report.getAsJsonObject("node").get("systemId").getAsInt());
        assertEquals("secret", report.getAsJsonObject("node").get("apiKey").getAsString());
        assertEquals(1, report.getAsJsonArray("sites").size(), "the node's site, not the other alias list's");
        assertEquals("Downtown", report.getAsJsonArray("sites").get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(0, report.getAsJsonObject("window").get("seq").getAsInt());
        assertTrue(report.getAsJsonObject("window").has("gap"), "20 minutes since the start: the longest window");
    }

    @Test
    void a_delivered_report_is_followed_by_the_next_window()
    {
        NodeReporter schedule = new NodeReporter(2);
        BroadcastifyHealthReporter reporter = reporter(schedule);
        HealthHistory history = history();
        reporter.check(history);
        assertEquals(1, mReceived.size());
        assertTrue(schedule.getNextAt() > history.latest().timeMs(), "not due again until the next interval");

        reporter.check(history);
        assertEquals(1, mReceived.size(), "nothing more until it's due");
    }

    @Test
    void a_server_that_doesnt_take_reports_is_tried_again_less_often()
    {
        for(int status: new int[]{404, 405, 410})
        {
            mStatus.set(status);
            mAnswer.set("<html>Not Found</html>");
            NodeReporter schedule = new NodeReporter(3);
            BroadcastifyHealthReporter reporter = reporter(schedule);
            long due = schedule.getNextAt();
            reporter.check(history());
            assertTrue(reporter.isToldNotTaken(), "HTTP " + status);
            assertFalse(schedule.isStopped(), "not stopped: HTTP " + status);
            long wait = schedule.getNextAt() - due;
            assertTrue(wait >= 2 * (NodeReporter.INTERVAL_MS - NodeReporter.JITTER_MS),
                "backs off as from a busy server: HTTP " + status + " waits " + wait);
        }
    }

    @Test
    void a_server_that_takes_reports_again_is_reported_to_as_before()
    {
        mStatus.set(404);
        mAnswer.set("<html>Not Found</html>");
        NodeReporter schedule = new NodeReporter(3);
        BroadcastifyHealthReporter reporter = reporter(schedule);
        HealthHistory history = history();
        reporter.check(history);
        reporter.check(history);
        assertEquals(1, mReceived.size(), "not again until the back off is over");

        //Later, it takes them.
        mStatus.set(200);
        mAnswer.set("{\"ok\":true}");
        HealthHistory later = later(history, schedule.getNextAt());
        reporter.check(later);
        assertEquals(2, mReceived.size());
        assertFalse(reporter.isToldNotTaken(), "said again if it stops taking them");
        assertTrue(mReceived.get(1).getAsJsonObject("window").has("gap"), "the window it couldn't send is too long");
    }

    @Test
    void a_page_that_isnt_a_report_answer_counts_as_not_taking_reports()
    {
        mAnswer.set("<html>ok</html>");
        NodeReporter schedule = new NodeReporter(4);
        BroadcastifyHealthReporter reporter = reporter(schedule);
        long due = schedule.getNextAt();
        reporter.check(history());
        assertTrue(reporter.isToldNotTaken());
        assertTrue(schedule.getNextAt() - due >= 2 * (NodeReporter.INTERVAL_MS - NodeReporter.JITTER_MS));
    }

    @Test
    void a_report_turned_down_as_wrong_is_counted_as_sent()
    {
        mStatus.set(400);
        mAnswer.set("{\"error\":\"window.end must be after window.start\"}");
        NodeReporter schedule = new NodeReporter(8);
        BroadcastifyHealthReporter reporter = reporter(schedule);
        HealthHistory history = history();
        reporter.check(history);
        assertEquals(1, schedule.window(later(history, history.latest().timeMs() + 60_000)).seq(), "counted as sent");
    }

    @Test
    void another_answer_leaves_the_window_to_the_next_report()
    {
        mStatus.set(422);
        mAnswer.set("{\"error\":\"?\"}");
        NodeReporter schedule = new NodeReporter(8);
        BroadcastifyHealthReporter reporter = reporter(schedule);
        HealthHistory history = history();
        reporter.check(history);
        NodeReporter.Window next = schedule.window(history);
        assertEquals(0, next.seq(), "not delivered");
        assertEquals(mReceived.getFirst().getAsJsonObject("window").get("start").getAsBigDecimal(),
            java.math.BigDecimal.valueOf(next.startMs(), 3), "the next report carries the window");
    }

    @Test
    void refused_credentials_stop_the_node()
    {
        mStatus.set(401);
        mAnswer.set("{\"error\":\"Invalid-API-Key\"}");
        NodeReporter schedule = new NodeReporter(5);
        reporter(schedule).check(history());
        assertTrue(schedule.isStopped());
    }

    @Test
    void a_paused_service_is_tried_again_after_retry_after()
    {
        mStatus.set(503);
        mRetryAfter.set("3600");
        mAnswer.set("{\"error\":\"not taking node health reports now\"}");
        NodeReporter schedule = new NodeReporter(6);
        HealthHistory history = history();
        BroadcastifyHealthReporter reporter = reporter(schedule);
        reporter.check(history);
        assertFalse(schedule.isStopped());
        assertFalse(reporter.isToldNotTaken());
        assertEquals(history.latest().timeMs() + 3_600_000, schedule.getNextAt());
    }
}
