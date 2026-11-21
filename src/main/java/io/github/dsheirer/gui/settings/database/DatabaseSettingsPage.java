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

import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.database.SchemaStatus;
import io.github.dsheirer.database.ageoff.AgeOffAction;
import io.github.dsheirer.database.ageoff.AgeOffManager;
import io.github.dsheirer.database.ageoff.RetentionPeriod;
import io.github.dsheirer.database.backup.BackupInterval;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsRow;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.database.DatabaseSettings;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.controlsfx.control.ToggleSwitch;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Settings page for the call and event database selection, the age-off rules and the database backups.
 *
 * Applying a different database asks whether to transfer the existing calls, and when the target schema already holds
 * sdrtrunk calls, whether to append to it, clear it, or create a separately named schema.  The database selection is
 * applied with the Apply button; the age-off and backup settings apply as they change.
 */
public class DatabaseSettingsPage extends SettingsPage
{
    private static final int MB_PER_GB = 1024;
    private static final String ERROR_STYLE = "-fx-text-fill: #d9534f;";

    private final DatabaseSettings mDatabaseSettings;
    private final DatabaseManager mDatabaseManager;
    private final Listener<DatabaseEvent> mDatabaseListener = event -> Platform.runLater(this::updateStatus);

    private final Label mStatusLabel = new Label();
    private final ComboBox<DatabaseType> mTypeComboBox = new ComboBox<>();
    private final TextField mHostField = new TextField();
    private final TextField mPortField = new TextField();
    private final TextField mDatabaseField = new TextField();
    private final TextField mSchemaField = new TextField();
    private final TextField mUserField = new TextField();
    private final PasswordField mPasswordField = new PasswordField();
    private final Label mLocationLabel = new Label();
    private final Label mResultLabel = new Label();
    private final Button mTestButton = new Button("Test Connection");
    private final Button mApplyButton = new Button("Apply");
    private final Button mResetButton = new Button("Reset");
    private final ProgressIndicator mProgress = new ProgressIndicator();
    private SettingsRow mHostRow;
    private SettingsRow mPortRow;
    private SettingsRow mDatabaseRow;
    private SettingsRow mUserRow;
    private SettingsRow mPasswordRow;

    private final ToggleSwitch mAgeRuleSwitch = new ToggleSwitch();
    private final Spinner<Integer> mAgeDaysSpinner = new Spinner<>(1, 36500, DatabaseSettings.DEFAULT_AGE_DAYS);
    private final ComboBox<AgeOffAction> mAgeActionComboBox = new ComboBox<>();
    private final ToggleSwitch mSizeRuleSwitch = new ToggleSwitch();
    private final Spinner<Integer> mSizeGBSpinner = new Spinner<>(1, 100_000, DatabaseSettings.DEFAULT_SIZE_LIMIT_MB / MB_PER_GB);
    private final ComboBox<AgeOffAction> mSizeActionComboBox = new ComboBox<>();
    private final ComboBox<RetentionPeriod> mRetentionComboBox = new ComboBox<>();
    private final ToggleSwitch mEventRuleSwitch = new ToggleSwitch();
    private final Spinner<Integer> mEventDaysSpinner = new Spinner<>(1, 36500, DatabaseSettings.DEFAULT_EVENT_AGE_DAYS);
    private final ToggleSwitch mStreamActivityRuleSwitch = new ToggleSwitch();
    private final Spinner<Integer> mStreamActivityDaysSpinner = new Spinner<>(1, 36500,
        DatabaseSettings.DEFAULT_STREAM_ACTIVITY_AGE_DAYS);
    private final Button mRunAgeOffButton = new Button("Run Age-Off Now");
    private final Label mAgeOffResultLabel = new Label();
    private final DatabaseBackupPane mBackupPane;

