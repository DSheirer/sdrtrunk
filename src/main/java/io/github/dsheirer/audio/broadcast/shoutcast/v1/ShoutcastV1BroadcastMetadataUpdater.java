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
package io.github.dsheirer.audio.broadcast.shoutcast.v1;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.IBroadcastMetadataUpdater;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.util.ThreadPool;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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

public class ShoutcastV1BroadcastMetadataUpdater implements IBroadcastMetadataUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(ShoutcastV1BroadcastMetadataUpdater.class);
    private final static String UTF8 = "UTF-8";

    private ShoutcastV1Configuration mShoutcastV1Configuration;
    private AliasModel mAliasModel;
    private NioSocketConnector mSocketConnector;
    private AtomicBoolean mUpdating = new AtomicBoolean();
    private Queue<String> mMetadataQueue = new LinkedTransferQueue<>();
    private boolean mStackTraceLoggingSuppressed;

    /**
     * Shoutcast song metadata updater.  Each metadata update is processed in the order received and the dispatch of
     * the HTTP update request is processed in a separate thread (runnable) to avoid delaying streaming of this
     * broadcaster.  When multiple metadata updates are received prior to completion of the current ongoing update
     * sequence, those updates will be queued and processed in the order received.
     */
    public ShoutcastV1BroadcastMetadataUpdater(ShoutcastV1Configuration shoutcastV1Configuration, AliasModel aliasModel)
    {
        mShoutcastV1Configuration = shoutcastV1Configuration;
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

//            mSocketConnector.getFilterChain().addLast("logger",
//                new LoggingFilter(ShoutcastV1BroadcastMetadataUpdater.class));

            mSocketConnector.getFilterChain().addLast("http_client_codec", new HttpClientCodec());

            //Each metadata update session is single-use, so we'll shut it down upon success or failure
            mSocketConnector.setHandler(new IoHandlerAdapter()
            {
                @Override
                public void exceptionCaught(IoSession session, Throwable cause) throws Exception
                {
                    //Single-use session - close it after we receive an error
                    session.closeNow();
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

        if(mUpdating.compareAndSet(false, true))
        {
            String song = mMetadataQueue.poll();

            while(song != null)
            {
                HttpRequest updateRequest = createUpdateRequest(song);

                if(updateRequest != null)
                {
                    ThreadPool.CACHED.submit(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                ConnectFuture connectFuture = getSocketConnector()
                                    .connect(new InetSocketAddress(mShoutcastV1Configuration.getHost(),
                                        mShoutcastV1Configuration.getPort()));
                                connectFuture.awaitUninterruptibly();
                                IoSession session = connectFuture.getSession();

                                if(session != null)
                                {
                                    session.write(updateRequest);
                                }
                            }
                            catch(Exception e)
                            {
                                Throwable throwableCause = e.getCause();

                                if(throwableCause instanceof ConnectException)
                                {
                                    //Do nothing, the server is unavailable
                                }
                                else
                                {
                                    if(!mStackTraceLoggingSuppressed)
                                    {
                                        mLog.error("Error sending metadata update.  Future errors will " +
                                            "be suppressed", e);

                                        mStackTraceLoggingSuppressed = true;
                                    }
                                }
                            }
                        }
                    });
                }

                //Fetch next metadata update to send
                song = mMetadataQueue.poll();
            }

            mUpdating.set(false);
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
                    sb.append("TO:").append(streamAs.get().getStreamTalkgroupAlias().getValue());
                }
                else
                {
                    sb.append("TO:").append(to);
                }

                if(!aliases.isEmpty())
                {
                    sb.append(" ").append(Joiner.on(", ").skipNulls().join(aliases));
                }
            }
            else
            {
                sb.append("TO:UNKNOWN");
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
                sb.append(" FROM:").append(from);

                List<Alias> aliases = aliasList.getAliases(from);

                if(!aliases.isEmpty())
                {
                    sb.append(" ").append(Joiner.on(", ").skipNulls().join(aliases));
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
            sb.append("pass=").append(mShoutcastV1Configuration.getPassword());
            sb.append("&mode=updinfo");
            sb.append("&song=").append(URLEncoder.encode(song, UTF8));

            Map<String,String> headers = new HashMap<>();

            HttpRequestImpl request = new HttpRequestImpl(HttpVersion.HTTP_1_0, HttpMethod.GET, "/admin.cgi",
                sb.toString(), headers);

            return request;
        }
        catch(UnsupportedEncodingException e)
        {
            //This should never happen
            mLog.error("UTF-8 encoding is not supported - can't update song metadata");
        }

        return null;
    }
}
