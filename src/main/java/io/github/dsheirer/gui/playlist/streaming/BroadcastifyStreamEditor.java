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
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyFeedConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Broadcastify streaming configuration editor
 */
public class BroadcastifyStreamEditor extends AbstractStreamEditor<BroadcastifyFeedConfiguration>
{
    private GridPane mEditorPane;
    private TextField mMountPointTextField;
    private IntegerTextField mFeedIdTextField;
    private TextField mMetadataDefaultFormatField;
    private TextField mMetadataFormatField;
    private TextField mMetadataIdleMessageField;
    private TextField mMetadataRadioFormatField;
    private TextField mMetadataTalkgroupFormatField;
    private TextField mMetadataTimeFormatField;
    private TextField mMetadataToneFormatField;

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
        getMetadataDefaultFormatField().setDisable(item == null);
        getMetadataFormatField().setDisable(item == null);
        getMetadataIdleMessageField().setDisable(item == null);
        getMetadataRadioFormatField().setDisable(item == null);
        getMetadataTalkgroupFormatField().setDisable(item == null);
        getMetadataTimeFormatField().setDisable(item == null);
        getMetadataToneFormatField().setDisable(item == null);

        if(item != null)
        {
            getFeedIdTextField().set(item.getFeedID());
            getMountPointTextField().setText(item.getMountPoint());
            getMetadataDefaultFormatField().setText(item.getMetadataDefaultFormat());
            getMetadataFormatField().setText(item.getMetadataFormat());
            getMetadataIdleMessageField().setText(item.getMetadataIdleMessage());
            getMetadataRadioFormatField().setText(item.getMetadataRadioFormat());
            getMetadataTalkgroupFormatField().setText(item.getMetadataTalkgroupFormat());
            getMetadataTimeFormatField().setText(item.getMetadataTimeFormat());
            getMetadataToneFormatField().setText(item.getMetadataToneFormat());
        }
        else
        {
            getFeedIdTextField().set(0);
            getMountPointTextField().setText(null);
            getMetadataDefaultFormatField().setText(null);
            getMetadataFormatField().setText(null);
            getMetadataIdleMessageField().setText(null);
            getMetadataRadioFormatField().setText(null);
            getMetadataTalkgroupFormatField().setText(null);
            getMetadataTimeFormatField().setText(null);
            getMetadataToneFormatField().setText(null);
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
            getItem().setMetadataDefaultFormat(getMetadataDefaultFormatField().getText());
            getItem().setMetadataFormat(getMetadataFormatField().getText());
            getItem().setMetadataIdleMessage(getMetadataIdleMessageField().getText());
            getItem().setMetadataRadioFormat(getMetadataRadioFormatField().getText());
            getItem().setMetadataTalkgroupFormat(getMetadataTalkgroupFormatField().getText());
            getItem().setMetadataTimeFormat(getMetadataTimeFormatField().getText());
            getItem().setMetadataToneFormat(getMetadataToneFormatField().getText());
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

            Label metadataSectionLabel = new Label("Metadata Customization");
            GridPane.setHalignment(metadataSectionLabel, HPos.CENTER);
            GridPane.setConstraints(metadataSectionLabel, 0, 6, 5, 1);
            mEditorPane.getChildren().add(metadataSectionLabel);

            Label metadataFormatLabel = new Label("Message Format");
            GridPane.setHalignment(metadataFormatLabel, HPos.RIGHT);
            GridPane.setConstraints(metadataFormatLabel, 0, 7);
            mEditorPane.getChildren().add(metadataFormatLabel);

            GridPane.setConstraints(getMetadataFormatField(), 1, 7, 3, 1);
            mEditorPane.getChildren().add(getMetadataFormatField());

            Label metadataFormatVarsLabel = new Label("Variables: FROM, TO, TIME, TONE, SITE, SYSTEM");
            GridPane.setHalignment(metadataFormatVarsLabel, HPos.LEFT);
            GridPane.setConstraints(metadataFormatVarsLabel, 4, 7);
            mEditorPane.getChildren().add(metadataFormatVarsLabel);

            Label metadataDefaultFormatLabel = new Label("Default Identifier Format");
            GridPane.setHalignment(metadataDefaultFormatLabel, HPos.RIGHT);
            GridPane.setConstraints(metadataDefaultFormatLabel, 0, 8);
            mEditorPane.getChildren().add(metadataDefaultFormatLabel);

            GridPane.setConstraints(getMetadataDefaultFormatField(), 1, 8, 3, 1);
            mEditorPane.getChildren().add(getMetadataDefaultFormatField());

            Label metadataDefaultFormatVarsLabel = new Label("Variables: ALIAS, ALIAS_LIST, GROUP, ID");
            GridPane.setHalignment(metadataDefaultFormatVarsLabel, HPos.LEFT);
            GridPane.setConstraints(metadataDefaultFormatVarsLabel, 4, 8);
            mEditorPane.getChildren().add(metadataDefaultFormatVarsLabel);

            Label metadataRadioFormatLabel = new Label("Override Radio Identifier Format");
            GridPane.setHalignment(metadataRadioFormatLabel, HPos.RIGHT);
            GridPane.setConstraints(metadataRadioFormatLabel, 0, 9);
            mEditorPane.getChildren().add(metadataRadioFormatLabel);

            GridPane.setConstraints(getMetadataRadioFormatField(), 1, 9, 3, 1);
            mEditorPane.getChildren().add(getMetadataRadioFormatField());

            Label metadataRadioFormatVarsLabel = new Label("Variables: ALIAS, ALIAS_LIST, GROUP, ID");
            GridPane.setHalignment(metadataRadioFormatVarsLabel, HPos.LEFT);
            GridPane.setConstraints(metadataRadioFormatVarsLabel, 4, 9);
            mEditorPane.getChildren().add(metadataRadioFormatVarsLabel);

            Label metadataTalkgroupFormatLabel = new Label("Override Talkgroup Identifier Format");
            GridPane.setHalignment(metadataTalkgroupFormatLabel, HPos.RIGHT);
            GridPane.setConstraints(metadataTalkgroupFormatLabel, 0, 10);
            mEditorPane.getChildren().add(metadataTalkgroupFormatLabel);

            GridPane.setConstraints(getMetadataTalkgroupFormatField(), 1, 10, 3, 1);
            mEditorPane.getChildren().add(getMetadataTalkgroupFormatField());

            Label metadataTalkgroupFormatVarsLabel = new Label("Variables: ALIAS, ALIAS_LIST, GROUP, ID");
            GridPane.setHalignment(metadataTalkgroupFormatVarsLabel, HPos.LEFT);
            GridPane.setConstraints(metadataTalkgroupFormatVarsLabel, 4, 10);
            mEditorPane.getChildren().add(metadataTalkgroupFormatVarsLabel);

            Label metadataToneFormatLabel = new Label("Override Tone Identifier Format");
            GridPane.setHalignment(metadataToneFormatLabel, HPos.RIGHT);
            GridPane.setConstraints(metadataToneFormatLabel, 0, 11);
            mEditorPane.getChildren().add(metadataToneFormatLabel);

            GridPane.setConstraints(getMetadataToneFormatField(), 1, 11, 3, 1);
            mEditorPane.getChildren().add(getMetadataToneFormatField());

            Label metadataToneFormatVarsLabel = new Label("Variables: ALIAS, ALIAS_LIST, GROUP, ID");
            GridPane.setHalignment(metadataToneFormatVarsLabel, HPos.LEFT);
            GridPane.setConstraints(metadataToneFormatVarsLabel, 4, 11);
            mEditorPane.getChildren().add(metadataToneFormatVarsLabel);

            Label metadataIdleMessageLabel = new Label("Idle Message");
            GridPane.setHalignment(metadataIdleMessageLabel, HPos.RIGHT);
            GridPane.setConstraints(metadataIdleMessageLabel, 0, 12);
            mEditorPane.getChildren().add(metadataIdleMessageLabel);

            GridPane.setConstraints(getMetadataIdleMessageField(), 1, 12, 3, 1);
            mEditorPane.getChildren().add(getMetadataIdleMessageField());

            Label metadataIdleMessageVarsLabel = new Label("Variables: TIME");
            GridPane.setHalignment(metadataIdleMessageVarsLabel, HPos.LEFT);
            GridPane.setConstraints(metadataIdleMessageVarsLabel, 4, 12);
            mEditorPane.getChildren().add(metadataIdleMessageVarsLabel);

            Label metadataTimeFormatLabel = new Label("Time Format");
            GridPane.setHalignment(metadataTimeFormatLabel, HPos.RIGHT);
            GridPane.setConstraints(metadataTimeFormatLabel, 0, 13);
            mEditorPane.getChildren().add(metadataTimeFormatLabel);

            GridPane.setConstraints(getMetadataTimeFormatField(), 1, 13);
            mEditorPane.getChildren().add(getMetadataTimeFormatField());
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

    private TextField getMetadataDefaultFormatField()
    {
        if(mMetadataDefaultFormatField == null)
        {
            mMetadataDefaultFormatField = new TextField();
            mMetadataDefaultFormatField.setDisable(true);
            mMetadataDefaultFormatField.textProperty().addListener(mEditorModificationListener);
        }

        return mMetadataDefaultFormatField;
    }

    private TextField getMetadataFormatField()
    {
        if(mMetadataFormatField == null)
        {
            mMetadataFormatField = new TextField();
            mMetadataFormatField.setDisable(true);
            mMetadataFormatField.textProperty().addListener(mEditorModificationListener);
        }

        return mMetadataFormatField;
    }

    private TextField getMetadataIdleMessageField()
    {
        if(mMetadataIdleMessageField == null)
        {
            mMetadataIdleMessageField = new TextField();
            mMetadataIdleMessageField.setDisable(true);
            mMetadataIdleMessageField.textProperty().addListener(mEditorModificationListener);
        }

        return mMetadataIdleMessageField;
    }

    private TextField getMetadataRadioFormatField()
    {
        if(mMetadataRadioFormatField == null)
        {
            mMetadataRadioFormatField = new TextField();
            mMetadataRadioFormatField.setDisable(true);
            mMetadataRadioFormatField.textProperty().addListener(mEditorModificationListener);
        }

        return mMetadataRadioFormatField;
    }

    private TextField getMetadataTalkgroupFormatField()
    {
        if(mMetadataTalkgroupFormatField == null)
        {
            mMetadataTalkgroupFormatField = new TextField();
            mMetadataTalkgroupFormatField.setDisable(true);
            mMetadataTalkgroupFormatField.textProperty().addListener(mEditorModificationListener);
        }

        return mMetadataTalkgroupFormatField;
    }

    private TextField getMetadataTimeFormatField()
    {
        if(mMetadataTimeFormatField == null)
        {
            mMetadataTimeFormatField = new TextField();
            mMetadataTimeFormatField.setDisable(true);
            mMetadataTimeFormatField.textProperty().addListener(mEditorModificationListener);
        }

        return mMetadataTimeFormatField;
    }

    private TextField getMetadataToneFormatField()
    {
        if(mMetadataToneFormatField == null)
        {
            mMetadataToneFormatField = new TextField();
            mMetadataToneFormatField.setDisable(true);
            mMetadataToneFormatField.textProperty().addListener(mEditorModificationListener);
        }

        return mMetadataToneFormatField;
    }
}
