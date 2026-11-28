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
package io.github.dsheirer.database.ageoff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Age-off removes the recording subfolders that become empty, only inside the recordings folder.
 */
public class AgeOffFolderTest
{
    @TempDir
    Path mTempDir;

    @Test
    public void emptyFoldersAreRemovedUpToTheRecordingsFolder() throws Exception
    {
        Path root = mTempDir.resolve("recordings");
        Path day = Files.createDirectories(root.resolve("Metro").resolve("TG_1001").resolve("2026-11-22"));
        Path otherDay = Files.createDirectories(root.resolve("Metro").resolve("TG_1002").resolve("2026-11-22"));
        Path first = Files.writeString(day.resolve("a.mp3"), "a");
        Path second = Files.writeString(day.resolve("b.mp3"), "b");
        Files.writeString(otherDay.resolve("c.mp3"), "c");

        assertTrue(AgeOffManager.deleteRecording(first.toString(), root));
        assertTrue(Files.isDirectory(day), "folder still has a recording");

        assertTrue(AgeOffManager.deleteRecording(second.toString(), root));
        assertFalse(Files.exists(day), "empty date folder removed");
        assertFalse(Files.exists(day.getParent()), "empty talkgroup folder removed");
        assertTrue(Files.isDirectory(root.resolve("Metro")), "system folder has another talkgroup");
        assertTrue(Files.isDirectory(root), "recordings folder is never removed");
    }

    @Test
    public void foldersOutsideTheRecordingsFolderAreKept() throws Exception
    {
        Path root = Files.createDirectories(mTempDir.resolve("recordings"));
        Path elsewhere = Files.createDirectories(mTempDir.resolve("old").resolve("2025-01-01"));
        Path file = Files.writeString(elsewhere.resolve("a.wav"), "a");

        assertTrue(AgeOffManager.deleteRecording(file.toString(), root));
        assertTrue(Files.isDirectory(elsewhere), "folder outside the recordings folder kept");

        Path flat = Files.writeString(root.resolve("b.mp3"), "b");
        assertTrue(AgeOffManager.deleteRecording(flat.toString(), root));
        assertTrue(Files.isDirectory(root));
    }
}
