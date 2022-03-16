/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.mp3;

import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.mp3.MP3Preference;
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
 * Preference settings for MP3
 */
public class MP3PreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3PreferenceEditor.class);
    private MP3Preference mMP3Preference;
    private GridPane mEditorPane;
    private ComboBox<MP3Setting> mMP3SettingComboBox;
    private ComboBox<InputAudioFormat> mAudioSampleRateComboBox;

    public MP3PreferenceEditor(UserPreferences userPreferences)
    {
        mMP3Preference = userPreferences.getMP3Preference();
        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
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
            Label topLabel = new Label("MP3 Encoder Preferences");
            mEditorPane.add(topLabel, 0, row++, 2, 1);

            Label label = new Label("(LAME) Encoder Setting:");
            GridPane.setHalignment(label, HPos.RIGHT);
            mEditorPane.add(label, 0, row);
            mEditorPane.add(getMP3SettingComboBox(), 1, row++);

            Label sampleRateLabel = new Label("Input Audio Sample Rate:");
            GridPane.setHalignment(sampleRateLabel, HPos.RIGHT);
            mEditorPane.add(sampleRateLabel, 0, row);
            mEditorPane.add(getAudioSampleRateComboBox(), 1, row++);

            Label notice = new Label("Note: sdrtrunk default 8 kHz audio rate is resampled to input sample rate before MP3 encoding");
            mEditorPane.add(notice, 0, row, 2, 1);
        }

        return mEditorPane;
    }

    private ComboBox<MP3Setting> getMP3SettingComboBox()
    {
        if(mMP3SettingComboBox == null)
        {
            mMP3SettingComboBox = new ComboBox<>();
            mMP3SettingComboBox.getItems().addAll(MP3Setting.values());
            mMP3SettingComboBox.getSelectionModel().select(mMP3Preference.getMP3Setting());
            mMP3SettingComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> mMP3Preference.setMP3Setting(newValue));
        }

        return mMP3SettingComboBox;
    }

    private ComboBox<InputAudioFormat> getAudioSampleRateComboBox()
    {
        if(mAudioSampleRateComboBox == null)
        {
            mAudioSampleRateComboBox = new ComboBox<>();
            mAudioSampleRateComboBox.getItems().addAll(InputAudioFormat.values());
            mAudioSampleRateComboBox.getSelectionModel().select(mMP3Preference.getAudioSampleRate());
            mAudioSampleRateComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> mMP3Preference.setAudioSampleRate(newValue));
        }

        return mAudioSampleRateComboBox;
    }
}
