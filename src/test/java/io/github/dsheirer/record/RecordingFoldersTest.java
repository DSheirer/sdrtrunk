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
package io.github.dsheirer.record;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Recording subfolders for each folder layout
 */
public class RecordingFoldersTest
{
    private static final long TIME = LocalDateTime.of(2026, 11, 22, 14, 5, 0).atZone(ZoneId.systemDefault())
        .toInstant().toEpochMilli();
    private static final RecordingFolders.CallFolders FOLDERS =
        new RecordingFolders.CallFolders("Metro", "Site 1", "Control A", "TG_1001");

    private static List<String> names(RecordingFolderLayout layout, boolean hour)
    {
        return RecordingFolders.getFolderNames(layout, hour, FOLDERS, TIME);
    }

    @Test
    public void layouts()
    {
        assertEquals(List.of(), names(RecordingFolderLayout.NONE, true), "no subfolders, hour ignored");
        assertEquals(List.of("2026-11-22"), names(RecordingFolderLayout.DATE, false));
        assertEquals(List.of("2026-11-22", "Metro"), names(RecordingFolderLayout.DATE_SYSTEM, false));
        assertEquals(List.of("Metro", "2026-11-22"), names(RecordingFolderLayout.SYSTEM_DATE, false));
        assertEquals(List.of("Metro", "Site-1", "2026-11-22"), names(RecordingFolderLayout.SYSTEM_SITE_DATE, false));
        assertEquals(List.of("Metro", "Control-A", "2026-11-22"), names(RecordingFolderLayout.SYSTEM_CHANNEL_DATE,
            false));
        assertEquals(List.of("Metro", "TG_1001", "2026-11-22"), names(RecordingFolderLayout.SYSTEM_TALKGROUP_DATE,
            false));
        assertEquals(List.of("TG_1001", "2026-11-22"), names(RecordingFolderLayout.TALKGROUP_DATE, false));
        assertEquals(List.of("Metro", "2026-11-22", "TG_1001"), names(RecordingFolderLayout.SYSTEM_DATE_TALKGROUP,
            false));
    }

    @Test
    public void hourFolderIsBelowDate()
    {
        assertEquals(List.of("Metro", "2026-11-22", "14"), names(RecordingFolderLayout.SYSTEM_DATE, true));
        assertEquals(List.of("2026-11-22", "14", "Metro"), names(RecordingFolderLayout.DATE_SYSTEM, true));
        assertEquals(List.of("Metro", "2026-11-22", "14", "TG_1001"),
            names(RecordingFolderLayout.SYSTEM_DATE_TALKGROUP, true));
    }

    @Test
    public void missingNamesUseFallbacks()
    {
        RecordingFolders.CallFolders empty = new RecordingFolders.CallFolders(null, " ", "..", null);
        assertEquals(List.of("Unknown-System", "Unknown-Site", "2026-11-22"),
            RecordingFolders.getFolderNames(RecordingFolderLayout.SYSTEM_SITE_DATE, false, empty, TIME));
        assertEquals(List.of("Unknown-System", "Unknown-Channel", "2026-11-22"),
            RecordingFolders.getFolderNames(RecordingFolderLayout.SYSTEM_CHANNEL_DATE, false, empty, TIME));
        assertEquals(List.of("Unknown", "2026-11-22"),
            RecordingFolders.getFolderNames(RecordingFolderLayout.TALKGROUP_DATE, false, empty, TIME));
    }

    @Test
    public void fileNameLeavesOutFolderNames()
    {
        for(RecordingFolderLayout layout: RecordingFolderLayout.values())
        {
            List<String> folders = names(layout, false);
            assertEquals(folders.contains("Metro"), layout.hasSystemFolder(), layout + " system");
            assertEquals(folders.contains("Site-1"), layout.hasSiteFolder(), layout + " site");
            assertEquals(folders.contains("Control-A"), layout.hasChannelFolder(), layout + " channel");
        }
    }

    @Test
    public void namesAreSafeFolderNames()
    {
        assertEquals("Fire-Ops", RecordingFolders.clean("Fire/Ops", "x"));
        assertEquals("a-b-c", RecordingFolders.clean("a\\b:c", "x"));
        assertEquals("Metro", RecordingFolders.clean("  .Metro.  ", "x"));
        assertEquals("con_", RecordingFolders.clean("con", "x"), "reserved Windows name");
        assertEquals(64, RecordingFolders.clean("A".repeat(100), "x").length());
        assertEquals("x", RecordingFolders.clean("...", "x"));
    }

    private static IdentifierCollection identifiers(Identifier... identifiers)
    {
        List<Identifier> list = new ArrayList<>(List.of(identifiers));
        list.add(new SystemConfigurationIdentifier("Metro"));
        list.add(new SiteConfigurationIdentifier("Site 1"));
        list.add(new ChannelNameConfigurationIdentifier("Control A"));
        return new IdentifierCollection(list);
    }

    @Test
    public void talkgroupFolderFromIdentifiers()
    {
        RecordingFolders.CallFolders group = RecordingFolders.CallFolders.from(identifiers(
            APCO25Talkgroup.create(1001), APCO25RadioIdentifier.createFrom(5551212)));
        assertEquals(new RecordingFolders.CallFolders("Metro", "Site 1", "Control A", "TG_1001"), group);

        PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(500));
        patchGroup.addPatchedTalkgroup(APCO25Talkgroup.create(1001));
        assertEquals("PATCH_500", RecordingFolders.CallFolders.from(identifiers(
            APCO25PatchGroup.create(patchGroup))).talkgroup());

        assertEquals("RADIO_1234", RecordingFolders.CallFolders.from(identifiers(
            APCO25RadioIdentifier.createTo(1234), APCO25RadioIdentifier.createFrom(5551212))).talkgroup(),
            "private call: TO radio");
        assertEquals("RADIO_5551212", RecordingFolders.CallFolders.from(identifiers(
            APCO25RadioIdentifier.createFrom(5551212))).talkgroup(), "no TO: FROM radio");
        assertEquals("Control A", RecordingFolders.CallFolders.from(identifiers()).talkgroup(),
            "no identifiers: channel name");
        assertEquals(new RecordingFolders.CallFolders(null, null, null, null),
            RecordingFolders.CallFolders.from(null));
    }
}
