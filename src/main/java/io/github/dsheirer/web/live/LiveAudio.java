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

package io.github.dsheirer.web.live;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.web.media.OpusStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live call audio for the audio.live topic, and call progress (audio length) for the calls topic.  Receives every call's audio segment, like the call manager, and while
 * anyone listens, encodes the new audio of each call as Opus every 40 milliseconds and hands the packets to the live
 * hub, which sends them to the subscriptions whose filter matches the call.  Each call is encoded once, however many
 * clients listen.
 *
 * It is registered with the channel processing manager before channels start and lives as long as the application,
 * since audio segment listeners only attach to channels that start after them.  Without listeners it only tracks
 * the segments and skips their audio.  While anyone subscribes to the calls topic, it reports each active call's
 * decoded audio length to the hub every cycle; the hub paces the call.progress messages per subscription.
 */
public class LiveAudio implements Listener<AudioSegment>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveAudio.class);
    private static final long INTERVAL_MS = 40;
    //Packets held while the hub doesn't know the call yet (the call manager publishes calls every 200 ms)
    private static final int MAX_PENDING_PACKETS = 100;

    private final LinkedTransferQueue<AudioSegment> mNewSegments = new LinkedTransferQueue<>();
    private final List<Tracked> mTracked = new ArrayList<>();
    private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor(
        new NamingThreadFactory("sdrtrunk web live audio"));
    private final Supplier<LiveHub> mHub;
    private ScheduledFuture<?> mFuture;

    /**
     * Constructs an instance
     * @param hub supplies the live hub while the web server runs, or null
     */
    public LiveAudio(Supplier<LiveHub> hub)
    {
        mHub = hub;
    }

    public synchronized void start()
    {
        if(mFuture == null)
        {
            mFuture = mExecutor.scheduleWithFixedDelay(this::processSafely, INTERVAL_MS, INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops and releases every tracked segment.
     */
    public synchronized void stop()
    {
        if(mFuture != null)
        {
            mFuture.cancel(false);
            mFuture = null;
        }

        try
        {
            mExecutor.submit(() -> {
                drain();

                for(Tracked tracked: mTracked)
                {
                    tracked.mSegment.decrementConsumerCount();
                }

                mTracked.clear();
            }).get(5, TimeUnit.SECONDS);
        }
        catch(Exception e)
        {
            LOGGER.error("Error stopping web live audio", e);
        }

        mExecutor.shutdownNow();
    }

    /**
     * New audio segment from a channel.  The consumer count is already incremented for this listener.
     */
    @Override
    public void receive(AudioSegment segment)
    {
        mNewSegments.add(segment);
    }

    private void processSafely()
    {
        try
        {
            process();
        }
        catch(Throwable t)
        {
            LOGGER.error("Error processing web live audio", t);
        }
    }

    /**
     * Encodes and publishes new audio.  Web live audio thread.
     */
    void process()
    {
        drain();
        LiveHub hub = mHub.get();
        boolean listening = hub != null && hub.hasSubscribers(Topic.AUDIO_LIVE);
        boolean progress = hub != null && hub.hasSubscribers(Topic.CALLS);
        long now = System.currentTimeMillis();
        Iterator<Tracked> it = mTracked.iterator();

        while(it.hasNext())
        {
            Tracked tracked = it.next();
            AudioSegment segment = tracked.mSegment;
            boolean complete = segment.isComplete();
            int count = segment.getAudioBufferCount();

            if(listening && !segment.isEncrypted() && !segment.isDoNotMonitor())
            {
                for(int x = tracked.mNextBuffer; x < count; x++)
                {
                    tracked.encode(segment.getAudioBuffer(x));
                }

                if(complete)
                {
                    tracked.flush();
                }

                tracked.publish(hub);
            }
            else
            {
                //Live only: skip the audio that arrived while nobody listened
                tracked.mPending.clear();
            }

            if(count > tracked.mNextBuffer)
            {
                tracked.mLastAudio = now;
            }

            tracked.mNextBuffer = count;

            if(progress && !complete)
            {
                hub.callProgress(segment.getId(), segment.getDuration(), now - tracked.mLastAudio, now);
            }

            if(complete)
            {
                if(hub != null && tracked.mEncoder != null)
                {
                    hub.audioEnded(segment.getId());
                }

                segment.decrementConsumerCount();
                it.remove();
            }
        }
    }

    private void drain()
    {
        AudioSegment segment = mNewSegments.poll();

        while(segment != null)
        {
            mTracked.add(new Tracked(segment));
            segment = mNewSegments.poll();
        }
    }

    /**
     * Count of tracked segments, for tests
     */
    int getTrackedCount()
    {
        return mTracked.size();
    }

    /**
     * One call's audio
     */
    private static class Tracked
    {
        private final AudioSegment mSegment;
        private final Deque<byte[]> mPending = new ArrayDeque<>();
        private OpusStream mEncoder;
        private int mNextBuffer;
        private int mSequence;
        private long mLastAudio = System.currentTimeMillis();

        Tracked(AudioSegment segment)
        {
            mSegment = segment;
        }

        void encode(float[] samples)
        {
            if(mEncoder == null)
            {
                mEncoder = new OpusStream();
            }

            mEncoder.encode(samples, mPending::add);
        }

        void flush()
        {
            if(mEncoder != null)
            {
                mEncoder.flush(mPending::add);
            }
        }

        /**
         * Hands the waiting packets to the hub once it knows the call.
         */
        void publish(LiveHub hub)
        {
            while(!mPending.isEmpty())
            {
                if(!hub.audioPacket(mSegment.getId(), mSequence, mPending.peekFirst()))
                {
                    //Call not published yet: keep the most recent audio for when it is
                    while(mPending.size() > MAX_PENDING_PACKETS)
                    {
                        mPending.pollFirst();
                        mSequence++;
                    }

                    return;
                }

                mPending.pollFirst();
                mSequence++;
            }
        }
    }
}
