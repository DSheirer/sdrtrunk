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

package io.github.dsheirer.monitor;

import io.github.dsheirer.monitor.ResourceMonitor.StorageUsage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceMonitorTest
{
    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * MB;

    @Test
    void sizeOfCountsNestedFiles(@TempDir Path folder) throws IOException
    {
        Files.write(folder.resolve("a.mp3"), new byte[1000]);
        Path child = Files.createDirectories(folder.resolve("2026/11"));
        Files.write(child.resolve("b.mp3"), new byte[234]);
        assertEquals(1234, ResourceMonitor.sizeOf(folder));
    }

    @Test
    void measureUsesTheLimitAboveTwoGigabytes(@TempDir Path folder) throws IOException
    {
        Files.write(folder.resolve("a.log"), new byte[10]);
        //A limit of 4096 MB overflowed an int multiplication before
        StorageUsage usage = ResourceMonitor.measure(folder, 4096 * MB);
        assertEquals(4 * GB, usage.limit());
        assertEquals(10, usage.used());
        assertTrue(usage.fraction() >= 0 && usage.fraction() < 0.001);
    }

    @Test
    void measureOfMissingFolderIsNull()
    {
        assertNull(ResourceMonitor.measure(null, MB));
    }

    @Test
    void fractionUsesTheSettingLimit()
    {
        StorageUsage usage = new StorageUsage(Path.of("x"), 500 * MB, 2000 * MB, 100 * GB);
        assertEquals(2000 * MB, usage.effectiveLimit());
        assertEquals(0.25, usage.fraction(), 1e-9);
        assertFalse(usage.isDriveLimited());
    }

    @Test
    void fractionUsesTheDriveWhenItHasLessSpace()
    {
        StorageUsage usage = new StorageUsage(Path.of("x"), 500 * MB, 2000 * MB, 500 * MB);
        assertEquals(1000 * MB, usage.effectiveLimit());
        assertEquals(0.5, usage.fraction(), 1e-9);
        assertTrue(usage.isDriveLimited());
    }

    @Test
    void fractionCanExceedTheLimit()
    {
        StorageUsage usage = new StorageUsage(Path.of("x"), 300 * MB, 200 * MB, 10 * GB);
        assertEquals(1.5, usage.fraction(), 1e-9);
    }

    @Test
    void zeroLimitDoesNotDivideByZero()
    {
        assertEquals(0.0, new StorageUsage(Path.of("x"), 0, 0, 0).fraction());
        assertEquals(1.0, new StorageUsage(Path.of("x"), 10, 0, 0).fraction());
    }
}
