/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.directory;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.directory.DirectoryPreference;
import java.io.File;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    private Label mApplicationLogsLabel;
    private Button mChangeApplicationLogsButton;
    private Button mResetApplicationLogsButton;
    private Label mApplicationLogsPathLabel;

    private Label mConfigurationLabel;
    private Button mChangeConfigurationButton;
    private Button mResetConfigurationButton;
    private Label mConfigurationPathLabel;

    private Label mEventLogsLabel;
    private Button mChangeEventLogsButton;
    private Button mResetEventLogsButton;
    private Label mEventLogsPathLabel;

    private Label mJmbeLabel;
    private Button mChangeJmbeButton;
    private Button mResetJmbeButton;
    private Label mJmbePathLabel;

    private Label mPlaylistLabel;
    private Button mChangePlaylistButton;
    private Button mResetPlaylistButton;
    private Label mPlaylistPathLabel;

    private Label mRecordingLabel;
    private Button mChangeRecordingButton;
    private Button mResetRecordingButton;
    private Label mRecordingPathLabel;

    private Label mScreenCaptureLabel;
    private Button mChangeScreenCaptureButton;
    private Button mResetScreenCaptureButton;
    private Label mScreenCapturePathLabel;

    private Label mStreamingLabel;
    private Button mChangeStreamingButton;
    private Button mResetStreamingButton;
    private Label mStreamingPathLabel;

    private Spinner<Integer> mRecordingSpinner;
    private Spinner<Integer> mEventLogSpinner;

    public DirectoryPreferenceEditor(UserPreferences userPreferences)
    {
        mDirectoryPreference = userPreferences.getDirectoryPreference();

        //Register to receive directory preference update notifications so we can update the path labels
        MyEventBus.getGlobalEventBus().register(this);

        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
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


            GridPane.setMargin(getApplicationLogsLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getApplicationLogsLabel(), 0, row);

            GridPane.setMargin(getApplicationLogsPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getApplicationLogsPathLabel(), 1, row);

            GridPane.setMargin(getChangeApplicationLogsButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeApplicationLogsButton(), 2, row);

            GridPane.setMargin(getResetApplicationLogsButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetApplicationLogsButton(), 3, row++);


            GridPane.setMargin(getConfigurationLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getConfigurationLabel(), 0, row);

            GridPane.setMargin(getConfigurationPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getConfigurationPathLabel(), 1, row);

            GridPane.setMargin(getChangeConfigurationButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeConfigurationButton(), 2, row);

            GridPane.setMargin(getResetConfigurationButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetConfigurationButton(), 3, row++);


            GridPane.setMargin(getEventLogsLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getEventLogsLabel(), 0, row);

            GridPane.setMargin(getEventLogsPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getEventLogsPathLabel(), 1, row);

            GridPane.setMargin(getChangeEventLogsButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeEventLogsButton(), 2, row);

            GridPane.setMargin(getResetEventLogsButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetEventLogsButton(), 3, row++);


            GridPane.setMargin(getJmbeLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getJmbeLabel(), 0, row);

            GridPane.setMargin(getJmbePathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getJmbePathLabel(), 1, row);

            GridPane.setMargin(getChangeJmbeButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeJmbeButton(), 2, row);

            GridPane.setMargin(getResetJmbeButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetJmbeButton(), 3, row++);


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


            GridPane.setMargin(getScreenCaptureLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getScreenCaptureLabel(), 0, row);

            GridPane.setMargin(getScreenCapturePathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getScreenCapturePathLabel(), 1, row);

            GridPane.setMargin(getChangeScreenCaptureButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeScreenCaptureButton(), 2, row);

            GridPane.setMargin(getResetScreenCaptureButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetScreenCaptureButton(), 3, row++);


            GridPane.setMargin(getStreamingLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getStreamingLabel(), 0, row);

            GridPane.setMargin(getStreamingPathLabel(), new Insets(0, 10, 0, 0));
            mEditorPane.add(getStreamingPathLabel(), 1, row);

            GridPane.setMargin(getChangeStreamingButton(), new Insets(2, 10, 2, 0));
            mEditorPane.add(getChangeStreamingButton(), 2, row);

            GridPane.setMargin(getResetStreamingButton(), new Insets(2, 0, 2, 0));
            mEditorPane.add(getResetStreamingButton(), 3, row++);

            Label monitorLabel = new Label("File storage usage monitoring - maximum size thresholds (MB)");
            GridPane.setMargin(monitorLabel, new Insets(15, 0, 2, 0));
            mEditorPane.add(monitorLabel, 0, row++, 4, 1);
            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 4, 1);

            mEditorPane.add(new Label("Event Logs"), 0, row);
            GridPane.setMargin(getEventLogSpinner(), new Insets(2, 2, 2, 0));
            mEditorPane.add(getEventLogSpinner(), 1, row++);

            mEditorPane.add(new Label("Recordings"), 0, row);
            GridPane.setMargin(getRecordingSpinner(), new Insets(2, 2, 2, 0));
            mEditorPane.add(getRecordingSpinner(), 1, row);
        }

        return mEditorPane;
    }

    /**
     * Recording directory maximum size threshold spinner
     * @return spinner
     */
    private Spinner<Integer> getRecordingSpinner()
    {
        if(mRecordingSpinner == null)
        {
            mRecordingSpinner = new Spinner<>(100, Integer.MAX_VALUE, mDirectoryPreference.getDirectoryMaxUsageRecordings(), 100);
            mRecordingSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mDirectoryPreference
                    .setDirectoryMaxUsageRecordings(newValue));
        }

        return mRecordingSpinner;
    }

    /**
     * Event log directory maximum size threshold spinner
     * @return spinner
     */
    private Spinner<Integer> getEventLogSpinner()
    {
        if(mEventLogSpinner == null)
        {
            mEventLogSpinner = new Spinner<>(100, Integer.MAX_VALUE, mDirectoryPreference.getDirectoryMaxUsageEventLogs(), 100);
            mEventLogSpinner.setEditable(true);
            mEventLogSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mDirectoryPreference
                    .setDirectoryMaxUsageEventLogs(newValue));
        }

        return mEventLogSpinner;
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

    private Label getApplicationLogsLabel()
    {
        if(mApplicationLogsLabel == null)
        {
            mApplicationLogsLabel = new Label("Application Logs");
        }

        return mApplicationLogsLabel;
    }

    private Button getChangeApplicationLogsButton()
    {
        if(mChangeApplicationLogsButton == null)
        {
            mChangeApplicationLogsButton = new Button("Change...");
            mChangeApplicationLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Application Logs Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryApplicationLog().toFile());
                    Stage stage = (Stage)getChangeApplicationLogsButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryApplicationLogs(selected.toPath());
                    }
                }
            });
        }

        return mChangeApplicationLogsButton;
    }

    private Button getResetApplicationLogsButton()
    {
        if(mResetApplicationLogsButton == null)
        {
            mResetApplicationLogsButton = new Button("Reset");
            mResetApplicationLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryApplicationLogs();
                }
            });
        }

        return mResetApplicationLogsButton;
    }

    private Label getApplicationLogsPathLabel()
    {
        if(mApplicationLogsPathLabel == null)
        {
            mApplicationLogsPathLabel = new Label(mDirectoryPreference.getDirectoryApplicationLog().toString());
        }

        return mApplicationLogsPathLabel;
    }

    private Label getConfigurationLabel()
    {
        if(mConfigurationLabel == null)
        {
            mConfigurationLabel = new Label("Configuration");
        }

        return mConfigurationLabel;
    }

    private Button getChangeConfigurationButton()
    {
        if(mChangeConfigurationButton == null)
        {
            mChangeConfigurationButton = new Button("Change...");
            mChangeConfigurationButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Configuration Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryConfiguration().toFile());
                    Stage stage = (Stage)getChangeConfigurationButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryConfiguration(selected.toPath());
                    }
                }
            });
        }

        return mChangeConfigurationButton;
    }

    private Button getResetConfigurationButton()
    {
        if(mResetConfigurationButton == null)
        {
            mResetConfigurationButton = new Button("Reset");
            mResetConfigurationButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryConfiguration();
                }
            });
        }

        return mResetConfigurationButton;
    }

    private Label getConfigurationPathLabel()
    {
        if(mConfigurationPathLabel == null)
        {
            mConfigurationPathLabel = new Label(mDirectoryPreference.getDirectoryConfiguration().toString());
        }

        return mConfigurationPathLabel;
    }

    private Label getEventLogsLabel()
    {
        if(mEventLogsLabel == null)
        {
            mEventLogsLabel = new Label("Event Logs");
        }

        return mEventLogsLabel;
    }

    private Button getChangeEventLogsButton()
    {
        if(mChangeEventLogsButton == null)
        {
            mChangeEventLogsButton = new Button("Change...");
            mChangeEventLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Event Logs Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryEventLog().toFile());
                    Stage stage = (Stage)getChangeEventLogsButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryEventLogs(selected.toPath());
                    }
                }
            });
        }

        return mChangeEventLogsButton;
    }

    private Button getResetEventLogsButton()
    {
        if(mResetEventLogsButton == null)
        {
            mResetEventLogsButton = new Button("Reset");
            mResetEventLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryEventLogs();
                }
            });
        }

        return mResetEventLogsButton;
    }

    private Label getEventLogsPathLabel()
    {
        if(mEventLogsPathLabel == null)
        {
            mEventLogsPathLabel = new Label(mDirectoryPreference.getDirectoryEventLog().toString());
        }

        return mEventLogsPathLabel;
    }

    private Label getJmbeLabel()
    {
        if(mJmbeLabel == null)
        {
            mJmbeLabel = new Label("JMBE Libraries");
        }

        return mJmbeLabel;
    }

    private Button getChangeJmbeButton()
    {
        if(mChangeJmbeButton == null)
        {
            mChangeJmbeButton = new Button("Change...");
            mChangeJmbeButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select JMBE Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryJmbe().toFile());
                    Stage stage = (Stage)getChangeJmbeButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryJmbe(selected.toPath());
                    }
                }
            });
        }

        return mChangeJmbeButton;
    }

    private Button getResetJmbeButton()
    {
        if(mResetJmbeButton == null)
        {
            mResetJmbeButton = new Button("Reset");
            mResetJmbeButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryJmbe();
                }
            });
        }

        return mResetJmbeButton;
    }

    private Label getJmbePathLabel()
    {
        if(mJmbePathLabel == null)
        {
            mJmbePathLabel = new Label(mDirectoryPreference.getDirectoryJmbe().toString());
        }

        return mJmbePathLabel;
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

    private Label getScreenCaptureLabel()
    {
        if(mScreenCaptureLabel == null)
        {
            mScreenCaptureLabel = new Label("Screen Captures");
        }

        return mScreenCaptureLabel;
    }

    private Button getChangeScreenCaptureButton()
    {
        if(mChangeScreenCaptureButton == null)
        {
            mChangeScreenCaptureButton = new Button("Change...");
            mChangeScreenCaptureButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Screen Capture Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryScreenCapture().toFile());
                    Stage stage = (Stage)getChangeScreenCaptureButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryScreenCapture(selected.toPath());
                    }
                }
            });
        }

        return mChangeScreenCaptureButton;
    }

    private Button getResetScreenCaptureButton()
    {
        if(mResetScreenCaptureButton == null)
        {
            mResetScreenCaptureButton = new Button("Reset");
            mResetScreenCaptureButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryScreenCapture();
                }
            });
        }

        return mResetScreenCaptureButton;
    }

    private Label getScreenCapturePathLabel()
    {
        if(mScreenCapturePathLabel == null)
        {
            mScreenCapturePathLabel = new Label(mDirectoryPreference.getDirectoryScreenCapture().toString());
        }

        return mScreenCapturePathLabel;
    }

    private Label getStreamingLabel()
    {
        if(mStreamingLabel == null)
        {
            mStreamingLabel = new Label("Streaming");
        }

        return mStreamingLabel;
    }

    private Button getChangeStreamingButton()
    {
        if(mChangeStreamingButton == null)
        {
            mChangeStreamingButton = new Button("Change...");
            mChangeStreamingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Streaming Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryStreaming().toFile());
                    Stage stage = (Stage)getChangeStreamingButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryStreaming(selected.toPath());
                    }
                }
            });
        }

        return mChangeStreamingButton;
    }

    private Button getResetStreamingButton()
    {
        if(mResetStreamingButton == null)
        {
            mResetStreamingButton = new Button("Reset");
            mResetStreamingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryStreaming();
                }
            });
        }

        return mResetStreamingButton;
    }

    private Label getStreamingPathLabel()
    {
        if(mStreamingPathLabel == null)
        {
            mStreamingPathLabel = new Label(mDirectoryPreference.getDirectoryStreaming().toString());
        }

        return mStreamingPathLabel;
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType != null && preferenceType == PreferenceType.DIRECTORY)
        {
            getApplicationRootPathLabel().setText(mDirectoryPreference.getDirectoryApplicationRoot().toString());
            getApplicationLogsPathLabel().setText(mDirectoryPreference.getDirectoryApplicationLog().toString());
            getEventLogsPathLabel().setText(mDirectoryPreference.getDirectoryEventLog().toString());
            getPlaylistPathLabel().setText(mDirectoryPreference.getDirectoryPlaylist().toString());
            getRecordingPathLabel().setText(mDirectoryPreference.getDirectoryRecording().toString());
            getScreenCapturePathLabel().setText(mDirectoryPreference.getDirectoryScreenCapture().toString());
            getStreamingPathLabel().setText(mDirectoryPreference.getDirectoryStreaming().toString());
        }
    }
}
