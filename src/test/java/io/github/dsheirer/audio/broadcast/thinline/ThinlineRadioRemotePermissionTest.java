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

package io.github.dsheirer.audio.broadcast.thinline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.dsheirer.preference.thinline.ThinlineRadioPreference.Remote;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ThinlineRadioRemotePermissionTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static ObjectNode enabled(boolean enabled)
    {
        return MAPPER.createObjectNode().put("enabled", enabled);
    }

    @Test
    void everyCommandThatChangesTheSiteNeedsAPermission()
    {
        List<String> changes = List.of("setTuner", "setPpm", "setFrequency", "saveTunerSettings", "setChannel",
            "saveChannel", "addChannel", "deleteChannel", "cloneChannel", "saveChannelSettings", "saveAlias",
            "deleteAlias", "saveChannelMap", "deleteChannelMap", "setStream", "saveStreamSettings", "addStream",
            "deleteStream", "setAliasStream", "setAliasStreamAll", "savePreferences", "desktopInput");

        for(String command: changes)
        {
            assertNotNull(ThinlineRadioHealthReporter.requiredRemote(command, MAPPER.createObjectNode()), command);
        }
    }

    @Test
    void readingStatusNeedsNoPermission()
    {
        for(String command: List.of("report", "watch", "playlist", "aliases", "channelSettings", "tunerSettings",
            "streamSettings", "preferences"))
        {
            assertNull(ThinlineRadioHealthReporter.requiredRemote(command, MAPPER.createObjectNode()), command);
        }
    }

    @Test
    void desktopAndSpectrumNeedPermissionToStartButNotToStop()
    {
        assertEquals(Remote.DESKTOP, ThinlineRadioHealthReporter.requiredRemote("desktop", enabled(true)));
        assertNull(ThinlineRadioHealthReporter.requiredRemote("desktop", enabled(false)));
        assertEquals(Remote.SPECTRUM, ThinlineRadioHealthReporter.requiredRemote("spectrum", enabled(true)));
        assertNull(ThinlineRadioHealthReporter.requiredRemote("spectrum", enabled(false)));
    }
}
