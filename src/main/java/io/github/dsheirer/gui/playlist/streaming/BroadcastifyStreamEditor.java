/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyFeedConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;

/**
 * Broadcastify streaming configuration editor
 */
public class BroadcastifyStreamEditor extends AbstractStreamEditor<BroadcastifyFeedConfiguration>
{
    private GridPane mEditorPane;
    private ToggleSwitch mVerboseLoggingToggle;
    private TextField mMountPointTextField;
    private IntegerTextField mFeedIdTextField;

    public BroadcastifyStreamEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public void setItem(BroadcastifyFeedConfiguration item)
    {
        super.setItem(item);

        getFeedIdTextField().setDisable(item == null);
        getMountPointTextField().setDisable(item == null);
        getVerboseLoggingToggle().setDisable(item == null);

        if(item != null)
        {
            getFeedIdTextField().set(item.getFeedID());
            getMountPointTextField().setText(item.getMountPoint());
            getVerboseLoggingToggle().setSelected(item.isVerboseLogging());
        }
        else
        {
            getFeedIdTextField().set(0);
            getMountPointTextField().setText(null);
            getVerboseLoggingToggle().setSelected(false);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        if(getItem() != null)
        {
            getItem().setFeedID(getFeedIdTextField().get());
            getItem().setMountPoint(getMountPointTextField().getText());
            getItem().setVerboseLogging(getVerboseLoggingToggle().isSelected());
        }

        super.save();
    }

    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.BROADCASTIFY;
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

            Label passwordLabel = new Label("Password");
            GridPane.setHalignment(passwordLabel, HPos.RIGHT);
            GridPane.setConstraints(passwordLabel, 2, 3);
            mEditorPane.getChildren().add(passwordLabel);

            GridPane.setConstraints(getMaskedPasswordTextField(), 3, 3);
            mEditorPane.getChildren().add(getMaskedPasswordTextField());
            GridPane.setConstraints(getUnMaskedPasswordTextField(), 3, 3);
            mEditorPane.getChildren().add(getUnMaskedPasswordTextField());
            GridPane.setConstraints(getShowPasswordCheckBox(), 4, 3);
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

            Label feedIdLabel = new Label("Feed ID");
            GridPane.setHalignment(feedIdLabel, HPos.RIGHT);
            GridPane.setConstraints(feedIdLabel, 0, 5);
            getEditorPane().getChildren().add(feedIdLabel);

            GridPane.setConstraints(getFeedIdTextField(), 1, 5);
            getEditorPane().getChildren().add(getFeedIdTextField());

            Label loggingLabel = new Label("Verbose Logging");
            GridPane.setHalignment(loggingLabel, HPos.RIGHT);
            GridPane.setConstraints(loggingLabel, 2, 5);
            getEditorPane().getChildren().add(loggingLabel);

            GridPane.setConstraints(getVerboseLoggingToggle(), 3, 5);
            getEditorPane().getChildren().add(getVerboseLoggingToggle());
        }

        return mEditorPane;
    }

    private ToggleSwitch getVerboseLoggingToggle()
    {
        if(mVerboseLoggingToggle == null)
        {
            mVerboseLoggingToggle = new ToggleSwitch();
            mVerboseLoggingToggle.setTooltip(new Tooltip("Turn on additional logging for connection troubleshooting"));
            mVerboseLoggingToggle.setDisable(true);
            mVerboseLoggingToggle.selectedProperty().addListener((ob, ol, ne) -> modifiedProperty().set(true));
        }

        return mVerboseLoggingToggle;
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

    private IntegerTextField getFeedIdTextField()
    {
        if(mFeedIdTextField == null)
        {
            mFeedIdTextField = new IntegerTextField();
            mFeedIdTextField.setDisable(true);
            mFeedIdTextField.textProperty().addListener(mEditorModificationListener);
        }

        return mFeedIdTextField;
    }
}
