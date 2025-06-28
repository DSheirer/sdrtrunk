/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.shoutcast.v2;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AudioStreamingBroadcaster;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.IBroadcastMetadataUpdater;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.AuthenticateBroadcast;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.CacheableXMLMetadata;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.ConfigureIcyName;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.ConfigureIcyPublic;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.MP3Audio;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.NegotiateMaxPayloadSize;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.RequestCipher;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.SetupBroadcast;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.Standby;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.StreamMimeType;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.UltravoxMessage;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.UltravoxMessageFactory;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.UltravoxMessageType;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.UltravoxMetadata;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox.UltravoxProtocolFactory;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.math3.util.FastMath;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShoutcastV2AudioStreamingBroadcaster extends AudioStreamingBroadcaster implements IBroadcastMetadataUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(ShoutcastV2AudioStreamingBroadcaster.class);
    private static final long RECONNECT_INTERVAL_MILLISECONDS = 30000; //30 seconds
    private int mMaxPayloadSize = 16377;

    private NioSocketConnector mSocketConnector;
    private IoSession mStreamingSession = null;
    private AliasModel mAliasModel;
    private long mLastConnectionAttempt = 0;
    private AtomicBoolean mConnecting = new AtomicBoolean();
    private LinkedTransferQueue<UltravoxMessage> mMetadataMessageQueue = new LinkedTransferQueue<>();


    /**
     * Shoutcast 2.x (Ultravox 2.1) broadcaster.  This broadcaster is compatible with Shoutcast 2.x and newer versions
     * of the server software.
     *
     * Note: use @see ShoutcastV1AudioBroadcaster for Shoutcast version 1.x and older.
     *
     * This broadcaster uses the Apache Mina library for the streaming socket connection.  The
     * ShoutcastV2IOHandler manages all interaction with the Shoutcast server and manages the overall broadcast state.
     *
     * @param configuration for the Shoutcast V2 stream
     */
    public ShoutcastV2AudioStreamingBroadcaster(ShoutcastV2Configuration configuration, InputAudioFormat inputAudioFormat,
                                                MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(configuration, inputAudioFormat, mp3Setting);
        mAliasModel = aliasModel;
    }

    public ShoutcastV2Configuration getConfiguration()
    {
        return (ShoutcastV2Configuration) getBroadcastConfiguration();
    }

    /**
     * Broadcasts the audio frame or sequence
     */
    @Override
    protected void broadcastAudio(byte[] audio, IdentifierCollection identifierCollection)
    {
        //Dispatch any queued metadata messages
        UltravoxMessage metadataMessage = mMetadataMessageQueue.poll();

        while(metadataMessage != null)
        {
            mStreamingSession.write(metadataMessage);
            metadataMessage = mMetadataMessageQueue.poll();
        }

        //Dispatch audio message
        if(audio != null && audio.length > 0 && connect() && mStreamingSession != null && mStreamingSession.isConnected())
        {
            MP3Audio mp3Audio = new MP3Audio();
            mp3Audio.setPayload(audio);

            mStreamingSession.write(mp3Audio);
        }
    }

    @Override
    public void update(IdentifierCollection identifierCollection)
    {
        List<UltravoxMessage> metadataMessages = getMetadataMessages(identifierCollection);

        if(!metadataMessages.isEmpty())
        {
            mMetadataMessageQueue.addAll(metadataMessages);
        }
    }

    @Override
    protected IBroadcastMetadataUpdater getMetadataUpdater()
    {
        return this;
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
//                    new LoggingFilter(ShoutcastV2AudioBroadcaster.class));

                mSocketConnector.getFilterChain().addLast("codec",
                    new ProtocolCodecFilter(new UltravoxProtocolFactory()));

                mSocketConnector.setHandler(new ShoutcastV2IOHandler());
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
                            mLog.error("Failed to connect", rie);
                        }
                        else
                        {
                            setBroadcastState(BroadcastState.ERROR);
                            mLog.error("Failed to connect - no exception is available");
                        }

                        disconnect();
                    }
                    catch(Throwable t)
                    {
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
        if(connected() && mStreamingSession != null)
        {
            mStreamingSession.closeNow();
        }
        else
        {
            mLastConnectionAttempt = System.currentTimeMillis();
        }
    }

    /**
     * Encodes the list of metadata in one or more Ultravox CacheableXMLMetadata messages according to the maximum negotiated
     * payload size for the current connection.  Each entry in the list of metadata strings should be an xml encoded
     * value:  <tag>value</tag>
     *
     * See UltravoxMetadata.TAG.asXML(String value)
     *
     * @param identifierCollection containing audio metadata tags and attributes
     * @return a sequence of CacheableXMLMetadata messages sufficient to carry the complete set of metadata
     */
    private List<UltravoxMessage> getMetadataMessages(IdentifierCollection identifierCollection)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><metadata>");

        try
        {
            sb.append(UltravoxMetadata.ALBUM_TITLE.asXML(getConfiguration().getName()));

            sb.append(UltravoxMetadata.BROADCAST_CLIENT_APPLICATION.asXML(SystemProperties.getInstance().getApplicationName()));

            if(getConfiguration().hasGenre())
            {
                sb.append(UltravoxMetadata.GENRE.asXML(getConfiguration().getGenre()));
            }

            if(getConfiguration().hasURL())
            {
                sb.append(UltravoxMetadata.URL.asXML(getConfiguration().getURL()));
            }

            if(identifierCollection != null)
            {
                StringBuilder sbTitle2 = new StringBuilder();

                AliasList aliasList = mAliasModel.getAliasList(identifierCollection);

                Identifier to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.PATCH_GROUP, Role.TO);

                if(to == null)
                {
                    to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);
                }

                if(to == null)
                {
                    List<Identifier> toIdentifiers = identifierCollection.getIdentifiers(Role.TO);

                    if(!toIdentifiers.isEmpty())
                    {
                        to = toIdentifiers.get(0);
                    }
                }

                if(to != null)
                {
                    List<Alias> aliases = aliasList.getAliases(to);

                    //Check for 'Stream As Talkgroup' alias and use this instead of the decoded TO value.
                    Optional<Alias> streamAs = aliases.stream().filter(alias -> alias.getStreamTalkgroupAlias() != null).findFirst();

                    if(streamAs.isPresent())
                    {
                        sbTitle2.append("TO:").append(streamAs.get().getStreamTalkgroupAlias().getValue());
                    }
                    else
                    {
                        sbTitle2.append("TO:").append(to);

                        if(!aliases.isEmpty())
                        {
                            sbTitle2.append(" ").append(Joiner.on(", ").skipNulls().join(aliases));
                        }
                    }
                }
                else
                {
                    sbTitle2.append("TO:UNKNOWN");
                }

                Identifier from = identifierCollection.getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

                if(from == null)
                {
                    List<Identifier> fromIdentifiers = identifierCollection.getIdentifiers(Role.FROM);

                    if(!fromIdentifiers.isEmpty())
                    {
                        from = fromIdentifiers.get(0);
                    }
                }

                if(from != null)
                {
                    sbTitle2.append(" FROM:").append(from);

                    List<Alias> aliases = aliasList.getAliases(from);

                    if(!aliases.isEmpty())
                    {
                        sbTitle2.append(" ").append(Joiner.on(", ").skipNulls().join(aliases));
                    }
                }
                else
                {
                    sbTitle2.append(" FROM:UNKNOWN");
                }

                sb.append(UltravoxMetadata.TITLE_2.asXML(sbTitle2.toString()));
            }
            else
            {
                sb.append(UltravoxMetadata.TITLE_2.asXML("Scanning ..."));
            }
        }
        catch(UnsupportedEncodingException uee)
        {
            mLog.error("UTF-8 Encoding is not supported - shoutcast/ultravox metadata will not be updated");
        }

        sb.append("</metadata>");

        byte[] xml = sb.toString().getBytes();

        int pointer = 0;
        int messageCounter = 1;
        int messageCount = (int) FastMath.ceil((double) xml.length / (double) (mMaxPayloadSize - 6));

        if(messageCount > 32)
        {
            messageCount = 32; //Max number of metadata messages in a sequence
        }


        List<UltravoxMessage> messages = new ArrayList<>();

        while(pointer < xml.length && messageCounter <= messageCount)
        {
            int payloadSize = FastMath.min(mMaxPayloadSize - 6, xml.length - pointer);

            byte[] payload = new byte[payloadSize + 6];

            payload[1] = (byte) (0x01);
            payload[3] = (byte) (messageCount & 0xFF);
            payload[5] = (byte) (messageCounter & 0xFF);

            System.arraycopy(xml, pointer, payload, 6, payloadSize);

            CacheableXMLMetadata message = new CacheableXMLMetadata();
            message.setPayload(payload);
            messages.add(message);

            pointer += payloadSize;
        }

        return messages;
    }


    /**
     * IO Handler for managing Shoutcast V2 connection and credentials
     */
    public class ShoutcastV2IOHandler extends IoHandlerAdapter
    {
        /**
         * Sends stream configuration and user credentials upon connecting to remote server
         */
        @Override
        public void sessionOpened(IoSession session) throws Exception
        {
            session.write(UltravoxMessageFactory.getMessage(UltravoxMessageType.REQUEST_CIPHER));
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception
        {
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
            if(cause instanceof IOException)
            {
                IOException ioe = (IOException)cause;

                if(ioe.getMessage() != null)
                {
                    String reason = ioe.getMessage();

                    if(reason.startsWith("Connection reset"))
                    {
                        mLog.info("Streaming connection reset by remote server - reestablishing connection");
                        disconnect();
                    }
                    else if(reason.startsWith("Operation timed out"))
                    {
                        mLog.info("Streaming connection timed out - resetting connection");
                        disconnect();
                    }
                    else
                    {
                        setBroadcastState(BroadcastState.ERROR);
                        disconnect();
                        mLog.error("Unrecognized IO error: " + reason + ". Streaming halted.");
                    }
                }
                else
                {
                    setBroadcastState(BroadcastState.ERROR);
                    disconnect();
                    mLog.error("Unspecified IO error - streaming halted.");
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
            if(object instanceof UltravoxMessage)
            {
                UltravoxMessage ultravoxMessage = (UltravoxMessage) object;

                switch(ultravoxMessage.getMessageType())
                {
                    case REQUEST_CIPHER:
                        String cipherKey = ((RequestCipher) ultravoxMessage).getCipher();

                        AuthenticateBroadcast authenticateBroadcast = new AuthenticateBroadcast();

                        authenticateBroadcast.setCredentials(cipherKey, getConfiguration().getStreamID(),
                            getConfiguration().getUserID(), getConfiguration().getPassword());

                        session.write(authenticateBroadcast);
                        break;
                    case AUTHENTICATE_BROADCAST:
                        if(ultravoxMessage.isErrorResponse())
                        {
                            String errorMessage = ultravoxMessage.getErrorMessage();

                            if(errorMessage.startsWith(AuthenticateBroadcast.STREAM_ID_ERROR))
                            {
                                setBroadcastState(BroadcastState.INVALID_MOUNT_POINT);
                            }
                            else
                            {
                                mLog.error("Invalid Credentials - response: " + ultravoxMessage.getPayload());
                                setBroadcastState(BroadcastState.INVALID_CREDENTIALS);
                            }
                        }
                        else
                        {
                            StreamMimeType streamMimeType = new StreamMimeType();
                            streamMimeType.setFormat(getConfiguration().getBroadcastFormat());
                            session.write(streamMimeType);
                        }
                        break;
                    case STREAM_MIME_TYPE:
                        if(ultravoxMessage.isErrorResponse())
                        {
                            mLog.error("Unsupported Audio Format:" + getConfiguration().getBroadcastFormat().toString() +
                                " - " + ultravoxMessage.getErrorMessage());
                            setBroadcastState(BroadcastState.UNSUPPORTED_AUDIO_FORMAT);
                        }
                        else
                        {
                            SetupBroadcast setupBroadcast = new SetupBroadcast();

                            //Use the same value for average and minimum bit rates
                            setupBroadcast.setBitRate(getConfiguration().getBitRate(), getConfiguration().getBitRate());

                            session.write(setupBroadcast);
                        }
                        break;
                    case SETUP_BROADCAST:
                        if(ultravoxMessage.isErrorResponse())
                        {
                            mLog.error("Unsupported Audio Bit Rate:" + getConfiguration().getBitRate() +
                                " - " + ultravoxMessage.getErrorMessage());
                            setBroadcastState(BroadcastState.UNSUPPORTED_AUDIO_FORMAT);
                        }
                        else
                        {
                            NegotiateMaxPayloadSize negotiateMaxPayloadSize = new NegotiateMaxPayloadSize();

                            negotiateMaxPayloadSize.setMaximumPayloadSize(16377, 4192);

                            session.write(negotiateMaxPayloadSize);
                        }
                        break;
                    case NEGOTIATE_MAX_PAYLOAD_SIZE:
                        if(ultravoxMessage.isErrorResponse())
                        {
                            mLog.error("Unsupported maximum payload size (18 min - 36 kbps max) - " +
                                ultravoxMessage.getErrorMessage());
                            setBroadcastState(BroadcastState.UNSUPPORTED_AUDIO_FORMAT);
                        }
                        else
                        {
                            mMaxPayloadSize = ((NegotiateMaxPayloadSize) ultravoxMessage).getMaximumPayloadSize();

                            ConfigureIcyPublic configureIcyPublic = new ConfigureIcyPublic();
                            configureIcyPublic.setPublic(getConfiguration().isPublic());
                            session.write(configureIcyPublic);
                        }
                        break;
                    case CONFIGURE_ICY_PUBLIC:
                        if(ultravoxMessage.isErrorResponse())
                        {
                            mLog.error("Error setting shoutcast stream as public - " + ultravoxMessage.getErrorMessage());
                            setBroadcastState(BroadcastState.ERROR);
                        }
                        else
                        {
                            ConfigureIcyName configureIcyName = new ConfigureIcyName();
                            configureIcyName.setName(getConfiguration().getName());
                            session.write(configureIcyName);
                        }
                        break;
                    case CONFIGURE_ICY_NAME:
                        if(ultravoxMessage.isErrorResponse())
                        {
                            mLog.error("Error setting shoutcast stream name - " + ultravoxMessage.getErrorMessage());
                            setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
                        }
                        else
                        {
                            session.write(new Standby());
                        }
                        break;
                    case STANDBY:
                        if(ultravoxMessage.isErrorResponse())
                        {
                            mLog.error("Error following stream configuration and standby - " +
                                ultravoxMessage.getErrorMessage());
                            setBroadcastState(BroadcastState.ERROR);
                        }
                        else
                        {
                            setBroadcastState(BroadcastState.CONNECTED);
                        }
                        break;
                    default:
                        mLog.error("Unrecognized ultravox message:" + ultravoxMessage.getMessageType() +
                            " payload:" + ultravoxMessage.getPayload());

                        setBroadcastState(BroadcastState.ERROR);
                }
            }
            else
            {
                mLog.debug("Unrecognized message received from shoutcast v2 server:" + object.toString());

                setBroadcastState(BroadcastState.ERROR);
            }
        }
    }
}
