/*
 * ******************************************************************************
 * Copyright (C) 2015-2026 Dennis Sheirer
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
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for accessing GitHub repositories and releases
 */
public class GitHub
{
    private final static Logger mLog = LoggerFactory.getLogger(GitHub.class);

    /**
     * Query suffix to request the maximum page size.  GitHub returns 30 releases per page by default, which could
     * eventually push older major version releases off of the first page.
     */
    private static final String RELEASES_PAGE_SIZE = "?per_page=100";

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
     * Obtains the latest release from the repository whose major version matches the specified major version.  Newer
     * releases with a different major version are ignored so that the downloaded JMBE creator produces a library that
     * is compatible with the JMBE API that this application was compiled against.
     * @param repositoryURL for the GitHub api
     * @param majorVersion that the release must match.
     * @return the latest matching release or null
     */
    public static Release getLatestRelease(String repositoryURL, int majorVersion)
    {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(repositoryURL + RELEASES_PAGE_SIZE)).build();

        try
        {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200)
            {
                return getLatestRelease(parseReleases(response.body()), majorVersion);
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
     * Finds the latest release in the list whose major version matches the specified major version.
     * @param releases to search
     * @param majorVersion that the release must match
     * @return latest matching release or null
     */
    public static Release getLatestRelease(List<Release> releases, int majorVersion)
    {
        Release latest = null;

        for(Release release: releases)
        {
            Version version = release.getVersion();

            if(version.hasMajor() && version.getMajor() == majorVersion &&
                (latest == null || latest.getVersion().compareTo(version) < 0))
            {
                latest = release;
            }
        }

        return latest;
    }

    /**
     * Parses a json string containing an array of GitHub release objects.  Release objects that don't have a
     * parsable version tag are ignored.
     * @param json string returned from URL call
     * @return list of releases, possibly empty
     */
    public static List<Release> parseReleases(String json)
    {
        List<Release> releases = new ArrayList<>();

        if(json != null)
        {
            JsonElement element = JsonParser.parseString(json);

            if(element.isJsonArray())
            {
                for(JsonElement child: element.getAsJsonArray())
                {
                    if(child.isJsonObject())
                    {
                        JsonObject releaseObject = child.getAsJsonObject();
                        Version version = getVersion(releaseObject);

                        if(version != null)
                        {
                            releases.add(new Release(version, releaseObject));
                        }
                    }
                }
            }
        }

        return releases;
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
