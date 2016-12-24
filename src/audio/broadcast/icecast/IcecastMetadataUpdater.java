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

import controller.ThreadPoolManager;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.http.HttpClientCodec;
import org.apache.mina.http.HttpRequestImpl;
import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpRequest;
import org.apache.mina.http.api.HttpVersion;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcecastMetadataUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(IcecastMetadataUpdater.class);
    private final static String UTF8 = "UTF-8";

    private ThreadPoolManager mThreadPoolManager;
    private IcecastConfiguration mIcecastConfiguration;

    private NioSocketConnector mSocketConnector;
    private AtomicBoolean mUpdating = new AtomicBoolean();
    private Queue<String> mMetadataQueue = new LinkedTransferQueue<>();
    private Map<String, String> mHTTPHeaders;
    private boolean mStackTraceLoggingSuppressed;

    /**
     * Icecast song metadata updater.  Each metadata update is processed in the order received and the dispatch of
     * the HTTP update request is processed in a separate thread (runnable) to avoid delaying streaming of this
     * broadcaster.  When multiple metadata updates are received prior to completion of the current ongoing update
     * sequence, those updates will be queued and processed in the order received.
     */
    public IcecastMetadataUpdater(ThreadPoolManager threadPoolManager, IcecastConfiguration icecastConfiguration)
    {
        mThreadPoolManager = threadPoolManager;
        mIcecastConfiguration = icecastConfiguration;
    }

    /**
     * Initializes the network socket and HTTP headers map
     */
    private void init()
    {
        if (mSocketConnector == null)
        {
            mSocketConnector = new NioSocketConnector();
//                mSocketConnector.getFilterChain().addLast("logger", new LoggingFilter(IcecastMetadataUpdater.class));
            mSocketConnector.getFilterChain().addLast("http_client_codec", new HttpClientCodec());

            //Each metadata update session is single-use, so we'll shut it down upon success or failure
            mSocketConnector.setHandler(new IoHandlerAdapter()
            {
                @Override
                public void exceptionCaught(IoSession session, Throwable cause) throws Exception
                {
//                        mLog.debug("Session " + session.getId() + " metadata update complete - ERROR received");
                    session.closeNow();
                }

                @Override
                public void messageReceived(IoSession session, Object message) throws Exception
                {
//                        mLog.debug("Session " + session.getId() + " metadata update complete - response received");
                    session.closeNow();
                }
            });

            mHTTPHeaders = new HashMap<>();
            mHTTPHeaders.put(IcecastHeader.USER_AGENT.getValue(), SystemProperties.getInstance().getApplicationName());
            mHTTPHeaders.put(IcecastHeader.AUTHORIZATION.getValue(), mIcecastConfiguration.getBase64EncodedCredentials());
        }
    }

    /**
     * Sends a song metadata update to the remote server.  Additional metadata updates received while the updater
     * is in the process of sending an existing metadata update will be queued and processed in received order.
     *
     * Note: due to thread timing, there is a slight chance that a concurrent update request will not be processed
     * and will remain in the update queue until the next metadata update is requested.  However, this is a design
     * trade-off to avoid having a scheduled runnable repeatedly processing the update queue.
     */
    public void update(String metadata)
    {
        if (metadata != null)
        {
            mMetadataQueue.offer(metadata);

            if (mUpdating.compareAndSet(false, true))
            {
                String metadataUpdate = mMetadataQueue.poll();

                while (metadataUpdate != null)
                {
                    //Ensure we're setup for network communications (only happens once)
                    init();

                    HttpRequest updateRequest = createUpdateRequest(metadataUpdate);

                    if (updateRequest != null)
                    {
                        mThreadPoolManager.scheduleOnce(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    ConnectFuture connectFuture = mSocketConnector
                                        .connect(new InetSocketAddress(mIcecastConfiguration.getHost(),
                                            mIcecastConfiguration.getPort()));
                                    connectFuture.awaitUninterruptibly();
                                    IoSession session = connectFuture.getSession();

                                    if (session != null)
                                    {
                                        session.write(updateRequest);
                                    }
                                }
                                catch (Exception e)
                                {
                                    Throwable throwableCause = e.getCause();

                                    if (throwableCause instanceof ConnectException)
                                    {
                                        //Do nothing, the server is unavailable
                                    }
                                    else
                                    {
                                        if (!mStackTraceLoggingSuppressed)
                                        {
                                            mLog.error("Error sending metadata update.  Future errors will " +
                                                "be suppressed", e);

                                            mStackTraceLoggingSuppressed = true;
                                        }
                                    }
                                }
                            }
                        }, 0l, TimeUnit.SECONDS);
                    }

                    //Fetch next metadata update to send
                    metadataUpdate = mMetadataQueue.poll();
                }

                mUpdating.set(false);
            }
        }
    }

    /**
     * Creates an HTTP GET request to update the metadata on the remote server
     */
    private HttpRequest createUpdateRequest(String metadata)
    {
        try
        {
            String songEncoded = URLEncoder.encode(metadata, UTF8);
            StringBuilder sb = new StringBuilder();
            sb.append("mode=updinfo");
            sb.append("&mount=").append(mIcecastConfiguration.getMountPoint());
            sb.append("&charset=UTF%2d8");
            sb.append("&song=").append(songEncoded);

            return new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/admin/metadata",
                sb.toString(), mHTTPHeaders);
        }
        catch (UnsupportedEncodingException e)
        {
            //This should never happen
            mLog.error("UTF-8 encoding is not supported - can't update song metadata");
        }

        return null;
    }
}
