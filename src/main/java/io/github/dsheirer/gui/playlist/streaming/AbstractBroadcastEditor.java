/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * Broadcast configuration editor base class
 * @param <T> type of broadcast configuration
 */
public abstract class AbstractBroadcastEditor<T extends BroadcastConfiguration> extends Editor<T>
{
    private PlaylistManager mPlaylistManager;
    private Button mSaveButton;
    private Button mResetButton;
    private TextField mFormatField;
    private TextField mNameTextField;
    private ToggleSwitch mEnabledSwitch;
    protected EditorModificationListener mEditorModificationListener = new EditorModificationListener();

    /**
     * Constructs an instance
     * @param playlistManager for access the stream manager
     */
    public AbstractBroadcastEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
        getFormatField().setText(getBroadcastServerType().toString());

        VBox buttonBox = new VBox();
        buttonBox.setPadding(new Insets(10,10,10,10));
        buttonBox.setSpacing(10);
        buttonBox.getChildren().addAll(getSaveButton(), getResetButton());

        HBox editorBox = new HBox();
        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        editorBox.getChildren().addAll(getEditorPane(), buttonBox);
        getChildren().addAll(editorBox);
    }

    protected PlaylistManager getPlaylistManager()
    {
        return mPlaylistManager;
    }

    protected abstract GridPane getEditorPane();

    public abstract BroadcastServerType getBroadcastServerType();

    @Override
    public void setItem(T item)
    {
        super.setItem(item);

        getNameTextField().setDisable(item == null);
        getEnabledSwitch().setDisable(item == null);

        if(item != null)
        {
            getNameTextField().setText(item.getName());
            getEnabledSwitch().selectedProperty().set(item.isEnabled());
        }
        else
        {
            getNameTextField().setText(null);
            getEnabledSwitch().selectedProperty().set(false);
        }
    }

    public void save()
    {
        BroadcastConfiguration configuration = getItem();

        if(configuration != null)
        {
            configuration.setEnabled(getEnabledSwitch().isSelected());

            //Detect stream name change so that we can update any aliases that might be using the previous name
            String previousName = configuration.getName();
            String updatedName = getNameTextField().getText();
            configuration.setName(getNameTextField().getText());

            if(previousName != null && !previousName.isEmpty() && !updatedName.contentEquals(previousName))
            {
                if(getPlaylistManager().getAliasModel().hasAliasesWithBroadcastChannel(previousName))
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.getButtonTypes().clear();
                    alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
                    alert.setTitle("Update Aliases");
                    alert.setHeaderText("Rename requires updating aliases for this stream");
                    alert.setContentText("Do you want to update aliases to new stream name?");
                    alert.initOwner(((Node)getSaveButton()).getScene().getWindow());

                    //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
                    alert.setResizable(true);
                    alert.onShownProperty().addListener(e -> {
                        Platform.runLater(() -> alert.setResizable(false));
                    });

                    alert.showAndWait().ifPresent(buttonType -> {
                        if(buttonType == ButtonType.YES)
                        {
                            getPlaylistManager().getAliasModel().updateBroadcastChannel(previousName, updatedName);
                        }
                    });
                }
            }

            //TODO: remove this after we get rid of Swing tables so that we don't have to announce these changes.
            mPlaylistManager.getBroadcastModel().process(new BroadcastEvent(configuration,
                BroadcastEvent.Event.CONFIGURATION_CHANGE));
        }

        modifiedProperty().set(false);
    }

    protected Button getSaveButton()
    {
        if(mSaveButton == null)
        {
            mSaveButton = new Button("Save");
            mSaveButton.setDisable(true);
            mSaveButton.setMaxWidth(Double.MAX_VALUE);
            mSaveButton.setOnAction(event -> save());
            mSaveButton.disableProperty().bind(modifiedProperty().not());
        }

        return mSaveButton;
    }

    protected Button getResetButton()
    {
        if(mResetButton == null)
        {
            mResetButton = new Button("Reset");
            mResetButton.setDisable(true);
            mResetButton.setMaxWidth(Double.MAX_VALUE);
            mResetButton.setOnAction(event -> setItem(getItem()));
            mResetButton.disableProperty().bind(modifiedProperty().not());
        }

        return mResetButton;
    }

    protected TextField getNameTextField()
    {
        if(mNameTextField == null)
        {
            mNameTextField = new TextField();
            mNameTextField.setDisable(true);
            mNameTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mNameTextField;
    }

    protected TextField getFormatField()
    {
        if(mFormatField == null)
        {
            mFormatField = new TextField();
            mFormatField.setDisable(true);
        }

        return mFormatField;
    }

    protected ToggleSwitch getEnabledSwitch()
    {
        if(mEnabledSwitch == null)
        {
            mEnabledSwitch = new ToggleSwitch();
            mEnabledSwitch.setDisable(true);
            mEnabledSwitch.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mEnabledSwitch;
    }

    /**
     * Simple string change listener that sets the editor modified flag to true any time text fields are edited.
     */
    public class EditorModificationListener implements ChangeListener<String>
    {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
        {
            modifiedProperty().set(true);
        }
    }
}
