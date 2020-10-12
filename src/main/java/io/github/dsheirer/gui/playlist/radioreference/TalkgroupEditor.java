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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.alias.ViewAliasRequest;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.Talkgroup;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TalkgroupEditor extends GridPane
{
    private static final Logger mLog = LoggerFactory.getLogger(TalkgroupEditor.class);
    private UserPreferences mUserPreferences;
    private PlaylistManager mPlaylistManager;
    private TextField mAlphaTagTextField;
    private TextField mDescriptionTextField;
    private TextField mTalkgroupTextField;
    private TextField mModeTextField;
    private Button mEditAliasButton;
    private TextField mAliasNameTextField;
    private TextField mAliasGroupTextField;
    private Button mCreateAliasButton;
    private Label mCreateLabel;
    private Label mNameLabel;
    private Label mGroupLabel;
    private Label mNotSupportedLabel;

    private RadioReferenceDecoder mRadioReferenceDecoder;
    private String mAliasListName;
    private System mSystem;
    private Talkgroup mTalkgroup;
    private Alias mAlias;
    private boolean mSetEncryptedDoNotMonitor;

    public TalkgroupEditor(UserPreferences userPreferences, PlaylistManager playlistManager)
    {
        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;

        setHgap(5);
        setVgap(5);

        int row = 0;

        Label radioReferenceLabel = new Label("Radio Reference Talkgroup Details");
        GridPane.setConstraints(radioReferenceLabel, 1, row);
        GridPane.setHalignment(radioReferenceLabel, HPos.LEFT);
        getChildren().add(radioReferenceLabel);

        Label talkgroupLabel = new Label("Talkgroup");
        GridPane.setConstraints(talkgroupLabel, 0, ++row);
        GridPane.setHalignment(talkgroupLabel, HPos.RIGHT);
        getChildren().add(talkgroupLabel);

        GridPane.setHgrow(getTalkgroupTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getTalkgroupTextField(), 1, row);
        getChildren().add(getTalkgroupTextField());

        Label alphaLabel = new Label("Alpha Tag");
        GridPane.setConstraints(alphaLabel, 0, ++row);
        GridPane.setHalignment(alphaLabel, HPos.RIGHT);
        getChildren().add(alphaLabel);

        GridPane.setHgrow(getAlphaTagTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getAlphaTagTextField(), 1, row);
        getChildren().add(getAlphaTagTextField());

        Label descriptionLabel = new Label("Description");
        GridPane.setConstraints(descriptionLabel, 0, ++row);
        GridPane.setHalignment(descriptionLabel, HPos.RIGHT);
        getChildren().add(descriptionLabel);

        GridPane.setHgrow(getDescriptionTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getDescriptionTextField(), 1, row);
        getChildren().add(getDescriptionTextField());

        Label modeLabel = new Label("Mode");
        GridPane.setConstraints(modeLabel, 0, ++row);
        GridPane.setHalignment(modeLabel, HPos.RIGHT);
        getChildren().add(modeLabel);

        GridPane.setHgrow(getModeTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getModeTextField(), 1, row);
        getChildren().add(getModeTextField());

        Separator separator2 = new Separator(Orientation.HORIZONTAL);
        GridPane.setConstraints(separator2, 0, ++row, 2,1);
        getChildren().add(separator2);

        //The following controls co-exist/overlap in the grid pane - visibility is controlled by the setTalkgroup()
        // method so that only one set is visible at any time.
        GridPane.setConstraints(getEditAliasButton(), 1, ++row);
        getChildren().add(getEditAliasButton());

        GridPane.setConstraints(getCreateLabel(), 1, row);
        GridPane.setHalignment(getCreateLabel(), HPos.LEFT);
        getChildren().add(getCreateLabel());

        GridPane.setConstraints(getNotSupportedLabel(), 1, row);
        GridPane.setHalignment(getNotSupportedLabel(), HPos.LEFT);
        getChildren().add(getNotSupportedLabel());

        GridPane.setConstraints(getNameLabel(), 0, ++row);
        GridPane.setHalignment(getNameLabel(), HPos.RIGHT);
        getChildren().add(getNameLabel());

        GridPane.setConstraints(getAliasNameTextField(), 1, row);
        getChildren().add(getAliasNameTextField());

        GridPane.setConstraints(getGroupLabel(), 0, ++row);
        GridPane.setHalignment(getGroupLabel(), HPos.RIGHT);
        getChildren().add(getGroupLabel());

        GridPane.setConstraints(getAliasGroupTextField(), 1, row);
        getChildren().add(getAliasGroupTextField());

        GridPane.setConstraints(getCreateAliasButton(), 1, ++row);
        getChildren().add(getCreateAliasButton());
    }


    public void setTalkgroup(Talkgroup talkgroup, System system, RadioReferenceDecoder decoder, Alias alias,
                             String aliasListName, String group, boolean setEncryptedDoNotMonitor)
    {
        mRadioReferenceDecoder = decoder;
        mTalkgroup = talkgroup;
        mSystem = system;
        mAliasListName = aliasListName;
        mAlias = alias;
        mSetEncryptedDoNotMonitor = setEncryptedDoNotMonitor;

        if(talkgroup != null)
        {
            getTalkgroupTextField().setText(decoder.format(talkgroup, system));
            getAlphaTagTextField().setText(talkgroup.getAlphaTag());
            getAliasNameTextField().setText(talkgroup.getAlphaTag());
            getDescriptionTextField().setText(talkgroup.getDescription());

            TalkgroupMode talkgroupMode = TalkgroupMode.lookup(talkgroup.getMode());
            TalkgroupEncryption talkgroupEncryption = TalkgroupEncryption.lookup(talkgroup.getEncryptionState());
            getModeTextField().setText(talkgroupMode.toString() + (talkgroupEncryption != TalkgroupEncryption.UNENCRYPTED ?
                " - " + talkgroupEncryption.toString() : ""));
        }
        else
        {
            getTalkgroupTextField().setText(null);
            getAlphaTagTextField().setText(null);
            getDescriptionTextField().setText(null);
            getModeTextField().setText(null);
            getAliasNameTextField().setText(null);
        }

        boolean supported = decoder.hasSupportedProtocol(system);

        getEditAliasButton().setVisible(mAlias != null);
        getCreateAliasButton().setVisible(mTalkgroup != null && mAlias == null && supported);
        getNameLabel().setVisible(mTalkgroup != null && mAlias == null && supported);
        getAliasNameTextField().setVisible(mTalkgroup != null && mAlias == null && supported);
        getGroupLabel().setVisible(mTalkgroup != null && mAlias == null && supported);
        getAliasGroupTextField().setText(group);
        getAliasGroupTextField().setVisible(mTalkgroup != null && mAlias == null && supported);
        getCreateLabel().setVisible(mTalkgroup != null && mAlias == null && supported);
        getNotSupportedLabel().setVisible(mTalkgroup != null && !supported);
    }

    private Label getNotSupportedLabel()
    {
        if(mNotSupportedLabel == null)
        {
            mNotSupportedLabel = new Label("Protocol Not Supported");
            mNotSupportedLabel.setVisible(false);
        }

        return mNotSupportedLabel;
    }

    private Label getCreateLabel()
    {
        if(mCreateLabel == null)
        {
            mCreateLabel = new Label("Create Talkgroup Alias");
            mCreateLabel.setVisible(false);
        }

        return mCreateLabel;
    }

    private Label getNameLabel()
    {
        if(mNameLabel == null)
        {
            mNameLabel = new Label("Name");
            mNameLabel.setVisible(false);
        }

        return mNameLabel;
    }

    private Label getGroupLabel()
    {
        if(mGroupLabel == null)
        {
            mGroupLabel = new Label("Group");
            mGroupLabel.setVisible(false);
        }

        return mGroupLabel;
    }

    private TextField getAliasNameTextField()
    {
        if(mAliasNameTextField == null)
        {
            mAliasNameTextField = new TextField();
            mAliasNameTextField.setMaxWidth(Double.MAX_VALUE);
            mAliasNameTextField.setVisible(false);
        }

        return mAliasNameTextField;
    }

    private TextField getAliasGroupTextField()
    {
        if(mAliasGroupTextField == null)
        {
            mAliasGroupTextField = new TextField();
            mAliasGroupTextField.setMaxWidth(Double.MAX_VALUE);
            mAliasGroupTextField.setVisible(false);
        }

        return mAliasGroupTextField;
    }

    private Button getCreateAliasButton()
    {
        if(mCreateAliasButton == null)
        {
            mCreateAliasButton = new Button("Create Talkgroup Alias");
            mCreateAliasButton.setVisible(false);
            mCreateAliasButton.setOnAction(event -> {
                if(mAliasListName == null)
                {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please select an Alias List",
                        ButtonType.OK);
                    alert.setTitle("Alias List Required");
                    alert.setHeaderText("An alias list is required to create aliases");
                    alert.initOwner((getCreateAliasButton()).getScene().getWindow());
                    alert.showAndWait();
                    MyEventBus.getGlobalEventBus().post(new FlashAliasListComboBoxRequest());
                }
                else if(mRadioReferenceDecoder != null && mTalkgroup != null && mSystem != null)
                {
                    Alias alias = mRadioReferenceDecoder.createAlias(mTalkgroup, mSystem, mAliasListName,
                        getAliasGroupTextField().getText());
                    alias.setName(getAliasNameTextField().getText());
                    if(mSetEncryptedDoNotMonitor &&
                        TalkgroupEncryption.lookup(mTalkgroup.getEncryptionState()) == TalkgroupEncryption.FULL)
                    {
                        int priority = io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR;
                        alias.addAliasID(new io.github.dsheirer.alias.id.priority.Priority(priority));
                    }

                    mPlaylistManager.getAliasModel().addAlias(alias);
                }
            });
        }

        return mCreateAliasButton;
    }

    private Button getEditAliasButton()
    {
        if(mEditAliasButton == null)
        {
            mEditAliasButton = new Button("View/Edit Alias");
            mEditAliasButton.setVisible(false);
            mEditAliasButton.setOnAction(event -> {
                if(mAlias != null)
                {
                    MyEventBus.getGlobalEventBus().post(new ViewAliasRequest(mAlias));
                }
            });
        }

        return mEditAliasButton;
    }

    public TextField getAlphaTagTextField()
    {
        if(mAlphaTagTextField == null)
        {
            mAlphaTagTextField = new TextField();
            mAlphaTagTextField.setMaxWidth(Double.MAX_VALUE);
            mAlphaTagTextField.setDisable(true);
        }

        return mAlphaTagTextField;
    }

    public TextField getDescriptionTextField()
    {
        if(mDescriptionTextField == null)
        {
            mDescriptionTextField = new TextField();
            mDescriptionTextField.setMaxWidth(Double.MAX_VALUE);
            mDescriptionTextField.setDisable(true);
        }

        return mDescriptionTextField;
    }

    public TextField getTalkgroupTextField()
    {
        if(mTalkgroupTextField == null)
        {
            mTalkgroupTextField = new TextField();
            mTalkgroupTextField.setMaxWidth(Double.MAX_VALUE);
            mTalkgroupTextField.setDisable(true);
        }

        return mTalkgroupTextField;
    }

    public TextField getModeTextField()
    {
        if(mModeTextField == null)
        {
            mModeTextField = new TextField();
            mModeTextField.setMaxWidth(Double.MAX_VALUE);
            mModeTextField.setPrefWidth(25);
            mModeTextField.setDisable(true);
        }

        return mModeTextField;
    }
}
