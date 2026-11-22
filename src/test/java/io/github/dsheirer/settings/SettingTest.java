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

package io.github.dsheirer.settings;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Setting enumeration.  The node path and key checks guard the stored values of earlier versions: a changed
 * path or key means users lose that setting.
 */
public class SettingTest
{
    @Test
    void nodePathsAreTheLegacyPackagePaths()
    {
        assertEquals("/io/github/dsheirer/preference/application", Setting.APPLICATION_THEME.getNodePath());
        assertEquals("/io/github/dsheirer/preference/duplicate", Setting.CALL_DETECT_DUPLICATE_RADIO.getNodePath());
        assertEquals("/io/github/dsheirer/preference/source", Setting.TUNER_CHANNELIZER_TYPE.getNodePath());
        assertEquals("/io/github/dsheirer/preference/decoder", Setting.JMBE_LIBRARY_PATH.getNodePath());
        assertEquals("/io/github/dsheirer/preference/identifier", Setting.TALKGROUP_FORMAT.getNodePath());
        assertEquals("/io/github/dsheirer/preference/calibration", Setting.VECTOR_ENABLED.getNodePath());
        assertEquals("/io/github/dsheirer/gui", Setting.MAIN_WINDOW_SPECTRAL_DIVIDER.getNodePath());
        assertEquals("/io/github/dsheirer/gui", Setting.RECENT_FILE.getNodePath());
        assertEquals("/io/github/dsheirer/gui/table", Setting.TABLE_COLUMN_ORDER.getNodePath());
        assertEquals("/io/github/dsheirer/gui/viewer", Setting.VIEWER_DMR_LAST_DIRECTORY.getNodePath());
        assertEquals("/io/github/dsheirer/vector/calibrate", Setting.VECTOR_CALIBRATION.getNodePath());

        for(Setting setting: Setting.values())
        {
            assertFalse(setting.getNodePath().startsWith("/io/github/dsheirer/settings"),
                setting + " must stay in its legacy node");
        }
    }

    @Test
    void keysAreUniqueWithinEachNode()
    {
        Set<String> keys = new HashSet<>();

        for(Setting setting: Setting.values())
        {
            assertTrue(keys.add(setting.getNodePath() + "|" + setting.getKey(new Object[setting.getArgumentCount()])),
                "Duplicate key for " + setting);
        }
    }

    @Test
    void templatedKeysMatchTheLegacyKeys()
    {
        assertEquals("ui.theme", Setting.APPLICATION_THEME.getKey());
        assertEquals("talkgroup.format.APCO25", Setting.TALKGROUP_FORMAT.getKey("APCO25"));
        assertEquals("stage.width.main.window", Setting.JAVAFX_STAGE_WIDTH.getKey("main.window"));
        assertEquals("state.maximizedmain.window", Setting.JAVAFX_STAGE_MAXIMIZED.getKey("main.window"));
        assertEquals("playlist.recent.file.3", Setting.RECENT_FILE.getKey("playlist", 3));
        assertEquals("now.playing.column.width.alias", Setting.TABLE_COLUMN_WIDTH.getKey("now.playing", "alias"));
        assertEquals("now.playing.column.order", Setting.TABLE_COLUMN_ORDER.getKey("now.playing"));
        assertEquals("events.column.2", Setting.SWING_COLUMN_WIDTH.getKey("events", 2));
        assertEquals("tile.server.key.thunderforest", Setting.MAP_TILE_SERVER_KEY.getKey("thunderforest"));
        assertEquals("FIR_FILTER - V1", Setting.VECTOR_CALIBRATION.getKey("FIR_FILTER - V1"));
    }

    @Test
    void windowKeysAreTheLegacyStageKeys()
    {
        assertEquals("main.window", Setting.Window.MAIN_WINDOW.getKey());
        assertEquals("user.preferences", Setting.Window.SETTINGS_EDITOR.getKey());
        assertEquals("playlist", Setting.Window.PLAYLIST_EDITOR.getKey());
        assertEquals("channel.map", Setting.Window.CHANNEL_MAP_EDITOR.getKey());
        assertEquals("icon.manager", Setting.Window.ICON_MANAGER.getKey());
        assertEquals("jmbe.editor", Setting.Window.JMBE_EDITOR.getKey());
        assertEquals("recording.viewer", Setting.Window.RECORDING_VIEWER.getKey());
        assertEquals("stage.x.user.preferences", Setting.JAVAFX_STAGE_X.getKey(Setting.Window.SETTINGS_EDITOR.getKey()));
    }

    @Test
    void wrongArgumentCountIsRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> Setting.APPLICATION_THEME.getKey("extra"));
        assertThrows(IllegalArgumentException.class, () -> Setting.TALKGROUP_FORMAT.getKey());
        assertThrows(IllegalArgumentException.class, () -> Setting.TALKGROUP_FORMAT.get("default"));
        assertThrows(IllegalArgumentException.class, () -> Setting.RECENT_FILE.with("playlist"));
    }

    @Test
    void valueStoresAndRemoves() throws BackingStoreException
    {
        Preferences node = Preferences.userRoot().node("sdrtrunk-test/" + UUID.randomUUID());

        try
        {
            Setting.Value value = new Setting.Value(node, Setting.TABLE_COLUMN_ORDER.getKey("test"));
            assertNull(value.get(null));
            value.put("a,b");
            assertEquals("a,b", node.get("test.column.order", null));
            value.putDouble(1.5);
            assertEquals(1.5, value.getDouble(0.0));
            value.remove();
            assertNull(value.get(null));
        }
        finally
        {
            node.removeNode();
        }
    }
}
