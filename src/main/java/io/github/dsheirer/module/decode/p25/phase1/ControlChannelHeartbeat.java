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
package io.github.dsheirer.module.decode.p25.phase1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires HTTP GET heartbeat pings to configured URLs when an RFSS Status Broadcast is observed
 * with matching system and site IDs, throttled to a configurable interval.
 */
public class ControlChannelHeartbeat
{
    private static final Logger mLog = LoggerFactory.getLogger(ControlChannelHeartbeat.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final int mSystemId;
    private final int mSiteId;
    private final String mChannelName;
    private final String mPushUrl2;
    private final String mKumaUrl;
    private final long mIntervalMs;
    private final AtomicLong mLastFiredMs = new AtomicLong(0L);

    /**
     * Constructs an instance.
     * @param systemId P25 system ID to match
     * @param siteId P25 site ID to match
     * @param channelName descriptive name for logging
     * @param pushUrl2 optional URL (may be null or blank)
     * @param kumaUrl optional Uptime Kuma push URL (may be null or blank)
     * @param intervalSeconds minimum seconds between successive pings
     */
    public ControlChannelHeartbeat(int systemId, int siteId, String channelName,
                                   String pushUrl2, String kumaUrl, int intervalSeconds)
    {
        mSystemId = systemId;
        mSiteId = siteId;
        mChannelName = channelName;
        mPushUrl2 = pushUrl2;
        mKumaUrl = kumaUrl;
        mIntervalMs = intervalSeconds * 1000L;
    }

    /**
     * Called whenever an RFSS Status Broadcast is received. Fires heartbeat pings if IDs match
     * and the throttle interval has elapsed.
     * @param systemId received system ID
     * @param siteId received site ID
     */
    public void onRFSSStatusBroadcast(int systemId, int siteId)
    {
        if(systemId != mSystemId || siteId != mSiteId)
        {
            return;
        }

        long now = System.currentTimeMillis();
        long last = mLastFiredMs.get();

        if((now - last) < mIntervalMs)
        {
            return;
        }

        if(!mLastFiredMs.compareAndSet(last, now))
        {
            return;
        }

        if(mPushUrl2 != null && !mPushUrl2.isBlank())
        {
            final String url = mPushUrl2;
            Thread.ofVirtual().start(() -> fireGet(url));
        }

        if(mKumaUrl != null && !mKumaUrl.isBlank())
        {
            final String url = mKumaUrl;
            Thread.ofVirtual().start(() -> fireGet(url));
        }
    }

    /**
     * Fires an HTTP GET to the specified URL.
     */
    private void fireGet(String url)
    {
        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            mLog.debug("Heartbeat ping [{}] channel [{}] -> HTTP {}", url, mChannelName, response.statusCode());
        }
        catch(Exception e)
        {
            mLog.warn("Heartbeat ping failed [{}] channel [{}]: {}", url, mChannelName, e.getMessage());
        }
    }

    /**
     * Configuration POJO for heartbeat entries.
     */
    public static class Config
    {
        public String channelName;
        public int systemId;
        public int siteId;
        public String pushUrl2;
        public String kumaUrl;
        public int intervalSeconds = 120;
    }
}
