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

import io.github.dsheirer.module.decode.DecoderType;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

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
    private final Supplier<ChannelInfo> mCurrentInfo;
    private final int mGeneration;
    private final CallTally mCalls = new CallTally();
    private boolean mCallsCounted;

    private String mProtocol;
    private long mFrequency;
    private final Map<String,Integer> mIdentity = new TreeMap<>();
    private long mGood;
    private long mBad;
    private boolean mBadCounted;
    private long mLockedMs;
    private long mLastGood;

    private int mRunning;
    private long mRunningMs;
    private long mRunningSince;
    private long mOpens;
    private long mCloses;
    private long mOpenMs;
    private long mOpenedAt;

    public ChannelHealth(ChannelInfo info)
    {
        this(info, null, 0);
    }

    /**
     * @param info of the channel when it started
     * @param currentInfo of the channel as it is now (edits to its name, system, site and alias list), or null when
     *                    it can't change
     * @param generation of the channel's totals: they start again from nothing when its decoder changes, and a
     *                   report doesn't take one generation's from another's
     */
    public ChannelHealth(ChannelInfo info, Supplier<ChannelInfo> currentInfo, int generation)
    {
        mInfo = info;
        mCurrentInfo = currentInfo;
        mGeneration = generation;
    }

    /**
     * What the channel is now: its name, system, site and alias list as they are, its decoder (and mode) as it was
     * when these totals started.
     */
    public ChannelInfo getInfo()
    {
        if(mCurrentInfo != null)
        {
            try
            {
                ChannelInfo now = mCurrentInfo.get();

                if(now != null)
                {
                    return new ChannelInfo(mInfo.channelId(), now.name(), now.system(), now.site(),
                        now.aliasListName(), mInfo.mode(), mInfo.decoder());
                }
            }
            catch(RuntimeException e)
            {
                //Being edited: as it was
            }
        }

        return mInfo;
    }

    public int getGeneration()
    {
        return mGeneration;
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
     * One of the channel's monitors started (or restarted) processing.  A channel can have two for a moment: a DMR
     * Capacity Plus rest channel that moves starts a new processing chain for the channel while the old one becomes a
     * traffic channel.  It's running while any is.
     */
    public synchronized void started(long now)
    {
        if(mRunning++ == 0)
        {
            mRunningSince = now;
        }
    }

    /**
     * One of the channel's monitors stopped processing (each stops once for each start).  When the last does, an open
     * squelch is cut off: its time counts as open, but it isn't counted as a transmission completed.
     */
    public synchronized void stopped(long now)
    {
        if(mRunning == 0)
        {
            return;
        }

        if(--mRunning == 0)
        {
            mRunningMs += Math.max(0, now - mRunningSince);

            if(mOpenedAt > 0)
            {
                mOpenMs += Math.max(0, now - mOpenedAt);
                mOpenedAt = 0;
            }
        }
    }

    /**
     * The frequency the channel is tuned to now (it can rotate through a site's control channels).
     */
    public synchronized void setFrequency(long frequency)
    {
        mFrequency = frequency;
    }

    /**
     * The channel is a trunked site's, whatever it decodes: its decoder can be nothing else (MPT1327, LTR, LTR-Net,
     * Passport, a P25 Phase 2 control channel).  It's reported as a site from the start, deaf or not.
     * @param protocol of the site, as a node health report names it
     */
    public synchronized void setProtocol(String protocol)
    {
        if(mProtocol == null)
        {
            mProtocol = protocol;
        }
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
        mBadCounted = true;

        if(valid)
        {
            good(timestamp);
        }
        else
        {
            mBad++;
        }
    }

    /**
     * A control message was decoded, from a decoder that passes on only those that decode (LTR, LTR-Net, Passport):
     * the messages it couldn't decode aren't counted, and a report leaves them out rather than say there were none.
     * @param protocol of the site, as a node health report names it
     * @param timestamp of the message
     */
    public synchronized void decoded(String protocol, long timestamp)
    {
        mProtocol = protocol;
        good(timestamp);
    }

    private void good(long timestamp)
    {
        mGood++;

        if(mLastGood > 0 && timestamp > mLastGood)
        {
            mLockedMs += Math.min(timestamp - mLastGood, LOCK_HOLD_MS);
        }

        mLastGood = Math.max(mLastGood, timestamp);
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

        boolean lockedNow = mLastGood > 0 && now - mLastGood <= LOCK_HOLD_MS;
        long running = mRunningMs + (mRunning > 0 ? Math.max(0, now - mRunningSince) : 0);
        long open = mOpenMs + (mOpenedAt > 0 ? Math.max(0, now - mOpenedAt) : 0);

        return new Totals(getInfo(), mProtocol, mFrequency, Collections.unmodifiableMap(new TreeMap<>(mIdentity)), mGood,
            mBad, mBadCounted, locked, lockedNow, mCallsCounted ? mCalls.totals() : null, running, mOpens, mCloses,
            open, mGeneration);
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
     * @param decoder of the channel, or null when it has none (or isn't known)
     */
    public record ChannelInfo(int channelId, String name, String system, String site, String aliasListName,
                              String mode, DecoderType decoder)
    {
        public ChannelInfo(int channelId, String name, String system, String site, String aliasListName, String mode)
        {
            this(channelId, name, system, site, aliasListName, mode, null);
        }
    }

    /**
     * One channel's totals at one moment, since it started.
     * @param protocol of the site, or null when no control message has been decoded (a conventional channel)
     * @param frequency tuned
     * @param identity of the site, as broadcast
     * @param good control messages decoded
     * @param bad control messages framed but not decoded
     * @param badCounted true when the decoder says which messages it couldn't decode (bad is measured)
     * @param lockedMs milliseconds with a good control message within the last 2 s
     * @param locked true when it was locked at this moment: a good control message within the last 2 s
     * @param calls announced, and their outcomes, or null when nothing counts them
     * @param runningMs milliseconds the channel was processing
     * @param opens squelch openings
     * @param closes squelch closings (transmissions completed)
     * @param openMs milliseconds open
     * @param generation of the channel's totals (they start again when its decoder changes)
     */
    public record Totals(ChannelInfo info, String protocol, long frequency, Map<String,Integer> identity, long good,
                         long bad, boolean badCounted, long lockedMs, boolean locked, CallTally.Totals calls,
                         long runningMs, long opens, long closes, long openMs, int generation)
    {
        /**
         * Totals of a channel's first generation.
         */
        public Totals(ChannelInfo info, String protocol, long frequency, Map<String,Integer> identity, long good,
                      long bad, boolean badCounted, long lockedMs, boolean locked, CallTally.Totals calls,
                      long runningMs, long opens, long closes, long openMs)
        {
            this(info, protocol, frequency, identity, good, bad, badCounted, lockedMs, locked, calls, runningMs, opens,
                closes, openMs, 0);
        }

        /**
         * True when the channel has decoded control messages: a trunked site.
         */
        public boolean isSite()
        {
            return protocol != null;
        }
    }
}
