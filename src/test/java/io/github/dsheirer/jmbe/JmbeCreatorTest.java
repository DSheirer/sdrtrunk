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

package io.github.dsheirer.jmbe;

import io.github.dsheirer.jmbe.github.GitHub;
import io.github.dsheirer.jmbe.github.Release;
import io.github.dsheirer.jmbe.github.Version;
import java.util.List;
import jmbe.iface.IAudioCodecLibrary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for selecting the JMBE creator release that matches the JMBE API major version.
 */
public class JmbeCreatorTest
{
    private static final String RELEASES_JSON = """
        [
          {"tag_name": "v3.0.0", "name": "JMBE 3.0.0", "assets": []},
          {"tag_name": "v2.1.1", "name": "JMBE 2.1.1", "assets": []},
          {"tag_name": "v2.0.9", "name": "JMBE 2.0.9", "assets": []},
          {"tag_name": "v1.0.9", "name": "JMBE 1.0.9", "assets": []},
          {"tag_name": "not-a-version", "name": "Junk", "assets": []},
          {"tag_name": "v2.1.0", "name": "JMBE 2.1.0", "assets": []}
        ]
        """;

    @Test
    void apiMajorVersionMatchesApiDependency()
    {
        String implementationVersion = IAudioCodecLibrary.class.getPackage().getImplementationVersion();
        assertNotNull(implementationVersion, "JMBE API jar manifest has no Implementation-Version");
        Version apiVersion = Version.fromString(implementationVersion);
        assertNotNull(apiVersion, "Unparsable JMBE API version: " + implementationVersion);
        assertEquals(JmbeCreator.JMBE_API_MAJOR_VERSION, (int)apiVersion.getMajor(),
            "JmbeCreator.JMBE_API_MAJOR_VERSION doesn't match the JMBE API dependency version " + implementationVersion);
    }

    @Test
    void parseIgnoresUnversionedReleases()
    {
        assertEquals(5, GitHub.parseReleases(RELEASES_JSON).size());
    }

    @Test
    void selectsLatestReleaseForMajorVersion()
    {
        List<Release> releases = GitHub.parseReleases(RELEASES_JSON);
        assertEquals("2.1.1", GitHub.getLatestRelease(releases, 2).getVersion().toString());
        assertEquals("3.0.0", GitHub.getLatestRelease(releases, 3).getVersion().toString());
        assertEquals("1.0.9", GitHub.getLatestRelease(releases, 1).getVersion().toString());
        assertNull(GitHub.getLatestRelease(releases, 4));
    }

    @Test
    void versionCompare()
    {
        assertEquals(0, Version.fromString("v2.1.1").compareTo(Version.fromString("2.1.1")));
        assertTrue(Version.fromString("v2.1.1a").compareTo(Version.fromString("v2.1.1")) > 0);
        assertTrue(Version.fromString("v2.1.1").compareTo(Version.fromString("v2.1.1a")) < 0);
        assertTrue(Version.fromString("v2.1.200").compareTo(Version.fromString("v2.1.199")) > 0);
        assertTrue(Version.fromString("v2.10.0").compareTo(Version.fromString("v2.9.9")) > 0);
    }
}
