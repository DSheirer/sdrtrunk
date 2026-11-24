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

package io.github.dsheirer.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NodeReportTest
{
    private static final long SINCE = 1_794_590_000_000L;

    /**
     * Totals of a P25 site and an FM channel as they would be measured, at two moments.
     */
    private static HealthSnapshot[] twoSnapshots()
    {
        ChannelHealth site = new ChannelHealth(ChannelHealthTest.info(101, "p25"));
        ChannelHealth fm = new ChannelHealth(ChannelHealthTest.info(102, "fm"));
        ChannelHealth other = new ChannelHealth(ChannelHealthTest.info(103, "fm")); //Doesn't feed the node
        site.started(SINCE);
        fm.started(SINCE);
        other.started(SINCE);
        site.setFrequency(854_787_500);
        fm.setFrequency(155_520_000);
        site.setCallsCounted();
        site.identity("wacn", 781824);
        site.identity("sysId", 939);
        site.identity("rfss", 1);
        site.identity("site", 5);
        site.identity("nac", 928);

        long t = SINCE + 100_000;

        for(int i = 0; i < 1000; i++)
        {
            site.control("p25", i % 50 != 0, t + i * 100L);
        }

        site.getCalls().count(null, CallOutcome.RECORDED);
        HealthSnapshot base = snapshot(SINCE + 200_000, site, fm, other);

        for(int i = 0; i < 3000; i++)
        {
            site.control("p25", i % 25 != 0, SINCE + 200_000 + i * 100L);
        }

        for(int i = 0; i < 40; i++)
        {
            site.getCalls().count(null, CallOutcome.RECORDED);
        }

        site.getCalls().count(null, CallOutcome.NO_RECORDER);
        site.getCalls().count(null, CallOutcome.ENCRYPTED);
        fm.opened(SINCE + 250_000);
        fm.closed(SINCE + 262_500);
        HealthSnapshot now = snapshot(SINCE + 500_000, site, fm, other);
        return new HealthSnapshot[]{base, now};
    }

    private static HealthSnapshot snapshot(long time, ChannelHealth... channels)
    {
        List<ChannelHealth.Totals> totals = java.util.Arrays.stream(channels).map(c -> c.totals(time)).toList();
        return new HealthSnapshot(time, SINCE, totals,
            List.of(new HealthSnapshot.RadioTotals("Airspy 1234", "airspy", (time - SINCE) / 1000.0, -0.03)));
    }

    private static JsonObject node()
    {
        JsonObject node = new JsonObject();
        node.addProperty("systemId", 1234);
        node.addProperty("apiKey", "key");
        return node;
    }

    @Test
    void a_report_is_what_changed_for_the_nodes_channels_alone()
    {
        HealthSnapshot[] s = twoSnapshots();
        JsonObject report = NodeReport.build(new NodeReporter.Window(s[0], s[1], false, 7), node(), "1.0.0",
            info -> info.channelId() == 101 || info.channelId() == 102);

        assertEquals(1, report.get("v").getAsInt());
        assertEquals("sdrtrunk", report.getAsJsonObject("recorder").get("name").getAsString());
        assertEquals("1.0.0", report.getAsJsonObject("recorder").get("version").getAsString());
        JsonObject window = report.getAsJsonObject("window");
        assertEquals((SINCE + 200_000) / 1000.0, window.get("start").getAsDouble());
        assertEquals((SINCE + 500_000) / 1000.0, window.get("end").getAsDouble());
        assertEquals("1794590500.000", window.get("end").toString(), "written plainly, to the millisecond");
        assertEquals(7, window.get("seq").getAsInt());
        assertFalse(window.has("gap"));

        JsonObject site = report.getAsJsonArray("sites").get(0).getAsJsonObject();
        assertEquals("Site 101", site.get("name").getAsString());
        assertEquals("p25", site.get("protocol").getAsString());
        assertEquals(781824, site.getAsJsonObject("id").get("wacn").getAsInt());
        JsonObject cc = site.getAsJsonObject("cc");
        assertEquals(854_787_500, cc.get("hz").getAsLong());
        assertEquals(2880, cc.get("good").getAsLong(), "the window's messages only");
        assertEquals(120, cc.get("bad").getAsLong());
        assertEquals(300.0, cc.get("lockedS").getAsDouble(), 0.2);
        JsonObject calls = site.getAsJsonObject("calls");
        assertEquals(42, calls.get("started").getAsLong());
        assertEquals(40, calls.get("recorded").getAsLong());
        assertEquals(1, calls.getAsJsonObject("notRecorded").get("noRecorder").getAsLong());
        assertEquals(1, calls.getAsJsonObject("notRecorded").get("encrypted").getAsLong());
        assertFalse(calls.getAsJsonObject("notRecorded").has("noSource"), "a reason with none is left out");

        assertEquals(1, report.getAsJsonArray("channels").size(), "only the channels that feed the node");
        JsonObject fm = report.getAsJsonArray("channels").get(0).getAsJsonObject();
        assertEquals(155_520_000, fm.get("hz").getAsLong());
        assertEquals("fm", fm.get("mode").getAsString());
        assertEquals(300.0, fm.get("heardS").getAsDouble());
        assertEquals(1, fm.get("opens").getAsLong());
        assertEquals(12.5, fm.get("openS").getAsDouble());
        assertEquals(1, fm.get("calls").getAsLong());
        assertFalse(fm.has("noiseDb"), "not measured: left out, never 0");

        JsonObject radio = report.getAsJsonArray("radios").get(0).getAsJsonObject();
        assertEquals("airspy", radio.get("kind").getAsString());
        assertEquals(300.0, radio.get("seconds").getAsDouble());
        assertEquals(-0.03, radio.get("ppmError").getAsDouble());
        assertFalse(radio.has("droppedS"));

        assertRequiredFields(report);
    }

    @Test
    void a_channel_whose_calls_nothing_counts_is_never_a_site_with_none()
    {
        ChannelHealth dmr = new ChannelHealth(ChannelHealthTest.info(104, "dmr"));
        dmr.started(SINCE);

        for(int i = 0; i < 100; i++)
        {
            dmr.control("dmr", true, SINCE + i * 100L);
        }

        HealthSnapshot now = snapshot(SINCE + 300_000, dmr);
        assertNull(NodeReport.build(new NodeReporter.Window(null, now, false, 0), node(), "1.0.0", info -> true));

        //With a frequency, it's the conventional channel it is.
        dmr.setFrequency(453_100_000);
        now = snapshot(SINCE + 300_000, dmr);
        JsonObject conventional = NodeReport.build(new NodeReporter.Window(null, now, false, 0), node(), "1.0.0",
            info -> true);
        assertFalse(conventional.has("sites"));
        assertEquals("dmr", conventional.getAsJsonArray("channels").get(0).getAsJsonObject().get("mode").getAsString());

        dmr.setCallsCounted();
        now = snapshot(SINCE + 300_000, dmr);
        assertEquals(1, NodeReport.build(new NodeReporter.Window(null, now, false, 0), node(), "1.0.0", info -> true)
            .getAsJsonArray("sites").size());
    }

    @Test
    void a_site_two_receivers_hear_has_an_entry_for_each_saying_which()
    {
        ChannelHealth rooftop = receiver(201, "Rooftop", 5);
        ChannelHealth indoor = receiver(202, "Indoor", 5);
        ChannelHealth other = receiver(203, "Other site", 6);
        HealthSnapshot now = snapshot(SINCE + 300_000, rooftop, indoor, other);
        JsonObject report = NodeReport.build(new NodeReporter.Window(null, now, false, 0), node(), "1.0.0",
            info -> true);

        java.util.Map<String,JsonObject> byReceiver = new java.util.HashMap<>();
        JsonObject single = null;

        for(var e: report.getAsJsonArray("sites"))
        {
            JsonObject site = e.getAsJsonObject();

            if(site.has("receiver"))
            {
                byReceiver.put(site.get("receiver").getAsString(), site);
            }
            else
            {
                single = site;
            }
        }

        assertEquals(java.util.Set.of("Rooftop", "Indoor"), byReceiver.keySet(), "the same site, twice");
        assertEquals(byReceiver.get("Rooftop").get("id"), byReceiver.get("Indoor").get("id"));
        assertEquals(6, single.getAsJsonObject("id").get("site").getAsInt(), "a site heard once says no receiver");
    }

    /**
     * A channel decoding a P25 site's control channel.
     */
    private static ChannelHealth receiver(int channelId, String name, int site)
    {
        ChannelHealth health = new ChannelHealth(ChannelHealthTest.info(channelId, "p25"));
        ChannelHealth named = new ChannelHealth(new ChannelHealth.ChannelInfo(channelId, name, health.getInfo().system(),
            "Downtown", health.getInfo().aliasListName(), "p25"));
        named.started(SINCE);
        named.setCallsCounted();
        named.identity("wacn", 781824);
        named.identity("sysId", 939);
        named.identity("site", site);
        named.control("p25", true, SINCE + 1000);
        return named;
    }

    @Test
    void no_report_when_nothing_feeds_the_node()
    {
        HealthSnapshot[] s = twoSnapshots();
        assertNull(NodeReport.build(new NodeReporter.Window(s[0], s[1], false, 0), node(), "1.0.0", info -> false));
    }

    @Test
    void the_first_window_starts_at_the_start()
    {
        HealthSnapshot[] s = twoSnapshots();
        JsonObject report = NodeReport.build(new NodeReporter.Window(null, s[1], false, 0), node(), "1.0.0",
            info -> info.channelId() == 101);
        assertEquals(SINCE / 1000.0, report.getAsJsonObject("window").get("start").getAsDouble());
        assertEquals(4000, report.getAsJsonArray("sites").get(0).getAsJsonObject().getAsJsonObject("cc")
            .get("good").getAsLong() + report.getAsJsonArray("sites").get(0).getAsJsonObject().getAsJsonObject("cc")
            .get("bad").getAsLong());
        assertRequiredFields(report);
    }

    @Test
    void reports_from_the_scheduler_follow_on()
    {
        HealthSnapshot[] s = twoSnapshots();
        HealthHistory history = new HealthHistory();
        history.add(s[0]);
        history.add(s[1]);
        NodeReporter reporter = new NodeReporter(1);
        assertTrue(reporter.isDue(s[1]));
        NodeReporter.Window window = reporter.window(history);
        assertNull(window.base(), "first: from the start");
        reporter.delivered(null);
        assertEquals(1, reporter.window(history).seq());
    }

    /**
     * What every report must have (node-health.md "Core", and what Broadcastify Calls' call-health checks).
     */
    static void assertRequiredFields(JsonObject report)
    {
        assertEquals(1, report.get("v").getAsInt());
        assertTrue(report.getAsJsonObject("node").has("systemId"));
        assertFalse(report.getAsJsonObject("recorder").get("name").getAsString().isEmpty());
        assertTrue(report.getAsJsonObject("recorder").has("version"));
        JsonObject window = report.getAsJsonObject("window");
        assertTrue(window.get("end").getAsDouble() > window.get("start").getAsDouble());
        assertTrue(window.get("seq").getAsLong() >= 0);
        assertTrue(report.has("sites") || report.has("channels"));

        if(report.has("sites"))
        {
            report.getAsJsonArray("sites").forEach(e -> {
                JsonObject site = e.getAsJsonObject();
                assertFalse(site.get("name").getAsString().isEmpty());
                assertFalse(site.get("protocol").getAsString().isEmpty());
                assertTrue(site.getAsJsonObject("cc").has("lockedS"));
                assertTrue(site.getAsJsonObject("cc").has("good"));
                assertTrue(site.getAsJsonObject("calls").has("started"));
                assertTrue(site.getAsJsonObject("calls").has("recorded"));
            });
        }

        if(report.has("channels"))
        {
            report.getAsJsonArray("channels").forEach(e -> {
                JsonObject channel = e.getAsJsonObject();
                for(String field: new String[]{"hz", "mode", "heardS", "opens", "openS", "calls"})
                {
                    assertTrue(channel.has(field), field);
                }
            });
        }
    }
}
