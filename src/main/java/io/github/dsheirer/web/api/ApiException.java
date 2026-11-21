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

import java.util.List;

/**
 * Error raised by a web API handler or service and returned to the client as an RFC 9457 problem details response:
 * {@code {"type", "title", "status", "detail", "errors": [{"field", "message"}]}}.
 */
public class ApiException extends RuntimeException
{
    private final int mStatus;
    private final String mTitle;
    private final List<FieldError> mErrors;

    /**
     * Constructs an instance
     * @param status HTTP status code
     * @param title short summary that doesn't change between occurrences
     * @param detail explanation of this occurrence, or null
     * @param errors field validation errors, or an empty list
     */
    public ApiException(int status, String title, String detail, List<FieldError> errors)
    {
        super(detail != null ? detail : title);
        mStatus = status;
        mTitle = title;
        mErrors = errors == null ? List.of() : List.copyOf(errors);
    }

    public ApiException(int status, String title, String detail)
    {
        this(status, title, detail, List.of());
    }

    public int getStatus()
    {
        return mStatus;
    }

    public String getTitle()
    {
        return mTitle;
    }

    /**
     * Explanation of this occurrence, or null
     */
    public String getDetail()
    {
        return getMessage().equals(mTitle) ? null : getMessage();
    }

    public List<FieldError> getErrors()
    {
        return mErrors;
    }

    public static ApiException badRequest(String detail)
    {
        return new ApiException(400, "Bad request", detail);
    }

    /**
     * Request body or parameters failed validation
     */
    public static ApiException invalid(List<FieldError> errors)
    {
        return new ApiException(400, "Validation failed", "One or more fields are invalid", errors);
    }

    public static ApiException invalid(String field, String message)
    {
        return invalid(List.of(new FieldError(field, message)));
    }

    public static ApiException unauthorized(String detail)
    {
        return new ApiException(401, "Sign-in required", detail);
    }

    public static ApiException forbidden(String detail)
    {
        return new ApiException(403, "Forbidden", detail);
    }

    public static ApiException notFound(String detail)
    {
        return new ApiException(404, "Not found", detail);
    }

    public static ApiException conflict(String detail)
    {
        return new ApiException(409, "Conflict", detail);
    }

    public static ApiException tooManyRequests(String detail)
    {
        return new ApiException(429, "Too many requests", detail);
    }

    public static ApiException unavailable(String detail)
    {
        return new ApiException(503, "Service unavailable", detail);
    }

    /**
     * Validation error for one field
     * @param field name of the field in the request body or the query parameter
     * @param message what is wrong with it
     */
    public record FieldError(String field, String message) {}
}
