/*
 * ******************************************************************************
 * Copyright (C) 2015-2020 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.jmbe.github;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Utilities for accessing GitHub repositories and releases
 */
public class GitHub
{
    private final static Logger mLog = LoggerFactory.getLogger(GitHub.class);

    private GitHub()
    {
        //There's nothing to construct in this class
    }

    /**
     * Download an artifact to the specified directory
     * @param url of the artifact to download
     * @param directory to store the downloaded file
     * @return path to the downloaded artifact or null
     */
    public static Path downloadArtifact(String url, Path directory)
    {
        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        try
        {
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFileDownload(directory,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE));

            if(response.statusCode() != 200)
            {
                mLog.error("HTTP Download Status Code: " + response.statusCode());
            }

            return response.body();

        }
        catch(Exception e)
        {
            mLog.error("Error downloading source code from GitHub", e);
        }

        return null;
    }

    /**
     * Obtain the latest release object from the repository.
     * @param repositoryURL for the GitHub api
     * @return the latest release or null
     */
    public static Release getLatestRelease(String repositoryURL)
    {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(repositoryURL)).build();

        try
        {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200)
            {
                return parseResponse(response.body());
            }
            else
            {
                mLog.error("Error while fetching latest releases - HTTP:" + response.statusCode());
            }
        }
        catch(IOException | InterruptedException e)
        {
            mLog.error("Error while detecting the current release version of JMBE library", e);
        }

        return null;
    }

    /**
     * Parses a json string containing an array or GitHub release objects and returns the latest release.
     * @param json string returned from URL call
     * @return latest release version
     */
    private static Release parseResponse(String json)
    {
        Release release = null;

        if(json != null)
        {
            JsonElement element = JsonParser.parseString(json);

            if(element.isJsonArray())
            {
                JsonArray array = element.getAsJsonArray();

                for(JsonElement child: array)
                {
                    if(child.isJsonObject())
                    {
                        JsonObject releaseObject = child.getAsJsonObject();
                        Version version = getVersion(releaseObject);

                        if(version != null)
                        {
                            if(release == null || release.getVersion().compareTo(version) < 0)
                            {
                                release = new Release(version, releaseObject);
                            }
                        }
                    }
                }
            }
        }

        return release;
    }

    /**
     * Extracts a version object from the tag_name.
     * @param jsonObject containing a tag_name formatted as: vAAAAA.BBBBB.CCCCx where:
     *  A = major, 0-99999
     *  B = minor, 0-99999
     *  C = release, 0-99999
     *  x = (optional)patch, a-z
     *
     * @return version instance or null
     */
    private static Version getVersion(JsonObject jsonObject)
    {
        JsonElement tagName = jsonObject.get("tag_name");

        if(tagName != null)
        {
            return Version.fromString(tagName.toString());
        }

        return null;
    }
}
