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
import audio.broadcast.BroadcastHandler;
import audio.broadcast.BroadcastState;
import audio.broadcast.Broadcaster;
import audio.broadcast.BroadcasterFactory;
import audio.broadcast.HttpAsyncAudioStreamer;
import audio.convert.IAudioConverter;
import controller.ThreadPoolManager;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import record.wave.AudioPacketMonoWaveReader;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Future;

public class IcecastHandler extends BroadcastHandler
{
    private final static Logger mLog = LoggerFactory.getLogger( IcecastHandler.class );

    private static final String HTTP_100_CONTINUE = "100-continue";
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 15000; //15 seconds
    private long mLastConnectionAttempt = 0;
    private CloseableHttpAsyncClient mHttpClient;
    private HttpAsyncAudioStreamer mAudioStreamer;
    private URI mURI;
    private int mCounter = 0;

    /**
     * Creates an Icecast broadcast handler.
     * @param configuration
     * @param audioConverter
     */
    public IcecastHandler(IcecastConfiguration configuration, IAudioConverter audioConverter)
    {
        super(configuration, audioConverter);

        mAudioStreamer = new HttpAsyncAudioStreamer(HttpPut.METHOD_NAME, getConfiguration().getMountPoint(),
                getAudioConverter(), 20);
    }

    /**
     * Configuration information
     */
    private IcecastConfiguration getConfiguration()
    {
        return (IcecastConfiguration)getBroadcastConfiguration();
    }

    private URI getURI() throws URISyntaxException
    {
        if(mURI == null)
        {
            mURI = new URIBuilder()
                    .setScheme("http")
                    .setHost(getConfiguration().getHost())
                    .setPort(getConfiguration().getPort())
                    .setPath(getConfiguration().getMountPoint())
                    .build();
        }

        return mURI;
    }

    /**
     * Process audio packets using Shoutcast V1 Protocol
     * @param audioPackets
     */
    @Override
    public void broadcast(List<AudioPacket> audioPackets)
    {
        if(mCounter > 10)
        {
            mLog.debug("Connecting ...");
            connect();
        }

        for(AudioPacket audioPacket: audioPackets)
        {
            mAudioStreamer.receive(audioPacket);

            mCounter++;
        }
    }

    /**
     * Indicates if the audio handler is currently connected to the remote server and capable of streaming audio.
     *
     * This method will attempt to establish a connection or reconnect to the streaming server.
     * @return true if the audio handler can stream audio
     */
    private boolean connect()
    {
        if(!connected())
        {
            createConnection();
        }

        return connected();
    }


    /**
     * Disconnect from the remote server
     */
    @Override
    public void disconnect()
    {

    }

    /**
     * Creates a connnection to the remote server using the shoutcast configuration information.  Once disconnected
     * following a successful connection, attempts to reestablish a connection on a set interval
     */
    private void createConnection()
    {
        if(!connected() && System.currentTimeMillis() - mLastConnectionAttempt >= RECONNECT_INTERVAL_MILLISECONDS)
        {
            mLog.debug("Creating a connection to icecast server");
            mLastConnectionAttempt = System.currentTimeMillis();

            if(mHttpClient == null)
            {
                mHttpClient = HttpAsyncClients.createDefault();
                mHttpClient.start();
            }

//            try
//            {
                HttpHost httpHost = new HttpHost(getConfiguration().getHost(), getConfiguration().getPort());
//                mAudioStreamer.addHeader(IcecastHeader.EXPECT.getValue(), HTTP_100_CONTINUE);

                mAudioStreamer.addHeader(IcecastHeader.AUTHORIZATION.getValue(), getConfiguration().getAuthorization());
                mAudioStreamer.addHeader(HttpHeaders.ACCEPT, "*/*");
                mAudioStreamer.addHeader(HttpHeaders.CONTENT_TYPE, getConfiguration().getBroadcastFormat().getValue());
                mAudioStreamer.addHeader(IcecastHeader.PUBLIC.getValue(), getConfiguration().isPublic() ? "1" : "0");
                mAudioStreamer.addHeader(IcecastHeader.NAME.getValue(), getConfiguration().getStreamName());
                mAudioStreamer.addHeader(IcecastHeader.DESCRIPTION.getValue(), getConfiguration().getHost());
                mAudioStreamer.addHeader(IcecastHeader.URL.getValue(), getConfiguration().getURL());
                mAudioStreamer.addHeader(IcecastHeader.GENRE.getValue(), getConfiguration().getGenre());
                mAudioStreamer.addHeader(IcecastHeader.USER_AGENT.getValue(), SystemProperties.getInstance().getApplicationName());

//                BasicAsyncRequestProducer producer = new BasicAsyncRequestProducer(httpHost, mAudioStreamer);
                BasicAsyncRequestProducer producer = new MyProducer(httpHost, mAudioStreamer, mAudioStreamer);
                AsyncCharConsumer<HttpResponse> consumer = new AsyncCharConsumer<HttpResponse>()
                {
                    @Override
                    protected void onCharReceived(CharBuffer charBuffer, IOControl ioControl) throws IOException
                    {
                        mLog.debug("Received consumer characters!");
                    }

                    @Override
                    protected void onResponseReceived(HttpResponse response) throws HttpException, IOException
                    {
                        mLog.debug("Received consumer response:\n" + response.toString());

                        switch(response.getStatusLine().getStatusCode())
                        {
                            case HttpStatus.SC_OK: //200
                            case HttpStatus.SC_CONTINUE: //100
                                mLog.debug("Connected to icecast server");
                                setBroadcastState(BroadcastState.CONNECTED);
                                break;
                            case HttpStatus.SC_UNAUTHORIZED: //401
                                setBroadcastState(BroadcastState.INVALID_PASSWORD);
                                mLog.error("Couldn't connect to Icecast2 server using login credentials");
                                break;
                            case HttpStatus.SC_FORBIDDEN: //403
                                String reason = response.getStatusLine().getReasonPhrase();

                                if(reason.contains("Content-Type"))
                                {
                                    setBroadcastState(BroadcastState.UNSUPPORTED_AUDIO_FORMAT);
                                    mLog.error("Icecast2 server does not support audio format");
                                }
                                else if(reason.startsWith("internal format"))
                                {
                                    setBroadcastState(BroadcastState.REMOTE_SERVER_ERROR);
                                    mLog.error("Icecast2 server has an error");
                                }
                                else if(reason.startsWith("too many sources"))
                                {
                                    setBroadcastState(BroadcastState.MAX_SOURCES_EXCEEDED);
                                    mLog.error("Icecast2 server maximum number of sources exceeded");
                                }
                                else if(reason.startsWith("Mountpoint"))
                                {
                                    setBroadcastState(BroadcastState.MOUNT_POINT_IN_USE);
                                    mLog.error("Icecast2 server mount point is already in use");
                                }
                            case HttpStatus.SC_INTERNAL_SERVER_ERROR: //500
                                setBroadcastState(BroadcastState.INVALID_PASSWORD);
                                mLog.error("Couldn't connect to Icecast2 server using login credentials");
                                break;
                            default:
                                setBroadcastState(BroadcastState.ERROR);
                                mLog.error("Error connecting to remote server.  HTTP Status Code:" +
                                    response.getStatusLine().getStatusCode() + " Reason:" +
                                    response.getStatusLine().getReasonPhrase());
                        }
                    }

                    @Override
                    protected HttpResponse buildResult(HttpContext httpContext) throws Exception
                    {
                        mLog.debug("Received consumer request for response!!!!!!");
                        return null;
                    }
                };

                mLog.debug("Sending connection request");
                Future<HttpResponse> response = mHttpClient.execute(producer, consumer, new FutureCallback<HttpResponse>()
                {
                    @Override
                    public void completed(HttpResponse httpResponse)
                    {
                        mLog.debug("Received final response:\n" + httpResponse.toString());
                    }

                    @Override
                    public void failed(Exception e)
                    {
                        mLog.debug("Received final response error", e);
                    }

                    @Override
                    public void cancelled()
                    {
                        mLog.debug("Received final response - cancelled");
                    }
                });

                mLog.debug("Create connection finished ... processing");
        }
    }

