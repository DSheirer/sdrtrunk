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

package io.github.dsheirer.module.decode.moto.message;

import io.github.dsheirer.module.decode.moto.Bandplan;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 6-entry OSW queue for multi-OSW message assembly.
 * Messages can be 1, 2, or 3 OSWs long with IDLE OSWs interleaved.
 * Reference: OP25 tk_smartnet.py lines 806-1764
 */
public class OswQueue
{
    public static final int QUEUE_SIZE = 6;
    public static final int QUEUE_RESET_ADDRESS = 0xFFE;

    private final Bandplan mBandplan;

    public OswQueue(Bandplan bandplan)
    {
        mBandplan = bandplan;
    }

    /**
     * Represents a single OSW entry in the queue.
     */
    public static class OswEntry
    {
        public final int address;
        public final boolean isGroup;
        public final int command;
        public final long timestamp;
        public final boolean isReset;
        public final boolean isChannel;
        public final int channelNumber;

        public OswEntry(int address, boolean isGroup, int command, long timestamp, Bandplan bandplan)
        {
            this.address = address;
            this.isGroup = isGroup;
            this.command = command;
            this.timestamp = timestamp;
            this.isReset = false;
            
            // Check if command field contains a valid channel number
            if(bandplan != null && command >= 0 && command <= 0x3FF)
            {
                double freq = bandplan.getDownlinkFrequency(command);
                this.isChannel = (freq > 0);
                this.channelNumber = this.isChannel ? command : 0;
            }
            else
            {
                this.isChannel = false;
                this.channelNumber = 0;
            }
        }

        private OswEntry()
        {
            this.address = QUEUE_RESET_ADDRESS;
            this.isGroup = false;
            this.command = 0;
            this.timestamp = System.currentTimeMillis();
            this.isReset = true;
            this.isChannel = false;
            this.channelNumber = 0;
        }

        public static OswEntry createReset()
        {
            return new OswEntry();
        }
    }

    private final Deque<OswEntry> mQueue = new ArrayDeque<>(QUEUE_SIZE);

    /**
     * Add an OSW entry to the queue. If the queue is full, the oldest entry is removed.
     */
    public void add(OswEntry entry)
    {
        if(mQueue.size() >= QUEUE_SIZE)
        {
            mQueue.removeFirst();
        }
        mQueue.addLast(entry);
    }

    /**
     * Add an OSW to the queue with the given parameters.
     */
    public void add(int address, boolean isGroup, int command, long timestamp)
    {
        add(new OswEntry(address, isGroup, command, timestamp, mBandplan));
    }

    /**
     * Insert a QUEUE_RESET marker.
     */
    public void addReset()
    {
        add(OswEntry.createReset());
    }

    /**
     * Push an entry to the front of the queue (for IDLE reordering).
     */
    public void pushFront(OswEntry entry)
    {
        if(mQueue.size() >= QUEUE_SIZE)
        {
            mQueue.removeLast();
        }
        ((ArrayDeque<OswEntry>)mQueue).addFirst(entry);
    }

    /**
     * Check if the queue is full (has QUEUE_SIZE entries).
     */
    public boolean isFull()
    {
        return mQueue.size() >= QUEUE_SIZE;
    }

    /**
     * Check if the queue is empty.
     */
    public boolean isEmpty()
    {
        return mQueue.isEmpty();
    }

    /**
     * Get entry at index (0 = oldest, 5 = newest).
     */
    public OswEntry get(int index)
    {
        int i = 0;
        for(OswEntry entry : mQueue)
        {
            if(i == index) return entry;
            i++;
        }
        return null;
    }

    /**
     * Get the newest entry (last added to queue).
     */
    public OswEntry getNewest()
    {
        return mQueue.peekLast();
    }

    /**
     * Get the second entry (index 1) - the OSW after the oldest.
     * For 2-OSW messages, this is the second OSW of the pair.
     */
    public OswEntry getSecond()
    {
        return get(1);
    }

    /**
     * Get the third entry (index 2) - for 3-OSW messages.
     */
    public OswEntry getThird()
    {
        return get(2);
    }

    /**
     * Get the middle entry (index 1). Alias for getSecond().
     * Kept for backward compatibility.
     */
    public OswEntry getMiddle()
    {
        return get(1);
    }

    /**
     * Get the oldest entry (OSW0 in OP25 terminology).
     */
    public OswEntry getOldest()
    {
        return get(0);
    }

    /**
     * Remove the newest entry from the queue.
     */
    public void removeNewest()
    {
        mQueue.removeLast();
    }

    /**
     * Remove the oldest entry from the queue.
     */
    public void removeOldest()
    {
        mQueue.removeFirst();
    }

    /**
     * Remove the middle entry from the queue.
     */
    public void removeMiddle()
    {
        // Middle is at index QUEUE_SIZE - 2 = 4
        // We need to remove it and shift newer entries down
        java.util.List<OswEntry> list = new java.util.ArrayList<>(mQueue);
        if(list.size() >= QUEUE_SIZE - 1)
        {
            list.remove(QUEUE_SIZE - 2);
            mQueue.clear();
            mQueue.addAll(list);
        }
    }

    /**
     * Clear the queue.
     */
    public void clear()
    {
        mQueue.clear();
    }

    /**
     * Get the current size of the queue.
     */
    public int size()
    {
        return mQueue.size();
    }

    /**
     * Check if any entry in the queue is a reset marker.
     */
    public boolean hasReset()
    {
        for(OswEntry entry : mQueue)
        {
            if(entry.isReset) return true;
        }
        return false;
    }

    /**
     * Remove all entries up to and including the first reset marker.
     */
    public void clearToReset()
    {
        while(!mQueue.isEmpty())
        {
            OswEntry entry = mQueue.removeFirst();
            if(entry.isReset) break;
        }
    }
}
