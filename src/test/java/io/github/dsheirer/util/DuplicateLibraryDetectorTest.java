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
package io.github.dsheirer.util;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DuplicateLibraryDetectorTest
{
    private static final String LIB = "/opt/sdr-trunk/bin/../lib/";

    /**
     * Jar names from a clean release image.
     */
    private static final List<String> CLEAN = List.of("JLargeArrays-1.7.jar", "JTransforms-3.2.jar", "api-v2.1.1.jar",
            "commons-lang3-3.21.0.jar", "guava-33.8.0-jre.jar", "jackson-annotations-2.23.jar",
            "jackson-core-2.23.0.jar", "java-lame-v3.98.4.jar", "javafx-swt.jar", "jhackrf-2026.01.3.1.jar",
            "jiconfont-1.0.0.jar", "jiconfont-font_awesome-4.7.0.1.jar", "jiconfont-javafx-1.0.0.jar",
            "jiconfont-swing-1.0.1.jar", "jrt-fs.jar", "jrtl-1.4.0.3.jar", "libresample4j-master.jar",
            "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar", "logback-classic-1.6.6.jar",
            "logback-core-1.6.6.jar", "sdr-trunk-nightly.jar");

    private static List<String> paths(List<String> names)
    {
        return names.stream().map(n -> LIB + n).toList();
    }

    @Test
    void cleanReleaseHasNoDuplicates()
    {
        assertTrue(DuplicateLibraryDetector.findDuplicates(paths(CLEAN)).isEmpty());
        assertNull(DuplicateLibraryDetector.getFolder(Map.of()));
    }

    @Test
    void newReleaseUnzippedOverOldReleaseIsDetected()
    {
        List<String> names = new java.util.ArrayList<>(CLEAN);
        names.addAll(List.of("api-v2.1.0.jar", "guava-33.6.0-jre.jar", "jrtl-1.4.0.2.jar", "sdr-trunk-0.6.1.jar"));

        Map<String,List<String>> duplicates = DuplicateLibraryDetector.findDuplicates(paths(names));

        assertEquals(List.of("api", "guava", "jrtl", "sdr-trunk"), List.copyOf(duplicates.keySet()));
        assertEquals(List.of(LIB + "jrtl-1.4.0.2.jar", LIB + "jrtl-1.4.0.3.jar"), duplicates.get("jrtl"));
        assertTrue(DuplicateLibraryDetector.describe(duplicates).contains("jrtl: jrtl-1.4.0.2.jar, jrtl-1.4.0.3.jar"));
        //Windows resolves a rooted path without a drive letter against the current drive, e.g. C:\opt\sdr-trunk\lib
        assertEquals(Path.of("/opt/sdr-trunk/lib").toAbsolutePath(), DuplicateLibraryDetector.getFolder(duplicates));
    }

    @Test
    void sameJarListedTwiceIsNotADuplicate()
    {
        assertTrue(DuplicateLibraryDetector.findDuplicates(List.of(LIB + "jrtl-1.4.0.3.jar", LIB + "jrtl-1.4.0.3.jar",
                "", "/some/classes/dir")).isEmpty());
    }
}
