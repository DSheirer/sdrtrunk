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

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Request parsing helpers shared by the web API handlers.
 */
public final class ApiSupport
{
    public static final String API_V1 = "/api/v1";
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private ApiSupport()
    {
    }

    /**
     * Page size from the limit query parameter: 1 to 500, default 50.
     */
    public static int getLimit(Context ctx)
    {
        String value = ctx.queryParam("limit");

        if(value == null || value.isBlank())
        {
            return DEFAULT_LIMIT;
        }

        try
        {
            int limit = Integer.parseInt(value.trim());

            if(limit >= 1 && limit <= MAX_LIMIT)
            {
                return limit;
            }
        }
        catch(NumberFormatException nfe)
        {
            //Reported below
        }

        throw ApiException.invalid("limit", "must be an integer from 1 to " + MAX_LIMIT);
    }

    /**
     * Cursor query parameter as a row id, or null for the first page.
     */
    public static Long getIdCursor(Context ctx)
    {
        String value = ctx.queryParam("cursor");

        if(value == null || value.isBlank())
        {
            return null;
        }

        try
        {
            return Long.parseLong(value.trim());
        }
        catch(NumberFormatException nfe)
        {
            throw ApiException.invalid("cursor", "isn't a cursor from this collection");
        }
    }

    /**
     * Numeric id path parameter.
     */
    public static long getIdParam(Context ctx, String name)
    {
        try
        {
            return Long.parseLong(ctx.pathParam(name));
        }
        catch(NumberFormatException nfe)
        {
            throw ApiException.notFound("No " + name + " " + ctx.pathParam(name));
        }
    }

    /**
     * Timestamp query parameter: ISO-8601 (2026-11-11T14:32:01Z) or epoch milliseconds.
     * @return epoch milliseconds, or null when the parameter is absent
     */
    public static Long getTimeParam(Context ctx, String name)
    {
        String value = ctx.queryParam(name);

        if(value == null || value.isBlank())
        {
            return null;
        }

        value = value.trim();

        try
        {
            return Long.parseLong(value);
        }
        catch(NumberFormatException nfe)
        {
            try
            {
                return Instant.parse(value).toEpochMilli();
            }
            catch(DateTimeParseException dtpe)
            {
                throw ApiException.invalid(name, "must be an ISO-8601 time or epoch milliseconds");
            }
        }
    }

    /**
     * Optional text query parameter, or null when absent or blank.
     */
    public static String getTextParam(Context ctx, String name)
    {
        String value = ctx.queryParam(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Reads the JSON request body.  The request must declare a JSON content type: browsers can't send that across
     * sites without a CORS check, which protects the session cookie from cross-site form posts.
     * @param type of the body
     * @return parsed body, never null
     */
    public static <T> T body(Context ctx, Class<T> type)
    {
        String contentType = ctx.contentType();

        if(contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json"))
        {
            throw new ApiException(415, "Unsupported media type", "The request body must be application/json");
        }

        try
        {
            T body = ctx.bodyAsClass(type);

            if(body == null)
            {
                throw ApiException.badRequest("The request body is empty");
            }

            return body;
        }
        catch(ApiException ae)
        {
            throw ae;
        }
        catch(Exception e)
        {
            throw ApiException.badRequest(describeJsonError(e));
        }
    }

    /**
     * Short description of a JSON parse error, without the parser's internal details.
     */
    static String describeJsonError(Throwable e)
    {
        Throwable cause = e;

        while(cause != null)
        {
            if(cause instanceof UnrecognizedPropertyException upe)
            {
                return "Unknown field '" + upe.getPropertyName() + "'";
            }

            if(cause instanceof MismatchedInputException mie && !mie.getPath().isEmpty())
            {
                String field = mie.getPath().getLast().getFieldName();
                return field != null ? "Field '" + field + "' has the wrong type" : "The request body isn't valid JSON";
            }

            cause = cause.getCause();
        }

        return "The request body isn't valid JSON";
    }

    /**
     * Indicates if the request method changes state (anything but GET, HEAD and OPTIONS).
     */
    public static boolean isStateChanging(Context ctx)
    {
        HandlerType method = ctx.method();
        return method != HandlerType.GET && method != HandlerType.HEAD && method != HandlerType.OPTIONS;
    }

    /**
     * ISO-8601 UTC timestamp for epoch milliseconds, or null.
     */
    public static String iso(Long epochMillis)
    {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis).toString();
    }
}
