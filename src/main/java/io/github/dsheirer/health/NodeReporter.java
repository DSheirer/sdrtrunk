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

import java.util.Random;

/**
 * When one node's health reports go, and what window each covers, as the node health report format sets it:
 *
 * - every 300 seconds (or what the service asks for), at a random point of the interval drawn once per node, each
 *   send moved by up to 15 seconds either way, the first at least 60 seconds after the start (an interval the
 *   service sets is counted from the node's point in the last, so the phase moves with it);
 * - no retry: a report that fails is carried by the next (its window starts where the last delivered one ended), up
 *   to 900 seconds, after which the window starts later and says so (gap);
 * - busy (429 or 503): wait Retry-After, or double the interval up to an hour (also for a service that doesn't take
 *   reports: 404, 405, 410, or an answer that isn't one);
 * - credentials refused (401 or 403): no more reports, until the node's settings change;
 * - a report turned down as wrong (400): counted as sent, so the next doesn't repeat it.
 *
 * Windows start and end at snapshot times exactly, so consecutive windows follow on without loss or overlap.  A
 * reporter that starts while recording is going on (its node's reporting started again, say) can't tell what an
 * earlier one reported: its first window starts at its own first snapshot and says some time before it wasn't
 * reported (gap), and its first report goes at least 60 seconds after that snapshot.  One that starts with the
 * recording (within its first 60 seconds, before any report can have gone) covers it from its start.
 *
 * Not thread safe: one reporting thread per node.
 */
public class NodeReporter
{
    public static final long INTERVAL_MS = 300_000;
    public static final long JITTER_MS = 15_000;
    public static final long FIRST_AFTER_MS = 60_000;
    public static final long MAX_WINDOW_MS = 900_000;
    public static final long MIN_INTERVAL_MS = 60_000;
    public static final long MAX_INTERVAL_MS = 3_600_000;

    /**
     * Why a report wasn't delivered.
     */
    public enum Failure
    {
        /** Not reached, or a server error: the next report covers this window too. */
        UNREACHABLE,
        /** 429 or 503: back off. */
        BUSY,
        /** 401 or 403: the node's credentials were refused, no more reports. */
        REFUSED,
        /** 400: the report was wrong.  Counted as sent, so the next doesn't repeat it. */
        REJECTED
    }

    /**
     * A report's window.
     * @param base snapshot the window starts at, or null to start at the start of the totals
     * @param now snapshot the window ends at
     * @param gap true when earlier time was never reported
     * @param seq this report's number for the node since the start, from 0
     */
    public record Window(HealthSnapshot base, HealthSnapshot now, boolean gap, long seq)
    {
        public long startMs()
        {
            return base != null ? base.timeMs() : now.sinceMs();
        }
    }

    private final Random mRandom;
    private long mInterval = INTERVAL_MS;
    private Long mSlot;
    private Long mNextAt;
    private Long mBackoff;
    private long[] mDeliveredTo;
    private long[] mBuilt;
    private long mSeq;
    private boolean mStopped;
    private Long mSince;
    private long mStartedAt;
    private boolean mFromStart = true;

    /**
     * @param key something of the node's own (its ID), to spread nodes that start together
     */
    public NodeReporter(String key)
    {
        this(System.nanoTime() ^ ((long)key.hashCode() << 32));
    }

    /**
     * @param seed for the node's random phase and jitter.  Seeds close together (nodes started together, with
     *             similar IDs) give unrelated phases: the seed is mixed first.
     */
    public NodeReporter(long seed)
    {
        mRandom = new Random(mix(seed));
    }

