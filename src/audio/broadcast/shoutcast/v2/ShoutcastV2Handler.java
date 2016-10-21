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
package audio.broadcast.shoutcast.v2;

import audio.AudioPacket;
import audio.broadcast.BroadcastFormat;
import audio.broadcast.BroadcastHandler;
import audio.broadcast.BroadcastState;
import audio.broadcast.Broadcaster;
import audio.broadcast.BroadcasterFactory;
import audio.broadcast.shoutcast.v2.message.AuthenticateBroadcast;
import audio.broadcast.shoutcast.v2.message.ConfigureIcyGenre;
import audio.broadcast.shoutcast.v2.message.ConfigureIcyName;
import audio.broadcast.shoutcast.v2.message.ConfigureIcyPublic;
import audio.broadcast.shoutcast.v2.message.ConfigureIcyURL;
import audio.broadcast.shoutcast.v2.message.MP3Audio;
import audio.broadcast.shoutcast.v2.message.NegotiateBufferSize;
import audio.broadcast.shoutcast.v2.message.NegotiateMaxPayloadSize;
import audio.broadcast.shoutcast.v2.message.RequestCipher;
import audio.broadcast.shoutcast.v2.message.SetupBroadcast;
import audio.broadcast.shoutcast.v2.message.Standby;
import audio.broadcast.shoutcast.v2.message.UltravoxMessage;
import audio.broadcast.shoutcast.v2.message.UltravoxMessageFactory;
import audio.broadcast.shoutcast.v2.message.UltravoxMessageType;
import audio.broadcast.shoutcast.v2.message.UltravoxMetadata;
import audio.broadcast.shoutcast.v2.message.XMLMetadata;
import audio.convert.IAudioConverter;
import controller.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import record.wave.AudioPacketMonoWaveReader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShoutcastV2Handler extends BroadcastHandler
{
    private final static Logger mLog = LoggerFactory.getLogger( ShoutcastV2Handler.class );

    private static final long RECONNECT_INTERVAL_MILLISECONDS = 15000; //15 seconds
    private static final boolean GET_RESPONSE = true;
    private static final boolean NO_RESPONSE_EXPECTED = false;
    private long mLastConnectionAttempt = 0;

    private Socket mSocket;
    private DataOutputStream mOutputStream;
    private DataInputStream mInputStream;
    private int mMaxPayloadSize = 16377;

    /**
     * Creates a Shoutcast Version 2 broadcast handler.
     * @param configuration details for shoutcast version 2
     * @param audioConverter to convert PCM audio
     */
    public ShoutcastV2Handler(ShoutcastV2Configuration configuration, IAudioConverter audioConverter)
    {
        super(configuration, audioConverter);
    }

    /**
     * Shoutcast V2 Configuration information
     */
    private ShoutcastV2Configuration getShoutcastConfiguration()
    {
        return (ShoutcastV2Configuration)getBroadcastConfiguration();
    }

    /**
     * Broadcast audio packets
     *
     * @param audioPackets
     */
    @Override
    public void broadcast(List<AudioPacket> audioPackets)
    {
        //If we're connected, send the audio, otherwise discard it
        if(connect())
        {
            byte[] convertedAudio = mAudioConverter.convert(audioPackets);

            if(convertedAudio != null)
            {
                List<UltravoxMessage> audioMessages = getAudioMessages(convertedAudio);

                try
                {
                    for(UltravoxMessage audioMessage: audioMessages)
                    {
                        send(audioMessage, NO_RESPONSE_EXPECTED);
                    }
                }
                catch(IOException e)
                {
                    mLog.error("Error while dispatching audio", e);
                    setBroadcastState(BroadcastState.BROADCAST_ERROR);
                    return;
                }
            }
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
//                    TerminateBroadcast terminate = (TerminateBroadcast)UltravoxMessageFactory
//                            .getMessage(UltravoxMessageType.TERMINATE_BROADCAST);
//                    response = send(terminate, out, in);
//                    mLog.debug("Terminate Response:" + (response.getPayload() != null ? response.getPayload() : ""));

    }

    /**
     * Sends the message.  When get response is true, reads from the input stream and converts the response to a
     * message that is returned.
     *
     * @param message to send to the shoutcast/ultravox server
     * @param getResponse true if the sent message will generate a response
     * @return response message when getResponse is set to true
     * @throws IOException if there is an issue while communicating with the server
     */
    private UltravoxMessage send(UltravoxMessage message, boolean getResponse) throws IOException
    {
        byte[] bytes = message.getMessage();

        mOutputStream.write(bytes);

        if(getResponse)
        {
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            int available = mInputStream.available();

            if(available > 0)
            {
                byte[] buffer = new byte[available];
                int read = mInputStream.read(buffer);

                return UltravoxMessageFactory.getMessage(buffer);
            }
        }

        return null;
    }

    /**
     * Loads the encoded audio byte array into a list of ultravox messages ready for transmission
     * @param audio to embed as message payload
     * @return list of messages containing the audio payload
     */
    private List<UltravoxMessage> getAudioMessages(byte[] audio)
    {
        List<UltravoxMessage> messages = new ArrayList<>();

        int pointer = 0;

        while(pointer < audio.length)
        {
            int payloadSize = Math.min(mMaxPayloadSize, audio.length - pointer);

            byte[] payload = new byte[payloadSize];

            System.arraycopy(audio, pointer, payload, 0, payloadSize);

            MP3Audio mp3Audio = (MP3Audio)UltravoxMessageFactory.getMessage(UltravoxMessageType.MP3_DATA);
            mp3Audio.setPayload(payload);
            messages.add(mp3Audio);

            pointer += payloadSize;
        }

        return messages;
    }

    /**
     * Encodes the list of metadata in one or more Ultravox XMLMetadata messages according to the maximum negotiated
     * payload size for the current connection.  Each entry in the list of metadata strings should be an xml encoded
     * value:  <tag>value</tag>
     *
     * See UltravoxMetadata.TAG.asXML(String value)
     *
     * @param xmlMetadata list of xml encoded values
     * @return a sequence of XMLMetadata messages sufficient to carry the complete set of metadata
     */
    private List<UltravoxMessage> getMetadataMessages(List<String> xmlMetadata)
    {
        List<UltravoxMessage> messages = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<metadata>\n");

        for(String metadata: xmlMetadata)
        {
            sb.append(metadata);
        }

        sb.append("\n</metadata>");

        mLog.debug("Metadata:" + sb.toString());
        byte[] xml = sb.toString().getBytes();

        int pointer = 0;
        int messageCounter = 1;
        int messageCount = (int)Math.ceil((double)xml.length / (double)(mMaxPayloadSize - 6));

        if(messageCount > 32)
        {
            messageCount = 32; //Max number of metadata messages in a sequence
        }


        while(pointer < xml.length && messageCounter <= messageCount)
        {
            int payloadSize = Math.min(mMaxPayloadSize - 6, xml.length - pointer);

            byte[] payload = new byte[payloadSize + 6];

            payload[1] = (byte)(0x01);
            payload[3] = (byte)(messageCount & 0xFF);
            payload[5] = (byte)(messageCounter & 0xFF);

            System.arraycopy(xml, pointer, payload, 6, payloadSize);

            XMLMetadata message = (XMLMetadata)UltravoxMessageFactory.getMessage(UltravoxMessageType.XML_METADATA);
            message.setPayload(payload);
            messages.add(message);

            pointer += payloadSize;
        }

        return messages;
    }


    /**
     * Creates a connnection to the remote server using the shoutcast configuration information.  Once disconnected
     * following a successful connection, attempts to reestablish a connection on a set interval
     */
    private void createConnection()
    {
        if(!connected() && System.currentTimeMillis() - mLastConnectionAttempt >= RECONNECT_INTERVAL_MILLISECONDS)
        {
            mLastConnectionAttempt = System.currentTimeMillis();

            createSocket();

            if (mSocket.isConnected())
            {
                RequestCipher requestCipher = (RequestCipher) UltravoxMessageFactory
                        .getMessage(UltravoxMessageType.REQUEST_CIPHER);

                try
                {
                    UltravoxMessage response = send(requestCipher, GET_RESPONSE);

                    if (response instanceof RequestCipher)
                    {
                        RequestCipher cipherResponse = (RequestCipher) response;

                        AuthenticateBroadcast authenticateBroadcast = (AuthenticateBroadcast) UltravoxMessageFactory
                                .getMessage(UltravoxMessageType.AUTHENTICATE_BROADCAST);
                        authenticateBroadcast.setCredentials(cipherResponse.getCipher(),
                                getShoutcastConfiguration().getStreamID(), getShoutcastConfiguration().getUserID(),
                                getShoutcastConfiguration().getPassword());
                        response = send(authenticateBroadcast, GET_RESPONSE);

                        if (response.isErrorResponse())
                        {
                            setBroadcastState(BroadcastState.INVALID_PASSWORD);
                            return;
                        }

                        UltravoxMessage streamMimeType = UltravoxMessageFactory
                                .getMessage(UltravoxMessageType.STREAM_MIME_TYPE);
                        streamMimeType.setPayload(getShoutcastConfiguration().getBroadcastFormat().getValue());
                        response = send(streamMimeType, GET_RESPONSE);

                        if (response.isErrorResponse())
                        {
                            setBroadcastState(BroadcastState.UNSUPPORTED_AUDIO_FORMAT);
                            mLog.error("Audio format [" + getShoutcastConfiguration().getBroadcastFormat() + "] not supported");
                            return;
                        }

                        SetupBroadcast setupBroadcast = (SetupBroadcast) UltravoxMessageFactory
                                .getMessage(UltravoxMessageType.SETUP_BROADCAST);
                        setupBroadcast.setBitRate(getShoutcastConfiguration().getBitRate(),
                                getShoutcastConfiguration().getBitRate());
                        response = send(setupBroadcast, GET_RESPONSE);

                        if (response.isErrorResponse())
                        {
                            setBroadcastState(BroadcastState.UNSUPPORTED_AUDIO_FORMAT);
                            mLog.error("Audio bit rate [" + getShoutcastConfiguration().getBitRate() + "] not supported");
                            return;
                        }

                        NegotiateMaxPayloadSize negotiateMaxPayloadSize = (NegotiateMaxPayloadSize) UltravoxMessageFactory
                                .getMessage(UltravoxMessageType.NEGOTIATE_MAX_PAYLOAD_SIZE);
                        negotiateMaxPayloadSize.setMaximumPayloadSize(mMaxPayloadSize, 14000);
                        response = send(negotiateMaxPayloadSize, GET_RESPONSE);
                        mMaxPayloadSize = ((NegotiateMaxPayloadSize) response).getMaximumPayloadSize();

                        List<String> metadata = new ArrayList<>();

                        if (getShoutcastConfiguration().getStreamName() != null)
                        {
                            metadata.add(UltravoxMetadata.TITLE_1.asXML(getShoutcastConfiguration().getStreamName()));

                            ConfigureIcyName icyName = (ConfigureIcyName) UltravoxMessageFactory
                                    .getMessage(UltravoxMessageType.CONFIGURE_ICY_NAME);
                            icyName.setName(getShoutcastConfiguration().getStreamName());
                            response = send(icyName, NO_RESPONSE_EXPECTED);
                        }

                        if (getShoutcastConfiguration().getStreamGenre() != null)
                        {
                            metadata.add(UltravoxMetadata.GENRE.asXML(getShoutcastConfiguration().getStreamGenre()));

                            ConfigureIcyGenre icyGenre = (ConfigureIcyGenre) UltravoxMessageFactory
                                    .getMessage(UltravoxMessageType.CONFIGURE_ICY_GENRE);
                            icyGenre.setGenre(getShoutcastConfiguration().getStreamGenre());
                            response = send(icyGenre, NO_RESPONSE_EXPECTED);
                        }

                        if (getShoutcastConfiguration().getURL() != null)
                        {
                            ConfigureIcyURL icyURL = (ConfigureIcyURL) UltravoxMessageFactory
                                    .getMessage(UltravoxMessageType.CONFIGURE_ICY_URL);
                            icyURL.setURL(getShoutcastConfiguration().getURL());
                            response = send(icyURL, NO_RESPONSE_EXPECTED);
                        }

                        ConfigureIcyPublic icyPublic = (ConfigureIcyPublic) UltravoxMessageFactory
                                .getMessage(UltravoxMessageType.CONFIGURE_ICY_PUBLIC);
                        icyPublic.setPublic(getShoutcastConfiguration().isPublic());
                        response = send(icyPublic, NO_RESPONSE_EXPECTED);

                        Standby standby = (Standby) UltravoxMessageFactory.getMessage(UltravoxMessageType.STANDBY);
                        response = send(standby, GET_RESPONSE);

                        if (response.isValidResponse())
                        {
                            setBroadcastState(BroadcastState.CONNECTED);
                        }
                        else
                        {
                            setBroadcastState(BroadcastState.DISCONNECTED);
                            mLog.error("Error message after configuration and sending standby:" +
                                    (response.getError() != null ? response.getError() : "no error message"));
                        }

                        metadata.add(UltravoxMetadata.BROADCAST_CLIENT_APPLICATION
                                .asXML(SystemProperties.getInstance().getApplicationName()));

                        mLog.debug("Fetching metadata messages from metadata...");
                        List<UltravoxMessage> messages = getMetadataMessages(metadata);

                        for (UltravoxMessage message : messages)
                        {
                            mLog.debug("Sending metadata message");
                            send(message, GET_RESPONSE);
                        }

                        mLog.debug("We're connected!!!");
                    }
                }
                catch (IOException e)
                {
                    setBroadcastState(BroadcastState.ERROR);
                    mLog.error("Error while creating connection to server", e);
                }
            }
        }
    }

    /**
     * Creates a socket connection to the remote server and sets the state to CONNECTING.
     */
    private void createSocket()
    {
        if(mSocket == null)
        {
            try
            {
                mSocket = new Socket(getShoutcastConfiguration().getHost(),
                                     getShoutcastConfiguration().getPort());
                mOutputStream = new DataOutputStream(mSocket.getOutputStream());
                mInputStream = new DataInputStream(mSocket.getInputStream());
            }
            catch(UnknownHostException uhe)
            {
                setBroadcastState(BroadcastState.UNKNOWN_HOST);
                mLog.error("Unknown host or port.  Unable to create connection to streaming server host[" +
                        getShoutcastConfiguration().getHost() + "] and port[" +
                        getShoutcastConfiguration().getPort() + "] - will reattempt connection periodically");
                return;
            }
            catch(IOException ioe)
            {
                setBroadcastState(BroadcastState.ERROR);

                mLog.error("Error connecting to streaming server host[" +
                        getShoutcastConfiguration().getHost() + "] and port[" +
                        getShoutcastConfiguration().getPort() + "]", ioe);
                return;
            }
        }

        if(mSocket.isConnected())
        {
            setBroadcastState(BroadcastState.CONNECTING);
        }
        else
        {
            try
            {
                SocketAddress address = getShoutcastConfiguration().getAddress();
                mSocket.connect(address);
                setBroadcastState(BroadcastState.CONNECTING);
            }
            catch(UnknownHostException uhe)
            {
                setBroadcastState(BroadcastState.UNKNOWN_HOST);
            }
            catch(IOException e)
            {
                setBroadcastState(BroadcastState.ERROR);

                mLog.error("Error connecting to streaming server host[" +
                        getShoutcastConfiguration().getHost() + "] and port[" +
                        getShoutcastConfiguration().getPort() + "]", e);
            }
        }
    }

    public static UltravoxMessage send(UltravoxMessage message,
                                       DataOutputStream out,
                                       DataInputStream in,
                                       int maxPayloadSize) throws IOException
    {
        byte[] bytes = message.getMessage();

        out.write(bytes);

        try
        {
            Thread.sleep(50);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        int available = in.available();

        if(available > 0)
        {
            byte[] buffer = new byte[available];
            int read = in.read(buffer);

            return UltravoxMessageFactory.getMessage(buffer);
        }

        return null;
    }


    public static void main(String[] args)
    {
        boolean test = false;

        ShoutcastV2Configuration config = new ShoutcastV2Configuration(BroadcastFormat.MP3);
        config.setStreamID(1);
        config.setAlias("Test Configuration");
        config.setHost("localhost");
        config.setPort(8000);
        config.setPassword("denny3");
        config.setStreamName("Denny's Audio Broadcast Test");
        config.setStreamGenre("Public Safety");
        config.setPublic(true);
        config.setURL("http://localhost:8000");
        config.setBitRate(16);

        if(test)
        {
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
