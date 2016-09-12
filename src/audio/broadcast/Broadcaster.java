/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.broadcast;

import audio.AudioPacket;
import audio.IAudioPacketListener;
import audio.broadcast.configuration.BroadcastConfiguration;
import audio.broadcast.handler.BroadcastHandler;
import audio.broadcast.handler.BroadcastState;
import controller.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Broadcaster implements IAudioPacketListener, Listener<AudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger( Broadcaster.class );

    private ThreadPoolManager mThreadPoolManager;
    private ScheduledFuture mScheduledTask;
    private AudioQueueProcessor mAudioQueueProcessor;
    private LinkedTransferQueue<AudioPacket> mAudioQueue = new LinkedTransferQueue<>();
    private BroadcastHandler mBroadcastHandler;
    private boolean mEnabled = true;

    /**
     * Broadcaster for sending broadcast audio to a remote broadcast server.
     */
    public Broadcaster(ThreadPoolManager threadPoolManager, BroadcastHandler broadcastHandler)
    {
        mThreadPoolManager = threadPoolManager;
        mBroadcastHandler = broadcastHandler;
        mAudioQueueProcessor = new AudioQueueProcessor();

        //Add broadcast state listener to stop processing if we have an error
        mBroadcastHandler.addListener(new BroadcastStateMonitor());
    }

    /**
     * Broadcast configuration used by this broadcaster
     */
    public BroadcastConfiguration getBroadcastConfiguration()
    {
        return mBroadcastHandler.getBroadcastConfiguration();
    }

    /**
     * Current state of the broadcaster's connection with the remote server
     */
    public BroadcastState getBroadcastState()
    {
        return mBroadcastHandler.getBroadcastState();
    }

    /**
     * Registers the listener to receive broadcast state changes
     */
    public void addListener(Listener<BroadcastState> listener)
    {
        mBroadcastHandler.addListener(listener);
    }

    /**
     * Removes the listener from receiving broadcast state changes
     */
    public void removeListener(Listener<BroadcastState> listener)
    {
        mBroadcastHandler.removeListener(listener);
    }

    /**
     * IAudioPacketListener interface method
     */
    @Override
    public Listener<AudioPacket> getAudioPacketListener()
    {
        return this;
    }

    /**
     * Disconnects the broadcaster and halts all broadcasting.
     */
    public void stop()
    {
        mEnabled = false;
        stopProcessor();
        mBroadcastHandler.disconnect();
    }

    /**
     * Pauses the broadcaster.
     * @param paused set to true to pause and false to continue
     */
    public void setPaused(boolean paused)
    {
        if(mEnabled)
        {
            mBroadcastHandler.setPaused(paused);
        }
    }

    /**
     * Primary audio packet input method.  Audio packets are queued and then processed by the underlying broadcast
     * handler for broadcast to the remote server.
     */
    public void receive(AudioPacket packet)
    {
        if(mEnabled)
        {
            startProcessor();
            mAudioQueue.add(packet);
        }
    }

    /**
     * Starts the audio queue processor.  The audio packet queue is serviced every 100 milliseconds.  Subsequent calls
     * to this method once running are ignored.
     */
    private void startProcessor()
    {
        if(mEnabled && mScheduledTask == null)
        {
            if(mThreadPoolManager != null)
            {
                mScheduledTask = mThreadPoolManager.scheduleFixedRate(ThreadPoolManager.ThreadType.AUDIO_PROCESSING,
                        mAudioQueueProcessor, 100, TimeUnit.MILLISECONDS );
            }
        }
    }

    /**
     * Stops the audio queue processor
     */
    private void stopProcessor()
    {
        if(mThreadPoolManager != null && mScheduledTask != null)
        {
            mThreadPoolManager.cancel(mScheduledTask);
        }
    }

    /**
     * Monitors the broadcast handler for error state and halts audio broadcasting
     */
    public class BroadcastStateMonitor implements Listener<BroadcastState>
    {
        @Override
        public void receive(BroadcastState broadcastState)
        {
            if(broadcastState.isErrorState())
            {
                stop();
            }
        }
    }

    /**
     * Processes audio packets from the audio packet queue and broadcasts the audio
     */
    public class AudioQueueProcessor implements Runnable
    {
        private AtomicBoolean mProcessing = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                List<AudioPacket> audioPackets = new ArrayList<>();

                mAudioQueue.drainTo(audioPackets);

                if(!audioPackets.isEmpty())
                {
                    mBroadcastHandler.broadcast(audioPackets);
                }

                mProcessing.set(false);
            }
        }
    }
}
