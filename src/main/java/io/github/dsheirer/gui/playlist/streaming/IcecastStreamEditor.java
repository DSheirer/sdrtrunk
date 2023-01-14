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


import io.github.dsheirer.audio.broadcast.icecast.IcecastConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;

/**
 * Icecast streaming configuration editor
 */
public abstract class IcecastStreamEditor extends AbstractStreamEditor<IcecastConfiguration>
{
    private GridPane mEditorPane;
    private ToggleSwitch mInlineToggleSwitch;
    private TextField mMountPointTextField;
    private TextField mUserNameTextField;
    private TextField mDescriptionTextField;
    private TextField mGenreTextField;
    private TextField mURLTextField;

    public IcecastStreamEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public void setItem(IcecastConfiguration item)
    {
        getMountPointTextField().setDisable(item == null);
        getInlineToggleSwitch().setDisable(item == null);
        getUserNameTextField().setDisable(item == null);
        getDescriptionTextField().setDisable(item == null);
        getGenreTextField().setDisable(item == null);
        getURLTextField().setDisable(item == null);

        if(item != null)
        {
            getMountPointTextField().setText(item.getMountPoint());
            getInlineToggleSwitch().setSelected(item.getInline());
            getUserNameTextField().setText(item.getUserName());
            getDescriptionTextField().setText(item.getDescription());
            getGenreTextField().setText(item.getGenre());
            getURLTextField().setText(item.getURL());
        }
        else
        {
            getMountPointTextField().setText(null);
            getInlineToggleSwitch().setSelected(false);
            getUserNameTextField().setText(null);
            getDescriptionTextField().setText(null);
            getGenreTextField().setText(null);
            getURLTextField().setText(null);
        }

        super.setItem(item);
    }

    @Override
    public void save()
    {
        if(getItem() != null)
        {
            getItem().setMountPoint(getMountPointTextField().getText());
            getItem().setInline(getInlineToggleSwitch().isSelected());
            getItem().setUserName(getUserNameTextField().getText());
            getItem().setDescription(getDescriptionTextField().getText());
            getItem().setGenre(getGenreTextField().getText());
            getItem().setURL(getURLTextField().getText());
        }

        super.save();
    }

