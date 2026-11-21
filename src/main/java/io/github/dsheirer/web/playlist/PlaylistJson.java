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

package io.github.dsheirer.web.playlist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.auth.PasswordHasher;

/**
 * JSON for playlist configuration objects (decoder, source, record and event log configurations, alias identifiers
 * and actions).  These classes are annotated for the playlist XML file, so this mapper reads those annotations: the
 * JSON field names and the "type" discriminators are the same as in the playlist file, for example
 * {"type": "decodeConfigNBFM", "bandwidth": "BW_12_5", ...}.
 *
 * Also computes resource versions (ETags) as a hash of the JSON representation, so any change from any source,
 * including the desktop editors, gives a new version.
 */
public final class PlaylistJson
{
    private static final String XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .setAnnotationIntrospector(new PlaylistIntrospector())
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final ObjectMapper VERSION_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private PlaylistJson()
    {
    }

    /**
     * Reads the playlist XML annotations, except the legacy xsi:type attributes.  Many configuration classes keep a
     * placeholder type property in the XML schema instance namespace that always writes null; in JSON it would
     * collide with the "type" discriminator.
     */
    private static class PlaylistIntrospector extends JacksonXmlAnnotationIntrospector
    {
        PlaylistIntrospector()
        {
            super(false);
        }

        @Override
        public boolean hasIgnoreMarker(com.fasterxml.jackson.databind.introspect.AnnotatedMember member)
        {
            com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty property =
                _findAnnotation(member, com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty.class);

            if(property != null && XML_SCHEMA_INSTANCE.equals(property.namespace()))
            {
                return true;
            }

            return super.hasIgnoreMarker(member);
        }
    }

    /**
     * JSON tree of a configuration object, or null for null.
     */
    public static JsonNode toTree(Object value)
    {
        return value == null ? null : MAPPER.valueToTree(value);
    }

    /**
     * Reads a configuration object from JSON.
     * @param field name of the request field, for the error message
     * @throws ApiException 400 when the JSON doesn't describe the type
     */
    public static <T> T fromTree(JsonNode node, Class<T> type, String field)
    {
        if(node == null || node.isNull())
        {
            return null;
        }

        T value;

        try
        {
            value = MAPPER.treeToValue(node, type);
        }
        catch(Exception e)
        {
            String message = e.getMessage() != null ? e.getMessage().split("\n")[0] : "isn't valid";
            throw ApiException.invalid(field, message.length() > 300 ? message.substring(0, 300) : message);
        }

        //The configuration classes ignore unknown fields so that older sdrtrunk versions can read newer playlist
        //files.  A request shouldn't lose a mistyped field silently, so compare it with what was understood.
        String unknown = findUnknown(node, MAPPER.valueToTree(value), field);

        if(unknown != null)
        {
            throw ApiException.invalid(field, "has an unknown field '" + unknown + "'");
        }

        return value;
    }

    /**
     * Path of the first field in the request that isn't in the parsed result, or null.
     */
    private static String findUnknown(JsonNode request, JsonNode parsed, String path)
    {
        if(request.isObject())
        {
            if(parsed == null || !parsed.isObject())
            {
                return null;
            }

            var fields = request.fields();

            while(fields.hasNext())
            {
                var entry = fields.next();

                if(!parsed.has(entry.getKey()))
                {
                    return path + "." + entry.getKey();
                }

                String nested = findUnknown(entry.getValue(), parsed.get(entry.getKey()), path + "." + entry.getKey());

                if(nested != null)
                {
                    return nested;
                }
            }
        }
        else if(request.isArray() && parsed != null && parsed.isArray() && request.size() == parsed.size())
        {
            for(int x = 0; x < request.size(); x++)
            {
                String nested = findUnknown(request.get(x), parsed.get(x), path + "[" + x + "]");

                if(nested != null)
                {
                    return nested;
                }
            }
        }

        return null;
    }

    /**
     * Strong ETag of a resource representation: a quoted hash of its JSON.
     */
    public static String version(Object representation)
    {
        try
        {
            return "\"" + PasswordHasher.sha256(VERSION_MAPPER.writeValueAsString(representation)).substring(0, 32) +
                "\"";
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Unable to compute a resource version", e);
        }
    }
}
