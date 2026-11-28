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
package io.github.dsheirer.gui.settings.database;

import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.backup.BackupInfo;
import io.github.dsheirer.database.backup.BackupInterval;
import io.github.dsheirer.database.backup.BackupManager;
import io.github.dsheirer.settings.database.DatabaseSettings;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Window;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Database backups: the list of backups with back up now, restore and delete actions, and the automatic backup
 * interval and number of backups to keep controls that the database settings page shows as rows.
 */
public class DatabaseBackupPane extends VBox
{
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("0.0");
    private final DatabaseSettings mSettings;
    private final DatabaseManager mDatabaseManager;
    private final BackupManager mBackupManager;
    private final ComboBox<BackupInterval> mIntervalComboBox = new ComboBox<>();
    private final Spinner<Integer> mRetainSpinner = new Spinner<>(1, DatabaseSettings.MAX_BACKUP_RETAIN,
        DatabaseSettings.DEFAULT_BACKUP_RETAIN);
    private final ListView<BackupInfo> mBackupList = new ListView<>();
    private final Button mBackupButton = new Button("Back Up Now");
    private final Button mRestoreButton = new Button("Restore Selected Backup...");
    private final Button mDeleteButton = new Button("Delete Selected Backup");
    private final Button mRefreshButton = new Button("Refresh");
    private final ProgressIndicator mProgress = new ProgressIndicator();
    private final Label mResultLabel = new Label();
    private final Listener<BackupInfo> mBackupListener = backup -> Platform.runLater(this::refresh);
    private boolean mBusy;

