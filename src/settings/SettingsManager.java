/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package settings;

import controller.ThreadPoolManager;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import sample.Listener;
import settings.ColorSetting.ColorSettingName;
import source.recording.RecordingConfiguration;
import source.tuner.configuration.TunerConfigurationEvent;
import source.tuner.configuration.TunerConfigurationModel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private TunerConfigurationModel mTunerConfigurationModel;
    private ThreadPoolManager mThreadPoolManager;
    private boolean mLoadingSettings = false;
    private AtomicBoolean mSettingsSavePending = new AtomicBoolean();

    public SettingsManager(ThreadPoolManager threadPoolManager,
                           TunerConfigurationModel tunerConfigurationModel)
    {
        //TODO: move settings into a SettingsModel
        //and update this class to only provide loading, saving, and model
        //change detection producing a save.

        mThreadPoolManager = threadPoolManager;
        mTunerConfigurationModel = tunerConfigurationModel;

        //Register for tuner config events so that we can save the settings
        mTunerConfigurationModel.addListener(this);

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
    public ColorSetting getColorSetting(ColorSettingName name)
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
    public void setColorSetting(ColorSettingName name, Color color)
    {
        ColorSetting setting = getColorSetting(name);

        setting.setColor(color);

        broadcastSettingChange(setting);

        scheduleSettingsSave();
    }

    public void resetColorSetting(ColorSettingName name)
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

    public List<RecordingConfiguration> getRecordingConfigurations()
    {
        return mSettings.getRecordingConfigurations();
    }

    public void addRecordingConfiguration(RecordingConfiguration config)
    {
        mSettings.addRecordingConfiguration(config);
        scheduleSettingsSave();
    }

    public void removeRecordingConfiguration(RecordingConfiguration config)
    {
        mSettings.removeRecordingConfiguration(config);
        scheduleSettingsSave();
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
        saveTunerConfigurationModel();

        JAXBContext context = null;

        SystemProperties props = SystemProperties.getInstance();

        Path settingsPath = props.getApplicationFolder("settings");

        String settingsDefault = props.get("settings.defaultFilename",
            "settings.xml");

        String settingsCurrent = props.get("settings.currentFilename",
            settingsDefault);

        Path filePath = settingsPath.resolve(settingsCurrent);

        File outputFile = new File(filePath.toString());

        try
        {
            if(!outputFile.exists())
            {
                outputFile.createNewFile();
            }
        }
        catch(Exception e)
        {
            mLog.error("SettingsManager - couldn't create file to save "
                + "settings [" + filePath.toString() + "]", e);
        }

        OutputStream out = null;

        try
        {
            out = new FileOutputStream(outputFile);

            try
            {
                context = JAXBContext.newInstance(Settings.class);

                Marshaller m = context.createMarshaller();

                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                m.marshal(mSettings, out);
            }
            catch(JAXBException e)
            {
                mLog.error("SettingsManager - jaxb exception while saving " +
                    "settings", e);
            }
        }
        catch(Exception e)
        {
            mLog.error("SettingsManager - coulcn't open outputstream to " +
                "save settings [" + filePath.toString() + "]");
        }
        finally
        {
            if(out != null)
            {
                try
                {
                    out.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
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

            JAXBContext context = null;

            InputStream in = null;

            try
            {
                in = new FileInputStream(settingsPath.toString());

                try
                {
                    context = JAXBContext.newInstance(Settings.class);

                    Unmarshaller m = context.createUnmarshaller();

                    mSettings = (Settings) m.unmarshal(in);
                }
                catch(JAXBException e)
                {
                    mLog.error("SettingsManager - jaxb exception while loading " +
                        "settings", e);
                }
            }
            catch(Exception e)
            {
                mLog.error("SettingsManager - coulcn't open inputstream to " +
                    "load settings [" + settingsPath.toString() + "]", e);
            }
            finally
            {
                if(in != null)
                {
                    try
                    {
                        in.close();
                    }
                    catch(IOException e)
                    {
                        mLog.error("SettingsManager - exception while closing " +
                            "the settings file inputstream reader", e);
                    }
                }
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

        loadTunerConfigurationModel();

        mLoadingSettings = false;
    }

    private void loadTunerConfigurationModel()
    {
        if(mSettings != null)
        {
            mTunerConfigurationModel.clear();

            mTunerConfigurationModel.addTunerConfigurations(
                mSettings.getTunerConfigurations());
        }
    }

    private void saveTunerConfigurationModel()
    {
        if(mSettings != null)
        {
            mSettings.setTunerConfigurations(
                mTunerConfigurationModel.getTunerConfigurations());
        }
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
                mThreadPoolManager.scheduleOnce(new SettingsSaveTask(),
                    2, TimeUnit.SECONDS);
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
