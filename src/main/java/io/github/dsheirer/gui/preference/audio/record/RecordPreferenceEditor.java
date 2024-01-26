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

package io.github.dsheirer.gui.preference.audio.record;

import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.gui.preference.PreferenceEditor;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.preference.mp3.MP3Preference;
import io.github.dsheirer.preference.record.RecordPreference;
import io.github.dsheirer.record.RecordFormat;
import jakarta.annotation.PostConstruct;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Preference settings for recording
 */
public class RecordPreferenceEditor extends PreferenceEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(RecordPreferenceEditor.class);
    private RecordPreference mRecordPreference;
    private MP3Preference mMP3Preference;
    private GridPane mEditorPane;
    private ComboBox<RecordFormat> mRecordFormatComboBox;
    private ComboBox<MP3Setting> mMP3SettingComboBox;
    private ComboBox<InputAudioFormat> mAudioSampleRateComboBox;
    private CheckBox mNormalizeAudioCheckBox;

    public RecordPreferenceEditor()
    {
    }

    @PostConstruct
    public void postConstruct()
    {
        mMP3Preference = getUserPreferences().getMP3Preference();
        mRecordPreference = getUserPreferences().getRecordPreference();
        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    @Override
    public PreferenceEditorType getPreferenceEditorType()
    {
        return PreferenceEditorType.AUDIO_RECORD;
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

            Label label = new Label("Audio Recording Format:");
            mEditorPane.add(label, 0, row);
            mEditorPane.add(getRecordFormatComboBox(), 1, row);

            row++;

            Separator separator = new Separator();
            GridPane.setHgrow(separator, Priority.ALWAYS);
            GridPane.setConstraints(separator, 0, row, 2, 1);
            mEditorPane.getChildren().add(separator);

            row++;

            Label topLabel = new Label("MP3 Encoder Preferences");
            mEditorPane.add(topLabel, 0, row++, 2, 1);

            mEditorPane.add(getNormalizeAudioCheckBox(), 1, row++);

            Label lameLabel = new Label("(LAME) Encoder Setting:");
            GridPane.setHalignment(lameLabel, HPos.RIGHT);
            mEditorPane.add(lameLabel, 0, row);
            mEditorPane.add(getMP3SettingComboBox(), 1, row++);

            Label sampleRateLabel = new Label("Input Audio Sample Rate:");
            GridPane.setHalignment(sampleRateLabel, HPos.RIGHT);
            mEditorPane.add(sampleRateLabel, 0, row);
            mEditorPane.add(getAudioSampleRateComboBox(), 1, row++);

            Label notice = new Label("Note: sdrtrunk default 8 kHz audio rate is resampled to input sample rate before MP3 encoding");
            mEditorPane.add(notice, 0, row++, 2, 1);
        }

        return mEditorPane;
    }

    private ComboBox<RecordFormat> getRecordFormatComboBox()
    {
        if(mRecordFormatComboBox == null)
        {
            mRecordFormatComboBox = new ComboBox<>();
            mRecordFormatComboBox.getItems().addAll(RecordFormat.values());
            mRecordFormatComboBox.getSelectionModel().select(mRecordPreference.getAudioRecordFormat());
            mRecordFormatComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<RecordFormat>()
            {
                @Override
                public void changed(ObservableValue<? extends RecordFormat> observable, RecordFormat oldValue, RecordFormat newValue)
                {
                    mRecordPreference.setAudioRecordFormat(newValue);
                }
            });
        }

        return mRecordFormatComboBox;
    }

    private ComboBox<MP3Setting> getMP3SettingComboBox()
    {
        if(mMP3SettingComboBox == null)
        {
            mMP3SettingComboBox = new ComboBox<>();
            mMP3SettingComboBox.getItems().addAll(MP3Setting.values());
            mMP3SettingComboBox.getSelectionModel().select(mMP3Preference.getMP3Setting());
            mMP3SettingComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        mMP3Preference.setMP3Setting(newValue);
                        updateAudioSampleRateComboBox();
                    });
        }

        return mMP3SettingComboBox;
    }

    /**
     * Updates the input sample rate combo box values and alerts the user whenever we auto-change an input sample rate
     * due to a change in the MP3 setting value.
     */
    private void updateAudioSampleRateComboBox()
    {
        InputAudioFormat currentSelection = getAudioSampleRateComboBox().getSelectionModel().getSelectedItem();
        MP3Setting setting = getMP3SettingComboBox().getSelectionModel().getSelectedItem();

        getAudioSampleRateComboBox().getItems().clear();
        getAudioSampleRateComboBox().getItems().addAll(setting.getSupportedSampleRates());

        if(currentSelection != null && getAudioSampleRateComboBox().getItems().contains(currentSelection))
        {
            getAudioSampleRateComboBox().getSelectionModel().select(currentSelection);
        }
        else
        {
            getAudioSampleRateComboBox().getSelectionModel().select(InputAudioFormat.getDefault());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sample Rate Updated");
            alert.setHeaderText("Input Sample Rate Updated");

            Label wrappingLabel = new Label("Previous input sample rate [" + currentSelection +
                    "] is not supported with encoder setting [" + setting + "].  Sample rate updated to default.");
            wrappingLabel.setWrapText(true);
            alert.getDialogPane().setContent(wrappingLabel);
            alert.show();
        }
    }

    private ComboBox<InputAudioFormat> getAudioSampleRateComboBox()
    {
        if(mAudioSampleRateComboBox == null)
        {
            mAudioSampleRateComboBox = new ComboBox<>();
            mAudioSampleRateComboBox.getItems().addAll(InputAudioFormat.values());
            mAudioSampleRateComboBox.getSelectionModel().select(mMP3Preference.getAudioSampleRate());
            mAudioSampleRateComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        if(newValue != null)
                        {
                            mMP3Preference.setAudioSampleRate(newValue);
                        }
                    });
        }

        return mAudioSampleRateComboBox;
    }

    private CheckBox getNormalizeAudioCheckBox()
    {
        if(mNormalizeAudioCheckBox == null)
        {
            mNormalizeAudioCheckBox = new CheckBox("Normalize Audio Before Encoding");
            mNormalizeAudioCheckBox.setSelected(mMP3Preference.isNormalizeAudioBeforeEncode());
            mNormalizeAudioCheckBox.onActionProperty().set(event ->
                    mMP3Preference.setNormalizeAudioBeforeEncode(getNormalizeAudioCheckBox().isSelected()));
        }

        return mNormalizeAudioCheckBox;
    }
}
