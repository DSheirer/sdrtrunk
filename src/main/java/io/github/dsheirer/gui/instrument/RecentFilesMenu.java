/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.gui.instrument;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.io.File;
import java.util.prefs.Preferences;

public class RecentFilesMenu extends Menu implements EventHandler<ActionEvent>
{
    private static final String PREFERENCE_RECENT_FILE = ".recent.file.";
    private Preferences mPreferences = Preferences.userNodeForPackage(RecentFilesMenu.class);
    private IFileSelectionListener mFileSelectionListener;
    private String mIdentifier;
    private int mSize;

    /**
     * Recently used files menu.  Uses java Preferences to store recently accessed files and provides access to those
     * files via child menu items to this menu.
     *
     * @param label to use for this menu.
     * @param uniqueIdentifier to use in addition to the class path of this class to uniquely identify recently accessed
     * files in a java Preferences data store.
     * @param size of the maximum number of recent files to present to the user
     */
    public RecentFilesMenu(String label, String uniqueIdentifier, int size)
    {
        super(label);
        mIdentifier = uniqueIdentifier;
        mSize = size;

        load();
    }

    /**
     * Registers the listener to receive file selection events when the user selects a recently used file.
     * @param listener to receive file selection events
     */
    public void setFileSelectionListener(IFileSelectionListener listener)
    {
        mFileSelectionListener = listener;
    }

    /**
     * Adds the file to the recently accessed files list.  If this file is already included in the recently files list,
     * it will be promoted to the first menu item and all other recently used files will be demoted.
     *
     * @param file to add to the recently used files list.
     */
    public void add(File file)
    {
        if(file != null)
        {
            String filePath = file.getAbsolutePath();

            int index = mSize - 1;

            //Check to see if this file is already in the recent files list
            for(int x = mSize - 1; x >= 0; x--)
            {
                String existingItem = mPreferences.get(getKey(x), null);

                if(existingItem != null && existingItem.equals(filePath))
                {
                    index = x;
                    break;
                }
            }

            //Shift all recent files down to make room for the newly added file at the top of the list
            while(index > 0)
            {
                String newerItem = mPreferences.get(getKey(index - 1), null);

                if(newerItem != null)
                {
                    mPreferences.put(getKey(index), newerItem);
                }

                index--;
            }

            mPreferences.put(getKey(0), file.getAbsolutePath());
        }

        load();
    }

    /**
     * Creates a unique key value for each numbered recently used file item in the preferences.
     *
     * @param index of recent file item.
     * @return unique key to use with the preferences instance.
     */
    private String getKey(int index)
    {
        return mIdentifier + PREFERENCE_RECENT_FILE + index;
    }

    /**
     * Clears and (re)loads all child recent file menu items.
     */
    private void load()
    {
        getItems().clear();

        for(int x = 0; x < mSize; x++)
        {
            String path = mPreferences.get(getKey(x), null);

            if(path != null)
            {
                File file = new File(path);

                if(file.isFile())
                {
                    MenuItem menuItem = new MenuItem(file.getName());
                    menuItem.setUserData(file);
                    menuItem.setOnAction(this);
                    getItems().add(menuItem);
                }
            }
        }

        if(getItems().size() > 0)
        {
            setDisable(false);
            getItems().add(new SeparatorMenuItem());

            MenuItem clearMenuItem = new MenuItem("Clear History");
            clearMenuItem.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    clearHistory();
                }
            });
            getItems().add(clearMenuItem);
        }
        else
        {
            setDisable(true);
        }
    }

    /**
     * Clears the recently used files history and removes all items from the preferences instance
     */
    private void clearHistory()
    {
        for(int x = 0; x < mSize; x++)
        {
            mPreferences.remove(getKey(x));
        }

        load();
    }

    /**
     * Event handler for all recent file child menu items.  Extracts the selected file stored as the user data object
     * in each of the menu items and notifies the listener that the recent file has been selected.
     *
     * @param event from a recent file menu item child of this menu.
     */
    @Override
    public void handle(ActionEvent event)
    {
        if(mFileSelectionListener != null)
        {
            if(event.getSource() instanceof MenuItem)
            {
                Object fileObject = ((MenuItem)event.getSource()).getUserData();

                if(fileObject instanceof File)
                {
                    mFileSelectionListener.fileSelected((File)fileObject);
                }
            }
        }
    }

    /**
     * Listener interface for file selection events.
     */
    public interface IFileSelectionListener
    {
        void fileSelected(File file);
    }
}
