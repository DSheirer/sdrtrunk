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

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.database.event.EventSearchCriteria;
import io.github.dsheirer.gui.calls.CallAliasResolver;
import io.github.dsheirer.web.api.RecordViews.CallView;
import io.github.dsheirer.web.api.RecordViews.EventView;
import io.github.dsheirer.web.auth.Permission;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.dsheirer.web.api.ApiSupport.API_V1;

/**
 * Stored calls and decode events (read only): /api/v1/calls and /api/v1/events.  Newest first.  The cursor is an
 * offset, so rows that arrive between page requests shift later pages: to follow new activity use the calls and
 * events WebSocket topics, and the since parameter to fill a gap.
 */
public class RecordsController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordsController.class);
    private static final int MAX_OFFSET = 1_000_000;

    private final Supplier<CallDatabase> mDatabase;
    private final CallAliasResolver mAliases;

    /**
     * Constructs an instance
     * @param database supplies the open database, or null when it isn't available
     * @param aliasModel for alias names, or null
     */
    public RecordsController(Supplier<CallDatabase> database, AliasModel aliasModel)
    {
        mDatabase = database;
        mAliases = new CallAliasResolver(aliasModel);
    }

    public void register(RoutesConfig routes)
    {
        routes.get(API_V1 + "/calls", this::listCalls, Permission.EVENTS_READ);
        routes.get(API_V1 + "/calls/count", this::countCalls, Permission.EVENTS_READ);
        routes.get(API_V1 + "/calls/{id}", this::getCall, Permission.EVENTS_READ);
        routes.get(API_V1 + "/calls/{id}/audio", this::getAudio, Permission.AUDIO_LISTEN);
        routes.get(API_V1 + "/calls/{id}/download", this::download, Permission.AUDIO_DOWNLOAD);
        routes.get(API_V1 + "/events", this::listEvents, Permission.EVENTS_READ);
        routes.get(API_V1 + "/events/{id}", this::getEvent, Permission.EVENTS_READ);
    }

    @OpenApi(path = API_V1 + "/calls", methods = HttpMethod.GET, tags = "Calls", summary = "Stored calls, newest first",
        queryParams = {
            @OpenApiParam(name = "to", description = "Talkgroup, patch group or radio: value or text"),
            @OpenApiParam(name = "toValue", description = "Exactly this talkgroup or radio value, including calls " +
                "where it is patched into a patch group; not with to"),
            @OpenApiParam(name = "from", description = "Radio: value or text"),
            @OpenApiParam(name = "talkerAlias"),
            @OpenApiParam(name = "protocol", description = "For example APCO25, DMR"),
            @OpenApiParam(name = "type", description = "GROUP, PATCH, PRIVATE, TELEPHONE or UNKNOWN"),
            @OpenApiParam(name = "system", description = "Text that the system name contains"),
            @OpenApiParam(name = "since", description = "Calls that started at or after: ISO-8601 or epoch ms"),
            @OpenApiParam(name = "until", description = "Calls that started at or before: ISO-8601 or epoch ms"),
            @OpenApiParam(name = "limit", type = Integer.class, description = "1 to 500, default 50"),
            @OpenApiParam(name = "cursor", description = "nextCursor from the previous page")
        },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = CallView[].class)))
    private void listCalls(Context ctx)
    {
        CallSearchCriteria criteria = RecordFilters.calls(ctx::queryParam);
        int limit = ApiSupport.getLimit(ctx);
        int offset = getOffset(ctx);
        List<CallRecord> rows = query(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getCallRepository().search(connection, criteria, offset, limit + 1);
            }
        });
        ctx.json(page(rows, limit, offset, call -> CallView.of(call, null, mAliases)));
    }

    @OpenApi(path = API_V1 + "/calls/count", methods = HttpMethod.GET, tags = "Calls",
        summary = "Count of stored calls, or of the calls that match the filters",
        description = "Takes the same filters as GET /calls.",
        queryParams = {
            @OpenApiParam(name = "to"), @OpenApiParam(name = "toValue"), @OpenApiParam(name = "from"),
            @OpenApiParam(name = "talkerAlias"), @OpenApiParam(name = "protocol"), @OpenApiParam(name = "type"),
            @OpenApiParam(name = "system"), @OpenApiParam(name = "since"), @OpenApiParam(name = "until")
        },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = CountView.class)))
    private void countCalls(Context ctx)
    {
        CallSearchCriteria criteria = RecordFilters.calls(ctx::queryParam);
        long count = query(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getCallRepository().count(connection, criteria);
            }
        });
        ctx.json(new CountView(count));
    }

    /**
     * Count of records
     */
    public record CountView(long count) {}

    @OpenApi(path = API_V1 + "/calls/{id}", methods = HttpMethod.GET, tags = "Calls", summary = "One stored call",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = CallView.class)),
            @OpenApiResponse(status = "404")
        })
    private void getCall(Context ctx)
    {
        long id = ApiSupport.getIdParam(ctx, "id");
        CallRecord call = query(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getCallRepository().find(connection, id).orElse(null);
            }
        });

        if(call == null)
        {
            throw ApiException.notFound("No call with id " + id);
        }

        ctx.json(CallView.of(call, null, mAliases));
    }

    @OpenApi(path = API_V1 + "/calls/{id}/audio", methods = HttpMethod.GET, tags = "Calls",
        summary = "Recorded audio of a call, for playback in the browser",
        description = "The recording as sdrtrunk wrote it (MP3 or WAV).  Supports Range requests for seeking.",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = {
            @OpenApiResponse(status = "200", description = "audio/mpeg or audio/wav"),
            @OpenApiResponse(status = "404", description = "No such call, or it has no recording")
        })
    private void getAudio(Context ctx) throws java.io.IOException
    {
        java.nio.file.Path file = recording(ApiSupport.getIdParam(ctx, "id"));
        ctx.header("Cache-Control", "private, max-age=3600");
        ctx.writeSeekableStream(java.nio.file.Files.newInputStream(file), contentType(file),
            java.nio.file.Files.size(file));
    }

    @OpenApi(path = API_V1 + "/calls/{id}/download", methods = HttpMethod.GET, tags = "Calls",
        summary = "Download the recording of a call as a file",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = {
            @OpenApiResponse(status = "200", description = "audio/mpeg or audio/wav attachment"),
            @OpenApiResponse(status = "404", description = "No such call, or it has no recording")
        })
    private void download(Context ctx) throws java.io.IOException
    {
        java.nio.file.Path file = recording(ApiSupport.getIdParam(ctx, "id"));
        ctx.header("Content-Disposition", "attachment; filename=\"" +
            file.getFileName().toString().replace("\"", "") + "\"");
        ctx.contentType(contentType(file));
        ctx.result(java.nio.file.Files.newInputStream(file));
    }

    /**
     * File of a call's completed recording.  Only MP3 and WAV files that exist are served, so a call row can't be
     * used to read other files.
     */
    private java.nio.file.Path recording(long id)
    {
        CallRecord call = query(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getCallRepository().find(connection, id).orElse(null);
            }
        });

        if(call == null)
        {
            throw ApiException.notFound("No call with id " + id);
        }

        if(!call.isPlayable())
        {
            throw ApiException.notFound("The call has no recording");
        }

        java.nio.file.Path file = java.nio.file.Path.of(call.getFilePath()).normalize();
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);

        if(!(name.endsWith(".mp3") || name.endsWith(".wav")) || !java.nio.file.Files.isRegularFile(file))
        {
            throw ApiException.notFound("The call's recording file isn't available");
        }

        return file;
    }

    private static String contentType(java.nio.file.Path file)
    {
        return file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".wav") ? "audio/wav" :
            "audio/mpeg";
    }

    @OpenApi(path = API_V1 + "/events", methods = HttpMethod.GET, tags = "Events",
        summary = "Stored decode events, newest first",
        queryParams = {
            @OpenApiParam(name = "talkgroup", description = "TO talkgroup or patch group: value or text"),
            @OpenApiParam(name = "radio", description = "Radio in either role: value or text"),
            @OpenApiParam(name = "category",
                description = "VOICE_CALL, ENCRYPTED_CALL, DATA, COMMAND, REGISTRATION or OTHER"),
            @OpenApiParam(name = "type", description = "Decode event type, for example CALL_GROUP, GPS"),
            @OpenApiParam(name = "protocol"),
            @OpenApiParam(name = "system", description = "Text that the system name contains"),
            @OpenApiParam(name = "details", description = "Text that the details contain"),
            @OpenApiParam(name = "since", description = "ISO-8601 or epoch ms"),
            @OpenApiParam(name = "until", description = "ISO-8601 or epoch ms"),
            @OpenApiParam(name = "limit", type = Integer.class, description = "1 to 500, default 50"),
            @OpenApiParam(name = "cursor", description = "nextCursor from the previous page")
        },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = EventView[].class)))
    private void listEvents(Context ctx)
    {
        EventSearchCriteria criteria = RecordFilters.events(ctx::queryParam);
        int limit = ApiSupport.getLimit(ctx);
        int offset = getOffset(ctx);
        List<EventRecord> rows = query(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getEventRepository().search(connection, criteria, offset, limit + 1);
            }
        });
        ctx.json(page(rows, limit, offset, event -> EventView.of(event, null, mAliases)));
    }

    @OpenApi(path = API_V1 + "/events/{id}", methods = HttpMethod.GET, tags = "Events",
        summary = "One stored decode event",
        pathParams = @OpenApiParam(name = "id", type = Long.class, required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = EventView.class)),
            @OpenApiResponse(status = "404")
        })
    private void getEvent(Context ctx)
    {
        long id = ApiSupport.getIdParam(ctx, "id");
        EventRecord event = query(database -> {
            try(Connection connection = database.getConnection())
            {
                return database.getEventRepository().find(connection, id).orElse(null);
            }
        });

        if(event == null)
        {
            throw ApiException.notFound("No event with id " + id);
        }

        ctx.json(EventView.of(event, null, mAliases));
    }

    private static int getOffset(Context ctx)
    {
        Long cursor = ApiSupport.getIdCursor(ctx);

        if(cursor == null)
        {
            return 0;
        }

        if(cursor < 0 || cursor > MAX_OFFSET)
        {
            throw ApiException.invalid("cursor", "isn't a cursor from this collection");
        }

        return cursor.intValue();
    }

    private static <R,T> Page<T> page(List<R> rows, int limit, int offset, java.util.function.Function<R,T> view)
    {
        boolean more = rows.size() > limit;
        List<R> page = more ? rows.subList(0, limit) : rows;
        return new Page<>(page.stream().map(view).toList(), more ? String.valueOf(offset + limit) : null);
    }

    private <T> T query(SqlQuery<T> query)
    {
        CallDatabase database = mDatabase.get();

        if(database == null)
        {
            throw ApiException.unavailable("The call database isn't available");
        }

        try
        {
            return query.run(database);
        }
        catch(SQLException se)
        {
            LOGGER.error("Web API call or event query failed", se);
            throw new ApiException(500, "Database error", "The database request failed.  See the application log.");
        }
    }

    @FunctionalInterface
    private interface SqlQuery<T>
    {
        T run(CallDatabase database) throws SQLException;
    }
}
