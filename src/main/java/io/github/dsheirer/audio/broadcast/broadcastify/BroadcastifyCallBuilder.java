/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.audio.broadcast.broadcastify;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for an HTTP body publisher to produce a broadcastify call event
 */
public class BroadcastifyCallBuilder
{
    private static final String DASH_DASH = "--";
    private static final String BOUNDARY = "--sdrtrunk-sdrtrunk-sdrtrunk";
    private List<Part> mParts = new ArrayList<>();

    /**
     * Constructs an instance
     */
    public BroadcastifyCallBuilder()
    {
    }

    /**
     * Access the static multi-part boundary string
     */
    public String getBoundary()
    {
        return BOUNDARY;
    }

    /**
     * Adds a string part to the call
     */
    public BroadcastifyCallBuilder addPart(FormField key, String value)
    {
        if(key != null && value != null)
        {
            mParts.add(new Part(key.getHeader(), value));
        }

        return this;
    }

    /**
     * Adds a number part to the call
     */
    public BroadcastifyCallBuilder addPart(FormField key, Number value)
    {
        if(key != null && value != null)
        {
            mParts.add(new Part(key.getHeader(), value.toString()));
        }

        return this;
    }

    /**
     * Creates a form-data item
     */
    private static String formatPart(Part part, String boundary)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(DASH_DASH).append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(part.mKey).append("\"\r\n\r\n");
        sb.append(part.getValue()).append("\r\n");
        return sb.toString();
    }

    /**
     * Creates the boundary closing item
     */
    private static String getClosingBoundary(String boundary)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(DASH_DASH).append(boundary).append(DASH_DASH).append("\r\n");
        return sb.toString();
    }

    /**
     * Creates a BodyPublisher for accessing the call form data
     */
    public HttpRequest.BodyPublisher build()
    {
        StringBuilder sb = new StringBuilder();

        for(Part part: mParts)
        {
            sb.append(formatPart(part, BOUNDARY));
        }

        sb.append(getClosingBoundary(BOUNDARY));

        return HttpRequest.BodyPublishers.ofByteArray(sb.toString().getBytes());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for(Part part: mParts)
        {
            sb.append(formatPart(part, BOUNDARY));
        }

        sb.append(getClosingBoundary(BOUNDARY));

        return sb.toString();
    }

    /**
     * Key:Value pair holder
     */
    public class Part
    {
        private String mKey;
        private String mValue;

        /**
         * Constructs a new part
         * @param key value
         * @param value item
         */
        public Part(String key, String value)
        {
            mKey = key;
            mValue = value;
        }

        public String getKey()
        {
            return mKey;
        }

        public String getValue()
        {
            return mValue;
        }
    }
}
