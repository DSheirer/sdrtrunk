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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The snapshots of the last half hour: enough to report any node's window after failed sends, up to the longest
 * window (NodeReporter.MAX_WINDOW_S).  Thread safe.
 */
public class HealthHistory
{
    /** How far back snapshots are kept. */
    public static final long KEEP_MS = 30 * 60 * 1000;

    private final Deque<HealthSnapshot> mSnapshots = new ArrayDeque<>();

    /**
     * Adds the latest snapshot, dropping those too old to need.
     */
    public synchronized void add(HealthSnapshot snapshot)
    {
        mSnapshots.addLast(snapshot);

        while(!mSnapshots.isEmpty() && mSnapshots.getFirst().timeMs() < snapshot.timeMs() - KEEP_MS)
        {
            mSnapshots.removeFirst();
        }
    }

    /**
     * The latest snapshot, or null when there is none yet.
     */
    public synchronized HealthSnapshot latest()
    {
        return mSnapshots.peekLast();
    }

    /**
     * The first snapshot at or after a time, or null.  A window that ended at a snapshot's time starts the next
     * window at exactly that snapshot.
     */
    public synchronized HealthSnapshot atOrAfter(long timeMs)
    {
        for(HealthSnapshot snapshot: mSnapshots)
        {
            if(snapshot.timeMs() >= timeMs)
            {
                return snapshot;
            }
        }

        return null;
    }
}
