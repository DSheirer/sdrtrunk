/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias.action;

import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating JavaFX editors for alias identifiers
 */
public class ActionEditorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(ActionEditorFactory.class);

    public static ActionEditor getEditor(AliasActionType type, UserPreferences userPreferences)
    {
        switch(type)
        {
            case BEEP:
                return new BeepEditor();
            case CLIP:
                return new ClipEditor();
            case SCRIPT:
                return new ScriptEditor();
            default:
                return new UnrecognizedActionEditor();
        }
    }
}
