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
package record.mp3;

import audio.AudioPacket;
import audio.IAudioPacketListener;
import audio.convert.MP3AudioConverter;
import module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.wave.AudioPacketMonoWaveReader;
import sample.Listener;
import util.TimeStamp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MP3 recorder for converting 8 kHz PCM audio packets to MP3 and writing to .mp3 file.
 */
public class MP3Recorder extends Module implements Listener<AudioPacket>, IAudioPacketListener
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3Recorder.class);

    private MP3AudioConverter mMP3Converter;
    private FileOutputStream mFileOutputStream;
    private String mFilePrefix;
    private Path mPath;

    private BufferProcessor mBufferProcessor;
    private ScheduledFuture<?> mProcessorHandle;
    private LinkedBlockingQueue<AudioPacket> mAudioPacketQueue = new LinkedBlockingQueue<>(500);
    private long mLastBufferReceived;

    private AtomicBoolean mRunning = new AtomicBoolean();

    /**
     * MP3 audio recorder module for converting audio packets to 16 kHz constant bit rate MP3 format and
     * recording to a file.
     */
    public MP3Recorder(String filePrefix)
    {
        mFilePrefix = filePrefix;

        int mp3BitRate = 16; //16 kHz
        boolean variableBitRate = false; //CBR
        mMP3Converter = new MP3AudioConverter(mp3BitRate, variableBitRate);
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

    /**
     * Starts this recorder using as a scheduled thread broadcast running under the executor argument
     *
     * @param executor to use in scheduling MP3 conversion and file writes.
     */
    public void start(ScheduledExecutorService executor)
    {
        if (mRunning.compareAndSet(false, true))
        {
            if (mBufferProcessor == null)
            {
                mBufferProcessor = new BufferProcessor();
            }

            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(mFilePrefix);
                sb.append("_");
                sb.append(TimeStamp.getLongTimeStamp("_"));
                sb.append(".mp3");

                mPath = Paths.get(sb.toString());
                mFileOutputStream = new FileOutputStream(mPath.toFile());

                mLog.info("Created MP3 Recording [" + sb.toString() + "]");

				/* Schedule the handler to run every second */
                mProcessorHandle = executor.scheduleAtFixedRate(mBufferProcessor, 0, 200, TimeUnit.MILLISECONDS);
            }
            catch (IOException io)
            {
                mLog.error("Error starting real buffer recorder", io);
            }
        }
    }

    /**
     * Stops the recorder.
     */
    public void stop()
    {
        mRunning.set(false);

        if (mProcessorHandle != null)
        {
            mProcessorHandle.cancel(true);
        }

        mProcessorHandle = null;

        processAudioPacketQueue();

        close();
    }

    /**
     * Closes the recording file.
     */
    private void close()
    {
        if (mFileOutputStream != null)
        {
            try
            {
                mFileOutputStream.close();
            }
            catch (IOException e)
            {
                mLog.error("Error closing output stream", e);
            }
        }
    }

    /**
     * Primary data insert method designed to accept a stream of audio packets and a final ending audio packet.
     *
     * Audio packets received before the recorder is started or after the recorder is stopped will be ignored.
     */
    @Override
    public void receive(AudioPacket audioPacket)
    {
        if (mRunning.get())
        {
            boolean success = mAudioPacketQueue.offer(audioPacket);

            if (!success)
            {
                mLog.error("recorder buffer overflow - purging [" + mPath.toFile().getAbsolutePath() + "]");

                mAudioPacketQueue.clear();
            }

            mLastBufferReceived = System.currentTimeMillis();
        }
    }

    /**
     * IAudioPacketListener interface method.
     */
    @Override
    public Listener<AudioPacket> getAudioPacketListener()
    {
        return this;
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
     * Processes the audio packet queue.
     */
    private void processAudioPacketQueue()
    {
        try
        {
            List<AudioPacket> mPacketsToProcess = new ArrayList<>();
            mAudioPacketQueue.drainTo(mPacketsToProcess);

            if (!mPacketsToProcess.isEmpty())
            {
                boolean stopRequested = false;
                int stopPacketIndex = 0;

                //An END audio packet is a request to stop the recorder
                for (AudioPacket packet : mPacketsToProcess)
                {
                    if (packet.getType() == AudioPacket.Type.END)
                    {
                        stopRequested = true;
                        stopPacketIndex = mPacketsToProcess.indexOf(packet);
                        continue;
                    }
                }

                if (stopRequested)
                {
                    mRunning.set(false);
                    mAudioPacketQueue.clear();

                    if (stopPacketIndex > 0)
                    {
                        List<AudioPacket> remainingPacketsToProcess = new ArrayList<>();

                        for (int x = 0; x < stopPacketIndex; x++)
                        {
                            remainingPacketsToProcess.add(mPacketsToProcess.get(x));
                        }

                        write(remainingPacketsToProcess);

                        remainingPacketsToProcess.clear();
                    }

                    stop();
                }
                else
                {
                    write(mPacketsToProcess);
                }

                mPacketsToProcess.clear();
            }
        }
        catch (IOException ioe)
        {
            mAudioPacketQueue.clear();

            mLog.error("IOException while processing audio packet queue for MP3 conversion and storage", ioe);
        }
    }

    /**
     * Converts the PCM audio packets to MP3 format and writes the converted audio to the output stream.
     */
    private void write(List<AudioPacket> audioPackets) throws IOException
    {
        byte[] mp3Audio = mMP3Converter.convert(audioPackets);

        mFileOutputStream.write(mp3Audio);
    }

    public class BufferProcessor implements Runnable
    {
        private AtomicBoolean mProcessing = new AtomicBoolean();

        public void run()
        {
            if (mProcessing.compareAndSet(false, true))
            {
                processAudioPacketQueue();

                mProcessing.set(false);
            }
        }
    }

    public static void main(String[] args)
    {
        Path path = Paths.get("/home/denny/Music/PCM.wav");
        mLog.debug("Opening: " + path.toString());

        final MP3Recorder recorder = new MP3Recorder("/home/denny/Music/denny_test");
        recorder.start(Executors.newSingleThreadScheduledExecutor());

        try (AudioPacketMonoWaveReader reader = new AudioPacketMonoWaveReader(path, true))
        {
            reader.setListener(recorder);
            reader.read();
            recorder.stop();
        }
        catch (IOException e)
        {
            mLog.error("Error", e);
        }

        mLog.debug("Finished");
    }
}
