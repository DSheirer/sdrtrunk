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

package io.github.dsheirer.gui.symbol;

import javafx.scene.layout.VBox;

/**
 * Base class for channel viewer panel
 */
public class ChannelView extends VBox
{
    private boolean mShowing = false;

    /**
     * Constructs an instance.
     */
    public ChannelView()
    {
    }

    /**
     * Indicates if this panel is currently showing.
      * @return true if showing.
     */
    protected boolean isShowing()
    {
        return mShowing;
    }

    /**
     * Toggle the showing state to indicate when the user has selected to show this panel.
     * @param showing true or false to disable the panel.
     */
    public void setShowing(boolean showing)
    {
        mShowing = showing;
    }
}
