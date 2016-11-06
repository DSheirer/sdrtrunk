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
package audio.broadcast.icecast;

import audio.AudioPacket;
import audio.broadcast.BroadcastFormat;
import audio.broadcast.BroadcastState;
import audio.broadcast.Broadcaster;
import audio.broadcast.BroadcastFactory;
import audio.convert.IAudioConverter;
import controller.ThreadPoolManager;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import record.wave.AudioPacketMonoWaveReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class IcecastHTTPBroadcaster extends Broadcaster implements Publisher<ByteBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger( IcecastHTTPBroadcaster.class );

    private static final String HTTP_100_CONTINUE = "100-continue";
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 15000; //15 seconds
    private long mLastConnectionAttempt = 0;
    private DefaultAsyncHttpClient mAsyncHttpClient;
    private Subscriber<? super ByteBuffer> mSubscriber;
    private List<AudioPacket> mPacketsToBroadcast = new ArrayList<>();
    private ConvertedAudioStreamSubscription mSubscription;
    private AtomicBoolean mConnecting = new AtomicBoolean();

    /**
     * Creates an Icecast 2.4.x (or newer) compatible broadcaster using HTTP 1.1 protocol
     *
     * Note: use @see IcecastTCPBroadcaster for Icecast version 2.3.x and older.
     * @param configuration
     * @param audioConverter
     */
    public IcecastHTTPBroadcaster(ThreadPoolManager threadPoolManager,
                                  IcecastHTTPConfiguration configuration,
                                  IAudioConverter audioConverter)
    {
        super(threadPoolManager, configuration, audioConverter);
    }

    /**
     * Configuration information
     */
    private IcecastHTTPConfiguration getConfiguration()
    {
        return (IcecastHTTPConfiguration)getBroadcastConfiguration();
    }

    /**
     * Broadcast any queued audio packets using Icecast V2 Protocol
     */
    @Override
    public void broadcast()
    {
        if(connect())
        {
            if (mSubscription != null && mSubscription.hasDemand())
            {
                mAudioQueue.drainTo(mPacketsToBroadcast, 5);

                if (!mPacketsToBroadcast.isEmpty())
                {
                    byte[] convertedAudio = getAudioConverter().convert(mPacketsToBroadcast);

                    mLog.debug("We have [" + mPacketsToBroadcast.size() +
                            "] packets - sending:" + convertedAudio.length + " bytes");

                    ByteBuffer buffer = ByteBuffer.wrap(convertedAudio);

                    mSubscription.broadcast(buffer);

                    mPacketsToBroadcast.clear();
                }
            }
        }
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber)
    {
        //Remove existing subscription, if there is one
        if (mSubscription != null)
        {
            mSubscription.cancel();
        }

        //Create the subscription
        mSubscription = new ConvertedAudioStreamSubscription(subscriber);
    }

    /**
     * Indicates if the audio handler is currently connected to the remote server and capable of streaming audio.
     *
     * This method will attempt to establish a connection or reconnect to the streaming server.
     * @return true if the audio handler can stream audio
     */
    private boolean connect()
    {
        if(canConnect())
        {
            createConnection();
        }

        return connected();
    }


    /**
     * Disconnect from the remote server
     */
    public void disconnect()
    {

    }

    /**
     * Creates a connnection to the remote server using the shoutcast configuration information.  Once disconnected
     * following a successful connection, attempts to reestablish a connection on a set interval
     */
    private void createConnection()
    {
        if(mConnecting.compareAndSet(false, true))
        {
            if(canConnect() && System.currentTimeMillis() - mLastConnectionAttempt >= RECONNECT_INTERVAL_MILLISECONDS)
            {
                mLog.debug("Creating a connection to icecast server");
                mLastConnectionAttempt = System.currentTimeMillis();

                DefaultAsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                        .setReadTimeout(-1)
                        .setRequestTimeout(-1)
                        .build();

                mAsyncHttpClient = new DefaultAsyncHttpClient(config);

                Uri uri = new Uri(Uri.HTTP, null, getConfiguration().getHost(), getConfiguration().getPort(),
                        getConfiguration().getMountPoint(), null);

                mLog.debug("URL:" + uri.toUrl());

                BoundRequestBuilder builder = mAsyncHttpClient.preparePut(uri.toUrl());

                //Use Basic (base64) authentication
                Realm realm = new Realm.Builder(getConfiguration().getUserName(), getConfiguration().getPassword())
                        .setScheme(Realm.AuthScheme.BASIC).setUsePreemptiveAuth(true).build();
                builder.setRealm(realm);
                builder.setBody(this);

                builder.addHeader(IcecastHeader.ACCEPT.getValue(), "*/*");
                builder.addHeader(IcecastHeader.CONTENT_TYPE.getValue(), getConfiguration().getBroadcastFormat().getValue());
                builder.addHeader(IcecastHeader.PUBLIC.getValue(), getConfiguration().isPublic() ? "1" : "0");

                if(getConfiguration().hasName())
                {
                    builder.addHeader(IcecastHeader.NAME.getValue(), getConfiguration().getName());
                }

                if(getConfiguration().hasDescription())
                {
                    builder.addHeader(IcecastHeader.DESCRIPTION.getValue(), getConfiguration().getDescription());
                }

                if(getConfiguration().hasURL())
                {
                    builder.addHeader(IcecastHeader.URL.getValue(), getConfiguration().getURL());
                }

                if(getConfiguration().hasGenre())
                {
                    builder.addHeader(IcecastHeader.GENRE.getValue(), getConfiguration().getGenre());
                }


                if(getConfiguration().hasBitRate())
                {
                    builder.addHeader(IcecastHeader.BITRATE.getValue(), String.valueOf(getConfiguration().getBitRate()));
                }

                builder.addHeader(IcecastHeader.USER_AGENT.getValue(),
                        SystemProperties.getInstance().getApplicationName());

                builder.addHeader(IcecastHeader.EXPECT.getValue(), HTTP_100_CONTINUE);

                ListenableFuture<Response> future = builder.execute(new AsyncHttpConnectionResponseHandler());

                mLog.debug("Create connection was submitted asynchronously");
            }
            else
            {
                mConnecting.set(false);
            }
        }
    }

    public class AsyncHttpConnectionResponseHandler implements AsyncHandler<Response>
    {
        @Override
        public void onThrowable(Throwable throwable)
        {
            mLog.error("BROADCAST error", throwable);
            setBroadcastState(BroadcastState.BROADCAST_ERROR);
            mConnecting.compareAndSet(true, false);
        }

        @Override
        public State onBodyPartReceived(HttpResponseBodyPart httpResponseBodyPart) throws Exception
        {
            mLog.debug("Got response body part");
            return State.CONTINUE;
        }

        @Override
        public State onStatusReceived(HttpResponseStatus httpResponseStatus) throws Exception
        {
            mLog.debug("Got response status: " + httpResponseStatus.toString());
            State returnState = State.ABORT;

            switch(httpResponseStatus.getStatusCode())
            {
                case 200: //200-OK
                    setBroadcastState(BroadcastState.CONNECTED);
                    returnState = State.CONTINUE;
                    break;
                case 100: //100-Continue
                    setBroadcastState(BroadcastState.CONNECTING);
                    returnState = State.CONTINUE;
                    break;
                case 401:
                    setBroadcastState(BroadcastState.INVALID_PASSWORD);
                    break;
                case 403:
                    String text = httpResponseStatus.getStatusText();

                    mLog.error("Error while connecting to Icecast server: " + httpResponseStatus.getStatusCode() +
                        " " + text);

                    if(text != null)
                    {
                        if(text.contains("Content-type not supported"))
                        {
                            setBroadcastState(BroadcastState.UNSUPPORTED_AUDIO_FORMAT);
                        }
                        else if(text.contains("too many sources"))
                        {
                            setBroadcastState(BroadcastState.MAX_SOURCES_EXCEEDED);
                        }
                        else if(text.contains("Mountpoint in use"))
                        {
                            setBroadcastState(BroadcastState.MOUNT_POINT_IN_USE);
                        }
                        else
                        {
                            setBroadcastState(BroadcastState.ERROR);
                        }
                    }
                    else
                    {
                        setBroadcastState(BroadcastState.ERROR);
                    }
                    break;
                case 500:
                    mLog.error("Error while connecting to Icecast server: " + httpResponseStatus.getStatusCode() +
                            " " + httpResponseStatus.getStatusText());
                    setBroadcastState(BroadcastState.REMOTE_SERVER_ERROR);
                    break;
                default:
                    mLog.error("Error while connecting to Icecast server - unrecognized status code: " +
                            httpResponseStatus.getStatusCode() + " " + httpResponseStatus.getStatusText());
                    setBroadcastState(BroadcastState.ERROR);
            }

            mConnecting.compareAndSet(true, false);

            return returnState;
        }

        @Override
        public State onHeadersReceived(HttpResponseHeaders httpResponseHeaders) throws Exception
        {
            mLog.debug("Got response headers!");
            return State.CONTINUE;
        }

        @Override
        public Response onCompleted() throws Exception
        {
            mLog.debug("We're completed");
            return null;
        }
    }

    /**
     * Subscription to monitor a converted audio stream subscriber and the subscriber's expressed demand for converted
     * audio byte buffers
     */
    public class ConvertedAudioStreamSubscription implements Subscription
    {
        private Subscriber<? super ByteBuffer> mSubscriber;
        private AtomicLong mDemand = new AtomicLong();

        /**
         * Constructs a subscription
         * @param subscriber to receive converted audio byte buffers
         */
        public ConvertedAudioStreamSubscription(Subscriber<? super ByteBuffer> subscriber)
        {
            mSubscriber = subscriber;
            mSubscriber.onSubscribe(this);
        }

        /**
         * Request method for the subscriber to register demand for audio byte buffers
         * @param requestedBufferCount to stream to the subscriber
         */
        @Override
        public void request(long requestedBufferCount)
        {
            mDemand.getAndAdd(requestedBufferCount);

            mLog.debug("Subscriber request:" + requestedBufferCount + " outstanding demand: " + mDemand.get());
        }

        /**
         * Indicates if this subscription currently has registered demand from the subscriber
         */
        public boolean hasDemand()
        {
            return mDemand.get() > 0;
        }

        /**
         * Broadcasts the buffer to the subscriber and decrements the internal buffer demand counter
         */
        public void broadcast(ByteBuffer buffer)
        {
            if(mSubscriber != null)
            {
                mSubscriber.onNext(buffer);
                mDemand.decrementAndGet();
            }
        }

        /**
         * Invoked by the subscriber to cancel the stream
         */
        @Override
        public void cancel()
        {
            mLog.debug("Subscriber has requested a cancel");
            stop();
        }
    }

    public static void main(String[] args)
    {
        boolean test = false;

        IcecastHTTPConfiguration config = new IcecastHTTPConfiguration(BroadcastFormat.MP3);

        config.setName("Broadcastify SDRTrunk Test #2");
        config.setHost("audio3.broadcastify.com");
        config.setPort(80);
        config.setMountPoint("/k0yrdpx7zn4h");
        config.setUserName("source");
        config.setPassword("k8j9405n");
        config.setDescription("SDRTrunk Test Feed #2");
        config.setGenre("Scanner");
        config.setPublic(true);
        config.setURL("http://www.radioreference.com");
        config.setBitRate(16);

        if(test)
        {
            mLog.debug("Auth:" + config.getEncodedCredentials());
        }
        else
        {
            ThreadPoolManager threadPoolManager = new ThreadPoolManager();

            final Broadcaster broadcaster = BroadcastFactory.getBroadcaster(threadPoolManager,config);

            Path path = Paths.get("/home/denny/Music/PCM.wav");
            mLog.debug("Opening: " + path.toString());

            mLog.debug("Registering and starting audio playback");

            while(true)
            {
                mLog.debug("Playback started [" + path.toString() + "]");

                try (AudioPacketMonoWaveReader reader = new AudioPacketMonoWaveReader(path, true))
                {
                    reader.setListener(broadcaster);
                    reader.read();
                }
                catch (IOException e)
                {
                    mLog.error("Error", e);
                }

                mLog.debug("Playback ended [" + path.toString() + "]");
            }
        }
    }
}
