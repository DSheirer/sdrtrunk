/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.icecast;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.IBroadcastMetadataUpdater;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import org.apache.mina.core.RuntimeIoException;
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

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.channels.UnresolvedAddressException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcecastBroadcastMetadataUpdater implements IBroadcastMetadataUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(IcecastBroadcastMetadataUpdater.class);
    private final static String UTF8 = "UTF-8";

    private IcecastConfiguration mIcecastConfiguration;
    private AliasModel mAliasModel;
    private NioSocketConnector mSocketConnector;
    private AtomicBoolean mUpdating = new AtomicBoolean();
    private Queue<String> mMetadataQueue = new LinkedTransferQueue<>();
    private Map<String,String> mHTTPHeaders;
    private boolean mStackTraceLoggingSuppressed;

    /**
     * Icecast song metadata updater.  Each metadata update is processed in the order received and the dispatch of
     * the HTTP update request is processed in a separate thread (runnable) to avoid delaying streaming of this
     * broadcaster.  When multiple metadata updates are received prior to completion of the current ongoing update
     * sequence, those updates will be queued and processed in the order received.
     */
    public IcecastBroadcastMetadataUpdater(IcecastConfiguration icecastConfiguration, AliasModel aliasModel)
    {
        mIcecastConfiguration = icecastConfiguration;
        mAliasModel = aliasModel;
    }

    /**
     * Socket connector - lazy constructor.
     */
    private NioSocketConnector getSocketConnector()
    {
        if(mSocketConnector == null)
        {
            mSocketConnector = new NioSocketConnector();
//            mSocketConnector.getFilterChain().addLast("logger", new LoggingFilter(IcecastBroadcastMetadataUpdater.class));
            mSocketConnector.getFilterChain().addLast("http_client_codec", new HttpClientCodec());

            //Each metadata update session is single-use, so we'll shut it down upon success or failure
            mSocketConnector.setHandler(new IoHandlerAdapter()
            {
                @Override
                public void exceptionCaught(IoSession session, Throwable cause) throws Exception
                {
                    //Single-use session - close it after we receive an error
                    session.closeNow();

                    mLog.error("Metadata update failed");
                }

                @Override
                public void messageReceived(IoSession session, Object message) throws Exception
                {
                    //Single-use session - close it after we receive a response
                    session.closeNow();
                }
            });
        }

        return mSocketConnector;
    }

    /**
     * Sends a song metadata update to the remote server.  Additional metadata updates received while the updater
     * is in the process of sending an existing metadata update will be queued and processed in received order.
     *
     * Note: due to thread timing, there is a slight chance that a concurrent update request will not be processed
     * and will remain in the update queue until the next metadata update is requested.  However, this is a design
     * trade-off to avoid having a scheduled runnable repeatedly processing the update queue.
     */
    public void update(IdentifierCollection identifierCollection)
    {
        mMetadataQueue.offer(getSong(identifierCollection));

        if(!mUpdating.get() && !mMetadataQueue.isEmpty())
        {
            Runnable runnable =  new Runnable()
            {
                @Override
                public void run()
                {
                    if(!mMetadataQueue.isEmpty() && mUpdating.compareAndSet(false, true))
                    {
                        try
                        {
                            ConnectFuture connectFuture = getSocketConnector()
                                .connect(new InetSocketAddress(mIcecastConfiguration.getHost(),
                                    mIcecastConfiguration.getPort()));
                            connectFuture.awaitUninterruptibly();
                            IoSession session = connectFuture.getSession();

                            if(session != null)
                            {
                                String song = mMetadataQueue.poll();

                                while(song != null)
                                {
                                    HttpRequest updateRequest = createUpdateRequest(song);

                                    if(updateRequest != null)
                                    {
                                        session.write(updateRequest);
                                    }

                                    //Fetch next metadata update to send
                                    song = mMetadataQueue.poll();
                                }

                                session.closeNow();
                            }
                        }
                        catch(UnresolvedAddressException | RuntimeIoException ure)
                        {
                            //Do nothing - the server is temporarily unavailable
                        }
                        catch(Exception e)
                        {
                            Throwable throwableCause = e.getCause();

                            if(!mStackTraceLoggingSuppressed)
                            {
                                mLog.error("Error sending metadata update.  Future errors will be suppressed", e);
                                mStackTraceLoggingSuppressed = true;
                            }
                        }

                        mUpdating.set(false);
                    }
                }
            };

            ThreadPool.SCHEDULED.schedule(runnable, 0l, TimeUnit.SECONDS);
        }
    }

    /**
     * Creates the song information for a metadata update
     */
    private String getSong(IdentifierCollection identifierCollection)
    {
        StringBuilder sb = new StringBuilder();

        if(identifierCollection != null)
        {
            AliasList aliasList = mAliasModel.getAliasList(identifierCollection);

            Identifier to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.PATCH_GROUP, Role.TO);

            if(to == null)
            {
                to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);
            }

            if(to != null)
            {
                sb.append("TO:").append(to);

                Alias toAlias = aliasList != null ? aliasList.getAlias(to) : null;

                if(toAlias != null)
                {
                    sb.append(" ").append(toAlias.getName());
                }
            }
            else
            {
                sb.append("TO:UNKNOWN");
            }

            Identifier from = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.FROM);

            if(from != null)
            {
                sb.append(" FROM:").append(from);

                Alias fromAlias = aliasList != null ? aliasList.getAlias(from) : null;

                if(fromAlias != null)
                {
                    sb.append(" ").append(fromAlias.getName());
                }
            }
            else
            {
                sb.append(" FROM:UNKNOWN");
            }
        }
        else
        {
            sb.append("Scanning ....");
        }

        return sb.toString();
    }

    /**
     * Creates an HTTP GET request to update the metadata on the remote server
     */
    private HttpRequest createUpdateRequest(String song)
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            sb.append("mode=updinfo");
            sb.append("&mount=").append(URLEncoder.encode(mIcecastConfiguration.getMountPoint(), UTF8));
            sb.append("&charset=UTF%2d8");
            sb.append("&song=").append(URLEncoder.encode(song, UTF8));

            HttpRequestImpl request = new HttpRequestImpl(HttpVersion.HTTP_1_1, HttpMethod.GET, "/admin/metadata",
                sb.toString(), getHTTPHeaders());

            return request;
        }
        catch(UnsupportedEncodingException e)
        {
            //This should never happen
            mLog.error("UTF-8 encoding is not supported - can't update song metadata");
        }

        return null;
    }

    /**
     * HTTP headers to use for a metadata update request.
     */
    private Map<String,String> getHTTPHeaders()
    {
        if(mHTTPHeaders == null)
        {
            mHTTPHeaders = new HashMap<>();
            mHTTPHeaders.put(IcecastHeader.USER_AGENT.getValue(), SystemProperties.getInstance().getApplicationName());
            mHTTPHeaders.put(IcecastHeader.AUTHORIZATION.getValue(), mIcecastConfiguration.getBase64EncodedCredentials());
        }

        return mHTTPHeaders;
    }
}
