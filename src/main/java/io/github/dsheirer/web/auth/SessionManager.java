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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Browser sessions, held in memory.  A session ends after the idle timeout with no requests, when the user signs
 * out, or when an administrator or a password change revokes it.
 */
public class SessionManager
{
    public static final int SESSION_ID_BYTES = 32;
    public static final int MAX_SESSIONS_PER_USER = 20;

    private final Map<String,Entry> mSessions = new ConcurrentHashMap<>();
    private final IntSupplier mTimeoutMinutes;
    private final LongSupplier mClock;

    /**
     * Constructs an instance
     * @param timeoutMinutes idle timeout, read on each check so that a settings change applies at once
     * @param clock epoch milliseconds
     */
    public SessionManager(IntSupplier timeoutMinutes, LongSupplier clock)
    {
        mTimeoutMinutes = timeoutMinutes;
        mClock = clock;
    }

    /**
     * Starts a session for the account.  When the account already has the maximum count of sessions, the least
     * recently used one ends.
     */
    public Session create(long userId, String username, String address)
    {
        purgeExpired();
        List<Entry> existing = mSessions.values().stream().filter(entry -> entry.session.userId() == userId)
            .sorted((a, b) -> Long.compare(a.lastAccess, b.lastAccess)).toList();

        for(int x = 0; x <= existing.size() - MAX_SESSIONS_PER_USER; x++)
        {
            mSessions.remove(existing.get(x).session.id());
        }

        long now = mClock.getAsLong();
        Session session = new Session(PasswordHasher.createSecret(SESSION_ID_BYTES), userId, username,
            PasswordHasher.createSecret(SESSION_ID_BYTES), now, address);
        mSessions.put(session.id(), new Entry(session, now));
        return session;
    }

    /**
     * Finds an active session and records the access, which extends its idle timeout.
     * @param id from the session cookie
     * @return session, or empty if there is none or it timed out
     */
    public Optional<Session> touch(String id)
    {
        if(id == null)
        {
            return Optional.empty();
        }

        Entry entry = mSessions.get(id);

        if(entry == null)
        {
            return Optional.empty();
        }

        long now = mClock.getAsLong();

        if(isExpired(entry, now))
        {
            mSessions.remove(id);
            return Optional.empty();
        }

        entry.lastAccess = now;
        return Optional.of(entry.session);
    }

    /**
     * Ends the session.
     */
    public void invalidate(String id)
    {
        if(id != null)
        {
            mSessions.remove(id);
        }
    }

    /**
     * Ends the account's sessions, except the one argument.
     * @param userId account
     * @param exceptSessionId session to keep, or null to end them all
     * @return count ended
     */
    public int invalidateUser(long userId, String exceptSessionId)
    {
        int count = 0;

        for(Entry entry: List.copyOf(mSessions.values()))
        {
            if(entry.session.userId() == userId && !entry.session.id().equals(exceptSessionId))
            {
                mSessions.remove(entry.session.id());
                count++;
            }
        }

        return count;
    }

    /**
     * Ends every session, for example when a different database (with different accounts) is selected.
     */
    public void invalidateAll()
    {
        mSessions.clear();
    }

    /**
     * Active sessions of the account
     */
    public List<SessionInfo> getSessions(long userId)
    {
        long now = mClock.getAsLong();
        return mSessions.values().stream()
            .filter(entry -> entry.session.userId() == userId && !isExpired(entry, now))
            .map(entry -> new SessionInfo(entry.session, entry.lastAccess))
            .toList();
    }

    /**
     * Count of active sessions
     */
    public int size()
    {
        purgeExpired();
        return mSessions.size();
    }

    /**
     * Removes the sessions that timed out.
     */
    public void purgeExpired()
    {
        long now = mClock.getAsLong();
        mSessions.values().removeIf(entry -> isExpired(entry, now));
    }

    private boolean isExpired(Entry entry, long now)
    {
        return now - entry.lastAccess > mTimeoutMinutes.getAsInt() * 60_000L;
    }

    /**
     * Session and when it was last used
     */
    public record SessionInfo(Session session, long lastAccess) {}

    private static class Entry
    {
        private final Session session;
        private volatile long lastAccess;

        Entry(Session session, long lastAccess)
        {
            this.session = session;
            this.lastAccess = lastAccess;
        }
    }
}
