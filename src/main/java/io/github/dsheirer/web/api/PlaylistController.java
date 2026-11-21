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

import com.fasterxml.jackson.databind.JsonNode;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.auth.AccessManager;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.playlist.PlaylistConfig.AliasConfig;
import io.github.dsheirer.web.playlist.PlaylistConfig.ChannelConfig;
import io.github.dsheirer.web.playlist.PlaylistJson;
import io.github.dsheirer.web.playlist.PlaylistService;
import io.github.dsheirer.web.playlist.PlaylistService.Versioned;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.util.List;
import java.util.Map;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * Editing the active playlist: channel configuration, channel start and stop, aliases and decoder defaults.
 *
 * Single resources return their version in the ETag header.  PUT and DELETE need it back as If-Match: 428 when it is
 * missing, 412 when the resource changed since it was read (by another web client or in the desktop editors).
 */
public class PlaylistController
{
    private final PlaylistService mService;
    private final AccountService mAccountService;

    /**
     * Constructs an instance
     * @param service for the playlist, or null when it isn't available
     * @param accountService for the audit log
     */
    public PlaylistController(PlaylistService service, AccountService accountService)
    {
        mService = service;
        mAccountService = accountService;
    }

    public void register(RoutesConfig routes)
    {
        routes.post(API_V1 + "/channels", this::createChannel, Permission.PLAYLIST_WRITE);
        routes.get(API_V1 + "/channels/{id}/configuration", this::getChannel, Permission.PLAYLIST_READ);
        routes.put(API_V1 + "/channels/{id}/configuration", this::updateChannel, Permission.PLAYLIST_WRITE);
        routes.delete(API_V1 + "/channels/{id}", this::deleteChannel, Permission.PLAYLIST_WRITE);
        routes.post(API_V1 + "/channels/{id}/start", this::startChannel, Permission.CHANNEL_CONTROL);
        routes.post(API_V1 + "/channels/{id}/stop", this::stopChannel, Permission.CHANNEL_CONTROL);
        routes.get(API_V1 + "/decoders", this::listDecoders, Permission.PLAYLIST_READ);
        routes.get(API_V1 + "/alias-lists", this::listAliasLists, Permission.PLAYLIST_READ);
        routes.get(API_V1 + "/aliases", this::listAliases, Permission.PLAYLIST_READ);
        routes.post(API_V1 + "/aliases", this::createAlias, Permission.PLAYLIST_WRITE);
        routes.get(API_V1 + "/aliases/{id}", this::getAlias, Permission.PLAYLIST_READ);
        routes.put(API_V1 + "/aliases/{id}", this::updateAlias, Permission.PLAYLIST_WRITE);
        routes.delete(API_V1 + "/aliases/{id}", this::deleteAlias, Permission.PLAYLIST_WRITE);
    }

    //Channels

