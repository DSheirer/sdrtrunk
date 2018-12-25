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

package io.github.dsheirer.gui.preference.directory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.directory.DirectoryPreference;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;


/**
 * Preference settings for channel event view
 */
public class DirectoryPreferenceEditor extends HBox
{
    private DirectoryPreference mDirectoryPreference;
    private GridPane mEditorPane;
    private Label mApplicationRootLabel;
    private Button mApplicationRootButton;
    private Label mApplicationRootPathLabel;
    private Label mPlaylistLabel;
    private Button mPlaylistButton;
    private Label mPlaylistPathLabel;
    private Label mRecordingLabel;
    private Button mRecordingButton;
    private Label mRecordingPathLabel;

    public DirectoryPreferenceEditor(UserPreferences userPreferences)
    {
        mDirectoryPreference = userPreferences.getDirectoryPreference();

        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));
//            GridPane.setHalignment(getChannelizerLabel(), HPos.LEFT);

            int row = 0;

            Label itemLabel = new Label("Item");
            GridPane.setMargin(itemLabel, new Insets(0, 10, 0, 0));
            mEditorPane.add(itemLabel, 0, row);

            Label directoryLabel = new Label("Directory");
            GridPane.setMargin(directoryLabel, new Insets(0, 10, 0, 0));
            mEditorPane.add(directoryLabel, 1, row++);

            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 3, 1);

            GridPane.setMargin(getApplicationRootLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getApplicationRootLabel(), 0, row);

            GridPane.setMargin(getApplicationRootPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getApplicationRootPathLabel(), 1, row);

            GridPane.setMargin(getApplicationRootButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getApplicationRootButton(), 2, row++);

            GridPane.setMargin(getPlaylistLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getPlaylistLabel(), 0, row);

            GridPane.setMargin(getPlaylistPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getPlaylistPathLabel(), 1, row);

            GridPane.setMargin(getPlaylistButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getPlaylistButton(), 2, row++);

            GridPane.setMargin(getRecordingLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getRecordingLabel(), 0, row);

            GridPane.setMargin(getRecordingPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getRecordingPathLabel(), 1, row);

            GridPane.setMargin(getRecordingButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getRecordingButton(), 2, row++);
        }

        return mEditorPane;
    }

    private Label getApplicationRootLabel()
    {
        if(mApplicationRootLabel == null)
        {
            mApplicationRootLabel = new Label("Application Root");
        }

        return mApplicationRootLabel;
    }

    private Button getApplicationRootButton()
    {
        if(mApplicationRootButton == null)
        {
            mApplicationRootButton = new Button("Change...");
            //TODO: add action
        }

        return mApplicationRootButton;
    }

    private Label getApplicationRootPathLabel()
    {
        if(mApplicationRootPathLabel == null)
        {
            mApplicationRootPathLabel = new Label(mDirectoryPreference.getDirectoryApplicationRoot().toString());
        }

        return mApplicationRootPathLabel;
    }

    private Label getPlaylistLabel()
    {
        if(mPlaylistLabel == null)
        {
            mPlaylistLabel = new Label("Playlists");
        }

        return mPlaylistLabel;
    }

    private Button getPlaylistButton()
    {
        if(mPlaylistButton == null)
        {
            mPlaylistButton = new Button("Change...");
            //TODO: add action
        }

        return mPlaylistButton;
    }

    private Label getPlaylistPathLabel()
    {
        if(mPlaylistPathLabel == null)
        {
            mPlaylistPathLabel = new Label(mDirectoryPreference.getDirectoryPlaylist().toString());
        }

        return mPlaylistPathLabel;
    }

    private Label getRecordingLabel()
    {
        if(mRecordingLabel == null)
        {
            mRecordingLabel = new Label("Recordings");
        }

        return mRecordingLabel;
    }

    private Button getRecordingButton()
    {
        if(mRecordingButton == null)
        {
            mRecordingButton = new Button("Change...");
            //TODO: add action
        }

        return mRecordingButton;
    }

    private Label getRecordingPathLabel()
    {
        if(mRecordingPathLabel == null)
        {
            mRecordingPathLabel = new Label(mDirectoryPreference.getDirectoryRecording().toString());
        }

        return mRecordingPathLabel;
    }
}
