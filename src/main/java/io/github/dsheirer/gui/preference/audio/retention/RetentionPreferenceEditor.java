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

package io.github.dsheirer.gui.preference.audio.retention;

import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.gui.preference.PreferenceEditor;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.preference.retention.AgeUnits;
import io.github.dsheirer.preference.retention.RetentionPolicy;
import io.github.dsheirer.preference.retention.RetentionPreference;
import io.github.dsheirer.preference.retention.SizeUnits;
import jakarta.annotation.PostConstruct;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Preference settings for audio/call retention
 */
public class RetentionPreferenceEditor extends PreferenceEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(RetentionPreferenceEditor.class);
    private RetentionPreference mRetentionPreference;
    private GridPane mEditorPane;
    private ComboBox<RetentionPolicy> mRetentionPolicyComboBox;
    private ComboBox<AgeUnits> mAgeUnitsComboBox;
    private ComboBox<SizeUnits> mSizeUnitsComboBox;
    private IntegerTextField mAgeValueTextField;
    private IntegerTextField mDirectorySizeTextField;

    /**
     * Constructs an instance.
     */
    public RetentionPreferenceEditor()
    {
    }

    @PostConstruct
    public void postConstruct()
    {
        mRetentionPreference = getUserPreferences().getRetentionPreference();
        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    @Override
    public PreferenceEditorType getPreferenceEditorType()
    {
        return PreferenceEditorType.AUDIO_CALL_RETENTION;
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));
            mEditorPane.setHgap(10);
            mEditorPane.setVgap(10);

            int row = 0;

            Label label = new Label("Call event retention settings for automatic deletion of call " +
                    "events and related audio.  Note: these settings do not apply to alias-based audio recordings.");
            label.setWrapText(true);
            mEditorPane.add(label, 0, row, 4, 1);

            Label autoDeleteLabel = new Label("Automatic Call Event Deletion");
            mEditorPane.add(autoDeleteLabel, 0, ++row, 4, 1);

            Label deleteByLabel = new Label("Delete By:");
            GridPane.setHalignment(deleteByLabel, HPos.RIGHT);
            mEditorPane.add(deleteByLabel, 1, ++row);
            mEditorPane.add(getRetentionPolicyComboBox(), 2, row);

            Label maxAgeLabel = new Label("Maximum Age:");
            GridPane.setHalignment(maxAgeLabel, HPos.RIGHT);
            mEditorPane.add(maxAgeLabel, 1, ++row);
            mEditorPane.add(getAgeValueTextField(), 2, row);
            mEditorPane.add(getAgeUnitsComboBox(), 3, row);

            Label maxSizeLabel = new Label("Maximum Directory Size:");
            GridPane.setHalignment(maxSizeLabel, HPos.RIGHT);
            mEditorPane.add(maxSizeLabel, 1, ++row);
            mEditorPane.add(getDirectorySizeTextField(), 2, row);
            mEditorPane.add(getSizeUnitsComboBox(), 3, row);

            updateControls();
        }

        return mEditorPane;
    }

    /**
     * Updates the enabled state of editor controls whenever the retention policy changes.
     */
    private void updateControls()
    {
        RetentionPolicy policy = getRetentionPolicyComboBox().getValue();

        switch(policy)
        {
            case AGE:
                getAgeValueTextField().setDisable(false);
                getAgeUnitsComboBox().setDisable(false);
                getDirectorySizeTextField().setDisable(true);
                break;
            case SIZE:
                getAgeValueTextField().setDisable(true);
                getAgeUnitsComboBox().setDisable(true);
                getDirectorySizeTextField().setDisable(false);
                break;
            default:
            case AGE_AND_SIZE:
                getAgeValueTextField().setDisable(false);
                getAgeUnitsComboBox().setDisable(false);
                getDirectorySizeTextField().setDisable(false);
                break;
        }
    }

    public ComboBox<RetentionPolicy> getRetentionPolicyComboBox()
    {
        if(mRetentionPolicyComboBox == null)
        {
            mRetentionPolicyComboBox = new ComboBox<>(FXCollections.observableArrayList(RetentionPolicy.values()));
            mRetentionPolicyComboBox.getSelectionModel().select(mRetentionPreference.getRetentionPolicy());
            mRetentionPolicyComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null)
                {
                    mRetentionPreference.setRetentionPolicy(newValue);
                }

                updateControls();
            });
        }

        return mRetentionPolicyComboBox;
    }

    /**
     * Age units combo box control.
     */
    public ComboBox<AgeUnits> getAgeUnitsComboBox()
    {
        if(mAgeUnitsComboBox == null)
        {
            mAgeUnitsComboBox = new ComboBox<>(FXCollections.observableArrayList(AgeUnits.values()));
            mAgeUnitsComboBox.getSelectionModel().select(mRetentionPreference.getAgeUnits());
            mAgeUnitsComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null)
                {
                    mRetentionPreference.setAgeUnits(newValue);
                }
            });
        }

        return mAgeUnitsComboBox;
    }

    /**
     * Size units combo box control.
     */
    public ComboBox<SizeUnits> getSizeUnitsComboBox()
    {
        if(mSizeUnitsComboBox == null)
        {
            mSizeUnitsComboBox = new ComboBox<>(FXCollections.observableArrayList(SizeUnits.values()));
            mSizeUnitsComboBox.getSelectionModel().select(mRetentionPreference.getSizeUnits());
            mSizeUnitsComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null)
                {
                    mRetentionPreference.setSizeUnits(newValue);
                }
            });
        }

        return mSizeUnitsComboBox;
    }

    /**
     * Age value text field.
     */
    private IntegerTextField getAgeValueTextField()
    {
        if(mAgeValueTextField == null)
        {
            mAgeValueTextField = new IntegerTextField();
            mAgeValueTextField.set(mRetentionPreference.getAgeValue());
            mAgeValueTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                Integer value = mAgeValueTextField.get();

                if(value != null)
                {
                    mRetentionPreference.setAgeValue(value);
                }
                else
                {
                    mAgeValueTextField.set(mRetentionPreference.getAgeValue());
                }
            });
            //Monitor field for lost focus and restore a valid value if the user entered value is bad
            mAgeValueTextField.focusedProperty().addListener((observable, oldValue, isFocused) -> {
                if(!isFocused)
                {
                    Integer value = mAgeValueTextField.get();

                    if(value == null || value <= 0)
                    {
                        mAgeValueTextField.setText(String.valueOf(mRetentionPreference.getAgeValue()));
                    }
                }
            });
        }

        return mAgeValueTextField;
    }

    /**
     * Directory size text field.
     */
    private IntegerTextField getDirectorySizeTextField()
    {
        if(mDirectorySizeTextField == null)
        {
            mDirectorySizeTextField = new IntegerTextField();
            mDirectorySizeTextField.set(mRetentionPreference.getMaxDirectorySize());
            mDirectorySizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                Integer value = mDirectorySizeTextField.get();

                if(value != null && value > 0)
                {
                    mRetentionPreference.setMaxDirectorySize(value);
                }
            });
            //Monitor field for lost focus and restore a valid value if the user entered value is bad
            mDirectorySizeTextField.focusedProperty().addListener((observable, oldValue, isFocused) -> {
                if(!isFocused)
                {
                    Integer value = mDirectorySizeTextField.get();

                    if(value == null || value <= 0)
                    {
                        mDirectorySizeTextField.setText(String.valueOf(mRetentionPreference.getMaxDirectorySize()));
                    }
                }
            });
        }

        return mDirectorySizeTextField;
    }
}