    /**
     * Constructs an instance
     * @param settings for the backup settings
     * @param databaseManager for the backups, or null when the database isn't available
     */
    public DatabaseBackupPane(DatabaseSettings settings, DatabaseManager databaseManager)
    {
        super(8);
        mSettings = settings;
        mDatabaseManager = databaseManager;
        mBackupManager = databaseManager != null ? databaseManager.getBackupManager() : null;

        mIntervalComboBox.getItems().addAll(BackupInterval.values());
        mIntervalComboBox.setValue(mSettings.getBackupInterval());
        mIntervalComboBox.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mSettings.setBackupInterval(value);
            }
        });
        mRetainSpinner.setEditable(true);
        mRetainSpinner.setPrefWidth(90);
        mRetainSpinner.getValueFactory().setValue(mSettings.getBackupRetain());
        mRetainSpinner.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mSettings.setBackupRetain(value);
            }
        });

        mBackupList.setPrefHeight(180);
        mBackupList.setPlaceholder(new Label("No backups"));
        mBackupList.setCellFactory(list -> new BackupCell());
        mBackupList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> updateButtons());

        mProgress.setPrefSize(18, 18);
        mProgress.setVisible(false);
        mResultLabel.setWrapText(true);
        mBackupButton.setOnAction(event -> backupNow());
        mRestoreButton.setOnAction(event -> restore());
        mDeleteButton.setOnAction(event -> delete());
        mRefreshButton.setOnAction(event -> refresh());
        HBox buttons = new HBox(10, mBackupButton, mRestoreButton, mDeleteButton, mRefreshButton, mProgress);
        buttons.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(mBackupList, buttons, mResultLabel);

        if(mBackupManager != null)
        {
            mBackupManager.addBackupListener(mBackupListener);
        }

        updateButtons();
        refresh();
    }

    /**
     * Automatic backup interval selector
     */
    public ComboBox<BackupInterval> getIntervalComboBox()
    {
        return mIntervalComboBox;
    }

    /**
     * Number of backups to keep
     */
    public Spinner<Integer> getRetainSpinner()
    {
        return mRetainSpinner;
    }

    /**
     * Folder that holds the backup files, or null when the database isn't available
     */
    public String getBackupDirectory()
    {
        return mBackupManager != null ? mBackupManager.getBackupDirectory().toString() : null;
    }

    /**
     * Deregisters from the backup manager
     */
    public void dispose()
    {
        if(mBackupManager != null)
        {
            mBackupManager.removeBackupListener(mBackupListener);
        }
    }

    /**
     * Text for a backup in the list
     */
    static String describe(BackupInfo backup)
    {
        StringBuilder sb = new StringBuilder();

        synchronized(TIME_FORMAT)
        {
            sb.append(TIME_FORMAT.format(new Date(backup.created())));
        }

        if(backup.isPreRestore())
        {
            sb.append("  (before restore)");
        }

        if(backup.isValid())
        {
            sb.append("   ").append(backup.metadata().calls()).append(" calls, ").append(backup.metadata().events())
                .append(" events");

            if(backup.metadata().hasPlaylists())
            {
                sb.append(", ").append(backup.metadata().playlists()).append(" playlists");
            }

            sb.append("   ");
        }
        else
        {
            sb.append("   unreadable backup file   ");
        }

        sb.append(backup.size() < 1024 * 1024 ? Math.max(1, backup.size() / 1024) + " KB" :
            SIZE_FORMAT.format(backup.size() / (1024.0 * 1024.0)) + " MB");

        if(backup.isValid() && backup.metadata().source() != null && !backup.metadata().source().isEmpty())
        {
            sb.append("   from ").append(backup.metadata().source());
        }

        return sb.toString();
    }

    private static class BackupCell extends ListCell<BackupInfo>
    {
        @Override
        protected void updateItem(BackupInfo item, boolean empty)
        {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : describe(item));
        }
    }

    private void setBusy(boolean busy)
    {
        mBusy = busy;
        mProgress.setVisible(busy);
        updateButtons();
    }

    private void updateButtons()
    {
        boolean available = mBackupManager != null && !mBusy;
        BackupInfo selected = mBackupList.getSelectionModel().getSelectedItem();
        mBackupButton.setDisable(!available);
        mRefreshButton.setDisable(!available);
        mRestoreButton.setDisable(!available || selected == null || !selected.isValid());
        mDeleteButton.setDisable(!available || selected == null);
    }

    /**
     * Reloads the list of backups
     */
    void refresh()
    {
        if(mBackupManager == null)
        {
            return;
        }

        BackupInfo selected = mBackupList.getSelectionModel().getSelectedItem();
        mBackupManager.listBackups().whenComplete((backups, error) -> Platform.runLater(() -> {
            if(error != null)
            {
                mResultLabel.setText("Unable to list backups: " + getMessage(error));
                return;
            }

            mBackupList.getItems().setAll(backups);

            if(selected != null)
            {
                for(BackupInfo backup: backups)
                {
                    if(backup.path().equals(selected.path()))
                    {
                        mBackupList.getSelectionModel().select(backup);
                    }
                }
            }

            updateButtons();
        }));
    }

    private void backupNow()
    {
        setBusy(true);
        mResultLabel.setText("Backing up ...");
        mBackupManager.backupNow().whenComplete((backup, error) -> Platform.runLater(() -> {
            setBusy(false);
            mResultLabel.setText(error != null ? "Backup failed: " + getMessage(error) :
                "Backup created: " + backup.path().getFileName() + " (" + backup.metadata().calls() + " calls, " +
                    backup.metadata().events() + " events, " + backup.metadata().playlists() + " playlists)");
            refresh();
        }));
    }

    private void restore()
    {
        BackupInfo backup = mBackupList.getSelectionModel().getSelectedItem();

        if(backup == null || !backup.isValid())
        {
            return;
        }

        String database = mDatabaseManager.getConfiguration().getDescription();

        if(!confirm("Restore Backup", "Restore the backup from " + describeTime(backup) + "?",
            (backup.metadata().hasPlaylists() ?
                "All calls, events and playlists in " + database + " are replaced with the " +
                    backup.metadata().calls() + " calls, " + backup.metadata().events() + " events and " +
                    backup.metadata().playlists() + " playlists in the backup.  The running channels stop and the " +
                    "playlist is reloaded from the backup.\n\n" :
                "All calls and events in " + database + " are replaced with the " + backup.metadata().calls() +
                    " calls and " + backup.metadata().events() + " events in the backup.  This backup has no " +
                    "playlists, so the playlists aren't changed.\n\n") +
                "The current contents are backed up first, so you can undo the restore by restoring that " +
                "'before restore' backup.\n\n" +
                "Recording files aren't changed.  Restored calls whose recording was deleted since the backup can't " +
                "be played, and calls recorded after the backup are no longer listed (their files remain on disk)."))
        {
            return;
        }

        setBusy(true);
        mResultLabel.setText("Restoring ...");
        mBackupManager.restore(backup, status -> Platform.runLater(() -> mResultLabel.setText(status)))
            .whenComplete((result, error) -> Platform.runLater(() -> {
                setBusy(false);

                if(error != null)
                {
                    mResultLabel.setText("Restore failed: " + getMessage(error) + "\nThe database wasn't changed.");
                }
                else
                {
                    mResultLabel.setText("Restored " + result.calls() + " calls, " + result.events() + " events" +
                        (result.playlists() >= 0 ? " and " + result.playlists() + " playlists" : "") +
                        (result.missingRecordings() > 0 ? " (" + result.missingRecordings() + " calls have a " +
                            "recording that no longer exists)" : "") + "." + (result.preRestore() != null ?
                        "  The previous contents are in backup " + result.preRestore().path().getFileName() + "." :
                        ""));
                }

                refresh();
            }));
    }

    private void delete()
    {
        BackupInfo backup = mBackupList.getSelectionModel().getSelectedItem();

        if(backup == null || !confirm("Delete Backup", "Delete the backup from " + describeTime(backup) + "?",
            backup.path().toString()))
        {
            return;
        }

        setBusy(true);
        mBackupManager.delete(backup).whenComplete((result, error) -> Platform.runLater(() -> {
            setBusy(false);
            mResultLabel.setText(error != null ? "Unable to delete the backup: " + getMessage(error) :
                "Deleted " + backup.path().getFileName());
            refresh();
        }));
    }

    private static String describeTime(BackupInfo backup)
    {
        synchronized(TIME_FORMAT)
        {
            return TIME_FORMAT.format(new Date(backup.created()));
        }
    }

    private boolean confirm(String title, String header, String content)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL);
        Window window = getScene() != null ? getScene().getWindow() : null;

        if(window != null)
        {
            alert.initOwner(window);
        }

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setMinWidth(560);
        Optional<ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == ButtonType.OK;
    }

    private static String getMessage(Throwable error)
    {
        Throwable cause = error;

        while((cause instanceof CompletionException || cause instanceof IllegalStateException) &&
            cause.getCause() != null)
        {
            cause = cause.getCause();
        }

        return cause.getMessage() != null ? cause.getMessage() : cause.toString();
    }

    /**
     * Backups shown in the list (for testing)
     */
    List<BackupInfo> getBackups()
    {
        return mBackupList.getItems();
    }
}
