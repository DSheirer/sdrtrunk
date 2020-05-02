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
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Broadcastify calls API configuration editor
 */
public class BroadcastifyCallEditor extends AbstractBroadcastEditor<BroadcastifyCallConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastifyCallEditor.class);
    private IntegerTextField mSystemIdTextField;
    private Button mTestButton;
    private IntegerTextField mMaxAgeTextField;
    private GridPane mEditorPane;

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

        if(item != null)
        {
            getSystemIdTextField().set(item.getSystemID());
        }
        else
        {
            getSystemIdTextField().set(0);
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

            Label systemIdLabel = new Label("System ID");
            GridPane.setHalignment(systemIdLabel, HPos.RIGHT);
            GridPane.setConstraints(systemIdLabel, 0, ++row);
            mEditorPane.getChildren().add(systemIdLabel);

            GridPane.setConstraints(getSystemIdTextField(), 1, row);
            mEditorPane.getChildren().add(getSystemIdTextField());

            Label maxAgeLabel = new Label("Max Recording Age (seconds)");
            GridPane.setHalignment(maxAgeLabel, HPos.RIGHT);
            GridPane.setConstraints(maxAgeLabel, 0, ++row);
            mEditorPane.getChildren().add(maxAgeLabel);

            GridPane.setConstraints(getMaxAgeTextField(), 1, row);
            mEditorPane.getChildren().add(getMaxAgeTextField());

            GridPane.setConstraints(getTestButton(), 1, ++row);
            mEditorPane.getChildren().add(getTestButton());
        }

        return mEditorPane;
    }

    protected IntegerTextField getMaxAgeTextField()
    {
        if(mMaxAgeTextField == null)
        {
            mMaxAgeTextField = new IntegerTextField();
            mMaxAgeTextField.setDisable(true);
            mMaxAgeTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mMaxAgeTextField;
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

    private Button getTestButton()
    {
        if(mTestButton == null)
        {
            mTestButton = new Button("Test Connection");
            mTestButton.setOnAction(event -> {
                int systemID = getSystemIdTextField().get();

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

                BroadcastifyCallConfiguration configToTest = new BroadcastifyCallConfiguration();
                configToTest.setSystemID(systemID);
                String result = BroadcastifyCallBroadcaster.testConnection(configToTest);

                if(result == null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Test successful.",
                        ButtonType.OK);
                    alert.setTitle("Test Connection");
                    alert.setHeaderText("Success!");
                    alert.initOwner(getTestButton().getScene().getWindow());
                    alert.show();
                    return;
                }
                else
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + result,
                        ButtonType.OK);
                    alert.setTitle("Test Connection");
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
