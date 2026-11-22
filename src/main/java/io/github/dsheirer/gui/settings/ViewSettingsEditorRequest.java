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

package io.github.dsheirer.gui.settings;

import io.github.dsheirer.gui.JavaFxWindowRequest;

/**
 * Request to show the settings dialog, optionally on a specific page
 */
public class ViewSettingsEditorRequest extends JavaFxWindowRequest
{
    private SettingsPageType mPageType;

    /**
     * Request to show the settings dialog on the specified page
     * @param pageType to show
     */
    public ViewSettingsEditorRequest(SettingsPageType pageType)
    {
        mPageType = pageType;
    }

    /**
     * Request to show the settings dialog on the page that it last showed
     */
    public ViewSettingsEditorRequest()
    {
    }

    /**
     * Page to show, or null to show the page that the dialog last showed
     */
    public SettingsPageType getPageType()
    {
        return mPageType;
    }
}
