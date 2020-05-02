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


import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;

/**
 * Shoutcast V2 streaming configuration editor
 */
public class ShoutcastV2StreamEditor extends AbstractStreamEditor<ShoutcastV2Configuration>
{
    private GridPane mEditorPane;
    private TextField mUserIdTextField;
    private IntegerTextField mStreamIdTextField;
    private TextField mUrlTextField;
    private TextField mGenreTextField;
    private ToggleSwitch mPublicToggleSwitch;

    public ShoutcastV2StreamEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.SHOUTCAST_V2;
    }

    @Override
    public void setItem(ShoutcastV2Configuration item)
    {
        super.setItem(item);

        getUrlTextField().setDisable(item == null);
        getGenreTextField().setDisable(item == null);
        getPublicToggleSwitch().setDisable(item == null);
        getUserIdTextField().setDisable(item == null);
        getStreamIdTextField().setDisable(item == null);

        if(item != null)
        {
            getUrlTextField().setText(item.getURL());
            getGenreTextField().setText(item.getGenre());
            getPublicToggleSwitch().setSelected(item.isPublic());
            getStreamIdTextField().set(item.getStreamID());
            getUserIdTextField().setText(item.getUserID());
        }
        else
        {
            getUrlTextField().setText(null);
            getGenreTextField().setText(null);
            getPublicToggleSwitch().setSelected(false);
            getStreamIdTextField().set(0);
            getUserIdTextField().setText(null);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        if(getItem() != null)
        {
            getItem().setURL(getUrlTextField().getText());
            getItem().setGenre(getGenreTextField().getText());
            getItem().setPublic(getPublicToggleSwitch().isSelected());
            getItem().setUserID(getUserIdTextField().getText());
            getItem().setStreamID(getStreamIdTextField().get());
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

            Label publicLabel = new Label("Public");
            GridPane.setHalignment(publicLabel, HPos.RIGHT);
            GridPane.setConstraints(publicLabel, 2, 1);
            mEditorPane.getChildren().add(publicLabel);

            GridPane.setConstraints(getPublicToggleSwitch(), 3, 1);
            mEditorPane.getChildren().add(getPublicToggleSwitch());

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

            Label userIdLabel = new Label("User ID");
            GridPane.setHalignment(userIdLabel, HPos.RIGHT);
            GridPane.setConstraints(userIdLabel, 0, 3);
            mEditorPane.getChildren().add(userIdLabel);

            GridPane.setConstraints(getUserIdTextField(), 1, 3);
            mEditorPane.getChildren().add(getUserIdTextField());

            Label streamIdLabel = new Label("Stream ID");
            GridPane.setHalignment(streamIdLabel, HPos.RIGHT);
            GridPane.setConstraints(streamIdLabel, 2, 3);
            mEditorPane.getChildren().add(streamIdLabel);

            GridPane.setConstraints(getStreamIdTextField(), 3, 3);
            mEditorPane.getChildren().add(getStreamIdTextField());

            Label passwordLabel = new Label("Password");
            GridPane.setHalignment(passwordLabel, HPos.RIGHT);
            GridPane.setConstraints(passwordLabel, 0, 4);
            mEditorPane.getChildren().add(passwordLabel);

            GridPane.setConstraints(getMaskedPasswordTextField(), 1, 4);
            mEditorPane.getChildren().add(getMaskedPasswordTextField());
            GridPane.setConstraints(getUnMaskedPasswordTextField(), 1, 4);
            mEditorPane.getChildren().add(getUnMaskedPasswordTextField());
            GridPane.setConstraints(getShowPasswordCheckBox(), 2, 4);
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

            GridPane.setConstraints(getUrlTextField(), 1, 6, 3, 1);
            mEditorPane.getChildren().add(getUrlTextField());

            Label genreLabel = new Label("Genre");
            GridPane.setHalignment(genreLabel, HPos.RIGHT);
            GridPane.setConstraints(genreLabel, 0, 7);
            mEditorPane.getChildren().add(genreLabel);

            GridPane.setConstraints(getGenreTextField(), 1, 7, 3, 1);
            mEditorPane.getChildren().add(getGenreTextField());
        }

        return mEditorPane;
    }

    private TextField getUserIdTextField()
    {
        if(mUserIdTextField == null)
        {
            mUserIdTextField = new TextField();
            mUserIdTextField.setDisable(true);
            mUserIdTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mUserIdTextField;
    }

    private IntegerTextField getStreamIdTextField()
    {
        if(mStreamIdTextField == null)
        {
            mStreamIdTextField = new IntegerTextField();
            mStreamIdTextField.setDisable(true);
            mStreamIdTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mStreamIdTextField;
    }

    private TextField getUrlTextField()
    {
        if(mUrlTextField == null)
        {
            mUrlTextField = new TextField();
            mUrlTextField.setDisable(true);
            mUrlTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mUrlTextField;
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

    private ToggleSwitch getPublicToggleSwitch()
    {
        if(mPublicToggleSwitch == null)
        {
            mPublicToggleSwitch = new ToggleSwitch();
            mPublicToggleSwitch.setDisable(true);
            mPublicToggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mPublicToggleSwitch;
    }
}
