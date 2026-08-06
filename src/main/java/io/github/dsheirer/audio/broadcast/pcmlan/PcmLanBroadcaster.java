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
package io.github.dsheirer.audio.broadcast.pcmlan;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.IAudioSegmentBroadcaster;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.util.ThreadPool;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streams 16-bit signed little-endian mono PCM audio over a TCP client connection.  Designed to feed
 * OpenToneDetect's lan_stream input source (or any compatible raw PCM TCP listener).
 *
 * The remote process is the TCP server; this broadcaster is the client.  Audio is sourced from
 * AudioSegment buffers in real time as they arrive from the decoder, not after segment completion.
 */
public class PcmLanBroadcaster extends AbstractAudioBroadcaster<PcmLanConfiguration>
    implements IAudioSegmentBroadcaster
{
    private final static Logger mLog = LoggerFactory.getLogger(PcmLanBroadcaster.class);
    private static final long RECONNECT_INTERVAL_MS = 3000;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final long PROCESSOR_INTERVAL_MS = 50;

    private final LinkedTransferQueue<AudioSegment> mNewSegmentQueue = new LinkedTransferQueue<>();
    private final List<ActiveSegment> mActiveSegments = new ArrayList<>();
    private final AtomicBoolean mConnecting = new AtomicBoolean();
    private final AtomicBoolean mProcessing = new AtomicBoolean();

    private volatile Socket mSocket;
    private volatile OutputStream mOutputStream;
    private long mLastConnectAttempt = 0;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture<?> mProcessorTask;

    public PcmLanBroadcaster(PcmLanConfiguration configuration)
    {
        super(configuration);
    }

    @Override
    public void start()
    {
        if(mProcessorTask == null)
        {
            mScheduler = Executors.newSingleThreadScheduledExecutor(
                new NamingThreadFactory("sdrtrunk pcm-lan broadcaster"));
            mProcessorTask = mScheduler.scheduleAtFixedRate(this::runProcessor,
                0, PROCESSOR_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        setBroadcastState(BroadcastState.READY);
    }

    @Override
    public void stop()
    {
        if(mProcessorTask != null)
        {
            mProcessorTask.cancel(true);
            mProcessorTask = null;
        }

        if(mScheduler != null)
        {
            mScheduler.shutdownNow();
            mScheduler = null;
        }

        tearDownConnection();
        drainAllSegments();

        if(!getBroadcastState().isErrorState())
        {
            setBroadcastState(BroadcastState.DISCONNECTED);
        }
    }

    @Override
    public void dispose()
    {
        stop();
    }

    @Override
    public int getAudioQueueSize()
    {
        return mNewSegmentQueue.size() + mActiveSegments.size();
    }

    /**
     * No-op.  This broadcaster consumes raw AudioSegments directly rather than completed MP3 recordings.
     */
    @Override
    public void receive(AudioRecording audioRecording)
    {
    }

    /**
     * Receives an AudioSegment dispatched by BroadcastModel.  The dispatcher has already incremented the
     * consumer count for this broadcaster, so we own one decrement responsibility.
     */
    @Override
    public void receiveAudioSegment(AudioSegment audioSegment)
    {
        if(audioSegment != null)
        {
            mNewSegmentQueue.add(audioSegment);
        }
    }

    private void runProcessor()
    {
        if(!mProcessing.compareAndSet(false, true))
        {
            return;
        }

        try
        {
            processQueue();
        }
        catch(Throwable t)
        {
            mLog.error("Error processing pcm-lan audio queue", t);
        }
        finally
        {
            mProcessing.set(false);
        }
    }

    private void processQueue()
    {
        boolean connected = isConnected();

        //Drain newly arrived segments
        AudioSegment newSegment;
        while((newSegment = mNewSegmentQueue.poll()) != null)
        {
            if(connected)
            {
                mActiveSegments.add(new ActiveSegment(newSegment));
            }
            else
            {
                //Not connected - drop the segment and release our consumer reference
                newSegment.decrementConsumerCount();
            }
        }

        if(!connected)
        {
            //Free any in-progress segments since we cannot stream them
            drainActiveSegments();
            attemptConnectIfDue();
            return;
        }

        Iterator<ActiveSegment> it = mActiveSegments.iterator();
        while(it.hasNext())
        {
            ActiveSegment active = it.next();
            int total = active.segment.getAudioBufferCount();

            try
            {
                while(active.nextBufferIndex < total)
                {
                    float[] buffer = active.segment.getAudioBuffer(active.nextBufferIndex++);
                    writePcm(resample(buffer));
                }
            }
            catch(IOException ioe)
            {
                mLog.warn("PCM-LAN write failed - tearing down connection: " + ioe.getMessage());
                tearDownConnection();
                //Drop the current segment and any remaining active segments
                active.segment.decrementConsumerCount();
                it.remove();
                drainActiveSegments();
                return;
            }

            if(active.segment.isComplete() && active.nextBufferIndex >= active.segment.getAudioBufferCount())
            {
                active.segment.decrementConsumerCount();
                it.remove();
                incrementStreamedAudioCount();
            }
        }

        //Flush at the end of each tick to keep latency low.  No-op if no data was written.
        try
        {
            OutputStream out = mOutputStream;
            if(out != null)
            {
                out.flush();
            }
        }
        catch(IOException ioe)
        {
            mLog.warn("PCM-LAN flush failed - tearing down connection: " + ioe.getMessage());
            tearDownConnection();
            drainActiveSegments();
        }
    }

    private void writePcm(float[] samples) throws IOException
    {
        OutputStream out = mOutputStream;
        if(out == null)
        {
            throw new IOException("Output stream is closed");
        }

        byte[] bytes = new byte[samples.length * 2];
        for(int i = 0; i < samples.length; i++)
        {
            int s = Math.round(samples[i] * 32767.0f);
            if(s > 32767) s = 32767;
            else if(s < -32768) s = -32768;
            bytes[2 * i] = (byte)(s & 0xFF);
            bytes[2 * i + 1] = (byte)((s >> 8) & 0xFF);
        }
        out.write(bytes);
    }

    private boolean isConnected()
    {
        Socket s = mSocket;
        return s != null && s.isConnected() && !s.isClosed() && mOutputStream != null;
    }

    private void attemptConnectIfDue()
    {
        long now = System.currentTimeMillis();
        if(now - mLastConnectAttempt < RECONNECT_INTERVAL_MS)
        {
            return;
        }

        if(!getBroadcastConfiguration().isValid())
        {
            setBroadcastState(BroadcastState.INVALID_SETTINGS);
            return;
        }

        if(!mConnecting.compareAndSet(false, true))
        {
            return;
        }

        mLastConnectAttempt = now;
        setBroadcastState(BroadcastState.CONNECTING);

        ThreadPool.CACHED.submit(() -> {
            Socket socket = null;
            try
            {
                socket = new Socket();
                socket.connect(new InetSocketAddress(getBroadcastConfiguration().getHost(),
                    getBroadcastConfiguration().getPort()), CONNECT_TIMEOUT_MS);
                socket.setTcpNoDelay(true);

                OutputStream out = new BufferedOutputStream(socket.getOutputStream(), 8192);
                mSocket = socket;
                mOutputStream = out;
                setBroadcastState(BroadcastState.CONNECTED);
            }
            catch(IOException ioe)
            {
                mLog.info("PCM-LAN connection to " + getBroadcastConfiguration().getHost() + ":" +
                    getBroadcastConfiguration().getPort() + " failed: " + ioe.getMessage());
                closeQuietly(socket);
                setBroadcastState(BroadcastState.NETWORK_UNAVAILABLE);
            }
            finally
            {
                mConnecting.set(false);
            }
        });
    }

    private void tearDownConnection()
    {
        OutputStream out = mOutputStream;
        Socket s = mSocket;
        mOutputStream = null;
        mSocket = null;

        if(out != null)
        {
            try { out.close(); } catch(IOException ignore) {}
        }
        closeQuietly(s);

        if(!getBroadcastState().isErrorState())
        {
            setBroadcastState(BroadcastState.DISCONNECTED);
        }
    }

    private void drainActiveSegments()
    {
        for(ActiveSegment active : mActiveSegments)
        {
            active.segment.decrementConsumerCount();
        }
        mActiveSegments.clear();
    }

    private void drainAllSegments()
    {
        AudioSegment seg;
        while((seg = mNewSegmentQueue.poll()) != null)
        {
            seg.decrementConsumerCount();
        }
        drainActiveSegments();
    }

    private static void closeQuietly(Socket s)
    {
        if(s != null)
        {
            try { s.close(); } catch(IOException ignore) {}
        }
    }

    private float[] resample(float[] input)
    {
        int targetRate = getBroadcastConfiguration().getOutputSampleRate();
        if(targetRate == 8000 || targetRate <= 0)
        {
            return input;
        }

        int inputLength = input.length;
        int outputLength = (int)Math.ceil(inputLength * (double)targetRate / 8000.0);
        if(outputLength <= 0)
        {
            return input;
        }

        float[] output = new float[outputLength];
        double ratio = 8000.0 / targetRate;

        for(int i = 0; i < outputLength; i++)
        {
            double srcIndex = i * ratio;
            int index0 = (int)srcIndex;
            int index1 = Math.min(index0 + 1, inputLength - 1);
            float fraction = (float)(srcIndex - index0);
            output[i] = input[index0] * (1.0f - fraction) + input[index1] * fraction;
        }

        return output;
    }

    private static class ActiveSegment
    {
        final AudioSegment segment;
        int nextBufferIndex;

        ActiveSegment(AudioSegment segment)
        {
            this.segment = segment;
            this.nextBufferIndex = 0;
        }
    }
}
