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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects more than one version of the same library jar on the class path.
 *
 * The release launch scripts use a 'lib/*' wildcard class path, so when a user unzips a new release over an older one,
 * the stale jars from the older release stay on the class path next to the new ones.  The JVM loads classes (and the
 * tuner libraries extract their native libraries) from whichever jar sorts first, which can silently run an older
 * library version, including native code with bugs that the newer version fixed.
 */
public class DuplicateLibraryDetector
{
    /**
     * Splits a jar file name into the library name and version.  The version starts at the first dash that is
     * followed by a digit, a 'v' and a digit, or 'nightly' (e.g. guava-33.8.0-jre, api-v2.1.1, sdr-trunk-nightly).
     */
    private static final Pattern JAR_NAME = Pattern.compile("^(.+?)-(v?\\d.*|nightly)\\.jar$");

    private DuplicateLibraryDetector()
    {
    }

    /**
     * Finds the libraries that have more than one version on this JVM's class path.
     * @return map of library name to the sorted jar paths for that library, empty when there are no duplicates.
     */
    public static Map<String,List<String>> findDuplicates()
    {
        String classPath = System.getProperty("java.class.path", "");
        return findDuplicates(List.of(classPath.split(Pattern.quote(File.pathSeparator))));
    }

    /**
     * Finds the libraries that have more than one version in the class path entries.  Entries that are not jar files
     * or that don't have a version in the file name are ignored.
     * @param classPathEntries to check
     * @return map of library name to the sorted jar paths for that library, empty when there are no duplicates.
     */
    public static Map<String,List<String>> findDuplicates(List<String> classPathEntries)
    {
        Map<String,TreeSet<String>> versions = new TreeMap<>();
        Map<String,TreeSet<String>> paths = new TreeMap<>();

        for(String entry: classPathEntries)
        {
            if(entry == null || entry.isBlank())
            {
                continue;
            }

            Path fileName = Path.of(entry).getFileName();

            if(fileName == null)
            {
                continue;
            }

            Matcher matcher = JAR_NAME.matcher(fileName.toString());

            if(matcher.matches())
            {
                versions.computeIfAbsent(matcher.group(1), k -> new TreeSet<>()).add(matcher.group(2));
                paths.computeIfAbsent(matcher.group(1), k -> new TreeSet<>()).add(entry);
            }
        }

        Map<String,List<String>> duplicates = new TreeMap<>();

        for(Map.Entry<String,TreeSet<String>> entry: versions.entrySet())
        {
            if(entry.getValue().size() > 1)
            {
                duplicates.put(entry.getKey(), new ArrayList<>(paths.get(entry.getKey())));
            }
        }

        return duplicates;
    }

    /**
     * Creates a description of the duplicate libraries for a log entry or a dialog.
     * @param duplicates from findDuplicates()
     * @return description, one line per library, listing the jar file names for each library.
     */
    public static String describe(Map<String,List<String>> duplicates)
    {
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String,List<String>> entry: duplicates.entrySet())
        {
            List<String> names = entry.getValue().stream().map(p -> Path.of(p).getFileName().toString()).toList();
            sb.append("  ").append(entry.getKey()).append(": ").append(String.join(", ", names)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Folder that holds the duplicate jars, for telling the user which folder to replace.
     * @param duplicates from findDuplicates()
     * @return folder of the first duplicate jar, or null if there are no duplicates or the path has no parent.
     */
    public static Path getFolder(Map<String,List<String>> duplicates)
    {
        if(duplicates.isEmpty())
        {
            return null;
        }

        return Path.of(duplicates.values().iterator().next().getFirst()).toAbsolutePath().normalize().getParent();
    }
}
