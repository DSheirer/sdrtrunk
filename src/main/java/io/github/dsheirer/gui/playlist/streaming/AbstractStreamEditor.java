/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.gui.control.IntegerTextField;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

import java.util.Optional;

/**
 * Base class for broadcast configuration editors.
 */
public abstract class AbstractStreamEditor<T extends BroadcastConfiguration> extends Editor<T>
{
    private PlaylistManager mPlaylistManager;
    private Button mSaveButton;
    private Button mResetButton;
    private TextField mFormatField;
    private TextField mNameTextField;
    private TextField mHostTextField;
    private TextField mUnMaskedPasswordTextField;
    private PasswordField mMaskedPasswordTextField;
    private CheckBox mShowPasswordCheckBox;
    private IntegerTextField mPortTextField;
    private IntegerTextField mDelayTextField;
    private IntegerTextField mMaxAgeTextField;
    private ToggleSwitch mEnabledSwitch;
    protected EditorModificationListener mEditorModificationListener = new EditorModificationListener();

    /**
     * Constructs an instance
     */
    public AbstractStreamEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        VBox buttonBox = new VBox();
        buttonBox.setPadding(new Insets(10,10,10,10));
        buttonBox.setSpacing(10);
        buttonBox.getChildren().addAll(getSaveButton(), getResetButton());

        HBox editorBox = new HBox();
        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        editorBox.getChildren().addAll(getEditorPane(), buttonBox);
        getChildren().addAll(editorBox);
    }

    @Override
    public void setItem(T item)
    {
        super.setItem(item);

        getNameTextField().setDisable(item == null);
        getHostTextField().setDisable(item == null);
        getPortTextField().setDisable(item == null);
        getMaskedPasswordTextField().setDisable(item == null);
        getUnMaskedPasswordTextField().setDisable(item == null);
        getEnabledSwitch().setDisable(item == null);
        getMaxAgeTextField().setDisable(item == null);
        getDelayTextField().setDisable(item == null);

        if(item != null)
        {
            getNameTextField().setText(item.getName());
            getHostTextField().setText(item.getHost());
            getPortTextField().set(item.getPort());
            getMaskedPasswordTextField().setText(item.getPassword());
            getEnabledSwitch().setSelected(item.isEnabled());
            getMaxAgeTextField().set((int)(item.getMaximumRecordingAge() / 1000)); //Convert millis to seconds
            getDelayTextField().set((int)(item.getDelay() / 1000)); //Convert millis to seconds
        }
        else
        {
            getNameTextField().setText(null);
            getHostTextField().setText(null);
            getPortTextField().set(0);
            getMaskedPasswordTextField().setText(null);
            getEnabledSwitch().setSelected(false);
            getMaxAgeTextField().set(0);
            getDelayTextField().set(0);
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
        BroadcastConfiguration configuration = getItem();

        if(configuration != null)
        {
            configuration.setEnabled(getEnabledSwitch().isSelected());
            configuration.setHost(getHostTextField().getText());
            configuration.setPort(getPortTextField().get());
            configuration.setPassword(getMaskedPasswordTextField().getText());
            configuration.setDelay(getDelayTextField().get() * 1000); //Convert seconds to millis
            configuration.setMaximumRecordingAge(getMaxAgeTextField().get() * 1000); //Convert seconds to millis

            //Detect stream name change so that we can update any aliases that might be using the previous name
            String previousName = configuration.getName();
            String updatedName = getNameTextField().getText();
            configuration.setName(getNameTextField().getText());

            if(previousName != null && !previousName.isEmpty() && !updatedName.contentEquals(previousName))
            {
                if(mPlaylistManager.getAliasModel().hasAliasesWithBroadcastChannel(previousName))
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.getButtonTypes().clear();
                    alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
                    alert.setTitle("Update Aliases");
                    alert.setHeaderText("Aliases must be updated to new stream name");
                    alert.setContentText("Do you want to update these aliases?");
                    alert.initOwner(((Node)getSaveButton()).getScene().getWindow());

                    //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
                    alert.setResizable(true);
                    alert.onShownProperty().addListener(e -> {
                        Platform.runLater(() -> alert.setResizable(false));
                    });

                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        mPlaylistManager.getAliasModel().updateBroadcastChannel(previousName, updatedName);
                    }
                }
            }
        }

        modifiedProperty().set(false);
    }

    public abstract BroadcastServerType getBroadcastServerType();

    protected abstract GridPane getEditorPane();

    protected TextField getFormatField()
    {
        if(mFormatField == null)
        {
            mFormatField = new TextField();
            mFormatField.setDisable(true);
        }

        return mFormatField;
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

    protected TextField getHostTextField()
    {
        if(mHostTextField == null)
        {
            mHostTextField = new TextField();
            mHostTextField.setDisable(true);
            mHostTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mHostTextField;
    }

    protected TextField getUnMaskedPasswordTextField()
    {
        if(mUnMaskedPasswordTextField == null)
        {
            mUnMaskedPasswordTextField = new TextField();
            mUnMaskedPasswordTextField.setDisable(true);
            mUnMaskedPasswordTextField.visibleProperty().bind(getShowPasswordCheckBox().selectedProperty());
            mUnMaskedPasswordTextField.textProperty().bindBidirectional(getMaskedPasswordTextField().textProperty());
        }

        return mUnMaskedPasswordTextField;
    }

    protected PasswordField getMaskedPasswordTextField()
    {
        if(mMaskedPasswordTextField == null)
        {
            mMaskedPasswordTextField = new PasswordField();
            mMaskedPasswordTextField.setDisable(true);
            mMaskedPasswordTextField.visibleProperty().bind(getShowPasswordCheckBox().selectedProperty().not());
            mMaskedPasswordTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mMaskedPasswordTextField;
    }

    protected CheckBox getShowPasswordCheckBox()
    {
        if(mShowPasswordCheckBox == null)
        {
            mShowPasswordCheckBox = new CheckBox("Show");
        }

        return mShowPasswordCheckBox;
    }

    protected IntegerTextField getPortTextField()
    {
        if(mPortTextField == null)
        {
            mPortTextField = new IntegerTextField();
            mPortTextField.setDisable(true);
            mPortTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mPortTextField;
    }

    protected IntegerTextField getDelayTextField()
    {
        if(mDelayTextField == null)
        {
            mDelayTextField = new IntegerTextField();
            mDelayTextField.setDisable(true);
            mDelayTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mDelayTextField;
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

    private Button getSaveButton()
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

    private Button getResetButton()
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
