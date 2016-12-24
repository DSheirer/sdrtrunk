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

import audio.broadcast.AudioBroadcaster;
import audio.broadcast.BroadcastState;
import audio.metadata.AudioMetadata;
import audio.metadata.Metadata;
import audio.metadata.MetadataType;
import controller.ThreadPoolManager;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcecastTCPAudioBroadcaster extends AudioBroadcaster
{
    private final static Logger mLog = LoggerFactory.getLogger( IcecastTCPAudioBroadcaster.class );
    private final static String TERMINATOR = "\r\n";
    private final static String SEPARATOR = ":";
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 30000; //30 seconds

    private NioSocketConnector mSocketConnector;
    private IoSession mStreamingSession = null;
    private IcecastMetadataUpdater mIcecastMetadataUpdater;

    private long mLastConnectionAttempt = 0;
    private byte[] mSilenceFrame;
    private AtomicBoolean mConnecting = new AtomicBoolean();

    /**
     * Creates an Icecast 2.3.2 compatible broadcaster using TCP and a pseudo HTTP 1.0 protocol.  This broadcaster is
     * compatible with Icecast version 2.3.2 and older versions of the server software.
     *
     * Note: use @see IcecastHTTPAudioBroadcaster for Icecast version 2.4.x and newer.
     *
     * @param configuration
     */
    public IcecastTCPAudioBroadcaster(DefaultAsyncHttpClient httpClient,
                                      ThreadPoolManager threadPoolManager,
                                      IcecastTCPConfiguration configuration)
    {
        super(threadPoolManager, configuration);
    }

    /**
     * Icecast broadcast configuration
     */
    private IcecastTCPConfiguration getConfiguration()
    {
        return (IcecastTCPConfiguration)getBroadcastConfiguration();
    }

    /**
     * Broadcasts the audio frame or sequence
     */
    @Override
    protected void broadcastAudio(byte[] audio)
    {
        if(connect())
        {
            try
            {
                send(audio);
            }
            catch(IOException ioe)
            {
                mLog.error("Error sending audio", ioe);
            }
        }
    }

    /**
     * Broadcasts an audio metadata update
     */
    @Override
    protected void broadcastMetadata(AudioMetadata metadata)
    {
        StringBuilder sb = new StringBuilder();

        if(metadata != null)
        {
            Metadata to = metadata.getMetadata(MetadataType.TO);

            sb.append("TO:");

            if(to != null)
            {
                if(to.hasAlias())
                {
                    sb.append(to.getAlias().getName());
                }
                else
                {
                    sb.append(to.getValue());
                }
            }
            else
            {
                sb.append("UNKNOWN");
            }

            Metadata from = metadata.getMetadata(MetadataType.FROM);

            sb.append(" FROM:");

            if(from != null)
            {

                if(from.hasAlias())
                {
                    sb.append(from.getAlias().getName());
                }
                else
                {
                    sb.append(from.getValue());
                }
            }
            else
            {
                sb.append("UNKNOWN");
            }
        }
        else
        {
            sb.append("Scanning ....");
        }

        if(mIcecastMetadataUpdater == null)
        {
            mIcecastMetadataUpdater = new IcecastMetadataUpdater(getThreadPoolManager(), getConfiguration());
        }

        mIcecastMetadataUpdater.update(sb.toString());
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
                mLog.debug("NIO socket connector - creating");
                mSocketConnector = new NioSocketConnector();
                mSocketConnector.setConnectTimeoutCheckInterval(10000);
                mSocketConnector.getFilterChain().addLast("logger",
                    new LoggingFilter(IcecastTCPAudioBroadcaster.class));
                mSocketConnector.getFilterChain().addLast("codec",
                    new ProtocolCodecFilter(new TextLineCodecFactory((Charset.forName("UTF-8")))));
                mLog.debug("NIO socket connector - setting handler");
                mSocketConnector.setHandler(new IcecastTCPIOHandler());
            }

            mStreamingSession = null;

            Runnable runnable = new Runnable()
            {
                @Override
                public void run()
                {
                    mLog.debug("Attempting connection to Icecast TCP Server");

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
                            setBroadcastState(BroadcastState.BROADCAST_ERROR);
                            mLog.debug("Failed to connect", rie);
                        }
                        else
                        {
                            mLog.debug("Failed to connect - no exception is available");
                        }

                        mConnecting.set(false);
                    }
                }
            };

            getThreadPoolManager().scheduleOnce(runnable, 0l, TimeUnit.SECONDS);

        }

        return connected();
    }


    /**
     * Disconnect from the remote broadcast server and cleanup input/output streams and socket connection
     */
    public void disconnect()
    {
        mLog.info("Disconnecting Icecast TCP audio broadcaster");

        if(mConnecting.get())
        {
            if(mStreamingSession != null)
            {
                mStreamingSession.closeNow();
            }
        }
    }

    /**
     * Creates an audio metadata description string that can optionally be included when connecting to the remote
     * broadcast server.
     */
    private String getAudioInfoMetadata()
    {
        StringBuilder sb = new StringBuilder();

        if(getConfiguration().hasBitRate() || getConfiguration().hasChannels() || getConfiguration().hasSampleRate())
        {
            sb.append(IcecastHeader.AUDIO_INFO.getValue()).append(SEPARATOR);

            boolean contentAdded = false;

            if(getConfiguration().hasBitRate())
            {
                sb.append("bitrate=").append(getConfiguration().getBitRate());
                contentAdded = true;
            }
            if(getConfiguration().hasChannels())
            {
                if(contentAdded)
                {
                    sb.append(";");
                }

                sb.append("channels=").append(getConfiguration().getChannels());

                contentAdded = true;
            }
            if(getConfiguration().hasSampleRate())
            {
                if(contentAdded)
                {
                    sb.append(";");
                }

                sb.append("samplerate=").append(getConfiguration().getSampleRate());
            }
        }

        return sb.toString();
    }

    /**
     * Sends the string data to the remote server
     *
     * @param data to send
     * @throws IOException if there is an error communicating with the remote server
     */
    private void send(String data) throws IOException
    {
        if(data != null && !data.isEmpty() && mStreamingSession != null && mStreamingSession.isConnected())
        {
            mStreamingSession.write(data);
        }
    }

    /**
     * Sends the byte data to the remote server
     * @param data to send
     * @throws IOException if there is an error communicating with the remote server
     */
    private void send(byte[] data) throws IOException
    {
        if(mStreamingSession != null && mStreamingSession.isConnected())
        {
            mStreamingSession.write(data);
        }
    }

    /**
     * IO Handler for managing Icecast TCP connection and credentials
     */
    public class IcecastTCPIOHandler extends IoHandlerAdapter
    {
        /**
         * Sends stream configuration and user credentials upon connecting to remote server
         */
        @Override
        public void sessionOpened(IoSession session) throws Exception
        {
            StringBuilder sb = new StringBuilder();
            sb.append("SOURCE ").append(getConfiguration().getMountPoint());
            sb.append(" HTTP/1.0").append(TERMINATOR);

            sb.append("Authorization: ").append(getConfiguration().getBase64EncodedCredentials()).append(TERMINATOR);
            sb.append(IcecastHeader.USER_AGENT.getValue()).append(SEPARATOR)
                .append(SystemProperties.getInstance().getApplicationName()).append(TERMINATOR);
            sb.append(IcecastHeader.CONTENT_TYPE.getValue()).append(SEPARATOR)
                .append(getConfiguration().getBroadcastFormat().getValue()).append(TERMINATOR);
            sb.append(IcecastHeader.PUBLIC.getValue()).append(SEPARATOR)
                .append(getConfiguration().isPublic() ? "1" : "0").append(TERMINATOR);

            if(getConfiguration().hasName())
            {
                sb.append(IcecastHeader.NAME.getValue()).append(SEPARATOR).append(getConfiguration().getName()).append(TERMINATOR);
            }

            if(getConfiguration().hasGenre())
            {
                sb.append(IcecastHeader.GENRE.getValue()).append(SEPARATOR)
                    .append(getConfiguration().getGenre()).append(TERMINATOR);
            }

            if(getConfiguration().hasDescription())
            {
                sb.append(IcecastHeader.DESCRIPTION.getValue()).append(SEPARATOR)
                    .append(getConfiguration().getDescription()).append(TERMINATOR);
            }

//                sb.append(getAudioInfoMetadata());

            sb.append(TERMINATOR).append(TERMINATOR);

            mLog.debug("Sending connection string");
            session.write(sb.toString());
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception
        {
            setBroadcastState(BroadcastState.DISCONNECTED);
            mLog.debug("Session Closed - State = Disconnected");

            mSocketConnector.dispose();
            mStreamingSession = null;
            mSocketConnector = null;

            mConnecting.set(false);
            super.sessionClosed(session);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception
        {
            mLog.debug("Exception", cause);
            super.exceptionCaught(session, cause);

            setBroadcastState(BroadcastState.BROADCAST_ERROR);
            mConnecting.set(false);
        }

        @Override
        public void messageReceived(IoSession session, Object object) throws Exception
        {
            if(object instanceof String)
            {
                String message = (String)object;

                if(message != null && !message.trim().isEmpty())
                {
                    if(message.startsWith("HTTP/1.0 200 OK"))
                    {
                        setBroadcastState(BroadcastState.CONNECTED);
                    }
                    else if(message.startsWith("HTTP/1.0 403 Mountpoint in use"))
                    {
                        setBroadcastState(BroadcastState.MOUNT_POINT_IN_USE);
                    }
                    else if(message.contains("Invalid Password") ||
                        message.contains("Authentication Required"))
                    {
                        setBroadcastState(BroadcastState.INVALID_PASSWORD);
                    }
                    else
                    {
                        mLog.error("Unrecognized server response:" + message);
                        setBroadcastState(BroadcastState.ERROR);
                    }

                    mLog.debug("Message Received: " + message.toString());
                }
            }
            else
            {
                mLog.debug("Message Received: " + object.getClass());
            }
        }

        @Override
        public void inputClosed(IoSession session) throws Exception
        {
            mLog.debug("Input Closed");
            super.inputClosed(session);
        }
    }
}
