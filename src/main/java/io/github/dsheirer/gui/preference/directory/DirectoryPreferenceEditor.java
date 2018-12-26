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

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.directory.DirectoryPreference;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


/**
 * Preference settings for channel event view
 */
public class DirectoryPreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(DirectoryPreferenceEditor.class);

    private DirectoryPreference mDirectoryPreference;
    private GridPane mEditorPane;
    private Label mApplicationRootLabel;
    private Button mChangeApplicationRootButton;
    private Button mResetApplicationRootButton;
    private Label mApplicationRootPathLabel;
    private Label mPlaylistLabel;
    private Button mChangePlaylistButton;
    private Button mResetPlaylistButton;
    private Label mPlaylistPathLabel;
    private Label mRecordingLabel;
    private Button mChangeRecordingButton;
    private Button mResetRecordingButton;
    private Label mRecordingPathLabel;

    public DirectoryPreferenceEditor(UserPreferences userPreferences)
    {
        mDirectoryPreference = userPreferences.getDirectoryPreference();

        //Register to receive directory preference update notifications so we can update the path labels
        MyEventBus.getEventBus().register(this);

        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));

            int row = 0;

            Label itemLabel = new Label("Item");
            GridPane.setMargin(itemLabel, new Insets(0, 10, 0, 0));
            mEditorPane.add(itemLabel, 0, row);

            Label directoryLabel = new Label("Directory");
            GridPane.setMargin(directoryLabel, new Insets(0, 10, 0, 0));
            mEditorPane.add(directoryLabel, 1, row++);

            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 4, 1);

            GridPane.setMargin(getApplicationRootLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getApplicationRootLabel(), 0, row);

            GridPane.setMargin(getApplicationRootPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getApplicationRootPathLabel(), 1, row);

            GridPane.setMargin(getChangeApplicationRootButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeApplicationRootButton(), 2, row);

            GridPane.setMargin(getResetApplicationRootButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetApplicationRootButton(), 3, row++);

            GridPane.setMargin(getPlaylistLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getPlaylistLabel(), 0, row);

            GridPane.setMargin(getPlaylistPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getPlaylistPathLabel(), 1, row);

            GridPane.setMargin(getChangePlaylistButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangePlaylistButton(), 2, row);

            GridPane.setMargin(getResetPlaylistButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetPlaylistButton(), 3, row++);

            GridPane.setMargin(getRecordingLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getRecordingLabel(), 0, row);

            GridPane.setMargin(getRecordingPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getRecordingPathLabel(), 1, row);

            GridPane.setMargin(getChangeRecordingButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeRecordingButton(), 2, row);

            GridPane.setMargin(getResetRecordingButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetRecordingButton(), 3, row++);
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

    private Button getChangeApplicationRootButton()
    {
        if(mChangeApplicationRootButton == null)
        {
            mChangeApplicationRootButton = new Button("Change...");
            mChangeApplicationRootButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Application Root Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryApplicationRoot().toFile());
                    Stage stage = (Stage)getChangeApplicationRootButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryApplicationRoot(selected.toPath());
                    }
                }
            });
        }

        return mChangeApplicationRootButton;
    }

    private Button getResetApplicationRootButton()
    {
        if(mResetApplicationRootButton == null)
        {
            mResetApplicationRootButton = new Button("Reset");
            mResetApplicationRootButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryApplicationRoot();
                }
            });
        }

        return mResetApplicationRootButton;
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

    private Button getChangePlaylistButton()
    {
        if(mChangePlaylistButton == null)
        {
            mChangePlaylistButton = new Button("Change...");
            mChangePlaylistButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Playlist Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryPlaylist().toFile());
                    Stage stage = (Stage)getChangePlaylistButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryPlaylist(selected.toPath());
                    }
                }
            });
        }

        return mChangePlaylistButton;
    }

    private Button getResetPlaylistButton()
    {
        if(mResetPlaylistButton == null)
        {
            mResetPlaylistButton = new Button("Reset");
            mResetPlaylistButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryPlaylist();
                }
            });
        }

        return mResetPlaylistButton;
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

    private Button getChangeRecordingButton()
    {
        if(mChangeRecordingButton == null)
        {
            mChangeRecordingButton = new Button("Change...");
            mChangeRecordingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Recording Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryRecording().toFile());
                    Stage stage = (Stage)getChangeRecordingButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryRecording(selected.toPath());
                    }
                }
            });
        }

        return mChangeRecordingButton;
    }

    private Button getResetRecordingButton()
    {
        if(mResetRecordingButton == null)
        {
            mResetRecordingButton = new Button("Reset");
            mResetRecordingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryRecording();
                }
            });
        }

        return mResetRecordingButton;
    }

    private Label getRecordingPathLabel()
    {
        if(mRecordingPathLabel == null)
        {
            mRecordingPathLabel = new Label(mDirectoryPreference.getDirectoryRecording().toString());
        }

        return mRecordingPathLabel;
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType != null && preferenceType == PreferenceType.DIRECTORY)
        {
            getApplicationRootPathLabel().setText(mDirectoryPreference.getDirectoryApplicationRoot().toString());
            getPlaylistPathLabel().setText(mDirectoryPreference.getDirectoryPlaylist().toString());
            getRecordingPathLabel().setText(mDirectoryPreference.getDirectoryRecording().toString());
        }
    }
}
