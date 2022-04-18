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

package io.github.dsheirer.gui.preference;

import io.github.dsheirer.gui.JavaFxWindowRequest;

/**
 * Request to launch the preferences editor and scroll to a specific editor view
 */
public class ViewUserPreferenceEditorRequest extends JavaFxWindowRequest
{
    private PreferenceEditorType mPreferenceType;

    /**
     * Request to show the preference editor and focus the specified editor type control
     * @param preferenceEditorType to focus
     */
    public ViewUserPreferenceEditorRequest(PreferenceEditorType preferenceEditorType)
    {
        mPreferenceType = preferenceEditorType;
    }

    /**
     * Request to simply show the preference editor without focusing any of the components
     */
    public ViewUserPreferenceEditorRequest()
    {
    }

    public PreferenceEditorType getPreferenceType()
    {
        return mPreferenceType;
    }
}
