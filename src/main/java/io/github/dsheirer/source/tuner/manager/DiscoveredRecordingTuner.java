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

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.recording.RecordingTuner;
import io.github.dsheirer.source.tuner.recording.RecordingTunerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovered tuner for recording tuner type.
 */
public class DiscoveredRecordingTuner extends DiscoveredTuner
{
    private static final Logger mLog = LoggerFactory.getLogger(DiscoveredRecordingTuner.class);
    private UserPreferences mUserPreferences;

    /**
     * Constructs an instance
     * @param userPreferences instance
     * @param recordingTunerConfiguration with recording path and frequency
     */
    public DiscoveredRecordingTuner(UserPreferences userPreferences,
                                    RecordingTunerConfiguration recordingTunerConfiguration)
    {
        mUserPreferences = userPreferences;
        setTunerConfiguration(recordingTunerConfiguration);

        //Default all recordings to be disabled on startup
        setEnabled(false);
    }

    /**
     * Access the tuner configuration as a recording tuner configuration
     */
    public RecordingTunerConfiguration getRecordingTunerConfiguration()
    {
        return (RecordingTunerConfiguration) getTunerConfiguration();
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.RECORDING_TUNER;
    }

    @Override
    public String getId()
    {
        return getRecordingTunerConfiguration().getPath();
    }

    @Override
    public void start()
    {
        if(!hasTuner())
        {
            mTuner = new RecordingTuner(mUserPreferences, this, getRecordingTunerConfiguration());

            try
            {
                mTuner.start();
            }
            catch(SourceException se)
            {
                setErrorMessage("Error - " + se.getMessage());
            }
        }
    }

    @Override
    public String toString()
    {
        return "Recording [" + getRecordingTunerConfiguration().getPath() + "]";
    }
}
