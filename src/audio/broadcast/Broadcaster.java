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
import audio.convert.IAudioConverter;
import controller.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Broadcaster implements IAudioPacketListener, Listener<AudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger( Broadcaster.class );

    private ThreadPoolManager mThreadPoolManager;
    private ScheduledFuture mScheduledTask;

    private AudioQueueProcessor mAudioQueueProcessor;
    protected LinkedTransferQueue<AudioPacket> mAudioQueue = new LinkedTransferQueue<>();

    private List<Listener<BroadcastState>> mBroadcastStateListeners = new CopyOnWriteArrayList<>();
    private BroadcastState mBroadcastState = BroadcastState.READY;

    private BroadcastConfiguration mBroadcastConfiguration;
    private IAudioConverter mAudioConverter;

    private boolean mEnabled = true;

    /**
     * Broadcaster for sending audio to a remote broadcast server.
     */
    public Broadcaster(ThreadPoolManager threadPoolManager,
                       BroadcastConfiguration broadcastConfiguration,
                       IAudioConverter audioConverter)
    {
        mThreadPoolManager = threadPoolManager;
        mBroadcastConfiguration = broadcastConfiguration;
        mAudioConverter = audioConverter;
        mAudioQueueProcessor = new AudioQueueProcessor();
    }

    /**
     * Broadcast configuration used by this broadcaster
     */
    public BroadcastConfiguration getBroadcastConfiguration()
    {
        return mBroadcastConfiguration;
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
     * Audio converter used by this broadcaster to convert PCM audio packets to the desired output format.
     */
    protected IAudioConverter getAudioConverter()
    {
        return mAudioConverter;
    }

    /**
     * Commands the broadcaster to broadcast any queued audio packets.  This method is invoked by a scheduled thread
     * pool task to periodically command the sub-class to broadcast queued audio packets.
     */
    protected abstract void broadcast();

    /**
     * Disconnects the broadcaster and halts all broadcasting.
     */
    public void stop()
    {
        mEnabled = false;
        stopProcessor();
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
                        mAudioQueueProcessor, 250, TimeUnit.MILLISECONDS );
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
     * Pauses or unpauses the broadcast
     */
    public void setPaused(boolean paused)
    {
        if(!getBroadcastState().isErrorState())
        {
            if(paused)
            {
                setBroadcastState(BroadcastState.PAUSED);
            }
            else
            {
                setBroadcastState(BroadcastState.READY);
            }
        }
    }

    /**
     * Registers the listener to receive broadcast state changes
     */
    public void addListener(Listener<BroadcastState> listener)
    {
        mBroadcastStateListeners.add(listener);
    }

    /**
     * Removes the listener from receiving broadcast state changes
     */
    public void removeListener(Listener<BroadcastState> listener)
    {
        mBroadcastStateListeners.remove(listener);
    }

    /**
     * Sets the state of the broadcast connection
     */
    protected void setBroadcastState(BroadcastState state)
    {
        if(mBroadcastState != state)
        {
            mLog.debug("Changing State to: " + state);
            mBroadcastState = state;

            for(Listener<BroadcastState> listener: mBroadcastStateListeners)
            {
                listener.receive(state);
            }
        }
    }

    /**
     * Current state of the broadcast connection
     */
    public BroadcastState getBroadcastState()
    {
        return mBroadcastState;
    }

    public boolean connected()
    {
        return getBroadcastState() == BroadcastState.CONNECTED;
    }

    /**
     * Indicates if this broadcaster can connect and is not currently in an error state or a connected state.
     */
    public boolean canConnect()
    {
        BroadcastState state = getBroadcastState();

        return state != BroadcastState.CONNECTED && !state.isErrorState();
    }


    /**
     * Audio packet queue processor.  Runs via a scheduled thread pool task as a timing processor to instruct
     * sub-classes to broadcast queued audio packets.  Internal state monitor ensures that subsequent invocations of
     * the broadcast method do not start until the previous invocation completes.
     */
    public class AudioQueueProcessor implements Runnable
    {
        private AtomicBoolean mProcessing = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                broadcast();

                mProcessing.set(false);
            }
        }
    }
}
