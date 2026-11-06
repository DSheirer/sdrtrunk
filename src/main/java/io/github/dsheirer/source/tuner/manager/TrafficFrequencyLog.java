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
package io.github.dsheirer.source.tuner.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent list of the frequencies each playlist channel uses: its control channel frequency and every traffic
 * channel frequency it has granted, one entry per channel and frequency with a call count and last-seen time.
 * Entries that go unused for two weeks are dropped.
 */
public class TrafficFrequencyLog
{
    private static final Logger mLog = LoggerFactory.getLogger(TrafficFrequencyLog.class);
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(14);
    private static final long LAST_SESSION_WINDOW = TimeUnit.MINUTES.toMillis(10);
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Path mPath;
    private final Map<String, Entry> mEntries = new TreeMap<>();
    private boolean mDirty;

    public static class Entry
    {
        public String owner;
        public boolean control;
        public long frequency;
        public int bandwidth;
        public long count;
        public long firstSeen;
        public long lastSeen;
    }

    /**
     * @param path of the json file, or null to keep the log in memory only
     */
    public TrafficFrequencyLog(Path path)
    {
        mPath = path;
    }

    /**
     * Records a traffic channel request granted by the owner's control channel.
     */
    public synchronized void record(String owner, long frequency, int bandwidth, long timestamp)
    {
        Entry entry = getEntry(owner, false, frequency, timestamp);
        entry.bandwidth = bandwidth;
        entry.count++;
        entry.lastSeen = timestamp;
        mDirty = true;
    }

    /**
     * Records that the owner's control (or conventional) channel is running on the frequency.
     */
    public synchronized void recordControl(String owner, long frequency, int bandwidth, long timestamp)
    {
        Entry entry = getEntry(owner, true, frequency, timestamp);
        entry.bandwidth = bandwidth;
        entry.lastSeen = timestamp;
        mDirty = true;
    }

    /**
     * Learned traffic frequencies for the owners, weighted by call count.
     */
    public synchronized List<TunerPlan.Learned> getLearned(long timestamp, Collection<String> owners)
    {
        prune(timestamp);

        List<TunerPlan.Learned> learned = new ArrayList<>();

        for(Entry entry: mEntries.values())
        {
            if(!entry.control && owners.contains(entry.owner))
            {
                learned.add(new TunerPlan.Learned(entry.frequency, entry.bandwidth, entry.count, false));
            }
        }

        return learned;
    }

    /**
     * Every control frequency the owners' channels have run on, which includes the alternates a multi-frequency
     * channel rotates through.
     */
    public synchronized List<Entry> getControls(Collection<String> owners)
    {
        List<Entry> controls = new ArrayList<>();

        for(Entry entry: mEntries.values())
        {
            if(entry.control && owners.contains(entry.owner))
            {
                controls.add(entry);
            }
        }

        return controls;
    }

    /**
     * Control channels that were running at the end of the last session, keyed by owner.  A channel that stopped
     * well before the others is left out.
     */
    public synchronized Map<String, Entry> getLastSessionControls()
    {
        long latest = 0;

        for(Entry entry: mEntries.values())
        {
            if(entry.control)
            {
                latest = Math.max(latest, entry.lastSeen);
            }
        }

        Map<String, Entry> controls = new TreeMap<>();

        for(Entry entry: mEntries.values())
        {
            if(entry.control && entry.lastSeen >= latest - LAST_SESSION_WINDOW)
            {
                Entry current = controls.get(entry.owner);

                if(current == null || entry.lastSeen > current.lastSeen)
                {
                    controls.put(entry.owner, entry);
                }
            }
        }

        return controls;
    }

    public synchronized int size()
    {
        return mEntries.size();
    }

    public synchronized void load()
    {
        if(mPath == null || !Files.exists(mPath))
        {
            return;
        }

        try
        {
            List<Entry> entries = MAPPER.readValue(mPath.toFile(), new TypeReference<List<Entry>>() {});
            mEntries.clear();

            for(Entry entry: entries)
            {
                if(entry.owner != null && entry.frequency > 0 && entry.bandwidth > 0)
                {
                    mEntries.put(key(entry.owner, entry.control, entry.frequency), entry);
                }
                else
                {
                    mDirty = true;
                }
            }

            prune(System.currentTimeMillis());
            mLog.info("Loaded " + mEntries.size() + " learned channel frequencies");
        }
        catch(Exception e)
        {
            mLog.error("Unable to read learned channel frequencies from [" + mPath + "]", e);
        }
    }

    public synchronized void save()
    {
        if(mPath == null || !mDirty)
        {
            return;
        }

        try
        {
            Path temp = mPath.resolveSibling(mPath.getFileName() + ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), new ArrayList<>(mEntries.values()));
            Files.move(temp, mPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            mDirty = false;
        }
        catch(Exception e)
        {
            mLog.error("Unable to save learned channel frequencies to [" + mPath + "]", e);
        }
    }

    private Entry getEntry(String owner, boolean control, long frequency, long timestamp)
    {
        String key = key(owner, control, frequency);
        Entry entry = mEntries.get(key);

        if(entry == null)
        {
            entry = new Entry();
            entry.owner = owner;
            entry.control = control;
            entry.frequency = frequency;
            entry.firstSeen = timestamp;
            mEntries.put(key, entry);

            if(!control)
            {
                mLog.info("Learned traffic frequency [" + frequency + "] for [" + owner + "] - " + mEntries.size() +
                    " known");
            }
        }

        return entry;
    }

    private static String key(String owner, boolean control, long frequency)
    {
        return owner + (control ? " control " : " traffic ") + frequency;
    }

    private void prune(long timestamp)
    {
        Iterator<Entry> it = mEntries.values().iterator();

        while(it.hasNext())
        {
            if(timestamp - it.next().lastSeen > MAX_AGE)
            {
                it.remove();
                mDirty = true;
            }
        }
    }
}
