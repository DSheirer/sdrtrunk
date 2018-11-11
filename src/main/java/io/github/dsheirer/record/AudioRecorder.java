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
package io.github.dsheirer.record;

import io.github.dsheirer.audio.IAudioPacketListener;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AudioRecorder extends Module implements Listener<ReusableAudioPacket>, IAudioPacketListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioRecorder.class);

    private LinkedBlockingQueue<ReusableAudioPacket> mAudioPacketQueue = new LinkedBlockingQueue<>(500);
    private List<ReusableAudioPacket> mPacketsToProcess = new ArrayList<>();

    private FileOutputStream mFileOutputStream;
    private AtomicBoolean mRunning = new AtomicBoolean();

    protected Path mPath;
    protected IdentifierCollection mIdentifierCollection;
    protected long mTimeRecordingStart;
    protected long mTimeLastPacketReceived;
    private BufferProcessor mBufferProcessor;
    private ScheduledFuture<?> mProcessorHandle;
    private Listener<AudioRecorder> mRecordingClosedListener;

    private long mSampleCount;

    /**
     * Abstract audio recorder that implements audio packet queueing and threaded audio conversion/writing to a file
     *
     * @param path for the output recording
     */
    public AudioRecorder(Path path)
    {
        mPath = path;
    }

    /**
     * Path for the audio recording file
     */
    public Path getPath()
    {
        return mPath;
    }

    /**
     * Latest audio identifier collection received for this recording
     */
    public IdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Timestamp of the last buffer received by this recorder - allows this recorder to be monitored for automatic
     * closure after a time period has elapsed.
     */
    public long getTimeLastPacketReceived()
    {
        return mTimeLastPacketReceived;
    }

    public long getTimeRecordingStart()
    {
        return mTimeRecordingStart;
    }

    /**
     * Recording length in milliseconds
     */
    public long getRecordingLength()
    {
        //Assumes audio sample rate of 8000 samples/second or 8 samples/milli-second
        return mSampleCount / 8;
    }

    /**
     * Implements the IAudioPacketListener interface and simply redirects to the Listener<ReusableAudioPacket> interface.
     * This is necessary since you can't have multiple methods with the same erasure (ie Listener<xxx>) in the
     * parent module class.
     */
    @Override
    public Listener<ReusableAudioPacket> getAudioPacketListener()
    {
        return this;
    }

    /**
     * Processes the audio packet and captures the latest Metadata for the recording for easy access.
     */
    @Override
    public void receive(ReusableAudioPacket audioPacket)
    {
        if(mRunning.get())
        {
            mTimeRecordingStart = System.currentTimeMillis();
            mTimeLastPacketReceived = mTimeRecordingStart;

            if(audioPacket.hasIdentifierCollection())
            {
                mIdentifierCollection = audioPacket.getIdentifierCollection();
            }

            boolean success = mAudioPacketQueue.offer(audioPacket);

            if(!success)
            {
                mLog.error("recorder buffer overflow - stopping recorder [" + getPath().toString() + "]");
                stop();
                audioPacket.decrementUserCount();
            }
        }
        else
        {
            audioPacket.decrementUserCount();
        }
    }

    /**
     * File output stream for the current recording.  Intended to allow sub-classes to write binary data to the file.
     */
    protected OutputStream getOutputStream()
    {
        return mFileOutputStream;
    }

    /**
     * Stops the recorder and flags the recording to be closed.  Use this method if you do not need any details about
     * the final recording.  Otherwise, use the close() method and register a closing listener.
     */
    public void stop()
    {
        close(null);
    }

    /**
     * Closes the recording file.  Upon successful closing of the recording file, the listener is notified that the
     * audio recorder is closed.  There is potential for the calling thread (here) and the buffer processor thread to
     * both inform the recording closed listener that the recording is ended.  So, we synchronize on the listener and
     * the first thread to get the lock informs the listener and then nullifies the listener pointer so that if the
     * second thread attempts a duplicate notification the listener would be null at that point.
     */
    public void close(Listener<AudioRecorder> listener)
    {
        mRecordingClosedListener = listener;

        if(!mRunning.compareAndSet(true, false))
        {
            synchronized(mRecordingClosedListener)
            {
                if(mRecordingClosedListener != null)
                {
                    mRecordingClosedListener.receive(AudioRecorder.this);
                    mRecordingClosedListener = null;
                }
            }
        }
    }


    /**
     * Records the list of audio packets in the sub-class specific audio format.
     */
    protected abstract void record(List<ReusableAudioPacket> audioPackets) throws IOException;

    /**
     * Starts this recorder as a scheduled thread running under the executor argument
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mTimeLastPacketReceived = System.currentTimeMillis();

            if(mBufferProcessor == null)
            {
                mBufferProcessor = new BufferProcessor();
            }

            try
            {
                mFileOutputStream = new FileOutputStream(mPath.toFile());

                /* Schedule the handler to run every half second */
                mProcessorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(mBufferProcessor, 0, 500, TimeUnit.MILLISECONDS);
            }
            catch(IOException io)
            {
                mLog.error("Error starting audio recorder [" + getPath().toString() + "]", io);

                mRunning.set(false);
            }
        }
    }

    /**
     * Processes the audio packet queue.
     */
    private void processAudioPacketQueue()
    {
        mAudioPacketQueue.drainTo(mPacketsToProcess);

        if(!mPacketsToProcess.isEmpty())
        {
            try
            {
                record(mPacketsToProcess);

                for(ReusableAudioPacket packet : mPacketsToProcess)
                {
                    if(packet.getType() == ReusableAudioPacket.Type.AUDIO)
                    {
                        mSampleCount += packet.getAudioSamples().length;
                    }

                    packet.decrementUserCount();
                }
            }
            catch(IOException ioe)
            {
                mLog.debug("Error while recording audio to [" + getPath().toString() + "] - stopping recorder");
                stop();
            }

            mPacketsToProcess.clear();
        }
    }

    /**
     * Disposes this audio recorder and prepares it for reclamation
     */
    @Override
    public void dispose()
    {
        stop();
    }

    /**
     * Not implemented.  Recorder modules are not appropriate for reset and reuse.
     */
    @Override
    public void reset()
    {
    }

    /**
     * Flushes any remaining audio to the output file.  This method should be implemented by subclasses where an audio
     * converter may contain residual frame data that should be flushed to disk before closing the audio file.
     */
    protected void flush()
    {
    }

    /**
     * Drains the audio packet queue and records the audio packets to file
     */
    public class BufferProcessor implements Runnable
    {
        private AtomicBoolean mProcessing = new AtomicBoolean();

        public void run()
        {
            if(mProcessing.compareAndSet(false, true))
            {
                processAudioPacketQueue();

                //If we've been stopped or closed, finish the queue, close the recording, notify the listener, and
                // cancel the future
                if(!mRunning.get())
                {
                    //Allow sub-classes to flush remaining audio frame data to disk.
                    flush();

                    if(mFileOutputStream != null)
                    {
                        try
                        {
                            mFileOutputStream.flush();
                            mFileOutputStream.close();
                        }
                        catch(IOException e)
                        {
                            mLog.error("Error closing output stream", e);
                        }
                    }

                    synchronized(mRecordingClosedListener)
                    {
                        if(mRecordingClosedListener != null)
                        {
                            mRecordingClosedListener.receive(AudioRecorder.this);
                            mRecordingClosedListener = null;
                        }
                    }

                    if(mProcessorHandle != null)
                    {
                        mProcessorHandle.cancel(false);
                        mProcessorHandle = null;
                    }
                }

                mProcessing.set(false);
            }
        }
    }
}
