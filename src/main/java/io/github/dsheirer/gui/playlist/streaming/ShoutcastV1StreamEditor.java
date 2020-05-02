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
import io.github.dsheirer.audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;

/**
 * Shoutcast V1 streaming configuration editor
 */
public class ShoutcastV1StreamEditor extends AbstractStreamEditor<ShoutcastV1Configuration>
{
    private GridPane mEditorPane;
    private TextField mDescriptionTextField;
    private TextField mGenreTextField;
    private ToggleSwitch mPublicToggleSwitch;

    public ShoutcastV1StreamEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.SHOUTCAST_V1;
    }

    @Override
    public void setItem(ShoutcastV1Configuration item)
    {
        super.setItem(item);

        getDescriptionTextField().setDisable(item == null);
        getGenreTextField().setDisable(item == null);
        getPublicToggleSwitch().setDisable(item == null);

        if(item != null)
        {
            getDescriptionTextField().setText(item.getDescription());
            getGenreTextField().setText(item.getGenre());
            getPublicToggleSwitch().setSelected(item.isPublic());
        }
        else
        {
            getDescriptionTextField().setText(null);
            getGenreTextField().setText(null);
            getPublicToggleSwitch().setSelected(false);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        if(getItem() != null)
        {
            getItem().setDescription(getDescriptionTextField().getText());
            getItem().setGenre(getGenreTextField().getText());
            getItem().setPublic(getPublicToggleSwitch().isSelected());
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

            Label passwordLabel = new Label("Password");
            GridPane.setHalignment(passwordLabel, HPos.RIGHT);
            GridPane.setConstraints(passwordLabel, 0, 3);
            mEditorPane.getChildren().add(passwordLabel);

            GridPane.setConstraints(getMaskedPasswordTextField(), 1, 3);
            mEditorPane.getChildren().add(getMaskedPasswordTextField());
            GridPane.setConstraints(getUnMaskedPasswordTextField(), 1, 3);
            mEditorPane.getChildren().add(getUnMaskedPasswordTextField());
            GridPane.setConstraints(getShowPasswordCheckBox(), 2, 3);
            mEditorPane.getChildren().add(getShowPasswordCheckBox());

            Label maxAgeLabel = new Label("Max Recording Age (seconds)");
            GridPane.setHalignment(maxAgeLabel, HPos.RIGHT);
            GridPane.setConstraints(maxAgeLabel, 0, 4);
            mEditorPane.getChildren().add(maxAgeLabel);

            GridPane.setConstraints(getMaxAgeTextField(), 1, 4);
            mEditorPane.getChildren().add(getMaxAgeTextField());

            Label delayLabel = new Label("Delay (seconds)");
            GridPane.setHalignment(delayLabel, HPos.RIGHT);
            GridPane.setConstraints(delayLabel, 2, 4);
            mEditorPane.getChildren().add(delayLabel);

            GridPane.setConstraints(getDelayTextField(), 3, 4);
            mEditorPane.getChildren().add(getDelayTextField());

            Label descriptionLabel = new Label("Description");
            GridPane.setHalignment(descriptionLabel, HPos.RIGHT);
            GridPane.setConstraints(descriptionLabel, 0, 5);
            mEditorPane.getChildren().add(descriptionLabel);

            GridPane.setConstraints(getDescriptionTextField(), 1, 5, 3, 1);
            mEditorPane.getChildren().add(getDescriptionTextField());

            Label genreLabel = new Label("Genre");
            GridPane.setHalignment(genreLabel, HPos.RIGHT);
            GridPane.setConstraints(genreLabel, 0, 6);
            mEditorPane.getChildren().add(genreLabel);

            GridPane.setConstraints(getGenreTextField(), 1, 6, 3, 1);
            mEditorPane.getChildren().add(getGenreTextField());
        }

        return mEditorPane;
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
