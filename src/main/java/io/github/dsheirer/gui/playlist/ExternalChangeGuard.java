/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist;

import java.util.function.Function;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Notices when the item in an editor is changed somewhere else, for example through the web API, and keeps the
 * editor from silently overwriting that change.
 *
 * The editor records the item's version when it loads or saves the item.  When the model reports a change to the
 * item and its version differs: an editor without unsaved edits reloads the item; an editor with unsaved edits asks
 * whether to reload (discarding the edits) or keep the edits (and save over the other change).  Changes that leave
 * the version the same, such as a channel starting, and the editor's own save, are ignored.
 *
 * @param <T> type of the edited item
 */
public class ExternalChangeGuard<T>
{
    private final Editor<T> mEditor;
    private final Function<T,String> mVersion;
    private final String mItemLabel;
    private String mBaseline;
    private boolean mSaving;
    private boolean mPrompting;

    /**
     * Constructs an instance
     * @param editor to guard
     * @param version of an item: changes whenever the item's configuration changes
     * @param itemLabel for the prompt, for example "channel"
     */
    public ExternalChangeGuard(Editor<T> editor, Function<T,String> version, String itemLabel)
    {
        mEditor = editor;
        mVersion = version;
        mItemLabel = itemLabel;
    }

    /**
     * The editor loaded the item (or none).  Call at the end of setItem().
     */
    public void loaded(T item)
    {
        mBaseline = item != null ? safeVersion(item) : null;
    }

    /**
     * Runs the editor's save.  Changes reported while it runs are the editor's own.
     */
    public void save(Runnable save)
    {
        mSaving = true;

        try
        {
            save.run();
        }
        finally
        {
            mSaving = false;
            loaded(mEditor.getItem());
        }
    }

    /**
     * The model reports that the item changed.  May be called on any thread.
     */
    public void changed(T item)
    {
        if(!Platform.isFxApplicationThread())
        {
            Platform.runLater(() -> changed(item));
            return;
        }

        if(mSaving || mPrompting || item == null || item != mEditor.getItem())
        {
            return;
        }

        String version = safeVersion(item);

        if(version == null || version.equals(mBaseline))
        {
            return;
        }

        if(!mEditor.modifiedProperty().get())
        {
            mEditor.setItem(item);
            return;
        }

        mPrompting = true;

        try
        {
            ButtonType reload = new ButtonType("Reload", ButtonBar.ButtonData.YES);
            ButtonType keep = new ButtonType("Keep My Changes", ButtonBar.ButtonData.NO);
            Alert alert = new Alert(Alert.AlertType.WARNING, "This " + mItemLabel + " was changed somewhere else, " +
                "for example from the web interface, while you were editing it.\n\nReload it to see that change " +
                "(your unsaved changes are discarded), or keep your changes and save them over it.", reload, keep);
            alert.setTitle("Changed Elsewhere");
            alert.setHeaderText("The " + mItemLabel + " was changed elsewhere");

            if(mEditor.getScene() != null && mEditor.getScene().getWindow() != null)
            {
                alert.initOwner(mEditor.getScene().getWindow());
            }

            if(alert.showAndWait().filter(button -> button == reload).isPresent())
            {
                mEditor.setItem(item);
            }
            else
            {
                //The user chose to overwrite the other change on save
                mBaseline = safeVersion(item);
            }
        }
        finally
        {
            mPrompting = false;
        }
    }

    private String safeVersion(T item)
    {
        try
        {
            return mVersion.apply(item);
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
