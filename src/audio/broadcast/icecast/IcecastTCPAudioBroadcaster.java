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
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Realm;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcecastTCPAudioBroadcaster extends AudioBroadcaster
{
    private final static Logger mLog = LoggerFactory.getLogger( IcecastTCPAudioBroadcaster.class );
    private final static String UTF8 = "UTF-8";
    private final static String TERMINATOR = "\r\n";
    private final static String SEPARATOR = ":";

    private static final long RECONNECT_INTERVAL_MILLISECONDS = 15000; //15 seconds
    private long mLastConnectionAttempt = 0;
    private AtomicBoolean mConnecting = new AtomicBoolean();
    private Socket mSocket;
    private DataOutputStream mOutputStream;
    private DataInputStream mInputStream;
    private DefaultAsyncHttpClient mDefaultAsyncHttpClient;
    private boolean mFirstMetadataUpdateSuppressed = false;
    private byte[] mSilenceFrame;

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

        mDefaultAsyncHttpClient = httpClient;
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
        boolean completed = false;

        while(!completed)
        {
            if(connect())
            {
                try
                {
                    send(audio);
                    completed = true;
                }
                catch(SocketException se)
                {
                    //The remote server likely disconnected - setup to reconnect
                    mLog.error("Resetting Icecast TCP Audio Broadcaster - socket error: " + se.getMessage());
                    disconnect();
                }
                catch(Exception e)
                {
                    completed = true;
                    mLog.error("Error sending audio", e);
                }
            }
            else
            {
                completed = true;
            }
        }
    }

    /**
     * Broadcasts an audio metadata update
     */
    @Override
    protected void broadcastMetadata(AudioMetadata metadata)
    {
        if(connected() && mFirstMetadataUpdateSuppressed)
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

            try
            {
                if(mDefaultAsyncHttpClient == null)
                {
                    mDefaultAsyncHttpClient = new DefaultAsyncHttpClient();
                }

                String songEncoded = URLEncoder.encode(sb.toString(), UTF8);

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
        else if(connected())
        {
            mFirstMetadataUpdateSuppressed = true;
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
        if(canConnect())
        {
            createConnection();
        }

        return connected();
    }


    /**
     * Disconnect from the remote broadcast server and cleanup input/output streams and socket connection
     */
    public void disconnect()
    {
        mLog.info("Disconnecting Icecast TCP audio broadcaster");

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

        if(!isErrorState())
        {
            setBroadcastState(BroadcastState.READY);
        }
    }

    /**
     * Creates a connection to the remote server using the icecast configuration information.  Once disconnected
     * following a successful connection, successive calls to this method will attempt to reestablish a connection on a
     * minimum reconnection attempt interval
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
                checkConnectionAttemptResponse();
            }

            mConnecting.set(false);
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
     * Creates a socket connection to the remote server and sets the state to CONNECTING or indicates an error state
     * if the socket is unable to connect to the remote server with the current configuration information.
     */
    private void createSocket()
    {
        if(mSocket == null)
        {
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
            catch(ConnectException ce)
            {
                setBroadcastState(BroadcastState.NO_SERVER);
                mLog.error("Connection refused.  Unable to create connection to streaming server host[" +
                        getConfiguration().getHost() + "] and port[" +
                        getConfiguration().getPort() + "]");
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
     *
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

    /**
     * Obtains any pending response from the remote server.
     * @return string response
     * @throws IOException if there is an error
     */
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

    /**
     * Checks for a remote server response following a connection attempt and evaluates the response for indication of a
     * successful connection or an error state
     */
    private void checkConnectionAttemptResponse()
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
            else if(response.startsWith("HTTP/1.0 403 Mountpoint in use"))
            {
                setBroadcastState(BroadcastState.MOUNT_POINT_IN_USE);
            }
            else if(response.contains("Invalid Password") || response.contains("Authentication Required"))
            {
                setBroadcastState(BroadcastState.INVALID_PASSWORD);
            }
            else
            {
                mLog.error("Unrecognized server response:" + response);
                setBroadcastState(BroadcastState.ERROR);
            }
        }
        else
        {
            setBroadcastState(BroadcastState.CONNECTED);
        }
    }
}