    protected GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 5, 10,10));
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(5);

            Label formatLabel = new Label("Format");
            GridPane.setHalignment(formatLabel, HPos.RIGHT);
            GridPane.setConstraints(formatLabel, 0, 0);
            mEditorPane.getChildren().add(formatLabel);

            getFormatField().setText(getBroadcastServerType().toString());
            GridPane.setConstraints(getFormatField(), 1, 0);
            mEditorPane.getChildren().add(getFormatField());

            Label enabledLabel = new Label("Enabled");
            GridPane.setHalignment(enabledLabel, HPos.RIGHT);
            GridPane.setConstraints(enabledLabel, 2, 0);
            mEditorPane.getChildren().add(enabledLabel);

            GridPane.setConstraints(getEnabledSwitch(), 3, 0);
            mEditorPane.getChildren().add(getEnabledSwitch());

            Label systemLabel = new Label("Name");
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 0, 1);
            mEditorPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getNameTextField(), 1, 1);
            mEditorPane.getChildren().add(getNameTextField());

            Label hostLabel = new Label("Server");
            GridPane.setHalignment(hostLabel, HPos.RIGHT);
            GridPane.setConstraints(hostLabel, 0, 2);
            mEditorPane.getChildren().add(hostLabel);

            GridPane.setConstraints(getHostTextField(), 1, 2);
            mEditorPane.getChildren().add(getHostTextField());

            Label portLabel = new Label("Port");
            GridPane.setHalignment(portLabel, HPos.RIGHT);
            GridPane.setConstraints(portLabel, 2, 2);
            mEditorPane.getChildren().add(portLabel);

            GridPane.setConstraints(getPortTextField(), 3, 2);
            mEditorPane.getChildren().add(getPortTextField());

            Label mountPointLabel = new Label("Mount Point");
            GridPane.setHalignment(mountPointLabel, HPos.RIGHT);
            GridPane.setConstraints(mountPointLabel, 0, 3);
            mEditorPane.getChildren().add(mountPointLabel);

            GridPane.setConstraints(getMountPointTextField(), 1, 3);
            mEditorPane.getChildren().add(getMountPointTextField());

            Label inlineLabel = new Label("Inline Metadata");
            GridPane.setHalignment(inlineLabel, HPos.RIGHT);
            GridPane.setConstraints(inlineLabel, 2, 3);
            mEditorPane.getChildren().add(inlineLabel);

            GridPane.setConstraints(getInlineToggleSwitch(), 3, 3);
            mEditorPane.getChildren().add(getInlineToggleSwitch());

            Label userNameLabel = new Label("User Name");
            GridPane.setHalignment(userNameLabel, HPos.RIGHT);
            GridPane.setConstraints(userNameLabel, 0, 4);
            mEditorPane.getChildren().add(userNameLabel);

            GridPane.setConstraints(getUserNameTextField(), 1, 4);
            mEditorPane.getChildren().add(getUserNameTextField());

            Label passwordLabel = new Label("Password");
            GridPane.setHalignment(passwordLabel, HPos.RIGHT);
            GridPane.setConstraints(passwordLabel, 2, 4);
            mEditorPane.getChildren().add(passwordLabel);

            GridPane.setConstraints(getMaskedPasswordTextField(), 3, 4);
            mEditorPane.getChildren().add(getMaskedPasswordTextField());
            GridPane.setConstraints(getUnMaskedPasswordTextField(), 3, 4);
            mEditorPane.getChildren().add(getUnMaskedPasswordTextField());
            GridPane.setConstraints(getShowPasswordCheckBox(), 4, 4);
            mEditorPane.getChildren().add(getShowPasswordCheckBox());

            Label maxAgeLabel = new Label("Max Recording Age (seconds)");
            GridPane.setHalignment(maxAgeLabel, HPos.RIGHT);
            GridPane.setConstraints(maxAgeLabel, 0, 5);
            mEditorPane.getChildren().add(maxAgeLabel);

            GridPane.setConstraints(getMaxAgeTextField(), 1, 5);
            mEditorPane.getChildren().add(getMaxAgeTextField());

            Label delayLabel = new Label("Delay (seconds)");
            GridPane.setHalignment(delayLabel, HPos.RIGHT);
            GridPane.setConstraints(delayLabel, 2, 5);
            mEditorPane.getChildren().add(delayLabel);

            GridPane.setConstraints(getDelayTextField(), 3, 5);
            mEditorPane.getChildren().add(getDelayTextField());

            Label descriptionLabel = new Label("Description");
            GridPane.setHalignment(descriptionLabel, HPos.RIGHT);
            GridPane.setConstraints(descriptionLabel, 0, 6);
            mEditorPane.getChildren().add(descriptionLabel);

            GridPane.setConstraints(getDescriptionTextField(), 1, 6, 3, 1);
            mEditorPane.getChildren().add(getDescriptionTextField());

            Label genreLabel = new Label("Genre");
            GridPane.setHalignment(genreLabel, HPos.RIGHT);
            GridPane.setConstraints(genreLabel, 0, 7);
            mEditorPane.getChildren().add(genreLabel);

            GridPane.setConstraints(getGenreTextField(), 1, 7, 3, 1);
            mEditorPane.getChildren().add(getGenreTextField());

            Label urlLabel = new Label("URL");
            GridPane.setHalignment(urlLabel, HPos.RIGHT);
            GridPane.setConstraints(urlLabel, 0, 8);
            mEditorPane.getChildren().add(urlLabel);

            GridPane.setConstraints(getURLTextField(), 1, 8, 3, 1);
            mEditorPane.getChildren().add(getURLTextField());
        }

        return mEditorPane;
    }

    private TextField getMountPointTextField()
    {
        if(mMountPointTextField == null)
        {
            mMountPointTextField = new TextField();
            mMountPointTextField.setDisable(true);
            mMountPointTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mMountPointTextField;
    }

    private ToggleSwitch getInlineToggleSwitch()
    {
        if(mInlineToggleSwitch == null)
        {
            mInlineToggleSwitch = new ToggleSwitch();
            mInlineToggleSwitch.setDisable(true);
            mInlineToggleSwitch.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mInlineToggleSwitch;
    }

    private TextField getUserNameTextField()
    {
        if(mUserNameTextField == null)
        {
            mUserNameTextField = new TextField();
            mUserNameTextField.setDisable(true);
            mUserNameTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mUserNameTextField;
    }

    private TextField getDescriptionTextField()
    {
        if(mDescriptionTextField == null)
        {
            mDescriptionTextField = new TextField();
            mDescriptionTextField.setDisable(true);
            mDescriptionTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mDescriptionTextField;
    }

    private TextField getGenreTextField()
    {
        if(mGenreTextField == null)
        {
            mGenreTextField = new TextField();
            mGenreTextField.setDisable(true);
            mGenreTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mGenreTextField;
    }

    private TextField getURLTextField()
    {
        if(mURLTextField == null)
        {
            mURLTextField = new TextField();
            mURLTextField.setDisable(true);
            mURLTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mURLTextField;
    }
}
