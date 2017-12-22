/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.gui.editor;

import javax.swing.*;

/**
 * Generic editor with change detection, save and reset functions.
 *
 * This editor supports hierarchical nesting of sub-component editors where changes are detected
 * at the lowest editor in the tree and percolated up to the top-level editor for managing the
 * configuration set as a whole.
 *
 * To configure this editor as a collective, register each parent editor as a saveRequestListener
 * on each child editor.  If no save request listener is registered, the editor will simply invoke
 * save when a change is detected and the user opts to save the contents.
 *
 * The top-level editor can override the saveRequest() method to perform custom save operations
 * and validation.
 */
public abstract class Editor<T> extends JPanel
{
    private static final long serialVersionUID = 1L;

    protected T mItem;
    protected boolean mModified = false;
    protected Editor<?> mSaveRequestListener;

    public Editor()
    {
    }

    /**
     * Requests to save the contents of this editor.
     *
     * If a save request listener is registered, the listener is notified that a save is requested,
     * otherwise the save is request is processed by the instance.
     *
     * This allows for a save event to be percolated up the hierarchical set of editors so that
     * a top-level editor can invoke custom operations such as configuration validation.
     */
    protected void saveRequest()
    {
        if(mSaveRequestListener != null)
        {
            mSaveRequestListener.saveRequest();
        }
        else
        {
            save();
        }
    }

    /**
     * Registers the editor to be notified when this editor contents have been saved.
     */
    public void setSaveRequestListener(Editor<?> listener)
    {
        mSaveRequestListener = listener;
    }


    /**
     * Sets the object to be edited.  If the contents from the previous object have changed, this
     * prompts the user to save these changes.  A saveRequest() is issued and either this editor
     * or the parent editor will respond to the save request and invoke the save() method.
     */
    public void setItem(T item)
    {
        if(isModified())
        {
            int option = JOptionPane.showConfirmDialog(
                Editor.this,
                "This item has changed.  Do you want to save these changes?",
                "Save Changes?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if(option == JOptionPane.YES_OPTION)
            {
                saveRequest();
            }

            setModified(false);
        }

        mItem = item;
    }

    /**
     * Current editing item
     */
    public T getItem()
    {
        return mItem;
    }

    public boolean hasItem()
    {
        return mItem != null;
    }

    /**
     * Sets the contents modified flag
     */
    public void setModified(boolean modified)
    {
        mModified = modified;
    }

    /**
     * Indicates if this editor contents have been modified
     */
    public boolean isModified()
    {
        return mModified;
    }

    /**
     * Save the contents of the editor
     */
    public abstract void save();

    /**
     * Reset the contents of the editor without saving any changes
     */
    public void reset()
    {
        mModified = false;
        setItem(mItem);
    }
}
