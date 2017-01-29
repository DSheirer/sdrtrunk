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

import audio.broadcast.BroadcastState;
import audio.convert.MP3AudioConverter;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.http.HttpClientCodec;
import org.apache.mina.http.HttpException;
import org.apache.mina.http.HttpRequestImpl;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpStatus;
import org.apache.mina.http.api.HttpVersion;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import util.ThreadPool;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcecastHTTPAudioBroadcaster extends IcecastAudioBroadcaster
{
    private static final Logger mLog = LoggerFactory.getLogger(IcecastHTTPAudioBroadcaster.class);
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 30000; //30 seconds

    private NioSocketConnector mSocketConnector;
    private IoSession mStreamingSession = null;
    private Map<String,String> mHTTPHeaders;

    private long mLastConnectionAttempt = 0;
    private AtomicBoolean mConnecting = new AtomicBoolean();

    /**
     * Creates an Icecast 2.4.x compatible broadcaster using HTTP 1.1 protocol.  This broadcaster is
     * compatible with Icecast version 2.4.x and newer versions of the server software.
     *
     * Note: use @see IcecastTCPAudioBroadcaster for Icecast version 2.3.x and older.
     *
     * This broadcaster uses the Apache Mina library for the streaming socket connection and for metadata updates.  The
     * IcecastHTTPIOHandler manages all interaction with the Icecast server and manages the overall broadcast state.
     *
     * @param configuration for the Icecast stream
     */
    public IcecastHTTPAudioBroadcaster(IcecastHTTPConfiguration configuration)
    {
        super(configuration);
    }

    /**
     * Broadcasts the audio frame or sequence
     */
    @Override
    protected void broadcastAudio(byte[] audio)
    {
        if(audio != null && audio.length > 0 && connect() && mStreamingSession != null && mStreamingSession.isConnected())
        {
            IoBuffer buffer = IoBuffer.allocate(audio.length);
            buffer.put(audio);
            buffer.flip();

            mStreamingSession.write(buffer);
        }
    }

    /**
     * (Re)Connects the broadcaster to the remote server if it currently is disconnected and indicates if the broadcaster
     * is currently connected to the remote server following any connection attempts.
     *
     * Attempts to connect via this method when the broadcast state indicates an error condition will be ignored.
     *
     * @return true if the audio handler can stream audio
     */
    private boolean connect()
    {
        if(!connected() && canConnect() &&
            (mLastConnectionAttempt + RECONNECT_INTERVAL_MILLISECONDS < System.currentTimeMillis()) &&
            mConnecting.compareAndSet(false, true))
        {
            mLastConnectionAttempt = System.currentTimeMillis();

            if(mSocketConnector == null)
            {
                mSocketConnector = new NioSocketConnector();
                mSocketConnector.setConnectTimeoutCheckInterval(10000);

//                mSocketConnector.getFilterChain().addLast("logger",
//                    new LoggingFilter(IcecastTCPAudioBroadcaster.class));

                mSocketConnector.getFilterChain().addLast("codec", new HttpClientCodec());
                mSocketConnector.setHandler(new IcecastHTTPIOHandler());
            }

            mStreamingSession = null;

            Runnable runnable = new Runnable()
            {
                @Override
                public void run()
                {
                    setBroadcastState(BroadcastState.CONNECTING);

                    try
                    {
                        ConnectFuture future = mSocketConnector
                            .connect(new InetSocketAddress(getBroadcastConfiguration().getHost(),
                                getBroadcastConfiguration().getPort()));
                        future.awaitUninterruptibly();
                        mStreamingSession = future.getSession();
                    }
                    catch(RuntimeIoException rie)
                    {
                        Throwable throwableCause = rie.getCause();

                        if(throwableCause instanceof ConnectException)
                        {
                            setBroadcastState(BroadcastState.NO_SERVER);
                        }
                        else if(throwableCause != null)
                        {
                            setBroadcastState(BroadcastState.ERROR);
                            mLog.debug("Failed to connect", rie);
                        }
                        else
                        {
                            setBroadcastState(BroadcastState.ERROR);
                            mLog.debug("Failed to connect - no exception is available");
                        }

                        disconnect();
                    }
                }
            };

            ThreadPool.SCHEDULED.schedule(runnable, 0l, TimeUnit.SECONDS);

        }

        return connected();
    }


    /**
     * Disconnect from the remote broadcast server and cleanup input/output streams and socket connection
     */
    public void disconnect()
    {
        if(mStreamingSession != null)
        {
            mStreamingSession.closeNow();
        }

        mConnecting.set(false);
    }

    /**
     * IO Handler for managing Icecast HTTP connection and credentials
     */
    public class IcecastHTTPIOHandler extends IoHandlerAdapter
    {
        @Override
        public void sessionOpened(IoSession session) throws Exception
        {
            //Send stream configuration and user credentials upon connecting to remote server
            HttpRequestImpl request = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.PUT,
                getConfiguration().getMountPoint(), "", getHTTPHeaders());

            session.write(request);
        }

        private Map<String,String> getHTTPHeaders()
        {
            if(mHTTPHeaders == null)
            {
                mHTTPHeaders = new HashMap<>();
                mHTTPHeaders.put(IcecastHeader.ACCEPT.getValue(), "*/*");
                mHTTPHeaders.put(IcecastHeader.CONTENT_TYPE.getValue(), getConfiguration().getBroadcastFormat().getValue());
                mHTTPHeaders.put(IcecastHeader.USER_AGENT.getValue(), SystemProperties.getInstance().getApplicationName());
                mHTTPHeaders.put(IcecastHeader.AUTHORIZATION.getValue(), getConfiguration().getBase64EncodedCredentials());
                mHTTPHeaders.put(IcecastHeader.PUBLIC.getValue(), getConfiguration().isPublic() ? "1" : "0");

                StringBuilder sb = new StringBuilder();
                sb.append("samplerate=").append(getConfiguration().getSampleRate()).append(";");
                sb.append("quality=").append(MP3AudioConverter.AUDIO_QUALITY).append(";");
                sb.append("channels=").append(getConfiguration().getChannels());
                mHTTPHeaders.put(IcecastHeader.AUDIO_INFO.getValue(), sb.toString());

                if(getConfiguration().hasName())
                {
                    mHTTPHeaders.put(IcecastHeader.NAME.getValue(), getConfiguration().getName());
                }

                if(getConfiguration().hasDescription())
                {
                    mHTTPHeaders.put(IcecastHeader.DESCRIPTION.getValue(), getConfiguration().getDescription());
                }

                if(getConfiguration().hasURL())
                {
                    mHTTPHeaders.put(IcecastHeader.URL.getValue(), getConfiguration().getURL());
                }

                if(getConfiguration().hasGenre())
                {
                    mHTTPHeaders.put(IcecastHeader.GENRE.getValue(), getConfiguration().getGenre());
                }


                if(getConfiguration().hasBitRate())
                {
                    mHTTPHeaders.put(IcecastHeader.BITRATE.getValue(), String.valueOf(getConfiguration().getBitRate()));
                }
            }

            return mHTTPHeaders;
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception
        {
            //If there is already an error state, don't override it.  Otherwise, set state to disconnected
            if(!getBroadcastState().isErrorState())
            {
                setBroadcastState(BroadcastState.DISCONNECTED);
            }

            mSocketConnector.dispose();
            mStreamingSession = null;
            mSocketConnector = null;

            mConnecting.set(false);
            super.sessionClosed(session);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable throwable) throws Exception
        {
            if(throwable instanceof ProtocolDecoderException)
            {
                Throwable cause = ((ProtocolDecoderException) throwable).getCause();

                if(cause instanceof HttpException &&
                    ((HttpException) cause).getStatusCode() == HttpStatus.CLIENT_ERROR_LENGTH_REQUIRED.code())
                {
                    //Ignore - Icecast 2.4 sometimes doesn't send any headers with their HTTP responses.  Mina expects a
                    //content-length for HTTP responses.
                }
                else
                {
                    mLog.error("HTTP protocol decoder error", throwable);
                    setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                    disconnect();
                }
            }
            else
            {
                mLog.error("Broadcast error", throwable);
                setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                disconnect();
            }

            mConnecting.set(false);
        }

        @Override
        public void messageReceived(IoSession session, Object object) throws Exception
        {
            if(object instanceof DefaultHttpResponse)
            {
                DefaultHttpResponse response = (DefaultHttpResponse) object;

                switch(response.getStatus())
                {
                    case INFORMATIONAL_CONTINUE:
                        break;
                    case SUCCESS_OK:
                        setBroadcastState(BroadcastState.CONNECTED);
                        mConnecting.set(false);
                        break;
                    case CLIENT_ERROR_UNAUTHORIZED:
                        setBroadcastState(BroadcastState.INVALID_CREDENTIALS);
                        disconnect();
                        break;
                    case CLIENT_ERROR_FORBIDDEN:
                        setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
                        disconnect();
                        break;
                    default:
                        setBroadcastState(BroadcastState.ERROR);
                        disconnect();
                        mLog.debug("Unspecified error: " + response.toString() + " Class:" + object.getClass());
                        break;
                }
            }
            else
            {
                mLog.error("Icecast HTTP broadcaster - unrecognized message [ " + object.getClass() +
                    "] received:" + object.toString());
            }
        }
    }
}
