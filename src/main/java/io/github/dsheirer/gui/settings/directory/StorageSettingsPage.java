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

package io.github.dsheirer.gui.settings.directory;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.directory.DirectorySettings;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.stage.DirectoryChooser;

/**
 * Settings for the folders where sdrtrunk stores its files, and the disk usage limits for event logs and recordings
 */
public class StorageSettingsPage extends SettingsPage
{
    private final DirectorySettings mDirectorySettings;
    private final List<Runnable> mPathRefreshers = new ArrayList<>();
    private Spinner<Integer> mRecordingSpinner;
    private Spinner<Integer> mEventLogSpinner;

    public StorageSettingsPage(Settings settings)
    {
        super(SettingsPageType.STORAGE, settings);
        mDirectorySettings = settings.getDirectorySettings();

        //Register to receive directory setting update notifications so we can update the path labels
        MyEventBus.getGlobalEventBus().register(this);

        SettingsSection application = addSection("Application Folders");
        application.addHelp("Folders that use their default location are inside the application root and move " +
            "with it.");
        addFolder(application, "Application Root", mDirectorySettings::getDirectoryApplicationRoot,
            mDirectorySettings::setDirectoryApplicationRoot, mDirectorySettings::resetDirectoryApplicationRoot,
            mDirectorySettings::getDefaultApplicationDirectory);
        addFolder(application, "Application Logs", mDirectorySettings::getDirectoryApplicationLog,
            mDirectorySettings::setDirectoryApplicationLogs, mDirectorySettings::resetDirectoryApplicationLogs,
            mDirectorySettings::getDefaultApplicationLogsDirectory);
        addFolder(application, "Configuration", mDirectorySettings::getDirectoryConfiguration,
            mDirectorySettings::setDirectoryConfiguration, mDirectorySettings::resetDirectoryConfiguration,
            mDirectorySettings::getDefaultConfigurationDirectory);
        addFolder(application, "Playlists", mDirectorySettings::getDirectoryPlaylist,
            mDirectorySettings::setDirectoryPlaylist, mDirectorySettings::resetDirectoryPlaylist,
            mDirectorySettings::getDefaultPlaylistDirectory);
        addFolder(application, "JMBE Libraries", mDirectorySettings::getDirectoryJmbe,
            mDirectorySettings::setDirectoryJmbe, mDirectorySettings::resetDirectoryJmbe,
            mDirectorySettings::getDefaultJmbeDirectory);

        SettingsSection output = addSection("Output Folders");
        addFolder(output, "Event Logs", mDirectorySettings::getDirectoryEventLog,
            mDirectorySettings::setDirectoryEventLogs, mDirectorySettings::resetDirectoryEventLogs,
            mDirectorySettings::getDefaultEventLogsDirectory);
        addFolder(output, "Recordings", mDirectorySettings::getDirectoryRecording,
            mDirectorySettings::setDirectoryRecording, mDirectorySettings::resetDirectoryRecording,
            mDirectorySettings::getDefaultRecordingDirectory);
        addFolder(output, "Screen Captures", mDirectorySettings::getDirectoryScreenCapture,
            mDirectorySettings::setDirectoryScreenCapture, mDirectorySettings::resetDirectoryScreenCapture,
            mDirectorySettings::getDefaultScreenCaptureDirectory);
        addFolder(output, "Streaming", mDirectorySettings::getDirectoryStreaming,
            mDirectorySettings::setDirectoryStreaming, mDirectorySettings::resetDirectoryStreaming,
            mDirectorySettings::getDefaultStreamingDirectory);

        SettingsSection usage = addSection("Disk Usage Monitoring");
        usage.addHelp("Maximum size thresholds for the status bar's disk usage monitors.");
        usage.addRow("Event logs (MB)", getEventLogSpinner())
            .defaultValue(getEventLogSpinner().valueProperty(), DirectorySettings.DEFAULT_USAGE_THRESHOLD_EVENT_LOGS_MB,
                value -> getEventLogSpinner().getValueFactory().setValue(value));
        usage.addRow("Recordings (MB)", getRecordingSpinner())
            .defaultValue(getRecordingSpinner().valueProperty(), DirectorySettings.DEFAULT_USAGE_THRESHOLD_RECORDINGS_MB,
                value -> getRecordingSpinner().getValueFactory().setValue(value));
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    /**
     * Adds a row for a folder with buttons to change it and reset it to the default, and the current path below.
     * @param section to add the row to
     * @param name of the folder
     * @param getter for the current path
     * @param setter for a newly selected path
     * @param reset to restore the default path
     * @param defaultPath for the folder
     */
    private void addFolder(SettingsSection section, String name, Supplier<Path> getter, Consumer<Path> setter,
                           Runnable reset, Supplier<Path> defaultPath)
    {
        Label pathLabel = new Label(getter.get().toString());
        pathLabel.getStyleClass().addAll("settings-help", "settings-value");
        pathLabel.setWrapText(true);
        BooleanProperty changed = new SimpleBooleanProperty(!getter.get().equals(defaultPath.get()));
        mPathRefreshers.add(() -> {
            pathLabel.setText(getter.get().toString());
            changed.set(!getter.get().equals(defaultPath.get()));
        });

        Button changeButton = new Button("Change...");
        changeButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select " + name + " Folder");
            File current = getter.get().toFile();

            if(current.isDirectory())
            {
                directoryChooser.setInitialDirectory(current);
            }

            File selected = directoryChooser.showDialog(changeButton.getScene().getWindow());

            if(selected != null)
            {
                setter.accept(selected.toPath());
            }
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event -> reset.run());

        section.addRow(name, changeButton, resetButton).content(pathLabel).track(changed, reset);
    }

    /**
     * Recording directory maximum size threshold spinner
     * @return spinner
     */
    private Spinner<Integer> getRecordingSpinner()
    {
        if(mRecordingSpinner == null)
        {
            mRecordingSpinner = new Spinner<>(100, Integer.MAX_VALUE, mDirectorySettings.getDirectoryMaxUsageRecordings(), 100);
            mRecordingSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mDirectorySettings
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
            mEventLogSpinner = new Spinner<>(100, Integer.MAX_VALUE, mDirectorySettings.getDirectoryMaxUsageEventLogs(), 100);
            mEventLogSpinner.setEditable(true);
            mEventLogSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mDirectorySettings
                    .setDirectoryMaxUsageEventLogs(newValue));
        }

        return mEventLogSpinner;
    }

    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType == SettingsType.DIRECTORY)
        {
            mPathRefreshers.forEach(Runnable::run);
        }
    }
}
