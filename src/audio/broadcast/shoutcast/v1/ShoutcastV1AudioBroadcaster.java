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
package audio.broadcast.shoutcast.v1;

import audio.broadcast.AudioBroadcaster;
import audio.broadcast.BroadcastState;
import audio.broadcast.IBroadcastMetadataUpdater;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ThreadPool;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShoutcastV1AudioBroadcaster extends AudioBroadcaster
{
    private final static Logger mLog = LoggerFactory.getLogger(ShoutcastV1AudioBroadcaster.class);
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 30000; //30 seconds

    private NioSocketConnector mSocketConnector;
    private IoSession mStreamingSession = null;
    private IBroadcastMetadataUpdater mMetadataUpdater;

    private long mLastConnectionAttempt = 0;
    private AtomicBoolean mConnecting = new AtomicBoolean();

    /**
     * Creates a Shoutcast v1 compatible broadcaster using TCP protocol.
     *
     * Note: use @see ShoutcastV2AudioBroadcaster for Shoutcast version 2.x and newer.
     *
     * This broadcaster uses the Apache Mina library for the streaming socket connection and for metadata updates.  The
     * ShoutcastV1IOHandler manages all interaction with the Shoutcast server and manages the overall broadcast state.
     *
     * @param configuration for the Shoutcast stream
     */
    public ShoutcastV1AudioBroadcaster(ShoutcastV1Configuration configuration)
    {
        super(configuration);
    }

    /**
     * Shoutcast V1 broadcast configuration
     */
    private ShoutcastV1Configuration getConfiguration()
    {
        return (ShoutcastV1Configuration) getBroadcastConfiguration();
    }

    @Override
    protected IBroadcastMetadataUpdater getMetadataUpdater()
    {
        if(mMetadataUpdater == null)
        {
            mMetadataUpdater = new ShoutcastV1BroadcastMetadataUpdater(getConfiguration());
        }

        return mMetadataUpdater;
    }

    /**
     * Broadcasts the audio frame or sequence
     */
    @Override
    protected void broadcastAudio(byte[] audio)
    {
        if(audio != null && audio.length > 0 && connect() && mStreamingSession != null && mStreamingSession.isConnected())
        {
            IoBuffer buf = IoBuffer.allocate(audio.length).setAutoExpand(false);
            buf.put(audio);
            buf.flip();
            mStreamingSession.write(buf);
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
//                    new LoggingFilter(ShoutcastV1AudioBroadcaster.class));

                mSocketConnector.getFilterChain().addLast("codec",
                    new ProtocolCodecFilter(new TextLineCodecFactory()));

                mSocketConnector.setHandler(new ShoutcastIOHandler());
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
        if(connected())
        {
            if(mStreamingSession != null)
            {
                mStreamingSession.closeNow();
            }
        }
    }

    /**
     * IO Handler for managing Icecast TCP connection and credentials
     */
    public class ShoutcastIOHandler extends IoHandlerAdapter
    {
        /**
         * Sends stream configuration and user credentials upon connecting to remote server
         */
        @Override
        public void sessionOpened(IoSession session) throws Exception
        {
            StringBuilder sb = new StringBuilder();

            //Password
            sb.append(getConfiguration().getPassword()).append(ShoutcastMetadata.COMMAND_TERMINATOR);

            //Metadata
            sb.append(ShoutcastMetadata.STREAM_NAME.encode(getConfiguration().getName()));
            sb.append(ShoutcastMetadata.PUBLIC.encode(getConfiguration().isPublic()));
            sb.append(ShoutcastMetadata.GENRE.encode(getConfiguration().getGenre()));
            sb.append(ShoutcastMetadata.DESCRIPTION.encode(getConfiguration().getDescription()));
            sb.append(ShoutcastMetadata.AUDIO_BIT_RATE.encode(getConfiguration().getBitRate()));

            //End of connection string
            sb.append(ShoutcastMetadata.COMMAND_TERMINATOR);

            session.write(sb.toString());
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
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception
        {
            if(cause instanceof IOException && ((IOException) cause).getMessage().startsWith("Connection reset"))
            {
                IOException ioe = (IOException) cause;

                if(ioe.getMessage() != null && ioe.getMessage().startsWith("Connection reset"))
                {
                    mLog.info("Streaming connection reset by remote server - reestablishing connection");
                    disconnect();
                    connect();
                }
                else
                {
                    mLog.error("IO Exception", ioe);
                }
            }
            else
            {
                mLog.error("Broadcast error", cause);
                setBroadcastState(BroadcastState.ERROR);
                disconnect();
            }

            mConnecting.set(false);
        }

        @Override
        public void messageReceived(IoSession session, Object object) throws Exception
        {
            if(object instanceof String)
            {
                String message = (String) object;

                if(message != null && !message.trim().isEmpty())
                {
                    if(message.startsWith("OK"))
                    {
                        setBroadcastState(BroadcastState.CONNECTED);
                    }
                    else if(message.startsWith("icy-caps:"))
                    {
                        //TODO: what does icy-caps:11 tell us?
                    }
                    else
                    {
                        mLog.error("Unrecognized server response:" + message);
                        setBroadcastState(BroadcastState.ERROR);
                    }
                }
            }
            else
            {
                mLog.error("Icecast TCP broadcaster - unrecognized message [ " + object.getClass() +
                    "] received:" + object.toString());
            }
        }
    }
}