    /**
     * Constructs an instance
     * @param settings for the database settings
     * @param databaseManager to test and switch databases, or null when there isn't a database (playlist editor)
     */
    public DatabaseSettingsPage(Settings settings, DatabaseManager databaseManager)
    {
        super(SettingsPageType.DATABASE, settings);
        mDatabaseSettings = settings.getDatabaseSettings();
        mDatabaseManager = databaseManager;
        mBackupPane = new DatabaseBackupPane(mDatabaseSettings, databaseManager);

        createDatabaseSection();
        createAgeOffSection();
        createBackupSection();

        loadConfiguration(mDatabaseSettings.getDatabaseConfiguration());
        updateStatus();

        if(mDatabaseManager != null)
        {
            mDatabaseManager.addListener(mDatabaseListener);
        }
    }

    /**
     * Deregisters from the database manager
     */
    public void dispose()
    {
        mBackupPane.dispose();

        if(mDatabaseManager != null)
        {
            mDatabaseManager.removeListener(mDatabaseListener);
        }
    }

    private void createDatabaseSection()
    {
        SettingsSection section = addSection("Database");
        section.addHelp("sdrtrunk stores a record of each call and decode event in this database.  The built-in " +
            "database needs no setup.  You can use a MariaDB or PostgreSQL server instead; sdrtrunk creates the " +
            "schema and tables automatically.  The user needs permission to create schemas and tables.");
        mStatusLabel.setWrapText(true);
        section.addContent(mStatusLabel);

        mTypeComboBox.getItems().addAll(DatabaseType.values());
        mTypeComboBox.setOnAction(event -> {
            DatabaseType type = mTypeComboBox.getValue();

            if(type != null)
            {
                DatabaseConfiguration stored = mDatabaseSettings.getStoredConfiguration(type);
                mPortField.setText(type.isNetwork() ? String.valueOf(stored.port()) : "");
                updateFieldVisibility();
            }
        });

        mPortField.setPrefColumnCount(6);
        mHostField.setPrefColumnCount(24);
        mDatabaseField.setPrefColumnCount(24);
        mUserField.setPrefColumnCount(24);
        mPasswordField.setPrefColumnCount(24);
        mSchemaField.setPrefColumnCount(24);
        mSchemaField.setTooltip(new Tooltip("Lower case letters, digits and underscores, starting with a letter"));
        mPasswordField.setTooltip(new Tooltip("Note: the password is stored in plain text in the sdrtrunk settings"));
        mDatabaseField.setTooltip(new Tooltip("PostgreSQL database that holds the sdrtrunk schema"));

        section.addRow("Database type", mTypeComboBox);
        mHostRow = section.addRow("Host", mHostField);
        mPortRow = section.addRow("Port", mPortField);
        mDatabaseRow = section.addRow("Database name", mDatabaseField)
            .help("PostgreSQL database that holds the sdrtrunk schema.");
        mUserRow = section.addRow("User", mUserField);
        mPasswordRow = section.addRow("Password", mPasswordField)
            .help("The password is stored in plain text in the sdrtrunk settings.");
        section.addRow("Schema", mSchemaField)
            .help("Lower case letters, digits and underscores, starting with a letter.");
        mLocationLabel.setWrapText(true);
        mLocationLabel.getStyleClass().add("settings-help");
        section.addContent(mLocationLabel);

        mProgress.setVisible(false);
        mProgress.setPrefSize(18, 18);
        mTestButton.setOnAction(event -> testConnection());
        mApplyButton.setOnAction(event -> apply());
        mResetButton.setTooltip(new Tooltip("Discard edits and show the database that is in use"));
        mResetButton.setOnAction(event -> {
            loadConfiguration(mDatabaseManager != null ? mDatabaseManager.getConfiguration() :
                mDatabaseSettings.getDatabaseConfiguration());
            mResultLabel.setText("");
        });
        HBox buttons = new HBox(10, mTestButton, mApplyButton, mResetButton, mProgress);
        buttons.setAlignment(Pos.CENTER_LEFT);
        section.addContent(buttons);
        mResultLabel.setWrapText(true);
        section.addContent(mResultLabel);
    }

