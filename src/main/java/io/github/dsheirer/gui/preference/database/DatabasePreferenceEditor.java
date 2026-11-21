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
package io.github.dsheirer.gui.preference.database;

import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.database.SchemaStatus;
import io.github.dsheirer.database.ageoff.AgeOffAction;
import io.github.dsheirer.database.ageoff.AgeOffManager;
import io.github.dsheirer.database.ageoff.RetentionPeriod;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.database.DatabasePreference;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Preference editor for the call database selection and the call audio age-off rules.
 *
 * Applying a different database asks whether to transfer the existing calls, and when the target schema already holds
 * sdrtrunk calls, whether to append to it, clear it, or create a separately named schema.
 */
public class DatabasePreferenceEditor extends VBox
{
    private static final int MB_PER_GB = 1024;

    private final DatabasePreference mPreference;
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
    private final Label mHostLabel = new Label("Host:");
    private final Label mPortLabel = new Label("Port:");
    private final Label mDatabaseLabel = new Label("Database:");
    private final Label mUserLabel = new Label("User:");
    private final Label mPasswordLabel = new Label("Password:");
    private final Label mResultLabel = new Label();
    private final Button mTestButton = new Button("Test Connection");
    private final Button mApplyButton = new Button("Apply");
    private final Button mResetButton = new Button("Reset");
    private final ProgressIndicator mProgress = new ProgressIndicator();

    private final CheckBox mAgeRuleCheckBox = new CheckBox("Remove calls older than");
    private final Spinner<Integer> mAgeDaysSpinner = new Spinner<>(1, 36500, 30);
    private final ComboBox<AgeOffAction> mAgeActionComboBox = new ComboBox<>();
    private final CheckBox mSizeRuleCheckBox = new CheckBox("Remove the oldest recordings when they exceed");
    private final Spinner<Integer> mSizeGBSpinner = new Spinner<>(1, 100_000, 10);
    private final ComboBox<AgeOffAction> mSizeActionComboBox = new ComboBox<>();
    private final ComboBox<RetentionPeriod> mRetentionComboBox = new ComboBox<>();
    private final Button mRunAgeOffButton = new Button("Run Age-Off Now");
    private final Label mAgeOffResultLabel = new Label();

    /**
     * Constructs an instance
     * @param userPreferences for the database preference
     * @param databaseManager to test and switch databases
     */
    public DatabasePreferenceEditor(UserPreferences userPreferences, DatabaseManager databaseManager)
    {
        mPreference = userPreferences.getDatabasePreference();
        mDatabaseManager = databaseManager;

        setPadding(new Insets(10));
        setSpacing(10);

        VBox content = new VBox(10, createDatabasePane(), new Separator(), createAgeOffPane());
        content.setPadding(new Insets(0, 10, 10, 0));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);

