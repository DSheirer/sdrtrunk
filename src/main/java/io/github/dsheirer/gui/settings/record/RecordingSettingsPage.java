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

package io.github.dsheirer.gui.settings.record;

import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.record.BasebandRecordFormat;
import io.github.dsheirer.record.RecordFormat;
import io.github.dsheirer.record.RecordingFolderLayout;
import io.github.dsheirer.record.RecordingFolders;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.mp3.MP3Settings;
import io.github.dsheirer.settings.record.RecordSettings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for audio and baseband recording, call recording folders, and the MP3 encoder used for recordings and streaming
 */
public class RecordingSettingsPage extends SettingsPage
{
    private final static Logger mLog = LoggerFactory.getLogger(RecordingSettingsPage.class);
    private RecordSettings mRecordSettings;
    private ComboBox<RecordFormat> mRecordFormatComboBox;
    private ComboBox<BasebandRecordFormat> mBasebandRecordFormatComboBox;
    private MP3Settings mMP3Settings;
    private ComboBox<MP3Setting> mMP3SettingComboBox;
    private ComboBox<InputAudioFormat> mAudioSampleRateComboBox;
    private ToggleSwitch mNormalizeAudioSwitch;
    private ToggleSwitch mRecordAllCallsSwitch;
    private ComboBox<RecordingFolderLayout> mFolderLayoutComboBox;
    private ToggleSwitch mHourFolderSwitch;
    private Label mFolderExampleLabel;

    public RecordingSettingsPage(Settings settings)
    {
        super(SettingsPageType.RECORDING, settings);
        mRecordSettings = settings.getRecordSettings();
        mMP3Settings = settings.getMP3Settings();

        SettingsSection section = addSection("Formats");
        section.addRow("Audio recording format", getRecordFormatComboBox())
            .defaultValue(getRecordFormatComboBox().valueProperty(), RecordSettings.DEFAULT_RECORD_FORMAT,
                getRecordFormatComboBox()::setValue);
        section.addRow("Baseband I/Q recording format", getBasebandRecordFormatComboBox())
            .defaultValue(getBasebandRecordFormatComboBox().valueProperty(), RecordSettings.DEFAULT_BASEBAND_RECORD_FORMAT,
                getBasebandRecordFormatComboBox()::setValue)
            .help("File format for tuner and channel baseband I/Q recordings.  Applies to recordings started " +
                "after the change.");

        SettingsSection calls = addSection("Call Recordings");
        calls.addRow("Record all calls", getRecordAllCallsSwitch())
            .defaultValue(getRecordAllCallsSwitch().selectedProperty(), RecordSettings.DEFAULT_RECORD_ALL_CALLS,
                getRecordAllCallsSwitch()::setSelected)
            .help("Records the audio of every call.  When off, only calls with an alias that has the Record option " +
                "are recorded.  Duplicate and Do Not Monitor calls are deleted after the retention time on the " +
                "Database page.  Use the Database page age-off rules to limit the disk space used by recordings.");
        calls.addRow("Organize call recordings", getFolderLayoutComboBox())
            .defaultValue(getFolderLayoutComboBox().valueProperty(), RecordSettings.DEFAULT_RECORDING_FOLDER_LAYOUT,
                getFolderLayoutComboBox()::setValue)
            .help("Applies to new call recordings; existing recordings stay where they are.  The date is the call " +
                "start date.  Talkgroup or Radio is the TO talkgroup (TG_1001), patch group (PATCH_500) or radio of " +
                "a private call (RADIO_1234), else the FROM radio, else the channel name.  Folder names use the " +
                "identifier values, not alias names, so they don't change when aliases change.");
        calls.addRow("Add an hour folder below the date folder", getHourFolderSwitch())
            .defaultValue(getHourFolderSwitch().selectedProperty(), RecordSettings.DEFAULT_RECORDING_HOUR_FOLDER,
                getHourFolderSwitch()::setSelected)
            .help("For busy systems.");
        calls.addContent(getFolderExampleLabel());
        updateFolderExample();

        SettingsSection mp3 = addSection("MP3 Encoder");
        mp3.addHelp("Used for MP3 audio recordings and for streaming.");
        mp3.addRow("LAME encoder setting", getMP3SettingComboBox())
            .defaultValue(getMP3SettingComboBox().valueProperty(), MP3Setting.getDefault(),
                getMP3SettingComboBox()::setValue);
        mp3.addRow("Input audio sample rate", getAudioSampleRateComboBox())
            .defaultValue(getAudioSampleRateComboBox().valueProperty(), InputAudioFormat.getDefault(),
                getAudioSampleRateComboBox()::setValue)
            .help("sdrtrunk's 8 kHz audio is resampled to the input sample rate before MP3 encoding.");
        mp3.addRow("Normalize audio before encoding", getNormalizeAudioSwitch())
            .defaultValue(getNormalizeAudioSwitch().selectedProperty(), MP3Settings.DEFAULT_NORMALIZE_BEFORE_ENCODE,
                getNormalizeAudioSwitch()::setSelected);
    }