    private void createAgeOffSection()
    {
        SettingsSection section = addSection("Age-Off");
        section.addHelp("Age-off rules remove calls and their recording files, and decode events, automatically (a " +
            "minute after startup and then every 5 minutes).  Calls in progress are never removed.  Only recordings " +
            "listed in the call database count toward the size limit.  When the size rule is enabled, the status " +
            "bar recordings meter shows usage against the size limit.");

        mRetentionComboBox.getItems().addAll(RetentionPeriod.values());
        mRetentionComboBox.setValue(mDatabaseSettings.getDuplicateDoNotMonitorRetention());
        mRetentionComboBox.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mDatabaseSettings.setDuplicateDoNotMonitorRetention(value);
                updateAgeOffControls();
            }
        });
        section.addRow("Delete duplicate and Do Not Monitor calls after", mRetentionComboBox)
            .defaultValue(mRetentionComboBox.valueProperty(), RetentionPeriod.DEFAULT, mRetentionComboBox::setValue)
            .help("Deletes the call and its recording.  A Do Not Monitor call is kept when an alias of the call has " +
                "the Record option.");

        mAgeActionComboBox.getItems().addAll(AgeOffAction.values());
        mSizeActionComboBox.getItems().addAll(AgeOffAction.values());

        for(Spinner<Integer> spinner: java.util.List.of(mAgeDaysSpinner, mSizeGBSpinner, mEventDaysSpinner,
            mStreamActivityDaysSpinner))
        {
            spinner.setEditable(true);
            spinner.setPrefWidth(100);
        }

        mAgeRuleSwitch.setSelected(mDatabaseSettings.isAgeRuleEnabled());
        mAgeDaysSpinner.getValueFactory().setValue(mDatabaseSettings.getAgeRuleDays());
        mAgeActionComboBox.setValue(mDatabaseSettings.getAgeRuleAction());
        mAgeRuleSwitch.selectedProperty().addListener((obs, old, selected) -> {
            mDatabaseSettings.setAgeRuleEnabled(selected);
            updateAgeOffControls();
        });
        mAgeDaysSpinner.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mDatabaseSettings.setAgeRuleDays(value);
            }
        });
        mAgeActionComboBox.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mDatabaseSettings.setAgeRuleAction(value);
            }
        });
        section.addRow("Remove calls older than", mAgeRuleSwitch, mAgeDaysSpinner, new Label("days"),
                mAgeActionComboBox)
            .defaultValue(mAgeRuleSwitch.selectedProperty(), false, mAgeRuleSwitch::setSelected)
            .defaultValue(mAgeDaysSpinner.valueProperty(), DatabaseSettings.DEFAULT_AGE_DAYS,
                mAgeDaysSpinner.getValueFactory()::setValue)
            .defaultValue(mAgeActionComboBox.valueProperty(), AgeOffAction.DELETE_CALL_AND_RECORDING,
                mAgeActionComboBox::setValue);

        mSizeRuleSwitch.setSelected(mDatabaseSettings.isSizeRuleEnabled());
        mSizeGBSpinner.getValueFactory().setValue(Math.max(1, mDatabaseSettings.getSizeRuleLimitMB() / MB_PER_GB));
        mSizeActionComboBox.setValue(mDatabaseSettings.getSizeRuleAction());
        mSizeRuleSwitch.selectedProperty().addListener((obs, old, selected) -> {
            mDatabaseSettings.setSizeRuleEnabled(selected);
            updateAgeOffControls();
        });
        mSizeGBSpinner.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mDatabaseSettings.setSizeRuleLimitMB(value * MB_PER_GB);
            }
        });
        mSizeActionComboBox.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mDatabaseSettings.setSizeRuleAction(value);
            }
        });
        section.addRow("Remove the oldest recordings when they exceed", mSizeRuleSwitch, mSizeGBSpinner,
                new Label("GB"), mSizeActionComboBox)
            .defaultValue(mSizeRuleSwitch.selectedProperty(), false, mSizeRuleSwitch::setSelected)
            .defaultValue(mSizeGBSpinner.valueProperty(), DatabaseSettings.DEFAULT_SIZE_LIMIT_MB / MB_PER_GB,
                mSizeGBSpinner.getValueFactory()::setValue)
            .defaultValue(mSizeActionComboBox.valueProperty(), AgeOffAction.DELETE_CALL_AND_RECORDING,
                mSizeActionComboBox::setValue);

        mEventRuleSwitch.setSelected(mDatabaseSettings.isEventRuleEnabled());
        mEventDaysSpinner.getValueFactory().setValue(mDatabaseSettings.getEventRuleDays());
        mEventRuleSwitch.selectedProperty().addListener((obs, old, selected) -> {
            mDatabaseSettings.setEventRuleEnabled(selected);
            updateAgeOffControls();
        });
        mEventDaysSpinner.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mDatabaseSettings.setEventRuleDays(value);
            }
        });
        section.addRow("Remove decode events older than", mEventRuleSwitch, mEventDaysSpinner, new Label("days"))
            .defaultValue(mEventRuleSwitch.selectedProperty(), true, mEventRuleSwitch::setSelected)
            .defaultValue(mEventDaysSpinner.valueProperty(), DatabaseSettings.DEFAULT_EVENT_AGE_DAYS,
                mEventDaysSpinner.getValueFactory()::setValue)
            .help("Decode events shown on the Events tab.");

        mStreamActivityRuleSwitch.setSelected(mDatabaseSettings.isStreamActivityRuleEnabled());
        mStreamActivityDaysSpinner.getValueFactory().setValue(mDatabaseSettings.getStreamActivityRuleDays());
        mStreamActivityRuleSwitch.selectedProperty().addListener((obs, old, selected) -> {
            mDatabaseSettings.setStreamActivityRuleEnabled(selected);
            updateAgeOffControls();
        });
        mStreamActivityDaysSpinner.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mDatabaseSettings.setStreamActivityRuleDays(value);
            }
        });
        section.addRow("Remove streaming activity older than", mStreamActivityRuleSwitch, mStreamActivityDaysSpinner,
                new Label("days"))
            .defaultValue(mStreamActivityRuleSwitch.selectedProperty(), true, mStreamActivityRuleSwitch::setSelected)
            .defaultValue(mStreamActivityDaysSpinner.valueProperty(), DatabaseSettings.DEFAULT_STREAM_ACTIVITY_AGE_DAYS,
                mStreamActivityDaysSpinner.getValueFactory()::setValue)
            .help("The calls sent to each stream, shown on the Streaming tab.  The calls and their recordings aren't " +
                "affected.");

        mRunAgeOffButton.setOnAction(event -> runAgeOff());
        mAgeOffResultLabel.setWrapText(true);
        HBox run = new HBox(10, mRunAgeOffButton, mAgeOffResultLabel);
        run.setAlignment(Pos.CENTER_LEFT);
        section.addContent(run);
        updateAgeOffControls();
    }

    private void createBackupSection()
    {
        SettingsSection section = addSection("Backups");
        section.addHelp("A backup holds the calls and events of the database (not the recording files).  Backups " +
            "can be restored into any database type.  Restoring replaces the calls and events in the database in " +
            "use, after making a 'pre-restore' backup of them, so a restore can be undone.  When there are more " +
            "backups than the number to keep, the oldest backups are deleted after each backup.");

        ComboBox<BackupInterval> interval = mBackupPane.getIntervalComboBox();
        section.addRow("Back up automatically", interval)
            .defaultValue(interval.valueProperty(), BackupInterval.DEFAULT, interval::setValue);
        Spinner<Integer> retain = mBackupPane.getRetainSpinner();
        section.addRow("Backups to keep", retain)
            .defaultValue(retain.valueProperty(), DatabaseSettings.DEFAULT_BACKUP_RETAIN,
                retain.getValueFactory()::setValue);

        String folder = mBackupPane.getBackupDirectory();
        section.addHelp("Backup folder: " + (folder != null ? folder : "not available"));
        section.addContent(mBackupPane);
    }

    private void updateAgeOffControls()
    {
        mAgeDaysSpinner.setDisable(!mAgeRuleSwitch.isSelected());
        mAgeActionComboBox.setDisable(!mAgeRuleSwitch.isSelected());
        mSizeGBSpinner.setDisable(!mSizeRuleSwitch.isSelected());
        mSizeActionComboBox.setDisable(!mSizeRuleSwitch.isSelected());
        mEventDaysSpinner.setDisable(!mEventRuleSwitch.isSelected());
        mStreamActivityDaysSpinner.setDisable(!mStreamActivityRuleSwitch.isSelected());
        RetentionPeriod retention = mRetentionComboBox.getValue();
        mRunAgeOffButton.setDisable(mDatabaseManager == null || ((retention == null || !retention.isEnabled()) &&
            !mAgeRuleSwitch.isSelected() && !mSizeRuleSwitch.isSelected() && !mEventRuleSwitch.isSelected() &&
            !mStreamActivityRuleSwitch.isSelected()));
    }

    private void runAgeOff()
    {
        if(mDatabaseManager == null)
        {
            return;
        }

        if(!confirm("Run Age-Off", "Apply the enabled age-off rules now?",
            "Calls, recording files, events and streaming activity that the rules select are deleted permanently."))
        {
            return;
        }

        mRunAgeOffButton.setDisable(true);
        mAgeOffResultLabel.setText("Running ...");
        mDatabaseManager.getAgeOffManager().runNow().whenComplete((result, error) -> Platform.runLater(() -> {
            updateAgeOffControls();

            if(error != null)
            {
                mAgeOffResultLabel.setText("Error: " + getMessage(error));
            }
            else
            {
                mAgeOffResultLabel.setText("Removed " + result.calls() + " calls, " + result.recordings() +
                    " recordings (" + (result.bytes() / AgeOffManager.BYTES_PER_MB) + " MB), " + result.events() +
                    " events and " + result.streamActivity() + " streaming rows");
            }
        }));
    }

    /**
     * Shows the configuration in the editor fields.
     */
    private void loadConfiguration(DatabaseConfiguration configuration)
    {
        DatabaseConfiguration network = configuration.type().isNetwork() ? configuration :
            mDatabaseSettings.getStoredConfiguration(DatabaseType.MARIADB);
        mTypeComboBox.setValue(configuration.type());
        mHostField.setText(network.host());
        mPortField.setText(configuration.type().isNetwork() ? String.valueOf(configuration.port()) : "");
        mDatabaseField.setText(network.database());
        mUserField.setText(network.user());
        mPasswordField.setText(network.password());
        mSchemaField.setText(configuration.schema());
        updateFieldVisibility();
    }

    private void updateFieldVisibility()
    {
        DatabaseType type = mTypeComboBox.getValue();
        boolean network = type != null && type.isNetwork();
        boolean database = type != null && type.requiresDatabaseName();

        for(SettingsRow row: new SettingsRow[]{mHostRow, mPortRow, mUserRow, mPasswordRow})
        {
            row.setVisible(network);
            row.setManaged(network);
        }

        mDatabaseRow.setVisible(database);
        mDatabaseRow.setManaged(database);

        if(!network && mDatabaseManager != null)
        {
            mLocationLabel.setText("Database files: " + mDatabaseManager.getDatabaseDirectory());
            mLocationLabel.setVisible(true);
            mLocationLabel.setManaged(true);
        }
        else
        {
            mLocationLabel.setVisible(false);
            mLocationLabel.setManaged(false);
        }
    }

    private void updateStatus()
    {
        if(mDatabaseManager == null)
        {
            mStatusLabel.setText("Call database is not available");
            return;
        }

        if(mDatabaseManager.isConnected())
        {
            mStatusLabel.setText("In use: " + mDatabaseManager.getConfiguration().getDescription());
            mStatusLabel.setStyle("");
        }
        else
        {
            mStatusLabel.setText("Not connected: " + mDatabaseManager.getConfiguration().getDescription() +
                (mDatabaseManager.getErrorMessage() != null ? " - " + mDatabaseManager.getErrorMessage() : "") +
                "\nCalls are still recorded and are stored when the database becomes available.");
            mStatusLabel.setStyle(ERROR_STYLE);
        }
    }

    /**
     * Creates a configuration from the editor fields.
     * @return configuration, or null if the fields are invalid (the problem is shown to the user)
     */
    private DatabaseConfiguration getEditedConfiguration()
    {
        DatabaseType type = mTypeComboBox.getValue();

        if(type == null)
        {
            showResult("Select a database", true);
            return null;
        }

        int port = 0;

        if(type.isNetwork())
        {
            try
            {
                port = Integer.parseInt(mPortField.getText().trim());
            }
            catch(NumberFormatException nfe)
            {
                showResult("Port must be a number", true);
                return null;
            }
        }

        DatabaseConfiguration configuration = new DatabaseConfiguration(type, mHostField.getText(), port,
            mDatabaseField.getText(), mSchemaField.getText(), mUserField.getText(), mPasswordField.getText());
        String problem = configuration.validate();

        if(problem != null)
        {
            showResult(problem, true);
            return null;
        }

        return configuration;
    }

    private void showResult(String text, boolean error)
    {
        mResultLabel.setText(text);
        mResultLabel.setStyle(error ? ERROR_STYLE : "");
    }

    private void setBusy(boolean busy)
    {
        mProgress.setVisible(busy);
        mTestButton.setDisable(busy);
        mApplyButton.setDisable(busy);
        mResetButton.setDisable(busy);
    }

    private void testConnection()
    {
        DatabaseConfiguration configuration = getEditedConfiguration();

        if(configuration == null || mDatabaseManager == null)
        {
            return;
        }

        setBusy(true);
        showResult("Connecting ...", false);
        mDatabaseManager.probe(configuration).whenComplete((result, error) -> Platform.runLater(() -> {
            setBusy(false);

            if(error != null)
            {
                showResult("Connection failed: " + getMessage(error), true);
            }
            else
            {
                showResult("Connection successful.  " + describe(configuration, result), false);
            }
        }));
    }

    private static String describe(DatabaseConfiguration configuration, DatabaseManager.ProbeResult result)
    {
        return switch(result.status())
        {
            case MISSING -> "Schema [" + configuration.schema() + "] doesn't exist and will be created.";
            case EMPTY -> "Schema [" + configuration.schema() + "] exists and the sdrtrunk tables will be created in it.";
            case SDRTRUNK -> "Schema [" + configuration.schema() + "] has sdrtrunk tables with " + result.callCount() +
                " calls and " + result.eventCount() + " events.";
        };
    }

    /**
     * Applies the edited database selection.
     */
    private void apply()
    {
        DatabaseConfiguration target = getEditedConfiguration();

        if(target == null || mDatabaseManager == null)
        {
            return;
        }

        DatabaseConfiguration current = mDatabaseManager.getConfiguration();

        if(target.equals(current) && mDatabaseManager.isConnected())
        {
            showResult("This database is already in use.", false);
            return;
        }

        setBusy(true);
        showResult("Connecting ...", false);

        mDatabaseManager.probe(target).whenComplete((result, error) -> Platform.runLater(() -> {
            if(error != null)
            {
                setBusy(false);
                showResult("Connection failed: " + getMessage(error), true);
                return;
            }

            //Same database and schema, for example a changed password: reconnect without moving any calls
            if(target.isSameLocation(current))
            {
                startSwitch(target, DatabaseManager.ExistingSchemaOption.APPEND, false);
                return;
            }

            chooseSchemaOption(target, result);
        }));
    }

    /**
     * Asks how to treat an existing sdrtrunk schema in the target database, then continues with the transfer question.
     */
    private void chooseSchemaOption(DatabaseConfiguration target, DatabaseManager.ProbeResult result)
    {
        if(result.status() != SchemaStatus.SDRTRUNK)
        {
            chooseTransfer(target, DatabaseManager.ExistingSchemaOption.APPEND);
            return;
        }

        ButtonType append = new ButtonType("Append", ButtonBar.ButtonData.YES);
        ButtonType clear = new ButtonType("Clear", ButtonBar.ButtonData.OTHER);
        ButtonType separate = new ButtonType("Create Separate Schema", ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", append, clear, separate, cancel);
        alert.initOwner(getWindow());
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle("Existing sdrtrunk Schema");
        alert.setHeaderText("Schema [" + target.schema() + "] already has sdrtrunk tables with " + result.callCount() +
            " calls and " + result.eventCount() + " events");
        alert.setContentText("Append: keep the existing calls and events and add new ones to them.  Playlists that " +
            "the schema doesn't have are copied to it, and the playlist in use replaces the schema's playlist of the " +
            "same name.\n\n" +
            "Clear: delete the existing calls, events and playlists and start with an empty schema.  Your playlists " +
            "are copied to it.  Recording files are not deleted.\n\n" +
            "Create Separate Schema: leave this schema as it is and use a new schema with a different name.");
        alert.getDialogPane().setMinWidth(560);

        Optional<ButtonType> choice = alert.showAndWait();

        if(choice.isEmpty() || choice.get() == cancel)
        {
            setBusy(false);
            showResult("Cancelled", false);
        }
        else if(choice.get() == append)
        {
            chooseTransfer(target, DatabaseManager.ExistingSchemaOption.APPEND);
        }
        else if(choice.get() == clear)
        {
            if(confirm("Clear Schema", "Delete all " + result.callCount() + " calls, " + result.eventCount() +
                " events and the playlists in schema [" + target.schema() + "]?", "This can't be undone."))
            {
                chooseTransfer(target, DatabaseManager.ExistingSchemaOption.CLEAR);
            }
            else
            {
                setBusy(false);
                showResult("Cancelled", false);
            }
        }
        else
        {
            chooseSeparateSchema(target, result.suggestedSchema());
        }
    }

    /**
     * Asks for the name of a separate schema and checks that it doesn't exist.
     */
    private void chooseSeparateSchema(DatabaseConfiguration target, String suggestion)
    {
        TextInputDialog dialog = new TextInputDialog(suggestion);
        dialog.initOwner(getWindow());
        dialog.setTitle("Create Separate Schema");
        dialog.setHeaderText("Name for the new schema");
        dialog.setContentText("Schema:");

        Optional<String> name = dialog.showAndWait();

        if(name.isEmpty())
        {
            setBusy(false);
            showResult("Cancelled", false);
            return;
        }

        String schema = name.get().trim().toLowerCase();

        if(!DatabaseConfiguration.isValidSchemaName(schema))
        {
            showError("Invalid Schema Name", "Schema names start with a letter and contain only lower case letters, " +
                "digits and underscores (max 63 characters).");
            chooseSeparateSchema(target, suggestion);
            return;
        }

        DatabaseConfiguration separate = target.withSchema(schema);

        mDatabaseManager.schemaExists(target, schema).whenComplete((exists, error) -> Platform.runLater(() -> {
            if(error != null)
            {
                setBusy(false);
                showResult("Error: " + getMessage(error), true);
            }
            else if(exists)
            {
                showError("Schema Exists", "Schema [" + schema + "] already exists.  Choose a different name.");
                chooseSeparateSchema(target, suggestion);
            }
            else
            {
                mSchemaField.setText(schema);
                chooseTransfer(separate, DatabaseManager.ExistingSchemaOption.APPEND);
            }
        }));
    }

    /**
     * Asks whether to transfer the calls and events from the current database, then switches.
     */
    private void chooseTransfer(DatabaseConfiguration target, DatabaseManager.ExistingSchemaOption option)
    {
        CompletableFuture<long[]> countFuture = CompletableFuture.supplyAsync(() -> {
            try
            {
                return mDatabaseManager.isConnected() ? new long[]{mDatabaseManager.getDatabase().getCallCount(),
                    mDatabaseManager.getDatabase().getEventCount()} : new long[]{0, 0};
            }
            catch(Exception e)
            {
                return new long[]{0, 0};
            }
        });

        countFuture.whenComplete((count, error) -> Platform.runLater(() -> {
            long calls = count != null ? count[0] : 0;
            long events = count != null ? count[1] : 0;

            if(calls <= 0 && events <= 0)
            {
                startSwitch(target, option, false);
                return;
            }

            ButtonType transfer = new ButtonType("Transfer", ButtonBar.ButtonData.YES);
            ButtonType skip = new ButtonType("Don't Transfer", ButtonBar.ButtonData.NO);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", transfer, skip, cancel);
            alert.initOwner(getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.setTitle("Transfer Calls and Events");
            alert.setHeaderText("Transfer your existing calls and events to the new database?");
            alert.setContentText("Copy the " + calls + " calls and " + events + " events in " +
                mDatabaseManager.getConfiguration().getDescription() + " to " + target.getDescription() +
                ".\n\nThe calls and events are copied; the current database isn't changed." +
                (option == DatabaseManager.ExistingSchemaOption.APPEND ?
                "  Calls and events that already exist in the new database are skipped." : "") +
                "\n\nYour playlists move to the new database either way, since sdrtrunk stores its playlists in " +
                "the database.");
            alert.getDialogPane().setMinWidth(560);

            Optional<ButtonType> choice = alert.showAndWait();

            if(choice.isEmpty() || choice.get() == cancel)
            {
                setBusy(false);
                showResult("Cancelled", false);
            }
            else
            {
                startSwitch(target, option, choice.get() == transfer);
            }
        }));
    }

    private void startSwitch(DatabaseConfiguration target, DatabaseManager.ExistingSchemaOption option,
                             boolean transfer)
    {
        setBusy(true);
        showResult("Switching ...", false);

        mDatabaseManager.switchDatabase(target, option, transfer,
                status -> Platform.runLater(() -> showResult(status, false)))
            .whenComplete((result, error) -> Platform.runLater(() -> {
                setBusy(false);

                if(error != null)
                {
                    showResult("Unable to switch databases: " + getMessage(error) + "\nThe current database is " +
                        "still in use.", true);
                }
                else
                {
                    loadConfiguration(target);
                    showResult("Now using " + target.getDescription() + (transfer ? ".  Transferred " +
                        result.transferred() + " calls" + (result.skipped() > 0 ? " (skipped " + result.skipped() +
                        " calls that already existed or were in progress)" : "") + " and " +
                        result.eventsTransferred() + " events" + (result.eventsSkipped() > 0 ? " (skipped " +
                        result.eventsSkipped() + " events that already existed)" : "") + "." : "."), false);
                }

                updateStatus();
            }));
    }

    private boolean confirm(String title, String header, String content)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(getWindow());
        alert.setTitle(title);
        alert.setHeaderText(header);
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    private void showError(String title, String content)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.initOwner(getWindow());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private Window getWindow()
    {
        return getScene() != null ? getScene().getWindow() : null;
    }

    private static String getMessage(Throwable error)
    {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