        loadConfiguration(mPreference.getDatabaseConfiguration());
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
        if(mDatabaseManager != null)
        {
            mDatabaseManager.removeListener(mDatabaseListener);
        }
    }

    private VBox createDatabasePane()
    {
        Label title = new Label("Call Database");
        title.setStyle("-fx-font-weight: bold;");
        Label description = new Label("sdrtrunk stores a record of each call in this database.  The built-in " +
            "database needs no setup.  You can use a MariaDB or PostgreSQL server instead; sdrtrunk creates the " +
            "schema and tables automatically.  The user needs permission to create schemas and tables.");
        description.setWrapText(true);
        mStatusLabel.setWrapText(true);

        mTypeComboBox.getItems().addAll(DatabaseType.values());
        mTypeComboBox.setOnAction(event -> {
            DatabaseType type = mTypeComboBox.getValue();

            if(type != null)
            {
                DatabaseConfiguration stored = mPreference.getStoredConfiguration(type);

                mPortField.setText(type.isNetwork() ? String.valueOf(stored.port()) : "");

                updateFieldVisibility();
            }
        });

        mPortField.setPrefColumnCount(6);
        mSchemaField.setTooltip(new Tooltip("Lower case letters, digits and underscores, starting with a letter"));
        mPasswordField.setTooltip(new Tooltip("Note: the password is stored in plain text in the sdrtrunk " +
            "preferences"));
        mDatabaseField.setTooltip(new Tooltip("PostgreSQL database that holds the sdrtrunk schema"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        int row = 0;
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setMaxWidth(420);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        grid.add(new Label("Type:"), 0, row);
        grid.add(mTypeComboBox, 1, row++);
        grid.add(mHostLabel, 0, row);
        grid.add(mHostField, 1, row++);
        grid.add(mPortLabel, 0, row);
        grid.add(mPortField, 1, row++);
        grid.add(mDatabaseLabel, 0, row);
        grid.add(mDatabaseField, 1, row++);
        grid.add(mUserLabel, 0, row);
        grid.add(mUserField, 1, row++);
        grid.add(mPasswordLabel, 0, row);
        grid.add(mPasswordField, 1, row++);
        grid.add(new Label("Schema:"), 0, row);
        grid.add(mSchemaField, 1, row++);
        grid.add(mLocationLabel, 1, row++);
        mLocationLabel.setWrapText(true);
        GridPane.setHgrow(mHostField, Priority.ALWAYS);
        mDatabaseLabel.setText("Database Name:");

        mProgress.setVisible(false);
        mProgress.setPrefSize(18, 18);
        mTestButton.setOnAction(event -> testConnection());
        mApplyButton.setOnAction(event -> apply());
        mResetButton.setTooltip(new Tooltip("Discard edits and show the database that is in use"));
        mResetButton.setOnAction(event -> {
            loadConfiguration(mDatabaseManager != null ? mDatabaseManager.getConfiguration() :
                mPreference.getDatabaseConfiguration());
            mResultLabel.setText("");
        });
        HBox buttons = new HBox(10, mTestButton, mApplyButton, mResetButton, mProgress);
        mResultLabel.setWrapText(true);

        return new VBox(8, title, description, mStatusLabel, grid, buttons, mResultLabel);
    }

    private VBox createAgeOffPane()
    {
        Label title = new Label("Call Audio Age-Off");
        title.setStyle("-fx-font-weight: bold;");
        Label description = new Label("Age-off rules remove calls and their recording files automatically " +
            "(a minute after startup and then every 5 minutes).  Calls in progress are never removed.  Only " +
            "recordings listed in the call database count toward the size limit.  When the size rule is enabled, " +
            "the status bar recordings meter shows usage against the size limit.");
        description.setWrapText(true);

        mAgeActionComboBox.getItems().addAll(AgeOffAction.values());
        mSizeActionComboBox.getItems().addAll(AgeOffAction.values());
        mAgeDaysSpinner.setEditable(true);
        mAgeDaysSpinner.setPrefWidth(100);
        mSizeGBSpinner.setEditable(true);
        mSizeGBSpinner.setPrefWidth(100);

        mAgeRuleCheckBox.setSelected(mPreference.isAgeRuleEnabled());
        mAgeDaysSpinner.getValueFactory().setValue(mPreference.getAgeRuleDays());
        mAgeActionComboBox.setValue(mPreference.getAgeRuleAction());
        mSizeRuleCheckBox.setSelected(mPreference.isSizeRuleEnabled());
        mSizeGBSpinner.getValueFactory().setValue(Math.max(1, mPreference.getSizeRuleLimitMB() / MB_PER_GB));
        mSizeActionComboBox.setValue(mPreference.getSizeRuleAction());

        mAgeRuleCheckBox.selectedProperty().addListener((obs, old, selected) -> {
            mPreference.setAgeRuleEnabled(selected);
            updateAgeOffControls();
        });
        mAgeDaysSpinner.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mPreference.setAgeRuleDays(value);
            }
        });
        mAgeActionComboBox.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mPreference.setAgeRuleAction(value);
            }
        });
        mSizeRuleCheckBox.selectedProperty().addListener((obs, old, selected) -> {
            mPreference.setSizeRuleEnabled(selected);
            updateAgeOffControls();
        });
        mSizeGBSpinner.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mPreference.setSizeRuleLimitMB(value * MB_PER_GB);
            }
        });
        mSizeActionComboBox.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mPreference.setSizeRuleAction(value);
            }
        });

        mRetentionComboBox.getItems().addAll(RetentionPeriod.values());
        mRetentionComboBox.setValue(mPreference.getDuplicateDoNotMonitorRetention());
        mRetentionComboBox.valueProperty().addListener((obs, old, value) -> {
            if(value != null)
            {
                mPreference.setDuplicateDoNotMonitorRetention(value);
                updateAgeOffControls();
            }
        });
        Label retentionLabel = new Label("Delete duplicate and Do Not Monitor calls after");
        Label retentionNote = new Label("Deletes the call and its recording.  A Do Not Monitor call is kept when " +
            "an alias of the call has the Record option.");
        retentionNote.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints ruleColumn = new ColumnConstraints();
        ruleColumn.setMinWidth(Region.USE_PREF_SIZE);
        grid.getColumnConstraints().add(ruleColumn);
        grid.add(retentionLabel, 0, 0);
        grid.add(mRetentionComboBox, 1, 0, 2, 1);
        grid.add(retentionNote, 0, 1, 3, 1);
        grid.add(mAgeRuleCheckBox, 0, 2);
        grid.add(new HBox(6, mAgeDaysSpinner, new Label("days")), 1, 2);
        grid.add(mAgeActionComboBox, 2, 2);
        grid.add(mSizeRuleCheckBox, 0, 3);
        grid.add(new HBox(6, mSizeGBSpinner, new Label("GB")), 1, 3);
        grid.add(mSizeActionComboBox, 2, 3);

        mRunAgeOffButton.setOnAction(event -> runAgeOff());
        mAgeOffResultLabel.setWrapText(true);
        updateAgeOffControls();

        return new VBox(8, title, description, grid, new HBox(10, mRunAgeOffButton, mAgeOffResultLabel));
    }

    private void updateAgeOffControls()
    {
        mAgeDaysSpinner.setDisable(!mAgeRuleCheckBox.isSelected());
        mAgeActionComboBox.setDisable(!mAgeRuleCheckBox.isSelected());
        mSizeGBSpinner.setDisable(!mSizeRuleCheckBox.isSelected());
        mSizeActionComboBox.setDisable(!mSizeRuleCheckBox.isSelected());
        RetentionPeriod retention = mRetentionComboBox.getValue();
        mRunAgeOffButton.setDisable(mDatabaseManager == null || ((retention == null || !retention.isEnabled()) &&
            !mAgeRuleCheckBox.isSelected() && !mSizeRuleCheckBox.isSelected()));
    }

    private void runAgeOff()
    {
        if(mDatabaseManager == null)
        {
            return;
        }

        if(!confirm("Run Age-Off", "Apply the enabled age-off rules now?",
            "Calls and recording files that the rules select are deleted permanently."))
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
                mAgeOffResultLabel.setText("Removed " + result.calls() + " calls and " + result.recordings() +
                    " recordings (" + (result.bytes() / AgeOffManager.BYTES_PER_MB) + " MB)");
            }
        }));
    }

    /**
     * Shows the configuration in the editor fields.
     */
    private void loadConfiguration(DatabaseConfiguration configuration)
    {
        DatabaseConfiguration network = configuration.type().isNetwork() ? configuration :
            mPreference.getStoredConfiguration(DatabaseType.MARIADB);
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

        for(javafx.scene.Node node: new javafx.scene.Node[]{mHostLabel, mHostField, mPortLabel, mPortField, mUserLabel,
            mUserField, mPasswordLabel, mPasswordField})
        {
            node.setVisible(network);
            node.setManaged(network);
        }

        mDatabaseLabel.setVisible(database);
        mDatabaseLabel.setManaged(database);
        mDatabaseField.setVisible(database);
        mDatabaseField.setManaged(database);

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
            mStatusLabel.setStyle("-fx-text-fill: #d9534f;");
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
        mResultLabel.setStyle(error ? "-fx-text-fill: #d9534f;" : "");
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
                " calls.";
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
            " calls");
        alert.setContentText("Append: keep the existing calls and add new calls to them.\n\n" +
            "Clear: delete the existing calls and start with an empty schema.  Recording files are not deleted.\n\n" +
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
            if(confirm("Clear Schema", "Delete all " + result.callCount() + " calls in schema [" + target.schema() +
                "]?", "This can't be undone."))
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
     * Asks whether to transfer the calls from the current database, then switches.
     */
    private void chooseTransfer(DatabaseConfiguration target, DatabaseManager.ExistingSchemaOption option)
    {
        CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(() -> {
            try
            {
                return mDatabaseManager.isConnected() ? mDatabaseManager.getDatabase().getCallCount() : 0L;
            }
            catch(Exception e)
            {
                return 0L;
            }
        });

        countFuture.whenComplete((count, error) -> Platform.runLater(() -> {
            long calls = count != null ? count : 0;

            if(calls <= 0)
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
            alert.setTitle("Transfer Calls");
            alert.setHeaderText("Transfer your existing calls to the new database?");
            alert.setContentText("Copy the " + calls + " calls in " + mDatabaseManager.getConfiguration()
                .getDescription() + " to " + target.getDescription() + ".\n\nThe calls are copied; the current " +
                "database isn't changed." + (option == DatabaseManager.ExistingSchemaOption.APPEND ?
                "  Calls that already exist in the new database are skipped." : ""));
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
                        result.transferred() + " calls" + (result.skipped() > 0 ? ", skipped " + result.skipped() +
                        " calls that already existed or were in progress" : "") + "." : "."), false);
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
