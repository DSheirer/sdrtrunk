/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.gui.preference;

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
            case TALKGROUP_FORMAT:
                return new TalkgroupFormatPreferenceEditor(userPreferences);
            case CHANNEL_EVENT:
                return new DecodeEventViewPreferenceEditor(userPreferences);
            case TUNER:
                return new TunerPreferenceEditor(userPreferences);
        }

        return null;
    }
}
