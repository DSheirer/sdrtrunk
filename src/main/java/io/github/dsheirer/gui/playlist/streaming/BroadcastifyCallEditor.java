/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallBroadcaster;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.controlsfx.control.ToggleSwitch;

/**
 * Broadcastify calls API configuration editor
 */
public class BroadcastifyCallEditor extends AbstractBroadcastEditor<BroadcastifyCallConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastifyCallEditor.class);
    private IntegerTextField mSystemIdTextField;
    private Button mTestButton;
    private IntegerTextField mMaxAgeTextField;
    private TextField mApiKeyTextField;
    private TextField mHostTextField;
    private GridPane mEditorPane;
    private ToggleSwitch mTestEnabledToggleSwitch;
    private IntegerTextField mTestIntervalTextField;

    /**
     * Constructs an instance
     * @param playlistManager for accessing the broadcast model
     */
    public BroadcastifyCallEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public void setItem(BroadcastifyCallConfiguration item)
    {
        super.setItem(item);

        getSystemIdTextField().setDisable(item == null);
        getApiKeyTextField().setDisable(item == null);
        getHostTextField().setDisable(item == null);
        getMaxAgeTextField().setDisable(item == null);
        getTestEnabledToggleSwitch().setDisable(item == null);
        getTestIntervalTextField().setDisable(item == null);

        if(item != null)
        {
            getSystemIdTextField().set(item.getSystemID());
            getApiKeyTextField().setText(item.getApiKey());
            getHostTextField().setText(item.getHost());
            getMaxAgeTextField().set((int)(item.getMaximumRecordingAge() / 1000));
            getTestEnabledToggleSwitch().setSelected(item.isTestEnabled());
            getTestIntervalTextField().set(item.getTestInterval());
        }
        else
        {
            getSystemIdTextField().set(0);
            getApiKeyTextField().setText(null);
            getHostTextField().setText(null);
            getMaxAgeTextField().set(0);
            getTestEnabledToggleSwitch().setSelected(false);
            getTestIntervalTextField().set(15);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void save()
    {
        if(getItem() != null)
        {
            int systemID = getSystemIdTextField().get() != null ? getSystemIdTextField().get() : 0;
            getItem().setSystemID(systemID);
            getItem().setHost(getHostTextField().getText());
            getItem().setApiKey(getApiKeyTextField().getText());
            getItem().setMaximumRecordingAge(getMaxAgeTextField().get() * 1000);
            getItem().setTestEnabled(getTestEnabledToggleSwitch().isSelected());
            getItem().setTestInterval(getTestIntervalTextField().get());
        }

        super.save();
    }

    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.BROADCASTIFY_CALL;
    }

    protected GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 5, 10,10));
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(5);

            int row = 0;

            Label formatLabel = new Label("Format");
            GridPane.setHalignment(formatLabel, HPos.RIGHT);
            GridPane.setConstraints(formatLabel, 0, row);
            mEditorPane.getChildren().add(formatLabel);

            GridPane.setConstraints(getFormatField(), 1, row);
            mEditorPane.getChildren().add(getFormatField());

            Label enabledLabel = new Label("Enabled");
            GridPane.setHalignment(enabledLabel, HPos.RIGHT);
            GridPane.setConstraints(enabledLabel, 2, row);
            mEditorPane.getChildren().add(enabledLabel);

            GridPane.setConstraints(getEnabledSwitch(), 3, row);
            mEditorPane.getChildren().add(getEnabledSwitch());

            Label systemLabel = new Label("Name");
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 0, ++row);
            mEditorPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getNameTextField(), 1, row);
            mEditorPane.getChildren().add(getNameTextField());

            Label apiKeyLabel = new Label("API Key");
            GridPane.setHalignment(apiKeyLabel, HPos.RIGHT);
            GridPane.setConstraints(apiKeyLabel, 0, ++row);
            mEditorPane.getChildren().add(apiKeyLabel);

            GridPane.setConstraints(getApiKeyTextField(), 1, row);
            mEditorPane.getChildren().add(getApiKeyTextField());

            Label systemIdLabel = new Label("System ID");
            GridPane.setHalignment(systemIdLabel, HPos.RIGHT);
            GridPane.setConstraints(systemIdLabel, 0, ++row);
            mEditorPane.getChildren().add(systemIdLabel);

            GridPane.setConstraints(getSystemIdTextField(), 1, row);
            mEditorPane.getChildren().add(getSystemIdTextField());

            Label hostLabel = new Label("Broadcastify URL");
            GridPane.setHalignment(hostLabel, HPos.RIGHT);
            GridPane.setConstraints(hostLabel, 0, ++row);
            mEditorPane.getChildren().add(hostLabel);

            GridPane.setConstraints(getHostTextField(), 1, row);
            mEditorPane.getChildren().add(getHostTextField());

            Label maxAgeLabel = new Label("Max Recording Age (seconds)");
            GridPane.setHalignment(maxAgeLabel, HPos.RIGHT);
            GridPane.setConstraints(maxAgeLabel, 0, ++row);
            mEditorPane.getChildren().add(maxAgeLabel);

            GridPane.setConstraints(getMaxAgeTextField(), 1, row);
            mEditorPane.getChildren().add(getMaxAgeTextField());

            Label testEnabledLabel = new Label("Send Periodic Keep-Alive");
            GridPane.setHalignment(testEnabledLabel, HPos.RIGHT);
            GridPane.setConstraints(testEnabledLabel, 0, ++row);
            mEditorPane.getChildren().add(testEnabledLabel);

            GridPane.setConstraints(getTestEnabledToggleSwitch(), 1, row);
            mEditorPane.getChildren().add(getTestEnabledToggleSwitch());

            Label testIntervalLabel = new Label("Ping Interval (minutes)");
            GridPane.setHalignment(testIntervalLabel, HPos.RIGHT);
            GridPane.setConstraints(testIntervalLabel, 3, row);
            mEditorPane.getChildren().add(testIntervalLabel);

            GridPane.setConstraints(getTestIntervalTextField(), 4, row);
            mEditorPane.getChildren().add(getTestIntervalTextField());

            GridPane.setConstraints(getTestButton(), 1, ++row);
            mEditorPane.getChildren().add(getTestButton());
        }

        return mEditorPane;
    }

    private IntegerTextField getMaxAgeTextField()
    {
        if(mMaxAgeTextField == null)
        {
            mMaxAgeTextField = new IntegerTextField();
            mMaxAgeTextField.setDisable(true);
            mMaxAgeTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mMaxAgeTextField;
    }

    private TextField getHostTextField()
    {
        if(mHostTextField == null)
        {
            mHostTextField = new TextField();
            mHostTextField.setDisable(true);
            mHostTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mHostTextField;
    }

    private TextField getApiKeyTextField()
    {
        if(mApiKeyTextField == null)
        {
            mApiKeyTextField = new TextField();
            mApiKeyTextField.setDisable(true);
            mApiKeyTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mApiKeyTextField;
    }

    private IntegerTextField getSystemIdTextField()
    {
        if(mSystemIdTextField == null)
        {
            mSystemIdTextField = new IntegerTextField();
            mSystemIdTextField.setDisable(true);
            mSystemIdTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mSystemIdTextField;
    }

    private ToggleSwitch getTestEnabledToggleSwitch()
    {
        if(mTestEnabledToggleSwitch == null)
        {
            mTestEnabledToggleSwitch = new ToggleSwitch();
            mTestEnabledToggleSwitch.setDisable(true);
            mTestEnabledToggleSwitch.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mTestEnabledToggleSwitch;
    }

    private IntegerTextField getTestIntervalTextField()
    {
        if(mTestIntervalTextField == null)
        {
            mTestIntervalTextField = new IntegerTextField();
            mTestIntervalTextField.setDisable(true);
            mTestIntervalTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mTestIntervalTextField;
    }

    private Button getTestButton()
    {
        if(mTestButton == null)
        {
            mTestButton = new Button("Test Connection");
            mTestButton.setOnAction(event -> {
                int systemID = getSystemIdTextField().get();
                String apiKey = getApiKeyTextField().getText();
                String host = getHostTextField().getText();

                if(apiKey == null || apiKey.isEmpty())
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter an API Key",
                        ButtonType.OK);
                    alert.setTitle("Test Connection");
                    alert.setHeaderText("A valid API Key is required");
                    alert.initOwner(getTestButton().getScene().getWindow());
                    alert.show();
                    return;
                }

                if(systemID < 1)
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a non-zero System ID",
                        ButtonType.OK);
                    alert.setTitle("Test Connection");
                    alert.setHeaderText("A valid System ID is required");
                    alert.initOwner(getTestButton().getScene().getWindow());
                    alert.show();
                    return;
                }

                if(host == null || host.isEmpty())
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a Broadcastify URL",
                        ButtonType.OK);
                    alert.setTitle("Test Connection");
                    alert.setHeaderText("A valid URL for Broadcastify is required");
                    alert.initOwner(getTestButton().getScene().getWindow());
                    alert.show();
                    return;
                }

                BroadcastifyCallConfiguration configToTest = new BroadcastifyCallConfiguration();
                configToTest.setSystemID(systemID);
                configToTest.setApiKey(apiKey);
                configToTest.setHost(host);

                String result = BroadcastifyCallBroadcaster.testConnection(configToTest);

                if(result != null && result.toLowerCase().startsWith("ok"))
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Test successful.",
                        ButtonType.OK);
                    alert.setTitle("Test Result");
                    alert.setHeaderText("Success!");
                    alert.initOwner(getTestButton().getScene().getWindow());
                    alert.show();
                    return;
                }
                else
                {
                    String message = null;

                    if(result != null && result.toLowerCase().startsWith("1 invalid-api-key"))
                    {
                        message = "Invalid API Key";
                    }
                    else if(result != null && result.toLowerCase().startsWith("1 api-key-access-denied"))
                    {
                        message = "Invalid System ID";
                    }

                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + message, ButtonType.OK);
                    alert.setTitle("Test Result");
                    alert.setHeaderText("Test Failed.");
                    alert.initOwner(getTestButton().getScene().getWindow());
                    alert.show();
                    return;
                }
            });
        }

        return mTestButton;
    }
}
