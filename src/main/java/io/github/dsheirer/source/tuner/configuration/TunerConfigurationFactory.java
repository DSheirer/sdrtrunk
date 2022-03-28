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

package io.github.dsheirer.source.tuner.configuration;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerConfiguration;
import io.github.dsheirer.source.tuner.airspy.AirspyTunerEditor;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerEditor;
import io.github.dsheirer.source.tuner.fcd.proplusV2.FCD2TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.proplusV2.FCD2TunerEditor;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerConfiguration;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerEditor;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.recording.RecordingTunerConfiguration;
import io.github.dsheirer.source.tuner.recording.RecordingTunerEditor;
import io.github.dsheirer.source.tuner.rtl.RTL2832UnknownTunerEditor;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KTunerEditor;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.r820t.R820TTunerEditor;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

/**
 * Factory methods to support tuner configurations and configuration editors.
 */
public class TunerConfigurationFactory
{
    /**
     * Creates a tuner configuration for the specified tuner type, unique ID and name
     */
    public static TunerConfiguration getTunerConfiguration(TunerType type, String uniqueID)
    {
        switch(type)
        {
            case AIRSPY_R820T:
                return new AirspyTunerConfiguration(uniqueID);
            case ELONICS_E4000:
                return new E4KTunerConfiguration(uniqueID);
            case FUNCUBE_DONGLE_PRO:
                return new FCD1TunerConfiguration(uniqueID);
            case FUNCUBE_DONGLE_PRO_PLUS:
                return new FCD2TunerConfiguration(uniqueID);
            case HACKRF_JAWBREAKER:
            case HACKRF_ONE:
            case HACKRF_RAD1O:
                return new HackRFTunerConfiguration(uniqueID);
            case RAFAELMICRO_R820T:
                return new R820TTunerConfiguration(uniqueID);
            case RECORDING:
                return new RecordingTunerConfiguration(uniqueID);
            default:
                throw new IllegalArgumentException("Unrecognized tuner type [" + type.name() + "]");
        }
    }

    /**
     * Creates a tuner editor gui for the specified tuner
     */
    public static TunerEditor getEditor(UserPreferences userPreferences, DiscoveredTuner discoveredTuner,
                                        TunerManager tunerManager)
    {
        switch(discoveredTuner.getTunerClass())
        {
            case AIRSPY:
                return new AirspyTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case FUNCUBE_DONGLE_PRO:
                return new FCD1TunerEditor(userPreferences, tunerManager, discoveredTuner);
            case FUNCUBE_DONGLE_PRO_PLUS:
                return new FCD2TunerEditor(userPreferences, tunerManager, discoveredTuner);
            case HACKRF:
                return new HackRFTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case RTL2832:
                if(discoveredTuner.hasTuner())
                {
                    switch(discoveredTuner.getTuner().getTunerType())
                    {
                        case ELONICS_E4000:
                            return new E4KTunerEditor(userPreferences, tunerManager, discoveredTuner);
                        case RAFAELMICRO_R820T:
                            return new R820TTunerEditor(userPreferences, tunerManager, discoveredTuner);
                    }
                }
                return new RTL2832UnknownTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case RECORDING_TUNER:
                return new RecordingTunerEditor(userPreferences, tunerManager, discoveredTuner);
            case TEST_TUNER:
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Unsupported tuner class: " + discoveredTuner.getTunerClass());
        }
    }
}
