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
package io.github.dsheirer.settings.source;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.settings.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The channelizer type setting: each type is saved and read back by a new settings instance (as at the next start),
 * and a missing or unrecognized stored value reads as the default, polyphase.
 */
public class TunerSettingsTest
{
    @AfterEach
    void removeSetting()
    {
        Setting.TUNER_CHANNELIZER_TYPE.remove();
    }

    @Test
    void channelizerTypeIsSavedAndRead()
    {
        for(ChannelizerType type : ChannelizerType.values())
        {
            new TunerSettings(settingsType -> {}).setChannelizerType(type);
            assertEquals(type.name(), Setting.TUNER_CHANNELIZER_TYPE.get(null));
            assertEquals(type, new TunerSettings(settingsType -> {}).getChannelizerType(), type.name());
        }
    }

    @Test
    void missingOrUnrecognizedTypeReadsAsPolyphase()
    {
        Setting.TUNER_CHANNELIZER_TYPE.remove();
        assertEquals(ChannelizerType.POLYPHASE, new TunerSettings(settingsType -> {}).getChannelizerType());

        Setting.TUNER_CHANNELIZER_TYPE.put("SOMETHING_ELSE");
        assertEquals(ChannelizerType.POLYPHASE, new TunerSettings(settingsType -> {}).getChannelizerType());

        Setting.TUNER_CHANNELIZER_TYPE.put("fast_convolution");
        assertEquals(ChannelizerType.FAST_CONVOLUTION, new TunerSettings(settingsType -> {}).getChannelizerType());
    }
}
