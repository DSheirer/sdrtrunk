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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.health.ChannelHealth;
import io.github.dsheirer.health.HealthHistory;
import io.github.dsheirer.health.HealthRegistry;
import io.github.dsheirer.health.HealthSnapshot;
import io.github.dsheirer.health.NodeReport;
import io.github.dsheirer.health.NodeReporter;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends a Broadcastify Calls node (a system ID) its node health reports: every 5 minutes, at a random point of the
 * node's own, how well this recorder hears the sites and channels whose calls go to the node.  Broadcastify uses
 * them to choose between nodes that upload the same system, and to tell a node's owner about outages and problems.
 *
 * One per system ID and server, shared by every Broadcastify Calls configuration that names that system ID: the
 * node's report is one, with every site and channel that has an alias streaming to any of those configurations.  The
 * report covers each whole (all its calls, not only those streamed here).  The reporter lasts while any of the
 * configurations' broadcasters runs, so one restarting while another runs keeps the node's schedule and numbering.
 */
public class BroadcastifyHealthReporter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastifyHealthReporter.class);
    /** How often the node looks whether its report is due: it goes within a second of when it is. */
    private static final long CHECK_INTERVAL_MS = 1_000;
    private static final Map<String,BroadcastifyHealthReporter> REPORTERS = new HashMap<>();
    /** One HTTP client for every reporter, made at the first: a stream that restarts doesn't make another. */
    private static HttpClient sHttpClient;

    private final String mKey;
    private final int mSystemId;
    private final String mUrl;
    private final AliasModel mAliasModel;
    private final HttpClient mHttpClient;
    private final NodeReporter mReporter;
    private final List<BroadcastifyCallConfiguration> mConfigurations = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> mFuture;
    /** Stopped: a check already under way sends nothing (a new reporter may have started for the node). */
    private volatile boolean mStopped;
    private volatile boolean mResume;
    private boolean mToldNotTaken;
    private boolean mToldUnreachable;
    private boolean mToldKeys;
    private String mLastMessage;
    private Set<String> mWarnedSites = Set.of();

    /**
     * For tests: a node of one configuration, with a given schedule.
     */
    BroadcastifyHealthReporter(BroadcastifyCallConfiguration configuration, AliasModel aliasModel,
                               HttpClient httpClient, NodeReporter reporter)
    {
        this(configuration.getSystemID(), BroadcastifyCallConfiguration.getHealthUrl(configuration.getHost()),
            aliasModel, httpClient, reporter);
        mConfigurations.add(configuration);
    }

    private BroadcastifyHealthReporter(int systemId, String url, AliasModel aliasModel, HttpClient httpClient,
                                       NodeReporter reporter)
    {
        mSystemId = systemId;
        mUrl = url;
        mKey = key(url, systemId);
        mAliasModel = aliasModel;
        mHttpClient = httpClient;
        mReporter = reporter;
    }

    private static String key(String url, int systemId)
    {
        return url + "#" + systemId;
    }

    /**
     * The reporter of a configuration's node, started: the one its system ID already has on its server, or a new one.
     * Release it when the configuration's broadcaster stops.
     * @param configuration of a broadcaster starting
     * @param aliasModel to find the node's channels
     * @return the reporter, or null when the configuration's upload server has nowhere to send reports
     */
    public static BroadcastifyHealthReporter acquire(BroadcastifyCallConfiguration configuration, AliasModel aliasModel)
    {
        return acquire(configuration, aliasModel, null, true);
    }

    /**
     * As acquire, with an HTTP client (null for its own), and whether to start checking for reports due.
     */
    static synchronized BroadcastifyHealthReporter acquire(BroadcastifyCallConfiguration configuration,
                                                           AliasModel aliasModel, HttpClient httpClient,
                                                           boolean start)
    {
        String url = BroadcastifyCallConfiguration.getHealthUrl(configuration.getHost());

        if(url == null)
        {
            LOGGER.info("Broadcastify Calls system [" + configuration.getSystemID() + "] - not sending health " +
                "reports: " + (BroadcastifyCallConfiguration.DEVELOPMENT_ENDPOINT.equals(configuration.getHost()) ?
                "Broadcastify's development server [" + configuration.getHost() + "] has no health endpoint" :
                "the upload server [" + configuration.getHost() + "] doesn't end in call-upload"));
            return null;
        }

        int systemId = configuration.getSystemID();
        BroadcastifyHealthReporter reporter = REPORTERS.get(key(url, systemId));

        if(reporter == null)
        {
            reporter = new BroadcastifyHealthReporter(systemId, url, aliasModel, httpClient != null ? httpClient :
                httpClient(), new NodeReporter(String.valueOf(systemId)));
            REPORTERS.put(reporter.mKey, reporter);
        }

        reporter.add(configuration);

        if(start)
        {
            reporter.start();
        }

        return reporter;
    }

    private static synchronized HttpClient httpClient()
    {
        if(sHttpClient == null)
        {
            sHttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20)).build();
        }

        return sHttpClient;
    }

    /**
     * A configuration's broadcaster stopped: the reporter stops when no other configuration of its node runs.
     */
    public static synchronized void release(BroadcastifyHealthReporter reporter,
                                            BroadcastifyCallConfiguration configuration)
    {
        reporter.mConfigurations.removeIf(c -> c == configuration);

        if(reporter.mConfigurations.isEmpty())
        {
            reporter.stop();
            REPORTERS.remove(reporter.mKey, reporter);
        }
    }

    /**
     * Another configuration of the node.  Its settings are new (a broadcaster starts when they change): reports
     * stopped by refused credentials start again.
     */
    private void add(BroadcastifyCallConfiguration configuration)
    {
        if(!mConfigurations.isEmpty())
        {
            BroadcastifyCallConfiguration first = mConfigurations.getFirst();

            if(first != configuration && !Objects.equals(first.getName(), configuration.getName()))
            {
                LOGGER.info("Broadcastify Calls streams [" + first.getName() + "] and [" + configuration.getName() +
                    "] both upload as system [" + mSystemId + "]: one health report covers both");
            }

            if(!mToldKeys && !Objects.equals(first.getApiKey(), configuration.getApiKey()))
            {
                mToldKeys = true;
                LOGGER.warn("Broadcastify Calls streams [" + first.getName() + "] and [" + configuration.getName() +
                    "] upload as system [" + mSystemId + "] with different API keys - health reports use the " +
                    "first's");
            }
        }

        if(mConfigurations.stream().noneMatch(c -> c == configuration))
        {
            mConfigurations.add(configuration);
        }

        mResume = true;
    }

    /**
     * The configurations reporting to the node, the first's API key used.
     */
    List<BroadcastifyCallConfiguration> getConfigurations()
    {
        return List.copyOf(mConfigurations);
    }

    /**
     * The node's schedule.
     */
    NodeReporter getSchedule()
    {
        return mReporter;
    }

    /**
     * True while the server says it doesn't take reports: said once, and reports back off until it takes one.
     */
    boolean isToldNotTaken()
    {
        return mToldNotTaken;
    }

    /**
     * Starts checking, every second, whether the node's next report is due.
     */
    private synchronized void start()
    {
        if(mFuture != null || mUrl == null)
        {
            return;
        }

        HealthRegistry.getInstance().getHistory(); //Starts the snapshots
        mFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::check, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS);
    }

    private synchronized void stop()
    {
        mStopped = true;

        if(mFuture != null)
        {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    /**
     * True when a channel's calls can stream to this node: its alias list has an alias with the broadcast channel of
     * one of the node's configurations.
     */
    boolean feeds(ChannelHealth.ChannelInfo info)
    {
        return feeds(info, streams());
    }

    private Set<String> streams()
    {
        Set<String> streams = new HashSet<>();

        for(BroadcastifyCallConfiguration configuration: mConfigurations)
        {
            if(configuration.getName() != null && !configuration.getName().isEmpty())
            {
                streams.add(configuration.getName());
            }
        }

        return streams;
    }

    private boolean feeds(ChannelHealth.ChannelInfo info, Set<String> streams)
    {
        return feeds(info, feedingAliasLists(streams));
    }

    private static boolean feeds(ChannelHealth.ChannelInfo info, FeedingAliasLists lists)
    {
        String aliasList = info.aliasListName();
        return aliasList != null && !aliasList.isEmpty() && lists.names().contains(aliasList);
    }

    /** The alias lists that fed the node when last worked out, for when they can't be now. */
    private volatile FeedingAliasLists mLastFeeding = new FeedingAliasLists(Set.of(), Set.of());

    private record FeedingAliasLists(Set<String> streams, Set<String> names)
    {
    }

    /**
     * The alias lists with an alias that streams to one of the node's streams.  The aliases are the user's, edited on
     * the JavaFX thread while this reads them on the scheduler's: an edit at the same moment is a
     * ConcurrentModificationException, so it's read again, and failing that the last lists worked out for the same
     * streams are used, so the report still goes.
     */
    private FeedingAliasLists feedingAliasLists(Set<String> streams)
    {
        if(streams.isEmpty())
        {
            return new FeedingAliasLists(streams, Set.of());
        }

        RuntimeException last = null;

        for(int attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                Set<String> names = new HashSet<>();

                for(Alias alias: List.copyOf(mAliasModel.getAliases()))
                {
                    String list = alias.getAliasListName();

                    if(list != null && !names.contains(list) && streams.stream().anyMatch(alias::hasBroadcastChannel))
                    {
                        names.add(list);
                    }
                }

                FeedingAliasLists lists = new FeedingAliasLists(Set.copyOf(streams), Set.copyOf(names));
                mLastFeeding = lists;
                return lists;
            }
            catch(ConcurrentModificationException | IndexOutOfBoundsException | NullPointerException e)
            {
                last = e;
            }
        }

        FeedingAliasLists previous = mLastFeeding;

        if(previous.streams().equals(streams))
        {
            LOGGER.debug("Aliases changing while a health report was made - using the alias lists found before", last);
            return previous;
        }

        throw last;
    }

    private void check()
    {
        HealthRegistry registry = HealthRegistry.getInstance();
        HealthHistory history = registry.getHistory();
        NodeReporter reporter = mReporter;
        Long nextAt = reporter.getNextAt();
        HealthSnapshot latest = history.latest();

        //Due between snapshots (they're 10 s apart): the window ends now, at one taken for it, not at the next.
        if(nextAt != null && latest != null && latest.timeMs() < nextAt && System.currentTimeMillis() >= nextAt &&
            !reporter.isStopped())
        {
            registry.snapshotNow();
        }

        check(history);
    }

    /**
     * Sends the node's report if one is due, from these snapshots.
     */
    void check(HealthHistory history)
    {
        try
        {
            if(mResume)
            {
                mResume = false;
                mReporter.resume();
            }

            List<BroadcastifyCallConfiguration> configurations = getConfigurations();

            if(configurations.isEmpty() || mReporter.isStopped() || mUrl == null)
            {
                return;
            }

            HealthSnapshot now = history.latest();

            if(!mReporter.isDue(now))
            {
                return;
            }

            NodeReporter.Window window = mReporter.window(history);

            if(window == null)
            {
                return;
            }

            //Which channels feed the node: worked out once per report, since the alias lists can change.
            FeedingAliasLists lists = feedingAliasLists(streams());
            Set<Integer> feeding = new HashSet<>();

            for(ChannelHealth.Totals totals: window.now().channels())
            {
                if(feeds(totals.info(), lists))
                {
                    feeding.add(totals.info().channelId());
                }
            }

            JsonObject node = new JsonObject();
            node.addProperty("systemId", mSystemId);
            node.addProperty("apiKey", configurations.getFirst().getApiKey());
            JsonObject report = NodeReport.build(window, node, SystemProperties.getInstance().getApplicationVersion(),
                info -> feeding.contains(info.channelId()));

            if(report != null)
            {
                warnSites(report);
                send(report, window.now());
            }
        }
        catch(Throwable t)
        {
            LOGGER.error("Error sending a Broadcastify Calls health report", t);
        }
    }

    /**
     * Broadcastify registers each system ID to one site, and labels every call uploaded as it with that site: warns
     * (once for each set of sites) when the node's report has more than one.  Uploads go on as before.
     */
    private void warnSites(JsonObject report)
    {
        Map<String,String> sites = report.has("sites") ? NodeReport.distinctSites(report.getAsJsonArray("sites")) :
            Map.of();

        if(sites.size() < 2)
        {
            mWarnedSites = Set.of();
            return;
        }

        if(!sites.keySet().equals(mWarnedSites))
        {
            mWarnedSites = Set.copyOf(sites.keySet());
            LOGGER.warn("Broadcastify system " + mSystemId + " is uploading " + sites.size() + " sites: " +
                String.join(", ", sites.values()) + ". Broadcastify Calls needs a system ID for each site you " +
                "upload, or calls from the other sites are labeled as the wrong site. Give each site its own " +
                "Broadcastify system ID. If you need more system IDs, apply at " +
                "https://www.broadcastify.com/apply/calls (you can use your existing API key).");
        }
    }

    /**
     * The sites the last warning named, by NodeReport.sameSite, or none.
     */
    Set<String> getWarnedSites()
    {
        return mWarnedSites;
    }

    private void send(JsonObject report, HealthSnapshot now)
    {
        if(mStopped)
        {
            return; //Stopped while the report was made
        }

        int systemId = mSystemId;
        HttpResponse<String> response;

        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(report.toString()))
                .build();
            response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch(InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            return;
        }
        catch(Exception e)
        {
            unreachable(now, systemId, e.getMessage());
            return;
        }

        int status = response.statusCode();
        String body = response.body() != null ? response.body().trim() : "";
        String said = body.length() > 200 ? body.substring(0, 200) : body;

        if(status >= 200 && status < 300)
        {
            JsonObject answer = jsonObject(body);

            if(answer == null)
            {
                notTaken(now, "it answered [" + said + "]");
                return;
            }

            mToldUnreachable = false;
            mToldNotTaken = false;
            Double nextS = number(answer, "nextS");
            mReporter.delivered(nextS);
            String message = answer.has("message") && answer.get("message").isJsonPrimitive() ?
                answer.get("message").getAsString() : null;

            if(message != null && !message.isEmpty() && !message.equals(mLastMessage))
            {
                LOGGER.info("Broadcastify Calls system [" + systemId + "]: " + message);
                mLastMessage = message;
            }
        }
        else if(status == 404 || status == 405 || status == 410)
        {
            notTaken(now, "HTTP " + status);
        }
        else if(status == 401 || status == 403)
        {
            LOGGER.warn("Broadcastify Calls refused the health report of system [" + systemId + "] (HTTP " + status +
                ": " + said + ") - not sending its reports until its settings change");
            mReporter.failed(now, NodeReporter.Failure.REFUSED, null);
        }
        else if(status == 429 || status == 503)
        {
            Double retryAfter = response.headers().firstValue("Retry-After").map(v -> {
                try
                {
                    return Double.parseDouble(v.trim());
                }
                catch(NumberFormatException nfe)
                {
                    return null;
                }
            }).orElse(null);
            mReporter.failed(now, NodeReporter.Failure.BUSY, retryAfter);
        }
        else if(status == 400)
        {
            LOGGER.warn("Broadcastify Calls turned down the health report of system [" + systemId + "]: " + said);
            mReporter.failed(now, NodeReporter.Failure.REJECTED, null);
        }
        else
        {
            unreachable(now, systemId, "HTTP " + status + (said.isEmpty() ? "" : " " + said));
        }
    }

    private void unreachable(HealthSnapshot now, int systemId, String why)
    {
        if(!mToldUnreachable)
        {
            mToldUnreachable = true;
            LOGGER.info("Broadcastify Calls health report of system [" + systemId + "] not sent: " + why);
        }

        mReporter.failed(now, NodeReporter.Failure.UNREACHABLE, null);
    }

    /**
     * The server doesn't take reports, or something else answered: says so once, and backs off as from a busy server
     * that gave no Retry-After (doubling the interval, up to an hour) until one is taken.
     */
    private void notTaken(HealthSnapshot now, String why)
    {
        if(!mToldNotTaken)
        {
            mToldNotTaken = true;
            LOGGER.info(mUrl + " doesn't take node health reports (" + why + ") - trying again less often, at " +
                "most hourly");
        }

        mReporter.failed(now, NodeReporter.Failure.BUSY, null);
    }

    private static JsonObject jsonObject(String body)
    {
        try
        {
            JsonElement element = JsonParser.parseString(body);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        }
        catch(Exception e)
        {
            return null;
        }
    }

    private static Double number(JsonObject object, String name)
    {
        try
        {
            return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsDouble() : null;
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
