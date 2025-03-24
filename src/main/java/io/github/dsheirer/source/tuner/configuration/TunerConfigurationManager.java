/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages settings and configurations for all tuner types.
 */
public class TunerConfigurationManager implements IDiscoveredTunerStatusListener
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerConfigurationManager.class);
    private static final String SETTINGS_FILE_NAME = "tuner_configuration.json";
    private UserPreferences mUserPreferences;
    private List<DisabledTuner> mDisabledTunerList = new ArrayList<>();
    private List<TunerConfiguration> mTunerConfigurations = new ArrayList<>();
    private AtomicBoolean mSavePending = new AtomicBoolean();
    private Lock mLock = new ReentrantLock();

    /**
     * Constructs an instance and loads the save configuration state.
     *
     * @param userPreferences to determine directories for accessing files
     */
    public TunerConfigurationManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        load();
    }

    /**
     * Loads settings from the persisted tuner state file
     */
    private void load()
    {
        Path configPath = getConfigurationFilePath();

        if(Files.exists(configPath))
        {
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try
            {
                TunerConfigurationState state = objectMapper.readValue(configPath.toFile(), TunerConfigurationState.class);
                mDisabledTunerList.addAll(state.getDisabledTuners());
                mTunerConfigurations.addAll(state.getTunerConfigurations());
            }
            catch(IOException ioe)
            {
                mLog.error("Error loading tuner configuration file", ioe);
            }
        }
    }

    /**
     * Tuner configuration state file (.json).
     */
    private Path getConfigurationFilePath()
    {
        return mUserPreferences.getDirectoryPreference().getDirectoryConfiguration().resolve(SETTINGS_FILE_NAME);
    }

    /**
     * Saves the current tuner configuration state to disk.
     */
    private void save()
    {
        TunerConfigurationState state = new TunerConfigurationState();

        mLock.lock();

        try
        {
            state.setDisabledTuners(new ArrayList<>(mDisabledTunerList));
            state.setTunerConfigurations(new ArrayList<>(mTunerConfigurations));
        }
        catch(Exception e)
        {
            mLog.error("Error", e);
        }
        finally
        {
            mLock.unlock();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try
        {
            objectMapper.writeValue(getConfigurationFilePath().toFile(), state);
        }
        catch(IOException ioe)
        {
            mLog.error("Error writing tuner configuration state to file [" + getConfigurationFilePath() + "]", ioe);
        }
    }

    /**
     * Schedules a configurations save task.  Subsequent calls to this method will be ignored until the
     * save event occurs, thus limiting repetitive saving to a minimum.
     */
    public void saveConfigurations()
    {
        if(mSavePending.compareAndSet(false, true))
        {
            ThreadPool.SCHEDULED.schedule(new ConfigurationSaveTask(), 2, TimeUnit.SECONDS);
        }
    }

    /**
     * Monitors discovered tuner enabled status and applies configurations or updates disable state of tuners.
     * @param discoveredTuner that has a status change.
     * @param previous tuner status
     * @param current tuner status
     */
    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        if(current == TunerStatus.DISABLED)
        {
            addDisabledTuner(discoveredTuner);
        }
        else if(current == TunerStatus.ENABLED)
        {
            removeDisabledTuner(discoveredTuner);

            if(discoveredTuner.hasTuner())
            {
                TunerType tunerType = discoveredTuner.getTuner().getTunerType();

                if(tunerType != TunerType.RECORDING)
                {
                    TunerConfiguration tunerConfiguration = getTunerConfiguration(tunerType, discoveredTuner.getId());

                    if(tunerConfiguration != null)
                    {
                        discoveredTuner.setTunerConfiguration(tunerConfiguration);
                        saveConfigurations();
                    }
                }
            }
        }
    }

    /**
     * Updates the tuner configuration with the current tuner PPM setting so that the value can be stored across
     * sessions.
     * @param discoveredTuner that has an updated PPM value.
     */
    public void updateTunerPPM(DiscoveredTuner discoveredTuner)
    {
        if(discoveredTuner != null)
        {
            TunerType tunerType = discoveredTuner.getTuner().getTunerType();

            if(tunerType != TunerType.RECORDING)
            {
                TunerConfiguration tunerConfiguration = getTunerConfiguration(tunerType, discoveredTuner.getId());

                if(tunerConfiguration != null)
                {
                    tunerConfiguration.setFrequencyCorrection(discoveredTuner.getTuner().getTunerController().getFrequencyCorrection());
                    saveConfigurations();
                }
            }
        }
    }

    /**
     * Adds the discovered tuner to the list of disabled tuners
     */
    private void addDisabledTuner(DiscoveredTuner discoveredTuner)
    {
        if(!isDisabled(discoveredTuner))
        {
            mLock.lock();

            try
            {
                mDisabledTunerList.add(new DisabledTuner(discoveredTuner.getTunerClass(), discoveredTuner.getId()));
            }
            finally
            {
                mLock.unlock();
            }

            saveConfigurations();
        }
    }

    /**
     * Removes the tuner from the disabled tuners list
     * @param discoveredTuner to remove
     */
    private void removeDisabledTuner(DiscoveredTuner discoveredTuner)
    {
        mLock.lock();

        try
        {
            mDisabledTunerList.removeIf(tuner -> tuner.matches(discoveredTuner));
        }
        finally
        {
            mLock.unlock();
        }

        saveConfigurations();
    }

    /**
     * Indicates if the discovered tuner is disabled.  This method should only be used to determine the disabled state
     * of a tuner when it is added to the system for use, such as tuner discovery at application startup, or for
     * USB tuner hotplug device add notifications.
     *
     * @param discoveredTuner to check for disabled status.
     * @return true if the tuner is supposed to be disabled.
     */
    public boolean isDisabled(DiscoveredTuner discoveredTuner)
    {
        return findDisabledTuner(discoveredTuner) != null;
    }

    /**
     * Finds the disabled tuner that matches the discovered tuner.
     * @param discoveredTuner to search for
     * @return disabled tuner instance or null if the discovered tuner is not currently disabled.
     */
    private DisabledTuner findDisabledTuner(DiscoveredTuner discoveredTuner)
    {
        DisabledTuner found = null;

        mLock.lock();

        try
        {
            for(DisabledTuner disabledTuner: mDisabledTunerList)
            {
                if(disabledTuner.matches(discoveredTuner))
                {
                    found = disabledTuner;
                    break;
                }
            }
        }
        finally
        {
            mLock.unlock();
        }

        return found;
    }

    /**
     * Adds the tuner configuration if one doesn't exist that matches the tuner type and unique id.
     * @param tunerConfiguration to add
     */
    public void addTunerConfiguration(TunerConfiguration tunerConfiguration)
    {
        if(!mTunerConfigurations.stream().filter(config -> config.getTunerType().equals(tunerConfiguration.getTunerType()) &&
                config.getUniqueID().equalsIgnoreCase(tunerConfiguration.getUniqueID())).findFirst().isPresent())
        {
            mTunerConfigurations.add(tunerConfiguration);
            saveConfigurations();
        }
    }

    /**
     * Removes the tuner configuration from this manager.
     * @param tunerConfiguration to remove
     */
    public void removeTunerConfiguration(TunerConfiguration tunerConfiguration)
    {
        mTunerConfigurations.remove(tunerConfiguration);
        saveConfigurations();
    }

    /**
     * Provides an existing or creates a new tuner configuration for the specified tuner type and unique ID value.
     *
     * Note: this method is not thread-safe.
     */
    public TunerConfiguration getTunerConfiguration(TunerType type, String uniqueID )
    {
        Optional<TunerConfiguration> optional = mTunerConfigurations.stream().filter(config -> config.getTunerType().equals(type) &&
                config.getUniqueID().equalsIgnoreCase(uniqueID)).findFirst();

        if(optional.isPresent())
        {
            return optional.get();
        }

        TunerConfiguration config = TunerFactory.getTunerConfiguration(type, uniqueID);
        addTunerConfiguration(config);
        return config;
    }

    /**
     * Get all tuner configurations that match the specified tuner type.
     * @param tunerType to match
     * @return list of all configurations.
     */
    public List<TunerConfiguration> getTunerConfigurations(TunerType tunerType)
    {
        return mTunerConfigurations.stream().filter(tunerConfiguration -> tunerConfiguration.getTunerType()
                .equals(tunerType)).toList();
    }

    /**
     * Saves the current tuner configuration state and resets the save pending flag
     */
    public class ConfigurationSaveTask implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                save();
            }
            catch(Exception e)
            {
                mLog.error("Error saving tuner configurations", e);
            }

            mSavePending.set(false);
        }
    }
}
