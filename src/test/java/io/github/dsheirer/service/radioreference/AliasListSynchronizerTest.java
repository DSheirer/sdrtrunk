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

package io.github.dsheirer.service.radioreference;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasListConfiguration;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceDecoder;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.Talkgroup;
import io.github.dsheirer.rrapi.type.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applying Radio Reference talkgroups to a tracked alias list, without the web service.
 */
public class AliasListSynchronizerTest
{
    private static final int SYSTEM_ID = 42;
    private static final String LIST = "Tracked";

    private AliasModel mAliasModel;
    private AliasListSynchronizer mSynchronizer;
    private RadioReferenceDecoder mDecoder;
    private System mSystem;
    private AliasListConfiguration mConfiguration;

    @BeforeEach
    void setup()
    {
        Settings settings = new Settings();
        mAliasModel = new AliasModel();
        mSynchronizer = new AliasListSynchronizer(mAliasModel, null, settings);

        Type p25 = new Type();
        p25.setTypeId(1);
        p25.setName("Project 25");
        mDecoder = new RadioReferenceDecoder(settings, Map.of(1, p25), Map.of(), Map.of(), Map.of());

        mSystem = new System();
        mSystem.setSystemId(SYSTEM_ID);
        mSystem.setTypeId(1);

        mConfiguration = mAliasModel.getOrCreateAliasListConfiguration(LIST);
        mConfiguration.track(SYSTEM_ID, "System");
    }

    private static Talkgroup talkgroup(int value, String alphaTag, int categoryId, long lastUpdated)
    {
        Talkgroup talkgroup = new Talkgroup();
        talkgroup.setDecimalValue(value);
        talkgroup.setAlphaTag(alphaTag);
        talkgroup.setTalkgroupCategoryId(categoryId);
        talkgroup.setLastUpdated(new Date(lastUpdated));
        return talkgroup;
    }

    private AliasListSynchronizer.Result apply(long lastUpdated, List<Talkgroup> talkgroups)
    {
        return mSynchronizer.apply(mConfiguration, SYSTEM_ID, "System", lastUpdated, talkgroups,
            Map.of(1, "Fire", 2, "Police"), mSystem, mDecoder);
    }

    private Alias alias(int value)
    {
        return mAliasModel.getAliases(LIST, AliasIDType.TALKGROUP).stream()
            .filter(a -> a.getAliasIdentifiers().stream().anyMatch(id ->
                new io.github.dsheirer.alias.id.talkgroup.Talkgroup(Protocol.APCO25, value).matches(id)))
            .findFirst().orElseThrow();
    }

    @Test
    void firstSynchronizationAddsMissingAndKeepsExistingNames()
    {
        //The user already has an alias for talkgroup 100 with their own name
        Alias existing = new Alias("My Name");
        existing.setAliasListName(LIST);
        existing.addAliasID(new io.github.dsheirer.alias.id.talkgroup.Talkgroup(Protocol.APCO25, 100));
        mAliasModel.addAlias(existing);

        AliasListSynchronizer.Result result = apply(5000,
            List.of(talkgroup(100, "FD Dispatch", 1, 4000), talkgroup(200, "PD Main", 2, 4000)));

        assertTrue(result.applied());
        assertEquals(1, result.added());
        assertEquals(0, result.updated());
        assertEquals("My Name", alias(100).getName());
        assertEquals("PD Main", alias(200).getName());
        assertEquals("Police", alias(200).getGroup());
        assertEquals(5000, mConfiguration.getSynchronizedLastUpdated());
        assertEquals(5000, mConfiguration.getRadioReferenceLastUpdated());
        assertTrue(mConfiguration.getLastChecked() > 0);
        assertFalse(mConfiguration.isUpdateAvailable());
    }

    @Test
    void laterSynchronizationUpdatesOnlyChangedTalkgroups()
    {
        apply(5000, List.of(talkgroup(100, "FD Dispatch", 1, 4000), talkgroup(200, "PD Main", 2, 4000)));
        alias(200).setName("User Edited");

        //Talkgroup 100 was renamed on Radio Reference after the first synchronization; 200 didn't change
        AliasListSynchronizer.Result result = apply(9000, List.of(talkgroup(100, "FD Dispatch 1", 2, 8000),
            talkgroup(200, "PD Main", 2, 4000), talkgroup(300, "EMS", 1, 8000)));

        assertEquals(1, result.added());
        assertEquals(1, result.updated());
        assertEquals("FD Dispatch 1", alias(100).getName());
        assertEquals("Police", alias(100).getGroup());
        assertEquals("User Edited", alias(200).getName());
        assertEquals("EMS", alias(300).getName());
        assertEquals(9000, mConfiguration.getSynchronizedLastUpdated());
    }

    @Test
    void checkOnlyRecordsTimestamps()
    {
        AliasListSynchronizer.Result result = apply(7000, null);

        assertFalse(result.applied());
        assertEquals(7000, mConfiguration.getRadioReferenceLastUpdated());
        assertEquals(0, mConfiguration.getSynchronizedLastUpdated());
        assertTrue(mConfiguration.isUpdateAvailable());
        assertTrue(mAliasModel.getAliases().isEmpty());
    }

    @Test
    void deletedAliasListIsIgnored()
    {
        mAliasModel.deleteAliasList(LIST);
        AliasListSynchronizer.Result result = apply(5000, List.of(talkgroup(100, "FD Dispatch", 1, 4000)));

        assertFalse(result.applied());
        assertTrue(mAliasModel.getAliases().isEmpty());
    }
}