    public static void main(String[] args)
    {
        boolean test = false;

        IcecastConfiguration config = new IcecastConfiguration(BroadcastFormat.MP3);
        config.setAlias("Test Configuration");
        config.setHost("localhost");
        config.setPort(8000);
        config.setMountPoint("/dennyTestStream");
        config.setUserName("source");
        config.setPassword("denny");
        config.setStreamName("Denny's Audio Broadcast Test");
        config.setDescription("Description");
        config.setGenre("Scanner Audio");
        config.setPublic(true);
        config.setURL("http://localhost:8000");
        config.setBitRate(16);

        if(test)
        {
            CloseableHttpClient client = HttpClients.createDefault();

            URI uri;

            try
            {
                uri = new URIBuilder()
                    .setScheme("http")
                    .setHost(config.getHost())
                    .setPort(config.getPort())
                    .setPath(config.getMountPoint())
                    .build();

                HttpPut httpPut = new HttpPut(uri);
                httpPut.setProtocolVersion(HttpVersion.HTTP_1_1);

                httpPut.addHeader(IcecastHeader.AUTHORIZATION.getValue(), config.getAuthorization());
                httpPut.addHeader(IcecastHeader.HOST.getValue(), config.getHost());
                httpPut.addHeader(IcecastHeader.ACCEPT.getValue(), "*/*");
                httpPut.addHeader(IcecastHeader.CONTENT_TYPE.getValue(), config.getBroadcastFormat().getValue());
                httpPut.addHeader(IcecastHeader.PUBLIC.getValue(), config.isPublic() ? "1" : "0");
                httpPut.addHeader(IcecastHeader.NAME.getValue(), config.getStreamName());
                httpPut.addHeader(IcecastHeader.DESCRIPTION.getValue(), config.getHost());
                httpPut.addHeader(IcecastHeader.URL.getValue(), config.getURL());
                httpPut.addHeader(IcecastHeader.GENRE.getValue(), config.getGenre());
                httpPut.addHeader(IcecastHeader.USER_AGENT.getValue(), SystemProperties.getInstance().getApplicationName());
                httpPut.addHeader(IcecastHeader.EXPECT.getValue(), HTTP_100_CONTINUE);

                CloseableHttpResponse response = client.execute(httpPut);

                mLog.debug("Response:" + response.toString());

            }
            catch(URISyntaxException e)
            {
                mLog.error("URI syntax exception", e);
            }
            catch(IOException ioe)
            {
                mLog.error("IO Exception", ioe);
            }
        }
        else
        {
            ThreadPoolManager threadPoolManager = new ThreadPoolManager();

            final Broadcaster broadcaster = BroadcasterFactory.getBroadcaster(threadPoolManager,config);

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
