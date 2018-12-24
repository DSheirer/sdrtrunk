/*
 * *********************************************************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 * *********************************************************************************************************************
 */
package io.github.dsheirer.record.wave;

import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.OverflowableReusableBufferTransferQueue;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WAVE audio recorder module for recording audio buffers to a wave file
 */
public class AudioPacketWaveRecorder extends Module implements Listener<ReusableAudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioPacketWaveRecorder.class);

    private WaveWriter mWriter;
    private Path mPath;
    private AudioFormat mAudioFormat;

    private OverflowableReusableBufferTransferQueue<ReusableAudioPacket> mTransferQueue =
        new OverflowableReusableBufferTransferQueue<>(500, 100);
    private BufferProcessor mBufferProcessor;
    private ScheduledFuture<?> mProcessorHandle;
    private long mLastBufferReceived;
    private List<ReusableAudioPacket> mAudioPacketsToProcess = new ArrayList<>();
    private IdentifierCollection mIdentifierCollection;
    private AtomicBoolean mRunning = new AtomicBoolean();

    /**
     * Wave audio recorder for AudioPackets
     *
     * @param path for the recording file
     */
    public AudioPacketWaveRecorder(Path path)
    {
        mPath = path;
        mAudioFormat = AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO;
    }

    /**
     * Identifier collection harvested from the most recent audio packet.
     * @return identifier collection or null.
     */
    public IdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Indicates if the recorder is currently running.
     */
    public boolean isRunning()
    {
        return mRunning.get();
    }

    /**
     * Timestamp of when the latest buffer was received by this recorder
     */
    public long getLastBufferReceived()
    {
        return mLastBufferReceived;
    }

    public Path getPath()
    {
        return mPath;
    }

    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mBufferProcessor == null)
            {
                mBufferProcessor = new BufferProcessor();
            }

            try
            {
                mWriter = new WaveWriter(mAudioFormat, mPath);

				/* Schedule the processor to run every 500 milliseconds */
                mProcessorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(mBufferProcessor, 0, 500, TimeUnit.MILLISECONDS);
            }
            catch(IOException io)
            {
                mLog.error("Error starting real buffer recorder", io);
            }
        }
    }

    @Override
    public void stop()
    {
        stop(null, null);
    }

    /**
     * Stops the recorder and optionally renames the file to the specified path argument and/or writes
     * the metadata to the recording.
     *
     * Note: both renaming path and wave metadata can be null.  If no renaming path is specified, the
     * original path name will remain for the audio file.
     *
     * @param path (optional) to rename the audio file.
     * @param waveMetadata (optional) to include in the recording
     */
    public void stop(Path path, WaveMetadata waveMetadata)
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mProcessorHandle != null)
            {
                mProcessorHandle.cancel(false);
                mProcessorHandle = null;
            }

            try
            {
                //Finish writing any residual audio buffers
                write();

                //Append the LIST and ID3 metadata to the end
                if(waveMetadata != null)
                {
                    mWriter.writeMetadata(waveMetadata);
                }

                if(mWriter != null)
                {
                    mWriter.close(path);
                    mWriter = null;
                }
            }
            catch(IOException ioe)
            {
                mLog.error("Error writing final audio buffers to recording during shutdown", ioe);
            }
        }
    }

    /**
     * Primary input method for receiving audio packets to enqueue for later writing to the wav file.
     */
    @Override
    public void receive(ReusableAudioPacket audioPacket)
    {
        if(mRunning.get())
        {
            mTransferQueue.offer(audioPacket);
            mLastBufferReceived = System.currentTimeMillis();
        }
        else
        {
            audioPacket.decrementUserCount();
        }
    }

    @Override
    public void dispose()
    {
        stop();
    }

    @Override
    public void reset()
    {
    }

    /**
     * Writes all audio currently in the queue to the file.  Captures any audio metadata from the packet and retains a
     * copy of the latest metadata to append to the end of the recording when stop() is invoked.
     *
     * @throws IOException if there are any errors writing the audio
     */
    private synchronized void write() throws IOException
    {
        mTransferQueue.drainTo(mAudioPacketsToProcess);

        for(ReusableAudioPacket audioPacket: mAudioPacketsToProcess)
        {
            if(audioPacket.getType() == ReusableAudioPacket.Type.AUDIO)
            {
                if(audioPacket.hasIdentifierCollection())
                {
                    mIdentifierCollection = audioPacket.getIdentifierCollection();
                }

                mWriter.writeData(ConversionUtils.convertToSigned16BitSamples(audioPacket.getAudioSamples()));
            }

            try
            {
                audioPacket.decrementUserCount();
            }
            catch(IllegalStateException ise)
            {
                mLog.error("Error while decrementing user count on audio packet while writing data to recording file", ise);
            }
        }

        mAudioPacketsToProcess.clear();
    }

    /**
     * Scheduled runnable to periodically write the enqueued audio packets to the file
     */
    public class BufferProcessor implements Runnable
    {
        public void run()
        {
            try
            {
                write();
            }
            catch(IOException ioe)
            {
				/* Stop this module if/when we get an IO exception */
                mTransferQueue.clear();
                stop();

                mLog.error("IO Exception while trying to write to the wave writer", ioe);
            }
        }
    }
}
