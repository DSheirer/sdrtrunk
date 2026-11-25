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

import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChannelMuteSettingsTest
{
    @Test
    public void keysAreValidPreferenceKeys()
    {
        String key = ChannelMuteSettings.mutedChannelKey("state police|troop a/framingham|ch 1 (main)");
        assertTrue(key.matches("[a-zA-Z0-9._\\-]+"), key);
        assertTrue(key.length() <= Preferences.MAX_KEY_LENGTH);
    }

    @Test
    public void longIdentitiesWithTheSamePrefixGetDifferentKeys()
    {
        String prefix = "x".repeat(200);
        String a = ChannelMuteSettings.mutedChannelKey(prefix + "|site|channel a");
        String b = ChannelMuteSettings.mutedChannelKey(prefix + "|site|channel b");

        assertTrue(a.length() <= Preferences.MAX_KEY_LENGTH);
        assertTrue(b.length() <= Preferences.MAX_KEY_LENGTH);
        assertNotEquals(a, b);
    }

    @Test
    public void keyMatchesTheSetting()
    {
        String identity = "state police|troop a/framingham|ch 1 (main)";
        String key = ChannelMuteSettings.mutedChannelKey(identity);
        assertTrue(key.startsWith("now.playing.channel.muted."), key);
        assertTrue(Setting.NOW_PLAYING_CHANNEL_MUTED.getKey(key.substring("now.playing.channel.muted.".length()))
            .equals(key), key);
    }
}
