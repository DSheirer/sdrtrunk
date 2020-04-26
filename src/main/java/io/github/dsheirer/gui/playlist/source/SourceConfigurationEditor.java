/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.source;

import io.github.dsheirer.source.config.SourceConfiguration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.HBox;

public abstract class SourceConfigurationEditor<T extends SourceConfiguration> extends HBox
{
    private T mSourceConfiguration;
    private BooleanProperty mModifiedProperty = new SimpleBooleanProperty();

    public SourceConfigurationEditor()
    {
    }

    /**
     * Indicates if this editor's contents have been modified
     */
    public BooleanProperty modifiedProperty()
    {
        return mModifiedProperty;
    }

    /**
     * Disable (true) or enable (false) the controls on this editor
     */
    public abstract void disable(boolean disable);

    /**
     * Saves the (modified) contents in the editor.  If the editor is not modified, this is a no-op.
     */
    public abstract void save();

    /**
     * Sets or resets the source configuration in the editor
     * @param sourceConfiguration
     */
    public void setSourceConfiguration(T sourceConfiguration)
    {
        mSourceConfiguration = sourceConfiguration;
        disable(mSourceConfiguration == null);
    }

    /**
     * Retrieves the source configuration.
     * @return source config or null
     */
    public T getSourceConfiguration()
    {
        return mSourceConfiguration;
    }

}
