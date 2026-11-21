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

package io.github.dsheirer.alias;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.playlist.PlaylistV2;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Alias list configuration persistence and alias model rename/delete handling.
 */
public class AliasListConfigurationTest
{
    private static ObjectMapper mapper()
    {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        return new XmlMapper(xmlModule).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void playlistRoundTrip() throws Exception
    {
        AliasListConfiguration configuration = new AliasListConfiguration("County P25");
        configuration.track(1234, "County Public Safety");
        configuration.setAutoUpdate(true);
        configuration.setRadioReferenceLastUpdated(2000L);
        configuration.setSynchronizedLastUpdated(1000L);
        configuration.setLastChecked(3000L);

        Alias alias = new Alias("Fire Dispatch");
        alias.setAliasListName("County P25");

        PlaylistV2 playlist = new PlaylistV2();
        playlist.getAliases().add(alias);
        playlist.getAliasListConfigurations().add(configuration);

        String xml = mapper().writeValueAsString(playlist);
        PlaylistV2 read = mapper().readValue(xml, PlaylistV2.class);

        assertEquals(1, read.getAliasListConfigurations().size(), xml);
        AliasListConfiguration readConfiguration = read.getAliasListConfigurations().get(0);
        assertEquals("County P25", readConfiguration.getName());
        assertEquals(1234, readConfiguration.getRadioReferenceSystemId());
        assertEquals("County Public Safety", readConfiguration.getRadioReferenceSystemName());
        assertTrue(readConfiguration.isAutoUpdate());
        assertEquals(2000L, readConfiguration.getRadioReferenceLastUpdated());
        assertEquals(1000L, readConfiguration.getSynchronizedLastUpdated());
        assertEquals(3000L, readConfiguration.getLastChecked());
        assertTrue(readConfiguration.isUpdateAvailable());
        assertEquals(1, read.getAliases().size());
    }

    @Test
    void legacyPlaylistWithoutConfigurations() throws Exception
    {
        String xml = "<playlist version=\"4\"><alias name=\"A\" list=\"L\"/></playlist>";
        PlaylistV2 read = mapper().readValue(xml, PlaylistV2.class);
        assertTrue(read.getAliasListConfigurations().isEmpty());
        assertEquals(1, read.getAliases().size());
    }

    @Test
    void trackingLookupRenameAndDelete()
    {
        AliasModel model = new AliasModel();
        model.addAliasListConfigurations(List.of(new AliasListConfiguration("Plain")));
        AliasListConfiguration tracked = model.getOrCreateAliasListConfiguration("Tracked");
        tracked.track(42, "System 42");

        //Configured alias lists are listed before they have any aliases
        assertTrue(model.aliasListNames().contains("Tracked"));
        assertEquals("Tracked", model.getAliasListNameTracking(42));
        assertNull(model.getAliasListNameTracking(7));
        assertFalse(model.getAliasListConfiguration("Plain").isTrackingRadioReferenceSystem());

        Alias alias = new Alias("TG 1");
        alias.setAliasListName("Tracked");
        model.addAlias(alias);

        model.renameAliasList("Tracked", "Renamed");
        assertEquals("Renamed", model.getAliasListNameTracking(42));
        assertEquals("Renamed", alias.getAliasListName());
        assertNull(model.getAliasListConfiguration("Tracked"));

        model.refreshAliasListNames();
        assertTrue(model.aliasListNames().contains("Plain"));

        model.deleteAliasList("Renamed");
        assertNull(model.getAliasListNameTracking(42));
        assertTrue(model.getAliases().isEmpty());
    }
}
