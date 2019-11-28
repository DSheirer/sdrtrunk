/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.VBox;

public abstract class Editor<T> extends VBox
{
    private T mItem;
    private BooleanProperty mModifiedProperty = new SimpleBooleanProperty();

    public Editor()
    {
        setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Saves the editor's values to the edited item.
     */
    public abstract void save();

    /**
     * Prepare the editor for disposal
     */
    public abstract void dispose();

    /**
     * Sets the item to be edited by this editor
     * @param item to be edited
     */
    public void setItem(T item)
    {
        mItem = item;
    }

    public T getItem()
    {
        return mItem;
    }

    /**
     * Observable property that indicates if the contents of the editor have been modified (and need saved or reset).
     */
    public BooleanProperty modifiedProperty()
    {
        return mModifiedProperty;
    }
}
