/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.map.DefaultIcon;
import io.github.dsheirer.map.MapIcon;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.source.recording.RecordingConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JacksonXmlRootElement(localName = "SDRTrunk_settings")
public class Settings
{
    private List<Setting> mSettings = new ArrayList<Setting>();

    private List<TunerConfiguration> mTunerConfiguration = new ArrayList<TunerConfiguration>();

    //No longer used
    private List<RecordingConfiguration> mRecordingConfigurations = new ArrayList<RecordingConfiguration>();

    public Settings()
    {
    }

    @JacksonXmlProperty(isAttribute = false, localName = "recording_configuration")
    public List<RecordingConfiguration> getRecordingConfigurations()
    {
        return Collections.emptyList();
    }

    public void setRecordingConfigurations(List<RecordingConfiguration> configs)
    {
    }

    @JacksonXmlProperty(isAttribute = false, localName = "tuner_configuration")
    public List<TunerConfiguration> getTunerConfigurations()
    {
        return mTunerConfiguration;
    }

    public void setTunerConfigurations(List<TunerConfiguration> configs)
    {
        mTunerConfiguration = configs;
    }

    @JacksonXmlProperty(isAttribute = false, localName = "setting")
    public List<Setting> getSettings()
    {
        return mSettings;
    }

    public void setSettings(ArrayList<Setting> settings)
    {
        mSettings = settings;
    }

    public Setting getSetting(String name)
    {
        for(Setting setting : mSettings)
        {
            if(setting.getName().contentEquals(name))
            {
                return setting;
            }
        }

        return null;
    }

    public void addSetting(Setting setting)
    {
        mSettings.add(setting);
    }

    public void removeSetting(Setting setting)
    {
        mSettings.remove(setting);
    }

    @JsonIgnore
    public DefaultIcon getDefaultIcon()
    {
        for(Setting setting : mSettings)
        {
            if(setting instanceof DefaultIcon)
            {
                return (DefaultIcon)setting;
            }
        }

        return null;
    }

    public ColorSetting getColorSetting(ColorSettingName name)
    {
        for(Setting setting : mSettings)
        {
            if(setting instanceof ColorSetting &&
                ((ColorSetting)setting).getColorSettingName() == name)
            {
                return (ColorSetting)setting;
            }
        }

        return null;
    }

    @JsonIgnore
    public List<ColorSetting> getColorSettings()
    {
        ArrayList<ColorSetting> colors = new ArrayList<ColorSetting>();

        for(Setting setting : mSettings)
        {
            if(setting instanceof ColorSetting)
            {
                colors.add((ColorSetting)setting);
            }
        }

        return colors;
    }

    public FileSetting getFileSetting(String name)
    {
        for(Setting setting : mSettings)
        {
            if(setting instanceof FileSetting &&
                setting.getName().contentEquals(name))
            {
                return (FileSetting)setting;
            }
        }

        return null;
    }

    public MapIcon getMapIcon(String name)
    {
        for(Setting setting : mSettings)
        {
            if(setting instanceof MapIcon &&
                setting.getName().contentEquals(name))
            {
                return (MapIcon)setting;
            }
        }

        return null;
    }

    public ArrayList<MapIcon> getMapIcons()
    {
        ArrayList<MapIcon> icons = new ArrayList<MapIcon>();

        for(Setting setting : mSettings)
        {
            if(setting instanceof MapIcon)
            {
                icons.add((MapIcon)setting);
            }
        }

        return icons;
    }

    public MapViewSetting getMapViewSetting(String name)
    {
        for(Setting setting : mSettings)
        {
            if(setting instanceof MapViewSetting &&
                setting.getName().contentEquals(name))
            {
                return (MapViewSetting)setting;
            }
        }

        return null;
    }
}
