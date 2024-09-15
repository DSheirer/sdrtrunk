/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3AudioConverter;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.http.HttpClientCodec;
import org.apache.mina.http.HttpRequestImpl;
import org.apache.mina.http.api.DefaultHttpResponse;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpVersion;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcecastHTTPAudioBroadcaster extends IcecastAudioBroadcaster
{
    private static final Logger mLog = LoggerFactory.getLogger(IcecastHTTPAudioBroadcaster.class);
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 30000; //30 seconds
    private static final String HTTP_1_0_OK_HEX_DUMP = "48 54 54 50 2F 31 2E 30 20 32 30 30 20 4F 4B";

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
    public IcecastHTTPAudioBroadcaster(IcecastHTTPConfiguration configuration, InputAudioFormat inputAudioFormat,
                                       MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(configuration, inputAudioFormat, mp3Setting, aliasModel);
    }

    /**
     * Broadcasts the audio frame or sequence
     */
    @Override
    protected void broadcastAudio(byte[] audio, IdentifierCollection identifierCollection)
    {
        if(audio != null && audio.length > 0 && connect() && mStreamingSession != null && mStreamingSession.isConnected())
        {
            IoBuffer buffer = IoBuffer.allocate(audio.length);

            if(mInlineActive)
            {
                byte[] metadata = IcecastMetadata.formatInline(IcecastMetadata.getTitle(identifierCollection, mAliasModel)).getBytes();
                buffer.setAutoExpand(true);
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
                    buffer.put(chunk);
                    if(mInlineRemaining == 0)
                    {
                        mInlineRemaining = mInlineInterval;
                        buffer.put(metadata);
                    }
                }
                buffer.shrink();
            }
            else
            {
                buffer.put(audio);
            }

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

//                LoggingFilter loggingFilter = new LoggingFilter(IcecastHTTPAudioBroadcaster.class);
//                loggingFilter.setMessageReceivedLogLevel(LogLevel.DEBUG);
//                mSocketConnector.getFilterChain().addLast("logger", loggingFilter);

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
                            setBroadcastState(BroadcastState.DISCONNECTED);
                            mLog.debug("Failed to connect", rie);
                        }
                        else
                        {
                            setBroadcastState(BroadcastState.DISCONNECTED);
                            mLog.debug("Failed to connect - no exception is available");
                        }

                        disconnect();
                    }

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
        if(mStreamingSession != null)
        {
            mStreamingSession.closeNow();
        }
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

                if(getConfiguration().hasInline())
                {
                    mInlineActive = true;
                    mInlineInterval = getConfiguration().getInlineInterval();
                    mHTTPHeaders.put(IcecastHeader.METAINT.getValue(), String.valueOf(mInlineInterval));
                }
                else
                {
                    mInlineActive = false;
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
                Throwable cause = throwable.getCause();

                /**
                 * Warning: Hack.  Icecast 2.4.2+ doesn't like to send HTTP headers along with their HTTP/1.0 OK
                 * message which causes Apache Mina to think that it hasn't received the full HTTP response.  So, we
                 * intercept the out of bounds exception and then inspect the message hex dump to see if it's an
                 * OK response and then play like everything is well and good.
                 */
                if(cause instanceof ArrayIndexOutOfBoundsException)
                {
                    String hexDump = ((ProtocolDecoderException)throwable).getHexdump();

                    if(hexDump.startsWith(HTTP_1_0_OK_HEX_DUMP))
                    {
                        setBroadcastState(BroadcastState.CONNECTED);
                    }
                    else
                    {
                        HttpDumpMessage message = new HttpDumpMessage(hexDump);

                        if(message.hasHttpResponseCode())
                        {
                            switch(message.getHttpResponseCode())
                            {
                                case 403: //Forbidden
                                    if(message.toString().contains("Mountpoint in use"))
                                    {
                                        mLog.error("Stream [" + getStreamName() + "] - unable to connect - mountpoint in use");
                                        setBroadcastState(BroadcastState.MOUNT_POINT_IN_USE);
                                        disconnect();
                                    }
                                    else
                                    {
                                        mLog.error("String [" + getStreamName() + "] - HTTP 403 protocol decoder error - message:\n\n" + message);
                                        setBroadcastState(BroadcastState.DISCONNECTED);
                                        disconnect();
                                    }
                                    break;
                                case 401: //Unauthorized
                                    if(message.toString().contains("Authentication Required"))
                                    {
                                        mLog.error("Stream [" + getStreamName() + "] - unable to connect - invalid credentials");
                                        setBroadcastState(BroadcastState.INVALID_CREDENTIALS);
                                        disconnect();
                                    }
                                    else
                                    {
                                        mLog.error("String [" + getStreamName() + "] - HTTP 401 protocol decoder error - message:\n\n" + message);
                                        setBroadcastState(BroadcastState.DISCONNECTED);
                                        disconnect();
                                    }
                                    break;
                                default:
                                        mLog.error("String [" + getStreamName() + "] - HTTP protocol decoder error - message:\n\n" + message);
                                        setBroadcastState(BroadcastState.DISCONNECTED);
                                        disconnect();
                            }
                        }
                        else
                        {
                            mLog.error("HTTP protocol decoder error - message:\n\n" + message);
                            setBroadcastState(BroadcastState.DISCONNECTED);
                            disconnect();
                        }
                    }
                }
                else
                {
                    mLog.error("HTTP protocol decoder error", throwable);
                    setBroadcastState(BroadcastState.DISCONNECTED);
                    disconnect();
                }
            }
            else
            {
                mLog.error("Broadcast error", throwable);
                setBroadcastState(BroadcastState.DISCONNECTED);
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
                        /**
                         * Only allow a generic error to update state if we've not already experienced a more
                         * specific error. Otherwise, trailing messages will clear the more meaningful error state.
                         */
                        if(!getBroadcastState().isErrorState())
                        {
                            setBroadcastState(BroadcastState.ERROR);
                        }
                        mLog.debug("Unspecified error: " + response.toString() + " Class:" + object.getClass());
                        disconnect();
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

    /**
     * Class for parsing HTTP response messages from Icecast 2.4.2+
     */
    public class HttpDumpMessage
    {
        private String mHexDump;
        private String mMessage;
        private int mHttpResponseCode = -1;

        public HttpDumpMessage(String hexDump)
        {
            mHexDump = hexDump;

            String[] split = mHexDump.split(" ");
            byte[] bytes = new byte[split.length];

            int pointer = 0;
            for(String a : split)
            {
                try
                {
                    int value = Integer.parseInt(a, 16);
                    bytes[pointer++] = (byte) (value & 0xFF);
                }
                catch(Exception e)
                {
                    pointer++;
                }
            }

            mMessage = new String(bytes);

            Pattern pattern = Pattern.compile("HTTP/1.0 (\\d{3})");
            Matcher m = pattern.matcher(mMessage);

            if(m.find())
            {
                try
                {
                    mHttpResponseCode = Integer.parseInt(m.group(1));
                }
                catch(Exception e)
                {
                    mLog.error("Unable to parse HTTP response code that was matched from message: " + m.group(1));
                }
            }
        }

        /**
         * Indicates if an HTTP response code was parsed from the message
         * @return true if it was parsed.
         */
        public boolean hasHttpResponseCode()
        {
            return mHttpResponseCode != -1;
        }

        /**
         * Response code parsed from the hexdump message
         * @return value.
         */
        public int getHttpResponseCode()
        {
            return mHttpResponseCode;
        }

        @Override
        public String toString()
        {
            return mMessage;
        }
    }

    public static void main(String[] args)
    {
        String text = "HTTP/1.0 403 Forbidden";

        Pattern pattern = Pattern.compile("HTTP/1.0 (\\d{3})");

        Matcher m = pattern.matcher(text);

        if(m.find())
        {
            mLog.info("Matching Group Count: " + m.groupCount());
            mLog.info("Match 0: " + m.group(0));
            mLog.info("Match 1: " + m.group(1));
        }
        else
        {
            mLog.info("No Match");
        }
    }
}
