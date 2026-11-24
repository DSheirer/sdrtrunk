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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Running totals of how well one standard (not traffic) channel is heard, since it started: as a trunked site's
 * control channel (control messages decoded and lost, seconds locked, the site's identity, the calls it announced) or
 * as a conventional channel (seconds running, openings, seconds open).  A channel counts as a site once it has decoded
 * a control message.
 *
 * Written by the channel's HealthMonitor and its traffic channel manager, read by the HealthRegistry.  Thread safe.
 */
public class ChannelHealth
{
    /** A control channel counts as locked until this long after its last good message. */
    public static final long LOCK_HOLD_MS = 2000;

    private final ChannelInfo mInfo;
    private final CallTally mCalls = new CallTally();
    private boolean mCallsCounted;

    private String mProtocol;
    private long mFrequency;
    private final Map<String,Integer> mIdentity = new TreeMap<>();
    private long mGood;
    private long mBad;
    private long mLockedMs;
    private long mLastGood;

    private boolean mRunning;
    private long mRunningMs;
    private long mRunningSince;
    private long mOpens;
    private long mCloses;
    private long mOpenMs;
    private long mOpenedAt;

    public ChannelHealth(ChannelInfo info)
    {
        mInfo = info;
    }

    public ChannelInfo getInfo()
    {
        return mInfo;
    }

    /**
     * Calls the channel's control channel announced.
     */
    public CallTally getCalls()
    {
        return mCalls;
    }

    /**
     * Something counts the calls this channel's control channel announces (its traffic channel manager).  Until then
     * a site's calls aren't known, and a report leaves the site out rather than say it had none.
     */
    public synchronized void setCallsCounted()
    {
        mCallsCounted = true;
    }

    /**
     * The channel started (or restarted) processing.
     */
    public synchronized void started(long now)
    {
        if(!mRunning)
        {
            mRunning = true;
            mRunningSince = now;
        }
    }

    /**
     * The channel stopped processing.
     */
    public synchronized void stopped(long now)
    {
        if(mRunning)
        {
            mRunningMs += Math.max(0, now - mRunningSince);
            mRunning = false;
        }

        closed(now);
    }

    /**
     * The frequency the channel is tuned to now (it can rotate through a site's control channels).
     */
    public synchronized void setFrequency(long frequency)
    {
        mFrequency = frequency;
    }

    /**
     * A control message was framed.
     * @param protocol of the site, as a node health report names it (p25, dmr, ...)
     * @param valid true when it decoded, false when it failed its CRC or FEC
     * @param timestamp of the message
     */
    public synchronized void control(String protocol, boolean valid, long timestamp)
    {
        mProtocol = protocol;

        if(valid)
        {
            mGood++;

            if(mLastGood > 0 && timestamp > mLastGood)
            {
                mLockedMs += Math.min(timestamp - mLastGood, LOCK_HOLD_MS);
            }

            mLastGood = Math.max(mLastGood, timestamp);
        }
        else
        {
            mBad++;
        }
    }

    /**
     * Part of the site's identity, as its control channel broadcasts it about itself (never from a neighbour's).
     * @param name of the field (wacn, sysId, rfss, site, nac, colorCode, ...)
     * @param value of the field
     */
    public synchronized void identity(String name, int value)
    {
        mIdentity.put(name, value);
    }

    /**
     * Squelch opened (a conventional channel).
     */
    public synchronized void opened(long now)
    {
        if(mOpenedAt == 0)
        {
            mOpens++;
            mOpenedAt = now;
        }
    }

    /**
     * Squelch closed (a conventional channel).
     */
    public synchronized void closed(long now)
    {
        if(mOpenedAt > 0)
        {
            mOpenMs += Math.max(0, now - mOpenedAt);
            mCloses++;
            mOpenedAt = 0;
        }
    }

    /**
     * The totals at this moment.
     */
    public synchronized Totals totals(long now)
    {
        long locked = mLockedMs;

        if(mLastGood > 0 && now > mLastGood)
        {
            locked += Math.min(now - mLastGood, LOCK_HOLD_MS);
        }

        long running = mRunningMs + (mRunning ? Math.max(0, now - mRunningSince) : 0);
        long open = mOpenMs + (mOpenedAt > 0 ? Math.max(0, now - mOpenedAt) : 0);

        return new Totals(mInfo, mProtocol, mFrequency, Collections.unmodifiableMap(new TreeMap<>(mIdentity)), mGood,
            mBad, locked, mCallsCounted ? mCalls.totals() : null, running, mOpens, mCloses, open);
    }

    /**
     * What the channel is, from its configuration.
     * @param channelId of the channel configuration
     * @param name of the channel
     * @param system name the user gave
     * @param site name the user gave
     * @param aliasListName of the channel (which aliases, and so which streams, its calls go to)
     * @param mode a conventional channel's mode as a node health report names it (fm, am, p25, dmr, nxdn), or null
     *             when the decoder can't be a conventional channel
     */
    public record ChannelInfo(int channelId, String name, String system, String site, String aliasListName,
                              String mode)
    {
    }

    /**
     * One channel's totals at one moment, since it started.
     * @param protocol of the site, or null when no control message has been decoded (a conventional channel)
     * @param frequency tuned
     * @param identity of the site, as broadcast
     * @param good control messages decoded
     * @param bad control messages framed but not decoded
     * @param lockedMs milliseconds with a good control message within the last 2 s
     * @param calls announced, and their outcomes, or null when nothing counts them
     * @param runningMs milliseconds the channel was processing
     * @param opens squelch openings
     * @param closes squelch closings (transmissions completed)
     * @param openMs milliseconds open
     */
    public record Totals(ChannelInfo info, String protocol, long frequency, Map<String,Integer> identity, long good,
                         long bad, long lockedMs, CallTally.Totals calls, long runningMs, long opens, long closes,
                         long openMs)
    {
        /**
         * True when the channel has decoded control messages: a trunked site.
         */
        public boolean isSite()
        {
            return protocol != null;
        }
    }
}
