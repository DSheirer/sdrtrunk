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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.util.ThreadPool;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SettingsManager implements Listener<TunerConfigurationEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(SettingsManager.class);

    private Settings mSettings = new Settings();
    private List<SettingChangeListener> mListeners = new ArrayList<>();
    private boolean mLoadingSettings = false;
    private AtomicBoolean mSettingsSavePending = new AtomicBoolean();

    public SettingsManager()
    {
        //TODO: move settings into a SettingsModel
        //and update this class to only provide loading, saving, and model
        //change detection producing a save.

        init();
    }

    /**
     * Loads settings from the current settings file, or the default settings file,
     * as specified in the current SDRTrunk system settings
     */
    private void init()
    {
        SystemProperties props = SystemProperties.getInstance();

        Path settingsFolder = props.getApplicationFolder("settings");

        String defaultSettingsFile =
            props.get("settings.defaultFilename", "settings.xml");

        String settingsFile =
            props.get("settings.currentFilename", defaultSettingsFile);

        load(settingsFolder.resolve(settingsFile));
    }

    @Override
    public void receive(TunerConfigurationEvent t)
    {
        if(!mLoadingSettings)
        {
            scheduleSettingsSave();
        }
    }

    public Settings getSettings()
    {
        return mSettings;
    }

    public void setSettings(Settings settings)
    {
        mSettings = settings;
    }

    public Setting getSetting(String name)
    {
        return mSettings.getSetting(name);
    }

    /**
     * Returns the current setting, or if the setting doesn't exist
     * returns a newly created setting with the specified parameters
     */
    public ColorSetting getColorSetting(ColorSetting.ColorSettingName name)
    {
        ColorSetting setting = mSettings.getColorSetting(name);

        if(setting == null)
        {
            setting = new ColorSetting(name);

            addSetting(setting);
        }

        return setting;
    }


    /**
     * Fetches the current setting and applies the parameter(s) to it.  Creates
     * the setting if it does not exist
     */
    public void setColorSetting(ColorSetting.ColorSettingName name, Color color)
    {
        ColorSetting setting = getColorSetting(name);

        setting.setColor(color);

        broadcastSettingChange(setting);

        scheduleSettingsSave();
    }

    public void resetColorSetting(ColorSetting.ColorSettingName name)
    {
        setColorSetting(name, name.getDefaultColor());
    }

    public void resetAllColorSettings()
    {
        for(ColorSetting color : mSettings.getColorSettings())
        {
            resetColorSetting(color.getColorSettingName());
        }
    }

    /**
     * Returns the current setting, or if the setting doesn't exist
     * returns a newly created setting with the specified parameters
     */
    public FileSetting getFileSetting(String name, String defaultPath)
    {
        FileSetting setting = mSettings.getFileSetting(name);

        if(setting == null)
        {
            setting = new FileSetting(name, defaultPath);

            addSetting(setting);
        }

        return setting;
    }

    /**
     * Fetches the current setting and applies the parameter(s) to it.  Creates
     * the setting if it does not exist
     */
    public void setFileSetting(String name, String path)
    {
        FileSetting setting = getFileSetting(name, path);

        setting.setPath(path);

        broadcastSettingChange(setting);

        scheduleSettingsSave();
    }

    /**
     * Adds the setting and stores the set of settings
     *
     * @param setting
     */
    private void addSetting(Setting setting)
    {
        mSettings.addSetting(setting);

        scheduleSettingsSave();

        broadcastSettingChange(setting);
    }

    public MapViewSetting getMapViewSetting(String name, GeoPosition position, int zoom)
    {
        MapViewSetting loc = mSettings.getMapViewSetting(name);

        if(loc != null)
        {
            return loc;
        }
        else
        {
            MapViewSetting newLoc = new MapViewSetting(name, position, zoom);

            addSetting(newLoc);

            return newLoc;
        }
    }

    public void setMapViewSetting(String name, GeoPosition position, int zoom)
    {
        MapViewSetting loc = getMapViewSetting(name, position, zoom);

        loc.setGeoPosition(position);
        loc.setZoom(zoom);

        scheduleSettingsSave();
    }

    private void save()
    {
        SystemProperties props = SystemProperties.getInstance();

        Path settingsFolder = props.getApplicationFolder("settings");

        String settingsDefault = props.get("settings.defaultFilename",
            "settings.xml");

        String settingsCurrent = props.get("settings.currentFilename",
            settingsDefault);

        Path settingsPath = settingsFolder.resolve(settingsCurrent);

        try(OutputStream out = Files.newOutputStream(settingsPath))
        {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(out, mSettings);
            out.flush();
        }
        catch(IOException ioe)
        {
            mLog.error("IO error while writing the settings to a file [" + settingsPath + "]", ioe);
        }
        catch(Exception e)
        {
            mLog.error("Error while saving settings file [" + settingsPath + "]", e);
        }
    }

    /**
     * Erases current settings and loads settings from the settingsPath filename,
     * if it exists.
     */
    public void load(Path settingsPath)
    {
        mLoadingSettings = true;

        if(Files.exists(settingsPath))
        {
            mLog.info("SettingsManager - loading settings file [" + settingsPath.toString() + "]");

            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try(InputStream in = Files.newInputStream(settingsPath))
            {
                mSettings = objectMapper.readValue(in, Settings.class);
            }
            catch(IOException ioe)
            {
                mLog.error("IO error while reading settings file", ioe);
            }
        }
        else
        {
            mLog.info("SettingsManager - settings does not exist [" +
                settingsPath.toString() + "]");
        }

        if(mSettings == null)
        {
            mSettings = new Settings();
        }

        mLoadingSettings = false;
    }

    public void broadcastSettingChange(Setting setting)
    {
        Iterator<SettingChangeListener> it = mListeners.iterator();

        while(it.hasNext())
        {
            SettingChangeListener listener = it.next();

            if(listener == null)
            {
                it.remove();
            }
            else
            {
                listener.settingChanged(setting);
            }
        }
    }

    public void broadcastSettingDeleted(Setting setting)
    {
        Iterator<SettingChangeListener> it = mListeners.iterator();

        while(it.hasNext())
        {
            SettingChangeListener listener = it.next();

            if(listener == null)
            {
                it.remove();
            }
            else
            {
                listener.settingDeleted(setting);
            }
        }
    }

    public void addListener(SettingChangeListener listener)
    {
        mListeners.add(listener);
    }

    public void removeListener(SettingChangeListener listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Schedules a settings save task.  Subsequent calls to this method will be ignored until the
     * save event occurs, thus limiting repetitive saving to a minimum.
     */
    private void scheduleSettingsSave()
    {
        if(!mLoadingSettings)
        {
            if(mSettingsSavePending.compareAndSet(false, true))
            {
                ThreadPool.SCHEDULED.schedule(new SettingsSaveTask(), 2, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Resets the settings save pending flag to false and proceeds to save the
     * settings.
     */
    public class SettingsSaveTask implements Runnable
    {
        @Override
        public void run()
        {
            mSettingsSavePending.set(false);

            save();
        }
    }
}
