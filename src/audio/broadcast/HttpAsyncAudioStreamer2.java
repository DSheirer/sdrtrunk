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
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpAsyncAudioStreamer2 implements HttpAsyncRequestProducer,
            IAudioPacketListener, Listener<AudioPacket>

{
    private final static Logger mLog = LoggerFactory.getLogger( HttpAsyncAudioStreamer2.class );

    private HttpHost mHttpHost;
    private HttpRequest mHttpRequest;
    private LinkedBlockingQueue<AudioPacket> mAudioQueue;
    private IAudioConverter mIAudioConverter;
    private IOControl mIOControl;
    private AtomicBoolean mSuspended = new AtomicBoolean();

    public HttpAsyncAudioStreamer2(HttpHost httpHost, HttpRequest httpRequest,
                                   IAudioConverter audioConverter, int queueSize)
    {
        mHttpHost = httpHost;
        mHttpRequest = httpRequest;
        mIAudioConverter = audioConverter;
        mAudioQueue = new LinkedBlockingQueue<>(queueSize);
    }

    /**
     * Queues the audio packet for conversion and streaming.  Audio packets are assumed to arrive in sequence.
     */
    @Override
    public void receive(AudioPacket audioPacket)
    {
        //Insert the packet in the queue.  If the queue is currently full, remove the head element and attempt
        //to reinsert the packet.
        if(!mAudioQueue.offer(audioPacket))
        {
            mLog.debug("Streamer - making room to add an audio packet - suspended:" + mSuspended.get() + " iocontrol null:" + (mIOControl == null));
            mAudioQueue.poll();
            mAudioQueue.offer(audioPacket);
        }

        if(mIOControl != null && mSuspended.compareAndSet(true, false))
        {
            mLog.debug("Streamer - content available, resuming streaming");
            mIOControl.requestOutput();
        }
    }

    @Override
    public Listener<AudioPacket> getAudioPacketListener()
    {
        return this;
    }


    @Override
    public HttpHost getTarget()
    {
        mLog.debug("Streamer - getTarget() invoked");
        return mHttpHost;
    }

    @Override
    public HttpRequest generateRequest() throws IOException, HttpException
    {
        mLog.debug("Streamer - generateRequest() invoked");
        return mHttpRequest;
    }

    @Override
    public void requestCompleted(HttpContext httpContext)
    {
        mLog.debug("Streamer - requestCompleted() invoked");
    }

    @Override
    public void failed(Exception e)
    {
        mLog.error("Streamer - failed() invoked", e);
    }

    @Override
    public void resetRequest() throws IOException
    {
        mLog.debug("Streamer - resetRequest() invoked");
    }

    @Override
    public void produceContent(ContentEncoder contentEncoder, IOControl ioControl) throws IOException
    {
        //IOControl is thread safe, so we'll keep a reference to it so that we can do flow control
        if(mIOControl == null)
        {
            mIOControl = ioControl;
        }

        AudioPacket packet = mAudioQueue.poll();

        if(packet != null)
        {
            List<AudioPacket> packets = new ArrayList<>();
            packets.add(packet);
            byte[] converted = mIAudioConverter.convert(packets);
            mLog.debug("Produce Content - writing converted audio: " + converted.length + " to:" + contentEncoder.getClass());
            int wrote = contentEncoder.write(ByteBuffer.wrap(converted));

            mLog.debug("We wrote [" + wrote + "] bytes to the content encoder");
        }
        else
        {
            mLog.debug("Our audio packet was null");
        }

        if(mAudioQueue.isEmpty() && mSuspended.compareAndSet(false, true))
        {
            mLog.debug("Produce Content - suspending output");
            mIOControl.suspendOutput();
        }

//        List<AudioPacket> packets = new ArrayList<>();
//        mAudioQueue.drainTo(packets);
//
//        if(packets.isEmpty() && mSuspended.compareAndSet(false, true))
//        {
//            mLog.debug("Produce Content - no audio - suspending output");
//            mIOControl.suspendOutput();
//        }
//        else
//        {
//            byte[] converted = mIAudioConverter.convert(packets);
//            mLog.debug("Produce Content - writing converted audio: " + converted.length);
//            contentEncoder.write(ByteBuffer.wrap(converted));
//        }
    }

    @Override
    public boolean isRepeatable()
    {
        return false;
    }

    @Override
    public void close() throws IOException
    {
        mLog.debug("We were closed!");
    }

}
