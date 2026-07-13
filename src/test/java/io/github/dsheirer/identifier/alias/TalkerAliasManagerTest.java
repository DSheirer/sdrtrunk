/*
 * ****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */
package io.github.dsheirer.identifier.alias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TalkerAliasManagerTest
{
    @TempDir
    Path mTemporaryDirectory;

    @Test
    void escapesCsvAndSanitizesFileName()
    {
        assertEquals("\"Unit \"\"12\"\", north\nline\"", TalkerAliasManager.csv("Unit \"12\", north\nline"));
        assertEquals("County_System_1", TalkerAliasManager.safeFileName("T-County/System:1"));
        assertEquals("Talker Aliases", TalkerAliasManager.safeFileName(""));
    }

    @Test
    void createsHeaderAndAppendsEscapedAliases() throws Exception
    {
        TalkerAliasManager manager = new TalkerAliasManager(mTemporaryDirectory, "T-Test/System", true);
        Path exportFile = mTemporaryDirectory.resolve("Test_System.csv");

        assertEquals("radio,alias\n", Files.readString(exportFile));

        manager.appendCsv(1001, "Unit \"12\", north");
        manager.appendCsv(1002, "Unit 14");

        assertEquals("radio,alias\n1001,\"Unit \"\"12\"\", north\"\n1002,\"Unit 14\"\n",
            Files.readString(exportFile));
    }

    @Test
    void doesNotCreateExportFileWhenDisabled()
    {
        TalkerAliasManager manager = new TalkerAliasManager(mTemporaryDirectory, "Test", false);
        manager.appendCsv(1001, "Unit 12");

        assertFalse(Files.exists(mTemporaryDirectory.resolve("Test.csv")));
    }
}
