/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.gui.preference;

import io.github.dsheirer.gui.preference.application.ApplicationPreferenceEditor;
import io.github.dsheirer.gui.preference.calibration.VectorCalibrationPreferenceEditor;
import io.github.dsheirer.gui.preference.call.CallManagementPreferenceEditor;
import io.github.dsheirer.gui.preference.colortheme.ColorThemePreferenceEditor;
import io.github.dsheirer.gui.preference.decoder.JmbeLibraryPreferenceEditor;
import io.github.dsheirer.gui.preference.directory.DirectoryPreferenceEditor;
import io.github.dsheirer.gui.preference.mp3.MP3PreferenceEditor;
import io.github.dsheirer.gui.preference.playback.PlaybackPreferenceEditor;
import io.github.dsheirer.gui.preference.record.RecordPreferenceEditor;
import io.github.dsheirer.gui.preference.tuner.TunerPreferenceEditor;
import io.github.dsheirer.preference.UserPreferences;
import javafx.scene.Node;

/**
 * Creates an editor for the specified preference editor type
 */
public class PreferenceEditorFactory
{
    public static Node getEditor(PreferenceEditorType preferenceEditorType, UserPreferences userPreferences)
    {
        switch(preferenceEditorType)
        {
            case APPLICATION:
                return new ApplicationPreferenceEditor(userPreferences);
            case AUDIO_CALL_MANAGEMENT:
                return new CallManagementPreferenceEditor(userPreferences);
            case AUDIO_MP3:
                return new MP3PreferenceEditor(userPreferences);
            case AUDIO_OUTPUT:
                return new PlaybackPreferenceEditor(userPreferences);
            case AUDIO_RECORD:
                return new RecordPreferenceEditor(userPreferences);
            case CHANNEL_EVENT:
                return new DecodeEventViewPreferenceEditor(userPreferences);
            case COLOR_THEME:
                return new ColorThemePreferenceEditor(userPreferences);
            case DIRECTORY:
                return new DirectoryPreferenceEditor(userPreferences);
            case JMBE_LIBRARY:
                return new JmbeLibraryPreferenceEditor(userPreferences);
            case SOURCE_TUNERS:
                return new TunerPreferenceEditor(userPreferences);
            case TALKGROUP_FORMAT:
                return new TalkgroupFormatPreferenceEditor(userPreferences);
            case VECTOR_CALIBRATION:
                return new VectorCalibrationPreferenceEditor(userPreferences);
        }

        return null;
    }
}
