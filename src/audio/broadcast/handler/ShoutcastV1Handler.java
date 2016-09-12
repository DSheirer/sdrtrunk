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
package audio.broadcast.handler;

import audio.AudioPacket;
import audio.broadcast.Broadcaster;
import audio.broadcast.BroadcasterFactory;
import audio.broadcast.configuration.BroadcastFormat;
import audio.broadcast.configuration.BroadcastMetadata;
import audio.broadcast.configuration.ShoutcastV1Configuration;
import audio.convert.IAudioConverter;
import controller.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.wave.AudioPacketMonoWaveReader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ShoutcastV1Handler extends BroadcastHandler
{
    private final static Logger mLog = LoggerFactory.getLogger( ShoutcastV1Handler.class );

    private static final long RECONNECT_INTERVAL_MILLISECONDS = 15000; //15 seconds
    private long mLastConnectionAttempt = 0;

    private Socket mSocket;
    private DataOutputStream mOutputStream;
    private DataInputStream mInputStream;

    private static final int mBufferSize = 2000;

    /**
     * Creates a Shoutcast Version 1 broadcast handler.
     * @param configuration
     * @param audioConverter
     */
    public ShoutcastV1Handler(ShoutcastV1Configuration configuration, IAudioConverter audioConverter)
    {
        super(configuration, audioConverter);
    }

    /**
     * Shoutcast V1 Configuration information
     */
    private ShoutcastV1Configuration getShoutcastConfiguration()
    {
        return (ShoutcastV1Configuration)getBroadcastConfiguration();
    }

    /**
     * Process audio packets using Shoutcast V1 Protocol
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
                try
                {
                    send(convertedAudio);
                }
                catch(IOException e)
                {
                    mLog.error("Error while dispatching audio", e);
                    setBroadcastState(BroadcastState.BROADCAST_ERROR);
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

    }

    /**
     * Creates a connnection to the remote server using the shoutcast configuration information.  Once disconnected
     * following a successful connection, attempts to reestablish a connection on a set interval
     */
    private void createConnection()
    {
        if(!connected() && System.currentTimeMillis() - mLastConnectionAttempt >= RECONNECT_INTERVAL_MILLISECONDS)
        {
            createSocket();

            if(mSocket.isConnected())
            {
                mLog.debug("Sending credentials");

                StringBuilder sb = new StringBuilder();

                //Password
                sb.append(getShoutcastConfiguration().getPassword()).append(BroadcastMetadata.COMMAND_TERMINATOR);

                //Metadata
                sb.append(BroadcastMetadata.STREAM_NAME.encode(getShoutcastConfiguration().getStreamName()));
                sb.append(BroadcastMetadata.URL.encode(getShoutcastConfiguration().getURL()));
                sb.append(BroadcastMetadata.PUBLIC.encode(getShoutcastConfiguration().isPublic()));
                sb.append(BroadcastMetadata.GENRE.encode(getShoutcastConfiguration().getGenre()));
                sb.append(BroadcastMetadata.AUDIO_BIT_RATE.encode(getShoutcastConfiguration().getBitRate()));

                //End of connection string
                sb.append(BroadcastMetadata.COMMAND_TERMINATOR);

                try
                {
                    send(sb.toString());

                    if(isInvalidResponse())
                    {
                        mLog.error("Invalid response after sending metadata");
                        return;
                    }
                    else
                    {
                        setBroadcastState(BroadcastState.CONNECTED);
                    }
                }
                catch(IOException ioe)
                {
                    mLog.debug("Error while connecting to server", ioe);
                    setBroadcastState(BroadcastState.ERROR);
                }

                mLog.debug("Connection established");
            }
        }
    }

    /**
     * Sends the string data to the remote server
     * @param data to send
     * @throws IOException if there is an error communicating with the remote server
     */
    private void send(String data) throws IOException
    {
        mLog.debug("Sending [" + data + "]");
        if(data != null && !data.isEmpty() && mOutputStream != null)
        {
            mOutputStream.writeBytes(data);
        }
    }

    /**
     * Sends the byte data to the remote server
     * @param data to send
     * @throws IOException if there is an error communicating with the remote server
     */
    private void send(byte[] data) throws IOException
    {
        mLog.debug("Sending bytes:" + data.length);
        if(connected())
        {
            int sent = 0;

            while(sent < data.length)
            {
                int available = data.length - sent;

                int sending = available <mBufferSize ? available : mBufferSize;

                mOutputStream.write(data, sent, sending);

                sent += sending;
            }
        }
    }

    private String getResponse() throws IOException
    {
        if(mInputStream != null)
        {
            int bytesAvailable = mInputStream.available();

            if(bytesAvailable > 0)
            {
                byte[] responseBuffer = new byte[bytesAvailable];

                int bytesRead = 0;

                while(bytesRead < bytesAvailable)
                {
                    bytesRead += mInputStream.read(responseBuffer, bytesRead, bytesAvailable - bytesRead);
                }

                return new String(responseBuffer);
            }
        }

        return null;
    }

    private boolean isInvalidResponse()
    {
        String response;

        try
        {
            response = getResponse();
        }
        catch(IOException e)
        {
            mLog.error("Error while retrieving server response message", e);
            setBroadcastState(BroadcastState.ERROR);
            return false;
        }

        if(response != null && !response.isEmpty())
        {
            if(response.startsWith("OK"))
            {
                return false;
            }
            else if(response.startsWith("Invalid Password"))
            {
                setBroadcastState(BroadcastState.INVALID_PASSWORD);
                return true;
            }

            mLog.debug("Unrecognized server response:" + response);
            return true;
        }

        return false;
    }

    /**
     * Creates a socket connection to the remote server and sets the state to CONNECTING.
     */
    private void createSocket()
    {
        if(mSocket == null)
        {
            mLog.debug("Creating socket");
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


    public static void main(String[] args)
    {
        ShoutcastV1Configuration config = new ShoutcastV1Configuration(BroadcastFormat.MP3);
        config.setAlias("Test Configuration");
        config.setHost("localhost");
        config.setPort(8000);
        config.setPassword("denny3");
        config.setStreamName("Denny's Audio Broadcast Test");
        config.setGenre("Scanner Audio");
        config.setPublic(true);
        config.setURL("http://localhost:8000");
        config.setBitRate(16);

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