    /**
     * SplitMix64's finalizer: every bit of the seed changes every bit of the result.  java.util.Random's own seeding
     * leaves the first values of nearby seeds nearly equal.
     */
    static long mix(long seed)
    {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * True when the node's credentials were refused: it sends no more reports.
     */
    public boolean isStopped()
    {
        return mStopped;
    }

    /**
     * The node's settings changed: reports stopped by refused credentials start again.
     */
    public void resume()
    {
        mStopped = false;
    }

    /**
     * When the next report is due, Unix milliseconds, or null before the first totals.
     */
    public Long getNextAt()
    {
        return mNextAt;
    }

    private long jitter()
    {
        return (long)((mRandom.nextDouble() * 2.0 - 1.0) * JITTER_MS);
    }

    /**
     * Report for a slot, jittered, but not before now.
     */
    private void aim(long slot, long now)
    {
        mSlot = slot;
        mNextAt = Math.max(slot + jitter(), now);
    }

    /**
     * On to the slot after this one, or if that's long past, the first still to come.
     */
    private void advance(long now)
    {
        long interval = mBackoff != null ? mBackoff : mInterval;
        long slot = (mSlot != null ? mSlot : now) + interval;

        while(slot + JITTER_MS < now)
        {
            slot += interval;
        }

        aim(slot, now);
    }

    /**
     * Whether a report is due, given the latest snapshot.
     */
    public boolean isDue(HealthSnapshot now)
    {
        if(mStopped || now == null)
        {
            return false;
        }

        if(mSince == null || mSince != now.sinceMs())
        {
            //The first snapshot this reporter sees, or totals that started over (so do the reports).  Windows cover
            //the totals from their start only when nothing of them can have been reported yet.
            mFromStart = mSince != null || now.timeMs() - now.sinceMs() < FIRST_AFTER_MS;
            mSince = now.sinceMs();
            mStartedAt = now.timeMs();
            mDeliveredTo = null;
            mBuilt = null;
            mSeq = 0;
        }

        if(mSlot == null)
        {
            long start = mFromStart ? now.sinceMs() : mStartedAt;
            long phase = (long)(mRandom.nextDouble() * mInterval);
            long slot = start + phase;

            while(slot < start + FIRST_AFTER_MS)
            {
                slot += mInterval;
            }

            aim(slot, start + FIRST_AFTER_MS);
        }

        return now.timeMs() >= mNextAt;
    }

    /**
     * The window of the report due: from the end of the last one delivered (or, after more than 15 minutes
     * undelivered, the latest start a window may have, with gap) to the latest snapshot.  Null without snapshots, or
     * when the window would have no time in it (it would end where it starts): wait for the next snapshot.
     */
    public Window window(HealthHistory history)
    {
        HealthSnapshot now = history.latest();

        if(now == null)
        {
            return null;
        }

        Long from = mDeliveredTo != null && mDeliveredTo[1] == now.sinceMs() ? mDeliveredTo[0] : null;
        long oldest = now.timeMs() - MAX_WINDOW_MS;
        HealthSnapshot base;
        boolean gap;

        if(from != null)
        {
            gap = from < oldest;
            base = history.atOrAfter(gap ? oldest : from);
        }
        else if(!mFromStart)
        {
            //Started while recording went on: from its own first snapshot, never over what an earlier one sent.
            gap = true;
            base = history.atOrAfter(Math.max(mStartedAt, oldest));
        }
        else if(now.timeMs() - now.sinceMs() > MAX_WINDOW_MS)
        {
            gap = true;
            base = history.atOrAfter(oldest);
        }
        else
        {
            gap = false;
            base = null;
        }

        long start = base != null ? base.timeMs() : now.sinceMs();

        if((base == null && (from != null || gap)) || start >= now.timeMs())
        {
            return null;
        }

        mBuilt = new long[]{now.timeMs(), now.sinceMs()};
        return new Window(base, now, gap, mSeq);
    }

    /**
     * The last window was delivered.
     * @param nextS the service's interval for the next report, seconds, or null
     */
    public void delivered(Double nextS)
    {
        if(mBuilt == null)
        {
            return;
        }

        mSeq++;
        mDeliveredTo = mBuilt;
        mBuilt = null;
        mBackoff = null;
        long end = mDeliveredTo[0];

        //The next slot is this one's plus the interval, not this report's end plus the interval: sends are moved
        //by jitter and made late by the time they wait for, and counting from the end would add both to every one.
        if(nextS != null && Double.isFinite(nextS))
        {
            mInterval = Math.max(MIN_INTERVAL_MS, Math.min(MAX_INTERVAL_MS, (long)(nextS * 1000)));
        }

        advance(end + 1000);
    }

    /**
     * The last window wasn't delivered.
     * @param now latest snapshot
     * @param failure why
     * @param retryAfterS a busy service's Retry-After, seconds, or null
     */
    public void failed(HealthSnapshot now, Failure failure, Double retryAfterS)
    {
        switch(failure)
        {
            case REFUSED -> mStopped = true;
            case BUSY -> {
                if(retryAfterS != null && Double.isFinite(retryAfterS))
                {
                    long after = Math.max(1000, Math.min(MAX_INTERVAL_MS, (long)(retryAfterS * 1000)));
                    mNextAt = now.timeMs() + after;
                }
                else
                {
                    mBackoff = Math.min((mBackoff != null ? mBackoff : mInterval) * 2, MAX_INTERVAL_MS);
                    advance(now.timeMs() + 1000);
                }
            }
            case REJECTED -> {
                mSeq++;
                mDeliveredTo = mBuilt != null ? mBuilt : new long[]{now.timeMs(), now.sinceMs()};
                advance(now.timeMs() + 1000);
            }
            case UNREACHABLE -> advance(now.timeMs() + 1000);
        }

        mBuilt = null;
    }
}
