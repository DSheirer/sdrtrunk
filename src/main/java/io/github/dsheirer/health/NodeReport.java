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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Makes a node health report (version 1): what one node's sites, conventional channels and tuners did over a window,
 * as counts and seconds.  The format is the one Trunk Recorder Pro, SDRTrunk and SDRTrunk VCE share
 * (https://github.com/TrunkRecorder/trunk-recorder-pro/blob/main/docs/api/node-health.md).  A field the recorder
 * doesn't measure is left out, never sent as 0.
 */
public class NodeReport
{
    public static final int VERSION = 1;
    public static final String RECORDER = "sdrtrunk";

    private NodeReport()
    {
    }

    /**
     * The report of a window, or null when nothing feeding the node ran in it.
     * @param window of the report
     * @param node the service's fields that say which node (for Broadcastify Calls: systemId and apiKey)
     * @param version of this recorder
     * @param feeds which channels feed the node
     */
    public static JsonObject build(NodeReporter.Window window, JsonObject node, String version,
                                   Predicate<ChannelHealth.ChannelInfo> feeds)
    {
        HealthSnapshot now = window.now();
        HealthSnapshot base = window.base();
        JsonArray sites = new JsonArray();
        List<ChannelHealth.ChannelInfo> siteChannels = new ArrayList<>();
        JsonArray channels = new JsonArray();

        for(ChannelHealth.Totals a: now.channels())
        {
            if(!feeds.test(a.info()))
            {
                continue;
            }

            ChannelHealth.Totals b = base != null ? base.channel(a.info().channelId()) : null;

            if(a.runningMs() - (b != null ? b.runningMs() : 0) <= 0)
            {
                continue; //Not running in the window
            }

            //A site is a channel that decodes control messages and whose calls are counted.  One whose calls nothing
            //counts (a DMR or NXDN channel that isn't trunked) is reported as the conventional channel it is, never as a
            //site with no calls.
            if(a.isSite() && a.calls() != null)
            {
                sites.add(site(a, b));
                siteChannels.add(a.info());
            }
            else if(a.info().mode() != null && a.frequency() > 0)
            {
                channels.add(channel(a, b));
            }
        }

        if(sites.isEmpty() && channels.isEmpty())
        {
            return null;
        }

        nameReceivers(sites, siteChannels);

        JsonObject report = new JsonObject();
        report.addProperty("v", VERSION);
        report.add("node", node);

        JsonObject recorder = new JsonObject();
        recorder.addProperty("name", RECORDER);
        recorder.addProperty("version", version != null ? version : "unknown");
        recorder.addProperty("upS", r1((now.timeMs() - now.sinceMs()) / 1000.0));
        report.add("recorder", recorder);

        JsonObject w = new JsonObject();
        w.addProperty("start", seconds(window.startMs()));
        w.addProperty("end", seconds(now.timeMs()));
        w.addProperty("seq", window.seq());

        if(window.gap())
        {
            w.addProperty("gap", true);
        }

        report.add("window", w);

        if(!sites.isEmpty())
        {
            report.add("sites", sites);
        }

        if(!channels.isEmpty())
        {
            report.add("channels", channels);
        }

        JsonArray radios = radios(base, now);

        if(!radios.isEmpty())
        {
            report.add("radios", radios);
        }

        return report;
    }

    /**
     * A site heard by more than one of the node's channels (receivers) has an entry for each: each says which
     * receiver it is, by its channel's name.  A site heard once doesn't.
     */
    private static void nameReceivers(JsonArray sites, List<ChannelHealth.ChannelInfo> channels)
    {
        Map<String,Integer> heard = new HashMap<>();

        for(int i = 0; i < sites.size(); i++)
        {
            heard.merge(sameSite(sites.get(i).getAsJsonObject()), 1, Integer::sum);
        }

        for(int i = 0; i < sites.size(); i++)
        {
            JsonObject site = sites.get(i).getAsJsonObject();

            if(heard.get(sameSite(site)) > 1)
            {
                ChannelHealth.ChannelInfo info = channels.get(i);
                site.addProperty("receiver", info.name() != null && !info.name().isBlank() ? info.name() :
                    "channel " + info.channelId());
            }
        }
    }

    /**
     * The different sites among a report's site entries (a site heard by several receivers is one), in the order
     * they're first listed: each one's sameSite key and name.  An entry whose control channel hasn't said who it is
     * yet is the site of another entry with its name and protocol, when there is one.
     */
    public static Map<String,String> distinctSites(JsonArray sites)
    {
        Map<String,String> distinct = new LinkedHashMap<>();
        Set<String> named = new HashSet<>();

        for(int i = 0; i < sites.size(); i++)
        {
            JsonObject site = sites.get(i).getAsJsonObject();
            JsonObject id = site.getAsJsonObject("id");

            if(id != null && !id.isEmpty())
            {
                named.add(site.get("protocol").getAsString() + "/" + site.get("name").getAsString());
            }
        }

        for(int i = 0; i < sites.size(); i++)
        {
            JsonObject site = sites.get(i).getAsJsonObject();
            JsonObject id = site.getAsJsonObject("id");
            String name = site.get("name").getAsString();

            if((id == null || id.isEmpty()) && named.contains(site.get("protocol").getAsString() + "/" + name))
            {
                continue;
            }

            distinct.putIfAbsent(sameSite(site), name);
        }

        //Two sites with one name are told apart by what they say they are.
        Map<String,Long> names = new HashMap<>();
        distinct.values().forEach(name -> names.merge(name, 1L, Long::sum));

        for(Map.Entry<String,String> entry: distinct.entrySet())
        {
            if(names.get(entry.getValue()) > 1)
            {
                entry.setValue(entry.getValue() + " " + entry.getKey().substring(entry.getKey().indexOf('/') + 1));
            }
        }

        return distinct;
    }

    /**
     * What makes two entries the same site: its protocol and the identity its control channel gives, or without one,
     * the name the user gave it.
     */
    public static String sameSite(JsonObject site)
    {
        JsonObject id = site.getAsJsonObject("id");
        return site.get("protocol").getAsString() + "/" +
            (id != null && !id.isEmpty() ? id.toString() : "name:" + site.get("name").getAsString());
    }

    private static JsonObject site(ChannelHealth.Totals a, ChannelHealth.Totals b)
    {
        JsonObject site = new JsonObject();
        site.addProperty("name", name(a.info()));
        site.addProperty("protocol", a.protocol());

        if(!a.identity().isEmpty())
        {
            JsonObject id = new JsonObject();

            for(Map.Entry<String,Integer> entry: a.identity().entrySet())
            {
                id.addProperty(entry.getKey(), entry.getValue());
            }

            site.add("id", id);
        }

        JsonObject cc = new JsonObject();

        if(a.frequency() > 0)
        {
            cc.addProperty("hz", a.frequency());
        }

        cc.addProperty("lockedS", r1((a.lockedMs() - (b != null ? b.lockedMs() : 0)) / 1000.0));
        cc.addProperty("good", a.good() - (b != null ? b.good() : 0));
        cc.addProperty("bad", a.bad() - (b != null ? b.bad() : 0));
        site.add("cc", cc);

        CallTally.Totals callsA = a.calls();
        CallTally.Totals callsB = b != null && b.calls() != null ? b.calls() : CallTally.Totals.EMPTY;
        JsonObject calls = new JsonObject();
        calls.addProperty("started", callsA.started() - callsB.started());
        calls.addProperty("recorded", callsA.get(CallOutcome.RECORDED) - callsB.get(CallOutcome.RECORDED));
        JsonObject notRecorded = new JsonObject();

        for(CallOutcome outcome: CallOutcome.values())
        {
            long count = callsA.get(outcome) - callsB.get(outcome);

            if(outcome != CallOutcome.RECORDED && count > 0)
            {
                notRecorded.addProperty(outcome.getReportName(), count);
            }
        }

        if(!notRecorded.isEmpty())
        {
            calls.add("notRecorded", notRecorded);
        }

        site.add("calls", calls);
        return site;
    }

    private static JsonObject channel(ChannelHealth.Totals a, ChannelHealth.Totals b)
    {
        JsonObject channel = new JsonObject();
        channel.addProperty("hz", a.frequency());
        channel.addProperty("mode", a.info().mode());
        channel.addProperty("name", name(a.info()));
        channel.addProperty("heardS", r1((a.runningMs() - (b != null ? b.runningMs() : 0)) / 1000.0));
        channel.addProperty("opens", a.opens() - (b != null ? b.opens() : 0));
        channel.addProperty("openS", r1((a.openMs() - (b != null ? b.openMs() : 0)) / 1000.0));
        channel.addProperty("calls", a.closes() - (b != null ? b.closes() : 0));
        return channel;
    }

    private static JsonArray radios(HealthSnapshot base, HealthSnapshot now)
    {
        JsonArray radios = new JsonArray();

        for(HealthSnapshot.RadioTotals a: now.radios())
        {
            HealthSnapshot.RadioTotals b = base != null ? base.radio(a.name()) : null;
            double seconds = a.seconds() - (b != null ? b.seconds() : 0);

            if(seconds <= 0)
            {
                continue;
            }

            JsonObject radio = new JsonObject();
            radio.addProperty("name", a.name());
            radio.addProperty("kind", a.kind());
            radio.addProperty("seconds", r1(seconds));

            if(a.ppmError() != null)
            {
                radio.addProperty("ppmError", a.ppmError());
            }

            radios.add(radio);
        }

        return radios;
    }

    /**
     * The name a report gives a channel: the user's site name when there is one, else the channel's name.
     */
    private static String name(ChannelHealth.ChannelInfo info)
    {
        if(info.site() != null && !info.site().isBlank())
        {
            return info.site();
        }

        return info.name() != null ? info.name() : "";
    }

    /**
     * Unix milliseconds as seconds, written plainly (1794590200.437, not 1.794590200437E9).
     */
    private static BigDecimal seconds(long ms)
    {
        return BigDecimal.valueOf(ms, 3);
    }

    private static double r1(double value)
    {
        return Math.round(value * 10.0) / 10.0;
    }
}
