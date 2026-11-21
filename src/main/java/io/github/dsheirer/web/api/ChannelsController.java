/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.web.api;

import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadataModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.settings.identifier.TalkgroupFormatSettings;
import io.github.dsheirer.web.api.RuntimeViews.ChannelStatusView;
import io.github.dsheirer.web.api.RuntimeViews.ChannelView;
import io.github.dsheirer.web.auth.Permission;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * Channels of the active playlist and their runtime status (read only): /api/v1/channels
 */
public class ChannelsController
{
    private final PlaylistManager mPlaylistManager;
    private final TalkgroupFormatSettings mFormat;

    /**
     * Constructs an instance
     * @param playlistManager with the channels, or null when it isn't available
     * @param format for identifiers in the channel status, or null
     */
    public ChannelsController(PlaylistManager playlistManager, TalkgroupFormatSettings format)
    {
        mPlaylistManager = playlistManager;
        mFormat = format;
    }

    public void register(RoutesConfig routes)
    {
        routes.get(API_V1 + "/channels", this::list, Permission.PLAYLIST_READ);
        routes.get(API_V1 + "/channels/{id}", this::get, Permission.PLAYLIST_READ);
        routes.get(API_V1 + "/channels/{id}/status", this::status, Permission.PLAYLIST_READ);
    }

    @OpenApi(path = API_V1 + "/channels", methods = HttpMethod.GET, tags = "Channels",
        summary = "Channels of the active playlist, in id order",
        queryParams = {
            @OpenApiParam(name = "system", description = "System name, ignoring case"),
            @OpenApiParam(name = "site", description = "Site name, ignoring case"),
            @OpenApiParam(name = "decoder", description = "Decoder type, for example P25_PHASE1"),
            @OpenApiParam(name = "processing", type = Boolean.class),
            @OpenApiParam(name = "autoStart", type = Boolean.class),
            @OpenApiParam(name = "limit", type = Integer.class, description = "1 to 500, default 50"),
            @OpenApiParam(name = "cursor", description = "nextCursor from the previous page")
        },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = ChannelView[].class)))
    private void list(Context ctx)
    {
        int limit = ApiSupport.getLimit(ctx);
        Long cursor = ApiSupport.getIdCursor(ctx);
        Predicate<Channel> filter = filter(ctx);
        List<Channel> rows = getChannels().stream()
            .filter(channel -> cursor == null || channel.getChannelID() > cursor)
            .filter(filter)
            .sorted(Comparator.comparingInt(Channel::getChannelID))
            .limit(limit + 1L)
            .toList();
        ctx.json(Page.of(rows, limit, channel -> String.valueOf(channel.getChannelID()), ChannelView::of));
    }

    @OpenApi(path = API_V1 + "/channels/{id}", methods = HttpMethod.GET, tags = "Channels", summary = "One channel",
        pathParams = @OpenApiParam(name = "id", type = Integer.class, required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ChannelView.class)),
            @OpenApiResponse(status = "404")
        })
    private void get(Context ctx)
    {
        ctx.json(ChannelView.of(find(ApiSupport.getIdParam(ctx, "id"))));
    }

    @OpenApi(path = API_V1 + "/channels/{id}/status", methods = HttpMethod.GET, tags = "Channels",
        summary = "Runtime state of the channel: one entry per timeslot while it is processing",
        pathParams = @OpenApiParam(name = "id", type = Integer.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = ChannelStatusResponse.class)))
    private void status(Context ctx)
    {
        Channel channel = find(ApiSupport.getIdParam(ctx, "id"));
        ctx.json(new ChannelStatusResponse(channel.getChannelID(), channel.isProcessing(),
            getStatus(mPlaylistManager, mFormat).stream()
                .filter(status -> status.channelId() == channel.getChannelID()).toList()));
    }

    /**
     * Status of a channel
     * @param timeslots runtime state per timeslot, empty when the channel isn't processing
     */
    public record ChannelStatusResponse(int channelId, boolean processing, List<ChannelStatusView> timeslots) {}

    /**
     * Current status of every processing channel timeslot, including traffic channels.
     */
    public static List<ChannelStatusView> getStatus(PlaylistManager playlistManager, TalkgroupFormatSettings format)
    {
        if(playlistManager == null)
        {
            return List.of();
        }

        ChannelMetadataModel model = playlistManager.getChannelProcessingManager().getChannelMetadataModel();
        List<ChannelStatusView> statuses = new ArrayList<>();

        for(ChannelMetadata metadata: model.getChannelMetadata())
        {
            statuses.add(ChannelStatusView.of(metadata, model.getChannelFromMetadata(metadata), format));
        }

        return statuses;
    }

    private Predicate<Channel> filter(Context ctx)
    {
        String system = ApiSupport.getTextParam(ctx, "system");
        String site = ApiSupport.getTextParam(ctx, "site");
        String decoder = ApiSupport.getTextParam(ctx, "decoder");
        Boolean processing = ctx.queryParam("processing") != null ?
            RecordFilters.flag(ctx::queryParam, "processing", false) : null;
        Boolean autoStart = ctx.queryParam("autoStart") != null ?
            RecordFilters.flag(ctx::queryParam, "autoStart", false) : null;

        return channel -> (system == null || system.equalsIgnoreCase(channel.getSystem())) &&
            (site == null || site.equalsIgnoreCase(channel.getSite())) &&
            (decoder == null || (channel.getDecodeConfiguration() != null &&
                channel.getDecodeConfiguration().getDecoderType().name().equals(decoder.toUpperCase(Locale.ROOT)))) &&
            (processing == null || channel.isProcessing() == processing) &&
            (autoStart == null || channel.isAutoStart() == autoStart);
    }

    private Channel find(long id)
    {
        return getChannels().stream().filter(channel -> channel.getChannelID() == id).findFirst()
            .orElseThrow(() -> ApiException.notFound("No channel with id " + id));
    }

    private List<Channel> getChannels()
    {
        if(mPlaylistManager == null)
        {
            throw ApiException.unavailable("Channels aren't available");
        }

        return snapshot(mPlaylistManager.getChannelModel().getChannels());
    }

    /**
     * Copies a list that another thread (the user interface or the playlist loader) may change while it is copied.
     */
    static <T> List<T> snapshot(List<T> list)
    {
        for(int attempt = 0; ; attempt++)
        {
            try
            {
                return new ArrayList<>(list);
            }
            catch(ConcurrentModificationException | IndexOutOfBoundsException e)
            {
                if(attempt >= 5)
                {
                    throw ApiException.unavailable("The channel list is changing; try again");
                }

                Thread.onSpinWait();
            }
        }
    }
}