    /**
     * Baseband (tuner and channel I/Q) recording file format selector
     */
    private ComboBox<BasebandRecordFormat> getBasebandRecordFormatComboBox()
    {
        if(mBasebandRecordFormatComboBox == null)
        {
            mBasebandRecordFormatComboBox = new ComboBox<>();
            mBasebandRecordFormatComboBox.getItems().addAll(BasebandRecordFormat.values());
            mBasebandRecordFormatComboBox.getSelectionModel().select(mRecordSettings.getBasebandRecordFormat());
            mBasebandRecordFormatComboBox.setTooltip(new Tooltip("File format for tuner and channel baseband " +
                "I/Q recordings.  Applies to recordings started after the change."));
            mBasebandRecordFormatComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if(newValue != null)
                    {
                        mRecordSettings.setBasebandRecordFormat(newValue);
                    }
                });
        }

        return mBasebandRecordFormatComboBox;
    }

    private ComboBox<RecordFormat> getRecordFormatComboBox()
    {
        if(mRecordFormatComboBox == null)
        {
            mRecordFormatComboBox = new ComboBox<>();
            mRecordFormatComboBox.getItems().addAll(RecordFormat.values());
            mRecordFormatComboBox.getSelectionModel().select(mRecordSettings.getAudioRecordFormat());
            mRecordFormatComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<RecordFormat>()
            {
                @Override
                public void changed(ObservableValue<? extends RecordFormat> observable, RecordFormat oldValue, RecordFormat newValue)
                {
                    mRecordSettings.setAudioRecordFormat(newValue);
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
            mMP3SettingComboBox.getSelectionModel().select(mMP3Settings.getMP3Setting());
            mMP3SettingComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        mMP3Settings.setMP3Setting(newValue);
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
            mAudioSampleRateComboBox.getSelectionModel().select(mMP3Settings.getAudioSampleRate());
            mAudioSampleRateComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        if(newValue != null)
                        {
                            mMP3Settings.setAudioSampleRate(newValue);
                        }
                    });
        }

        return mAudioSampleRateComboBox;
    }

    private ToggleSwitch getNormalizeAudioSwitch()
    {
        if(mNormalizeAudioSwitch == null)
        {
            mNormalizeAudioSwitch = new ToggleSwitch();
            mNormalizeAudioSwitch.setSelected(mMP3Settings.isNormalizeAudioBeforeEncode());
            mNormalizeAudioSwitch.selectedProperty().addListener((observable, oldValue, normalize) ->
                    mMP3Settings.setNormalizeAudioBeforeEncode(normalize));
        }

        return mNormalizeAudioSwitch;
    }

    private ToggleSwitch getRecordAllCallsSwitch()
    {
        if(mRecordAllCallsSwitch == null)
        {
            mRecordAllCallsSwitch = new ToggleSwitch();
            mRecordAllCallsSwitch.setSelected(mRecordSettings.isRecordAllCalls());
            mRecordAllCallsSwitch.selectedProperty().addListener((observable, oldValue, selected) ->
                mRecordSettings.setRecordAllCalls(selected));
        }

        return mRecordAllCallsSwitch;
    }

    private ComboBox<RecordingFolderLayout> getFolderLayoutComboBox()
    {
        if(mFolderLayoutComboBox == null)
        {
            mFolderLayoutComboBox = new ComboBox<>();
            mFolderLayoutComboBox.getItems().addAll(RecordingFolderLayout.values());
            mFolderLayoutComboBox.setVisibleRowCount(RecordingFolderLayout.values().length);
            mFolderLayoutComboBox.setValue(mRecordSettings.getRecordingFolderLayout());
            mFolderLayoutComboBox.valueProperty().addListener((observable, oldValue, layout) -> {
                if(layout != null)
                {
                    mRecordSettings.setRecordingFolderLayout(layout);
                }

                updateFolderExample();
            });
        }

        return mFolderLayoutComboBox;
    }

    private ToggleSwitch getHourFolderSwitch()
    {
        if(mHourFolderSwitch == null)
        {
            mHourFolderSwitch = new ToggleSwitch();
            mHourFolderSwitch.setSelected(mRecordSettings.isRecordingHourFolder());
            mHourFolderSwitch.selectedProperty().addListener((observable, oldValue, selected) -> {
                mRecordSettings.setRecordingHourFolder(selected);
                updateFolderExample();
            });
        }

        return mHourFolderSwitch;
    }

    private Label getFolderExampleLabel()
    {
        if(mFolderExampleLabel == null)
        {
            mFolderExampleLabel = new Label();
            mFolderExampleLabel.getStyleClass().add("settings-help");
            mFolderExampleLabel.setStyle("-fx-font-family: monospace;");
        }

        return mFolderExampleLabel;
    }

    /**
     * Shows where a call recording is written with the selected folder layout, from the same folder logic used for
     * recordings, and enables the hour folder option only for layouts with a date folder.
     */
    private void updateFolderExample()
    {
        RecordingFolderLayout layout = getFolderLayoutComboBox().getValue();
        getHourFolderSwitch().setDisable(layout == null || !layout.hasDate());
        String folders = layout != null ? RecordingFolders.getExample(layout, getHourFolderSwitch().isSelected()) : "";
        getFolderExampleLabel().setText("Example: <recordings>/" + (folders.isEmpty() ? "" : folders + "/") +
            "<recording file>");
    }
}