    @OpenApi(path = API_V1 + "/channels", methods = HttpMethod.POST, tags = "Channels",
        summary = "Create a channel in the active playlist",
        description = "source and decode use the playlist file's field names; GET /decoders has the defaults for " +
            "each decoder.",
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = ChannelConfig.class), required = true),
        responses = {
            @OpenApiResponse(status = "201", content = @OpenApiContent(from = ChannelConfig.class)),
            @OpenApiResponse(status = "400", description = "Invalid configuration")
        })
    private void createChannel(Context ctx)
    {
        ChannelConfig request = ApiSupport.body(ctx, ChannelConfig.class);
        Versioned<ChannelConfig> created = service().createChannel(request);
        audit(ctx, "channel.create", created.value().id() + " " + created.value().name());
        ctx.header("Location", API_V1 + "/channels/" + created.value().id() + "/configuration");
        respond(ctx.status(201), created);
    }

    @OpenApi(path = API_V1 + "/channels/{id}/configuration", methods = HttpMethod.GET, tags = "Channels",
        summary = "A channel's full configuration, with its version in the ETag header",
        pathParams = @OpenApiParam(name = "id", type = Integer.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = ChannelConfig.class)))
    private void getChannel(Context ctx)
    {
        respond(ctx, service().getChannel(channelId(ctx)));
    }

    @OpenApi(path = API_V1 + "/channels/{id}/configuration", methods = HttpMethod.PUT, tags = "Channels",
        summary = "Replace a channel's configuration",
        description = "Needs If-Match.  A processing channel keeps running with its old configuration until it is " +
            "restarted; the response says so with restartRequired.",
        pathParams = @OpenApiParam(name = "id", type = Integer.class, required = true),
        headers = @OpenApiParam(name = "If-Match", required = true, description = "ETag from the last GET"),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = ChannelConfig.class), required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ChannelUpdateView.class)),
            @OpenApiResponse(status = "412", description = "Changed since it was read"),
            @OpenApiResponse(status = "428", description = "If-Match is missing")
        })
    private void updateChannel(Context ctx)
    {
        ChannelConfig request = ApiSupport.body(ctx, ChannelConfig.class);
        PlaylistService.ChannelUpdate update = service().updateChannel(channelId(ctx), ctx.header("If-Match"),
            request);
        audit(ctx, "channel.update", update.channel().value().id() + " " + update.channel().value().name());
        ctx.header("ETag", update.channel().version());
        ctx.json(new ChannelUpdateView(update.channel().value(), update.restartRequired()));
    }

    /**
     * Updated channel configuration
     * @param restartRequired true when the channel is processing and uses the new configuration after a restart
     */
    public record ChannelUpdateView(ChannelConfig configuration, boolean restartRequired) {}

    @OpenApi(path = API_V1 + "/channels/{id}", methods = HttpMethod.DELETE, tags = "Channels",
        summary = "Delete a channel, stopping it first",
        pathParams = @OpenApiParam(name = "id", type = Integer.class, required = true),
        headers = @OpenApiParam(name = "If-Match", required = true, description = "ETag from the last GET"),
        responses = {
            @OpenApiResponse(status = "204"),
            @OpenApiResponse(status = "412", description = "Changed since it was read")
        })
    private void deleteChannel(Context ctx)
    {
        int id = channelId(ctx);
        service().deleteChannel(id, ctx.header("If-Match"));
        audit(ctx, "channel.delete", String.valueOf(id));
        ctx.status(204);
    }

    @OpenApi(path = API_V1 + "/channels/{id}/start", methods = HttpMethod.POST, tags = "Channels",
        summary = "Start processing a channel",
        pathParams = @OpenApiParam(name = "id", type = Integer.class, required = true),
        responses = {
            @OpenApiResponse(status = "204"),
            @OpenApiResponse(status = "409", description = "The channel couldn't start, for example no tuner")
        })
    private void startChannel(Context ctx)
    {
        int id = channelId(ctx);
        service().start(id);
        audit(ctx, "channel.start", String.valueOf(id));
        ctx.status(204);
    }

    @OpenApi(path = API_V1 + "/channels/{id}/stop", methods = HttpMethod.POST, tags = "Channels",
        summary = "Stop processing a channel",
        pathParams = @OpenApiParam(name = "id", type = Integer.class, required = true),
        responses = @OpenApiResponse(status = "204"))
    private void stopChannel(Context ctx)
    {
        int id = channelId(ctx);
        service().stop(id);
        audit(ctx, "channel.stop", String.valueOf(id));
        ctx.status(204);
    }

    @OpenApi(path = API_V1 + "/decoders", methods = HttpMethod.GET, tags = "Channels",
        summary = "Decoders that a channel can use, each with its default decode configuration",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = DecoderView[].class)))
    private void listDecoders(Context ctx)
    {
        List<DecoderView> decoders = DecoderType.PRIMARY_DECODERS.stream()
            .map(type -> new DecoderView(type.name(), type.getDisplayString(),
                type.getProtocol() != null ? type.getProtocol().name() : null,
                PlaylistJson.toTree(DecoderFactory.getDecodeConfiguration(type))))
            .toList();
        ctx.json(new Page<>(decoders, null));
    }

    /**
     * A decoder
     * @param defaults default decode configuration, to use as the decode field of a channel
     */
    public record DecoderView(String decoder, String label, String protocol, JsonNode defaults) {}

    //Aliases

    @OpenApi(path = API_V1 + "/alias-lists", methods = HttpMethod.GET, tags = "Aliases",
        summary = "Alias lists with their alias counts",
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = AliasListView[].class)))
    private void listAliasLists(Context ctx)
    {
        Map<String,Integer> lists = service().getAliasLists();
        ctx.json(new Page<>(lists.entrySet().stream()
            .map(entry -> new AliasListView(entry.getKey(), entry.getValue())).toList(), null));
    }

    public record AliasListView(String name, int aliasCount) {}

    @OpenApi(path = API_V1 + "/aliases", methods = HttpMethod.GET, tags = "Aliases",
        summary = "Aliases in id order",
        queryParams = {
            @OpenApiParam(name = "list", description = "Only this alias list"),
            @OpenApiParam(name = "limit", type = Integer.class, description = "1 to 500, default 50"),
            @OpenApiParam(name = "cursor", description = "nextCursor from the previous page")
        },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = AliasConfig[].class)))
    private void listAliases(Context ctx)
    {
        int limit = ApiSupport.getLimit(ctx);
        Long cursor = ApiSupport.getIdCursor(ctx);
        List<Versioned<AliasConfig>> rows = service().listAliases(ApiSupport.getTextParam(ctx, "list"),
            cursor != null ? cursor : 0, limit);
        ctx.json(Page.of(rows, limit, alias -> String.valueOf(alias.value().id()), Versioned::value));
    }

    @OpenApi(path = API_V1 + "/aliases", methods = HttpMethod.POST, tags = "Aliases", summary = "Create an alias",
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = AliasConfig.class), required = true),
        responses = @OpenApiResponse(status = "201", content = @OpenApiContent(from = AliasConfig.class)))
    private void createAlias(Context ctx)
    {
        Versioned<AliasConfig> created = service().createAlias(ApiSupport.body(ctx, AliasConfig.class));
        audit(ctx, "alias.create", created.value().id() + " " + created.value().name());
        ctx.header("Location", API_V1 + "/aliases/" + created.value().id());
        respond(ctx.status(201), created);
    }

    @OpenApi(path = API_V1 + "/aliases/{id}", methods = HttpMethod.GET, tags = "Aliases",
        summary = "An alias, with its version in the ETag header",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = AliasConfig.class)))
    private void getAlias(Context ctx)
    {
        respond(ctx, service().getAlias(ApiSupport.getIdParam(ctx, "id")));
    }

    @OpenApi(path = API_V1 + "/aliases/{id}", methods = HttpMethod.PUT, tags = "Aliases", summary = "Replace an alias",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        headers = @OpenApiParam(name = "If-Match", required = true, description = "ETag from the last GET"),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = AliasConfig.class), required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = AliasConfig.class)),
            @OpenApiResponse(status = "412", description = "Changed since it was read")
        })
    private void updateAlias(Context ctx)
    {
        Versioned<AliasConfig> updated = service().updateAlias(ApiSupport.getIdParam(ctx, "id"),
            ctx.header("If-Match"), ApiSupport.body(ctx, AliasConfig.class));
        audit(ctx, "alias.update", updated.value().id() + " " + updated.value().name());
        respond(ctx, updated);
    }

    @OpenApi(path = API_V1 + "/aliases/{id}", methods = HttpMethod.DELETE, tags = "Aliases", summary = "Delete an alias",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        headers = @OpenApiParam(name = "If-Match", required = true, description = "ETag from the last GET"),
        responses = @OpenApiResponse(status = "204"))
    private void deleteAlias(Context ctx)
    {
        long id = ApiSupport.getIdParam(ctx, "id");
        service().deleteAlias(id, ctx.header("If-Match"));
        audit(ctx, "alias.delete", String.valueOf(id));
        ctx.status(204);
    }

    //Support

    private PlaylistService service()
    {
        if(mService == null)
        {
            throw ApiException.unavailable("The playlist isn't available");
        }

        return mService;
    }

    private static int channelId(Context ctx)
    {
        long id = ApiSupport.getIdParam(ctx, "id");

        if(id < 0 || id > Integer.MAX_VALUE)
        {
            throw ApiException.notFound("No channel with id " + id);
        }

        return (int)id;
    }

    private static void respond(Context ctx, Versioned<?> versioned)
    {
        ctx.header("ETag", versioned.version());
        ctx.json(versioned.value());
    }

    private void audit(Context ctx, String action, String target)
    {
        mAccountService.audit(AccessManager.principal(ctx).username(), action, target, true, ctx.ip(), null);
    }
}
