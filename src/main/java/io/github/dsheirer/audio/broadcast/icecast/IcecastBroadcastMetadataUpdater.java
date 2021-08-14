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

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.IBroadcastMetadataUpdater;
import io.github.dsheirer.audio.broadcast.icecast.IcecastConfiguration;
import io.github.dsheirer.audio.broadcast.icecast.IcecastMetadata;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class IcecastBroadcastMetadataUpdater implements IBroadcastMetadataUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(IcecastBroadcastMetadataUpdater.class);
    private final static String UTF8 = "UTF-8";
    private HttpClient mHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private IcecastConfiguration mIcecastConfiguration;
    private AliasModel mAliasModel;
    private boolean mConnectionLoggingSuppressed = false;

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
     * Sends a song metadata update to the remote server.  Additional metadata updates received while the updater
     * is in the process of sending an existing metadata update will be queued and processed in received order.
     *
     * Note: due to thread timing, there is a slight chance that a concurrent update request will not be processed
     * and will remain in the update queue until the next metadata update is requested.  However, this is a design
     * trade-off to avoid having a scheduled runnable repeatedly processing the update queue.
     */
    public void update(IdentifierCollection identifierCollection)
    {
        if(mIcecastConfiguration.hasInline())
        {
            // Never send out-of-band metadata if inline metadata is enabled.
            return;
        }

        StringBuilder sb = new StringBuilder();

        try
        {
            sb.append("http://");
            sb.append(mIcecastConfiguration.getHost());
            sb.append(":");
            sb.append(mIcecastConfiguration.getPort());
            sb.append("/admin/metadata?mode=updinfo&mount=");
            sb.append(URLEncoder.encode(mIcecastConfiguration.getMountPoint(), UTF8));
            sb.append("&charset=UTF%2d8");
            sb.append("&song=").append(URLEncoder.encode(IcecastMetadata.getTitle(identifierCollection, mAliasModel), UTF8));
        }
        catch(UnsupportedEncodingException uee)
        {
            mLog.error("Error encoding metadata information to UTF-8", uee);
            sb = null;
        }

        if(sb != null)
        {
            final String metadataUpdateURL = sb.toString();
            URI uri = URI.create(metadataUpdateURL);

            ThreadPool.CACHED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .header(IcecastHeader.AUTHORIZATION.getValue(), mIcecastConfiguration.getBase64EncodedCredentials())
                            .header(IcecastHeader.USER_AGENT.getValue(), SystemProperties.getInstance().getApplicationName())
                            .GET()
                            .build();

                        HttpResponse<String> response = null;

                        try
                        {
                            response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        }
                        catch(IOException ioe)
                        {
                            if(!mConnectionLoggingSuppressed)
                            {
                                mLog.error("IO Error submitting Icecast metadata update [" +
                                    (metadataUpdateURL != null ? metadataUpdateURL : "no url"), ioe);
                                mConnectionLoggingSuppressed = true;
                            }
                        }
                        catch(InterruptedException ie)
                        {
                            mLog.error("Interrupted Exception Error", ie);
                        }

                        if(response != null)
                        {
                            if(response.statusCode() == 200)
                            {
                                mConnectionLoggingSuppressed = false;
                            }
                            else
                            {
                                if(!mConnectionLoggingSuppressed)
                                {
                                    mLog.info("Error submitting Icecast 2 Metadata update to URL [" + metadataUpdateURL +
                                        "] HTTP Response Code [" + response.statusCode() + "] Body [" + response.body() + "]");
                                    mConnectionLoggingSuppressed = true;
                                }
                            }
                        }
                    }
                    catch(Throwable t)
                    {
                        mLog.error("There was an error submitting an Icecast metadata update", t);
                    }
                }
            });
        }
    }

}
