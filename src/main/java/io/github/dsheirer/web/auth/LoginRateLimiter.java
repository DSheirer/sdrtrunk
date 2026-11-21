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

package io.github.dsheirer.web.auth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limits sign-in attempts per remote address, on top of the per-account lockout, so that one address can't try
 * passwords across many accounts quickly.
 */
public class LoginRateLimiter
{
    public static final int MAX_ATTEMPTS = 10;
    public static final long WINDOW_MS = 60_000;
    private static final int MAX_TRACKED_ADDRESSES = 10_000;

    private final Map<String,Deque<Long>> mAttempts = new ConcurrentHashMap<>();
    private final LongSupplier mClock;

    public LoginRateLimiter(LongSupplier clock)
    {
        mClock = clock;
    }

    /**
     * Records a sign-in attempt from the address.
     * @return true if the attempt is allowed, false if the address made too many attempts within the last minute
     */
    public boolean tryAcquire(String address)
    {
        long now = mClock.getAsLong();

        if(mAttempts.size() > MAX_TRACKED_ADDRESSES)
        {
            mAttempts.values().removeIf(times -> {
                synchronized(times)
                {
                    return times.isEmpty() || now - times.peekLast() > WINDOW_MS;
                }
            });
        }

        Deque<Long> times = mAttempts.computeIfAbsent(address == null ? "" : address, key -> new ArrayDeque<>());

        synchronized(times)
        {
            while(!times.isEmpty() && now - times.peekFirst() > WINDOW_MS)
            {
                times.pollFirst();
            }

            if(times.size() >= MAX_ATTEMPTS)
            {
                return false;
            }

            times.addLast(now);
            return true;
        }
    }
}
