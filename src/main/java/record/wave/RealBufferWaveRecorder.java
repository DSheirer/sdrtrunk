/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package record.wave;

import module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.ConversionUtils;
import sample.Listener;
import sample.real.IFilteredRealBufferListener;
import sample.real.RealBuffer;
import util.TimeStamp;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WAVE audio recorder module for recording real sample buffers to a wave file
 */
public class RealBufferWaveRecorder extends Module
    implements IFilteredRealBufferListener, Listener<RealBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(RealBufferWaveRecorder.class);

    private WaveWriter mWriter;
    private String mFilePrefix;
    private Path mFile;
    private AudioFormat mAudioFormat;

    private BufferProcessor mBufferProcessor;
    private ScheduledFuture<?> mProcessorHandle;
    private LinkedBlockingQueue<RealBuffer> mBuffers = new LinkedBlockingQueue<>(500);
    private long mLastBufferReceived;

    private AtomicBoolean mRunning = new AtomicBoolean();

    public RealBufferWaveRecorder(int sampleRate, String filePrefix)
    {
        mAudioFormat = new AudioFormat(sampleRate,  //SampleRate
            16,     //Sample Size
            1,      //Channels
            true,   //Signed
            false); //Little Endian

        mFilePrefix = filePrefix;
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

    public Path getFile()
    {
        return mFile;
    }

    public void start(ScheduledExecutorService executor)
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mBufferProcessor == null)
            {
                mBufferProcessor = new BufferProcessor();
            }

            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(mFilePrefix);
                sb.append("_");
                sb.append(TimeStamp.getLongTimeStamp("_"));
                sb.append(".wav");

                mFile = Paths.get(sb.toString());

                mWriter = new WaveWriter(mAudioFormat, mFile);

				/* Schedule the processor to run every 500 milliseconds */
                mProcessorHandle = executor.scheduleAtFixedRate(mBufferProcessor, 0, 500, TimeUnit.MILLISECONDS);
            }
            catch(IOException io)
            {
                mLog.error("Error starting real buffer recorder", io);
            }
        }
    }

    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            try
            {
                write();

                if(mWriter != null)
                {
                    mWriter.close();
                    mWriter = null;
                }
            }
            catch(IOException ioe)
            {
                mLog.error("Error writing final audio buffers to recording during shutdown", ioe);
            }
        }
    }

    @Override
    public void receive(RealBuffer buffer)
    {
        if(mRunning.get())
        {
            boolean success = mBuffers.offer(buffer);

            if(!success)
            {
                mLog.error("recorder buffer overflow - purging [" + mFile.toFile().getAbsolutePath() + "]");
                mBuffers.clear();
            }

            mLastBufferReceived = System.currentTimeMillis();
        }
    }

    @Override
    public Listener<RealBuffer> getFilteredRealBufferListener()
    {
        return this;
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
     * Writes all audio currently in the queue to the file
     * @throws IOException if there are any errors writing the audio
     */
    private void write() throws IOException
    {
        RealBuffer buffer = mBuffers.poll();

        while(buffer != null)
        {
            mWriter.write(ConversionUtils.convertToSigned16BitSamples(buffer));
            buffer = mBuffers.poll();
        }
    }

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
                mBuffers.clear();
                stop();

                mLog.error("IO Exception while trying to write to the wave writer", ioe);
            }
        }
    }
}
