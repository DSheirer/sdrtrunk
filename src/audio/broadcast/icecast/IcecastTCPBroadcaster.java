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
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Realm;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import record.wave.AudioPacketMonoWaveReader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcecastTCPBroadcaster extends Broadcaster
{
    private final static Logger mLog = LoggerFactory.getLogger( IcecastTCPBroadcaster.class );
    private final static String UTF8 = "UTF-8";
    private final static String TERMINATOR = "\r\n";
    private final static String SEPARATOR = ":";

    private static final long RECONNECT_INTERVAL_MILLISECONDS = 15000; //15 seconds
    private long mLastConnectionAttempt = 0;
    private List<AudioPacket> mPacketsToBroadcast = new ArrayList<>();
    private AtomicBoolean mConnecting = new AtomicBoolean();
    private Socket mSocket;
    private DataOutputStream mOutputStream;
    private DataInputStream mInputStream;
    private DefaultAsyncHttpClient mDefaultAsyncHttpClient;

    private int mCounter;

    /**
     * Creates an Icecast 2.3.2 compatible broadcaster using TCP and a pseudo HTTP 1.0 protocol.  This broadcaster is
     * compatible with Icecast version 2.3.2 and older versions of the server software.
     *
     * Note: use @see IcecastHTTPBroadcaster for Icecast version 2.4.x and newer.
     *
     * @param configuration
     * @param audioConverter
     */
    public IcecastTCPBroadcaster(ThreadPoolManager threadPoolManager,
                                 IcecastTCPConfiguration configuration,
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
            mAudioQueue.drainTo(mPacketsToBroadcast, 5);

            if (!mPacketsToBroadcast.isEmpty())
            {
                byte[] convertedAudio = getAudioConverter().convert(mPacketsToBroadcast);

                mLog.debug("We have [" + mPacketsToBroadcast.size() +
                        "] packets - sending:" + convertedAudio.length + " bytes");

                try
                {
                    send(convertedAudio);
                }
                catch(SocketException se)
                {
                    disconnect();
                }
                catch(IOException e)
                {
                    mLog.error("Error sending audio", e);
                }

                mPacketsToBroadcast.clear();

                mCounter++;

                if(mCounter % 10 == 0)
                {
                    updateMetadata("Song Count " + mCounter);
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
        if(mOutputStream != null)
        {
            try
            {
                mOutputStream.flush();
                mOutputStream.close();
            }
            catch(IOException ioe)
            {
                mLog.debug("Error closing output stream", ioe);
            }

            mOutputStream = null;
        }

        if(mInputStream != null)
        {
            try
            {
                mInputStream.close();
            }
            catch(IOException ioe)
            {
                mLog.debug("Error closing input stream", ioe);
            }

            mInputStream = null;
        }

        if(mSocket != null)
        {
            try
            {
                mSocket.close();
            }
            catch(IOException ioe)
            {
                mLog.error("Error closing socket", ioe);
            }

            mSocket = null;
        }

        mLog.debug("Icecast stream is now disconnected");
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

                createSocket();

                if(mSocket.isConnected())
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SOURCE ").append(getConfiguration().getMountPoint());
                    sb.append(" HTTP/1.0").append(TERMINATOR);

                    sb.append("Authorization: ").append(getConfiguration().getEncodedCredentials()).append(TERMINATOR);
                    sb.append(IcecastHeader.USER_AGENT.getValue()).append(SEPARATOR)
                            .append(SystemProperties.getInstance().getApplicationName()).append(TERMINATOR);
                    sb.append(IcecastHeader.CONTENT_TYPE.getValue()).append(SEPARATOR)
                            .append(getConfiguration().getBroadcastFormat().getValue()).append(TERMINATOR);

                    sb.append(IcecastHeader.PUBLIC.getValue()).append(SEPARATOR)
                            .append(getConfiguration().isPublic() ? "1" : "0").append(TERMINATOR);

                    if(getConfiguration().hasName())
                    {
                        sb.append(IcecastHeader.NAME.getValue()).append(SEPARATOR)
                                .append(getConfiguration().getName()).append(TERMINATOR);
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

                    sb.append(getAudioInfoMetadata());

                    sb.append(TERMINATOR).append(TERMINATOR);

                    try
                    {
                        mLog.debug("Sending connection string");
                        send(sb.toString());
                    }
                    catch(IOException e)
                    {
                        mLog.error("Error while connecting with ...\n" + sb.toString(), e);
                    }
                }

                mLog.debug("Checking response ...");
                checkResponse();
            }

            mConnecting.set(false);
        }
    }

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
     * Creates a socket connection to the remote server and sets the state to CONNECTING.
     */
    private void createSocket()
    {
        if(mSocket == null)
        {
            mLog.debug("Creating socket");
            try
            {
                mSocket = new Socket(getConfiguration().getHost(),
                        getConfiguration().getPort());
                mOutputStream = new DataOutputStream(mSocket.getOutputStream());
                mInputStream = new DataInputStream(mSocket.getInputStream());
            }
            catch(UnknownHostException uhe)
            {
                setBroadcastState(BroadcastState.UNKNOWN_HOST);
                mLog.error("Unknown host or port.  Unable to create connection to streaming server host[" +
                        getConfiguration().getHost() + "] and port[" +
                        getConfiguration().getPort() + "] - will reattempt connection periodically");
                return;
            }
            catch(IOException ioe)
            {
                setBroadcastState(BroadcastState.ERROR);

                mLog.error("Error connecting to streaming server host[" +
                        getConfiguration().getHost() + "] and port[" +
                        getConfiguration().getPort() + "]", ioe);
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
                SocketAddress address = getConfiguration().getAddress();
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
                        getConfiguration().getHost() + "] and port[" +
                        getConfiguration().getPort() + "]", e);
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
        if(connected())
        {
            int sent = 0;

            while(sent < data.length)
            {
                int available = data.length - sent;

                mOutputStream.write(data, sent, available);

                sent += available;
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

    private void checkResponse()
    {
        String response = null;

        try
        {
            Thread.sleep(250);

            response = getResponse();
        }
        catch(InterruptedException ie)
        {
            mLog.debug("Interrupted ...", ie);
        }
        catch(IOException e)
        {
            mLog.error("Error while retrieving server response message", e);
            setBroadcastState(BroadcastState.ERROR);
        }

        if(response != null && !response.isEmpty())
        {
            if(response.startsWith("HTTP/1.0 200 OK"))
            {
                setBroadcastState(BroadcastState.CONNECTED);
            }
            else if(response.startsWith("Invalid Password"))
            {
                setBroadcastState(BroadcastState.INVALID_PASSWORD);
            }
            else
            {
                mLog.debug("Unrecognized server response:" + response);
                setBroadcastState(BroadcastState.ERROR);
            }
        }
        else
        {
            mLog.debug("Response was empty");
        }
    }

    private void updateMetadata(String songName)
    {
        if(songName != null && !songName.isEmpty())
        {
            try
            {
                if(mDefaultAsyncHttpClient == null)
                {
                    mDefaultAsyncHttpClient = new DefaultAsyncHttpClient();
                }

                String songEncoded = URLEncoder.encode(songName, UTF8);

                StringBuilder query = new StringBuilder();
                query.append("/admin/metadata?mode=updinfo");
                query.append("&mount=").append(getConfiguration().getMountPoint());
                query.append("&charset=UTF%2d8");
                query.append("&song=").append(songEncoded);

                Uri uri = new Uri(Uri.HTTP, null, getConfiguration().getHost(), getConfiguration().getPort(),
                        query.toString(), null);

                BoundRequestBuilder builder = mDefaultAsyncHttpClient.prepareGet(uri.toUrl());

                //Use Basic (base64) authentication
                Realm realm = new Realm.Builder(getConfiguration().getUserName(), getConfiguration().getPassword())
                        .setScheme(Realm.AuthScheme.BASIC).setUsePreemptiveAuth(true).build();
                builder.setRealm(realm);
                builder.addHeader(IcecastHeader.USER_AGENT.getValue(), SystemProperties.getInstance().getApplicationName());

                mDefaultAsyncHttpClient.executeRequest(builder.build());
            }
            catch(Exception e)
            {
                mLog.debug("Error while updating metadata", e);
            }
        }
    }

    public static void main(String[] args)
    {
        boolean test = false;

        IcecastTCPConfiguration config = new IcecastTCPConfiguration(BroadcastFormat.MP3);

        config.setName("Broadcastify SDRTrunk #2");
        config.setHost("audio3.broadcastify.com");
        config.setPort(80);
        config.setMountPoint("/k0yrdpx7zn4h");
        config.setUserName("source");
        config.setPassword("k8j9405n");
        config.setDescription("SDRTrunk Test Feed #2");
        config.setGenre("Scanner");
        config.setPublic(false);
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
