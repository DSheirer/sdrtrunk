/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.icecast;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyFeedConfiguration;
import io.github.dsheirer.audio.broadcast.icecast.codec.IcecastCodecFactory;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3AudioConverter;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.UnresolvedAddressException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcecastTCPAudioBroadcaster extends IcecastAudioBroadcaster
{
    private final static Logger mLog = LoggerFactory.getLogger(IcecastTCPAudioBroadcaster.class);
    private final static String TERMINATOR = "\n";
    private final static String SEPARATOR = ":";
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 3000; //3 seconds
    private static final long CONNECTION_ATTEMPT_TIMEOUT_MILLISECONDS = 5000; //5 seconds
    private static final int WRITE_TIMEOUT_SECONDS = 5;

    private NioSocketConnector mSocketConnector;
    private IoSession mStreamingSession = null;

    private long mLastConnectionAttempt = 0;
    private boolean mVerboseLogging = false;
    private AtomicBoolean mConnecting = new AtomicBoolean();

    /**
     * Creates an Icecast 2.3.2 compatible broadcaster using TCP and a pseudo HTTP 1.0 protocol.  This broadcaster is
     * compatible with Icecast version 2.3.2 and older versions of the server software.
     *
     * Note: use @see IcecastHTTPAudioBroadcaster for Icecast version 2.4.x and newer.
     *
     * This broadcaster uses the Apache Mina library for the streaming socket connection and for metadata updates.  The
     * ShoutcastV2IOHandler manages all interaction with the Icecast server and manages the overall broadcast state.
     *
     * @param configuration for the Icecast stream
     */
    public IcecastTCPAudioBroadcaster(IcecastTCPConfiguration configuration, InputAudioFormat inputAudioFormat,
                                      MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(configuration, inputAudioFormat, mp3Setting, aliasModel);

        if(configuration instanceof BroadcastifyFeedConfiguration broadcastify)
        {
            mVerboseLogging = broadcastify.isVerboseLogging();
        }
    }

    /**
     * Broadcasts the audio frame or sequence
     */
    @Override
    protected void broadcastAudio(byte[] audio, IdentifierCollection identifierCollection)
    {
        if(audio != null && audio.length > 0 && connect() && mStreamingSession != null && mStreamingSession.isConnected())
        {
            if(mInlineActive)
            {
                if (mInlineRemaining == -1)
                {
                    mInlineRemaining = mInlineInterval;
                }
                int audioOffset = 0;
                while(audioOffset < audio.length)
                {
                    byte[] chunk = Arrays.copyOfRange(audio, audioOffset, Math.min(audioOffset + mInlineRemaining, audio.length));
                    mInlineRemaining -= chunk.length;
                    audioOffset += chunk.length;

                    if(mVerboseLogging)
                    {
                        mLog.info("Broadcasting audio data - sending [" + chunk.length + "/" + audio.length + "] bytes");
                    }

                    mStreamingSession.write(chunk);
                    if(mInlineRemaining == 0)
                    {
                        mInlineRemaining = mInlineInterval;

                        byte[] metadata = IcecastMetadata.formatInline(IcecastMetadata.getTitle(identifierCollection, mAliasModel)).getBytes();

                        if(mVerboseLogging)
                        {
                            mLog.info("Sending inline metadata: " + new String(metadata));
                        }

                        mStreamingSession.write(metadata);
                    }
                }
            }
            else
            {
                if(mVerboseLogging)
                {
                    mLog.info("Broadcasting audio data [" + audio.length + "] bytes");
                }
                mStreamingSession.write(audio);
            }
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
                mSocketConnector.getSessionConfig().setWriteTimeout(WRITE_TIMEOUT_SECONDS);

                if(mVerboseLogging)
                {
                    LoggingFilter loggingFilter = new LoggingFilter(IcecastTCPAudioBroadcaster.class);
                    loggingFilter.setMessageSentLogLevel(LogLevel.NONE);
                    mSocketConnector.getFilterChain().addLast("logger", loggingFilter);
                }

                mSocketConnector.getFilterChain().addLast("codec",
                    new ProtocolCodecFilter(new IcecastCodecFactory()));
                mSocketConnector.setHandler(new IcecastTCPIOHandler());
            }

            mStreamingSession = null;

            Runnable runnable = new Runnable()
            {
                @Override
                public void run()
                {
                    if(mVerboseLogging)
                    {
                        mLog.info("Attempting connection ...");
                    }
                    setBroadcastState(BroadcastState.CONNECTING);

                    try
                    {
                        ConnectFuture future = mSocketConnector
                            .connect(new InetSocketAddress(getBroadcastConfiguration().getHost(),
                                getBroadcastConfiguration().getPort()));

                        if(mVerboseLogging)
                        {
                            mLog.info("Socket created - asynchronous connect requested - entering wait period");
                        }

                        boolean connected = future.await(CONNECTION_ATTEMPT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

                        if(connected)
                        {
                            if(mVerboseLogging)
                            {
                                mLog.info("Connected.");
                            }

                            mStreamingSession = future.getSession();
                            mConnecting.set(false);
                            return;
                        }
                        else
                        {
                            if(mVerboseLogging)
                            {
                                mLog.info("Not Connected.  Connection attempt timeout [" + CONNECTION_ATTEMPT_TIMEOUT_MILLISECONDS + "ms] exceeded");
                            }
                        }
                    }
                    catch(RuntimeIoException rioe)
                    {
                        if(rioe.getCause() instanceof SocketException)
                        {
                            if(mVerboseLogging)
                            {
                                mLog.info("Socket error.  This usually indicates sdrtrunk can't reach the server " +
                                        "address over the current network connection.  Setting state to " +
                                        "NETWORK UNAVAILABLE", rioe);
                            }
                            setBroadcastState(BroadcastState.NETWORK_UNAVAILABLE);
                            mConnecting.set(false);
                            return;
                        }
                    }
                    catch(UnresolvedAddressException uae)
                    {
                        if(mVerboseLogging)
                        {
                            mLog.info("Unresolved Address error.  This means the domain name services can't resolve " +
                                    "the server URL to an IP address.  Setting state to NETWORK UNAVAILABLE", uae);
                        }

                        setBroadcastState(BroadcastState.NETWORK_UNAVAILABLE);
                        mConnecting.set(false);
                        return;
                    }
                    catch(Exception e)
                    {
                        if(mVerboseLogging)
                        {
                            mLog.info("Unknown error.  An error occurred while attempting to connect to the server.", e);
                        }
                        mLog.error("Error", e);
                        //Disregard ... we'll disconnect and try again
                    }
                    catch(Throwable t)
                    {
                        mLog.error("Throwable error caught", t);
                    }

                    if(mVerboseLogging)
                    {
                        mLog.info("Starting disconnect sequence since an error occurred while trying to connect.");
                    }

                    disconnect();
                    mConnecting.set(false);
                }
            };

            ThreadPool.CACHED.submit(runnable);
        }

        return connected();
    }


    /**
     * Disconnect from the remote broadcast server and cleanup input/output streams and socket connection
     */
    public void disconnect()
    {
        if(connected() && mStreamingSession != null)
        {
            if(mVerboseLogging)
            {
                mLog.info("Routine disconnect requested from a connected state with a non-null streaming session");
            }

            mStreamingSession.closeNow();
        }
        else
        {
            //Only set broadcast state to disconnected if it's not already in an error state, to prevent a restart.  We
            //want to preserve the error state that got us here, so the user can see it.
            if(!getBroadcastState().isErrorState())
            {
                if(mVerboseLogging)
                {
                    mLog.info("Disconnect requested - previous non-error state was [" + getBroadcastState() +
                            "] - changing state to DISCONNECTED");
                }

                setBroadcastState(BroadcastState.DISCONNECTED);
            }

            mLastConnectionAttempt = System.currentTimeMillis();
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

            sb.append(IcecastHeader.AUDIO_INFO.getValue()).append(SEPARATOR);
            sb.append("samplerate=").append(getConfiguration().getSampleRate()).append(";");
            sb.append("quality=").append(MP3AudioConverter.AUDIO_QUALITY).append(";");
            sb.append("channels=").append(getConfiguration().getChannels()).append(TERMINATOR);

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

            if(getConfiguration().hasInline())
            {
                mInlineActive = true;
                mInlineInterval = getConfiguration().getInlineInterval();
                sb.append(IcecastHeader.METAINT.getValue()).append(SEPARATOR)
                    .append(String.valueOf(mInlineInterval))
                    .append(TERMINATOR);
            }
            else
            {
                mInlineActive = false;
            }

            if(mVerboseLogging)
            {
                mLog.info("Session opened. Sending: " + sb);
            }


            session.write(sb.toString());
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception
        {
            if(mVerboseLogging)
            {
                mLog.info("Session closed.  Setting connecting flag to false.");
            }

            mLastConnectionAttempt = System.currentTimeMillis();

            //If there is already an error state, don't override it.  Otherwise, set state to disconnected
            if(!getBroadcastState().isErrorState())
            {
                setBroadcastState(BroadcastState.DISCONNECTED);
            }

            mStreamingSession = null;

            mConnecting.set(false);

            super.sessionClosed(session);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception
        {
            if(!(cause instanceof IOException))
            {
                mLog.error("[" + getStreamName() + "] Broadcast error", cause);
            }

            if(mVerboseLogging)
            {
                mLog.info("Session error caught.", cause);
            }

            disconnect();
        }

        @Override
        public void messageReceived(IoSession session, Object object) throws Exception
        {
            if(object instanceof String message)
            {
                if(mVerboseLogging)
                {
                    mLog.info("Message Received [" + message + "]");
                }

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
                    else if(message.contains("Content-Type: text/html"))
                    {
                        //Disregard ... this normally follows the mount point in use error
                    }
                    else if(message.contains("Invalid Password") ||
                        message.contains("Authentication Required"))
                    {
                        setBroadcastState(BroadcastState.INVALID_CREDENTIALS);
                    }
                    else if(message.contains("HTTP/1.1 501"))
                    {
                        disconnect(); //So that we can reconnect later
                    }
                    else
                    {
                        mLog.error("Unrecognized server response:" + message);

                        /**
                         * Only allow a generic error to update state if we've not already experienced a more
                         * specific error. Otherwise, trailing messages will clear the more meaningful error state.
                         */
                        if(!getBroadcastState().isErrorState())
                        {
                            setBroadcastState(BroadcastState.ERROR);
                        }
                    }
                }
            }
            else
            {
                mLog.error("[" + getStreamName() + "]Icecast TCP broadcaster - unrecognized message [ " + object.getClass() +
                    "] received:" + object.toString());
            }
        }
    }
}
