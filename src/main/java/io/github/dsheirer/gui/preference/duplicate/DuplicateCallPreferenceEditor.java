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

package io.github.dsheirer.gui.preference.duplicate;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.duplicate.DuplicateCallDetectionPreference;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preference settings for duplicate call audio handling
 */
public class DuplicateCallPreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(DuplicateCallPreferenceEditor.class);
    private DuplicateCallDetectionPreference mPreference;
    private GridPane mEditorPane;
    private ToggleSwitch mDetectDuplicateTalkgroups;
    private ToggleSwitch mDetectDuplicateRadios;
    private ToggleSwitch mSuppressDuplicateListening;
    private ToggleSwitch mSuppressDuplicateRecording;
    private ToggleSwitch mSuppressDuplicateStreaming;

    /**
     * Constructs an instance
     */
    public DuplicateCallPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getDuplicateCallDetectionPreference();

        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    private GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            int row = 0;
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));
            mEditorPane.setHgap(10);
            mEditorPane.setVgap(10);

            Label detectionLabel = new Label("Duplicate Call Detection.  Detect duplicate" +
                " calls across channels that share a common System name in each channel configuration.");
            detectionLabel.setWrapText(true);
            GridPane.setConstraints(detectionLabel, 0, row, 2, 1);
            mEditorPane.getChildren().add(detectionLabel);

            GridPane.setConstraints(getDetectDuplicateTalkgroups(), 0, ++row);
            mEditorPane.getChildren().add(getDetectDuplicateTalkgroups());

            Label talkgroupLabel = new Label("Talkgroup.  Detect duplicate calls by matching talkgroup or " +
                "patchgroup values");
            talkgroupLabel.setWrapText(true);
            GridPane.setConstraints(talkgroupLabel, 1, row);
            mEditorPane.getChildren().add(talkgroupLabel);

            GridPane.setConstraints(getDetectDuplicateRadios(), 0, ++row);
            mEditorPane.getChildren().add(getDetectDuplicateRadios());

            Label radioLabel = new Label("Radio ID.  Detect duplicate calls by matching radio identifiers.");
            radioLabel.setWrapText(true);
            GridPane.setConstraints(radioLabel, 1, row);
            mEditorPane.getChildren().add(radioLabel);

            Separator separator = new Separator();
            GridPane.setHgrow(separator, Priority.ALWAYS);
            GridPane.setConstraints(separator, 0, ++row, 2, 1);
            mEditorPane.getChildren().add(separator);

            Label suppressionLabel = new Label("Duplicate Call Suppression.  When duplicate call audio is detected, " +
                "suppress the audio during:");
            suppressionLabel.setWrapText(true);
            GridPane.setConstraints(suppressionLabel, 0, ++row, 2, 1);
            mEditorPane.getChildren().add(suppressionLabel);

            GridPane.setConstraints(getSuppressDuplicateListening(), 0, ++row);
            mEditorPane.getChildren().add(getSuppressDuplicateListening());

            Label listeningLabel = new Label("Listening");
            listeningLabel.setWrapText(true);
            GridPane.setConstraints(listeningLabel, 1, row);
            mEditorPane.getChildren().add(listeningLabel);

            GridPane.setConstraints(getSuppressDuplicateRecording(), 0, ++row);
            mEditorPane.getChildren().add(getSuppressDuplicateRecording());

            Label recordingLabel = new Label("Recording");
            recordingLabel.setWrapText(true);
            GridPane.setConstraints(recordingLabel, 1, row);
            mEditorPane.getChildren().add(recordingLabel);

            GridPane.setConstraints(getSuppressDuplicateStreaming(), 0, ++row);
            mEditorPane.getChildren().add(getSuppressDuplicateStreaming());

            Label streamingLabel = new Label("Streaming");
            streamingLabel.setWrapText(true);
            GridPane.setConstraints(streamingLabel, 1, row);
            mEditorPane.getChildren().add(streamingLabel);
        }

        return mEditorPane;
    }

    private ToggleSwitch getDetectDuplicateTalkgroups()
    {
        if(mDetectDuplicateTalkgroups == null)
        {
            mDetectDuplicateTalkgroups = new ToggleSwitch();
            mDetectDuplicateTalkgroups.setSelected(mPreference.isDuplicateCallDetectionByTalkgroupEnabled());
            mDetectDuplicateTalkgroups.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateCallDetectionByTalkgroupEnabled(newValue));
        }

        return mDetectDuplicateTalkgroups;
    }

    private ToggleSwitch getDetectDuplicateRadios()
    {
        if(mDetectDuplicateRadios == null)
        {
            mDetectDuplicateRadios = new ToggleSwitch();
            mDetectDuplicateRadios.setSelected(mPreference.isDuplicateCallDetectionByRadioEnabled());
            mDetectDuplicateRadios.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateCallDetectionByRadioEnabled(newValue));
        }

        return mDetectDuplicateRadios;
    }

    private ToggleSwitch getSuppressDuplicateListening()
    {
        if(mSuppressDuplicateListening == null)
        {
            mSuppressDuplicateListening = new ToggleSwitch();
            mSuppressDuplicateListening.disableProperty()
                .bind(Bindings.and(getDetectDuplicateTalkgroups().selectedProperty().not(),
                    getDetectDuplicateRadios().selectedProperty().not()));
            mSuppressDuplicateListening.setSelected(mPreference.isDuplicatePlaybackSuppressionEnabled());
            mSuppressDuplicateListening.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicatePlaybackSuppressionEnabled(newValue));
        }

        return mSuppressDuplicateListening;
    }

    private ToggleSwitch getSuppressDuplicateRecording()
    {
        if(mSuppressDuplicateRecording == null)
        {
            mSuppressDuplicateRecording = new ToggleSwitch();
            mSuppressDuplicateRecording.disableProperty()
                .bind(Bindings.and(getDetectDuplicateTalkgroups().selectedProperty().not(),
                    getDetectDuplicateRadios().selectedProperty().not()));
            mSuppressDuplicateRecording.setSelected(mPreference.isDuplicateRecordingSuppressionEnabled());
            mSuppressDuplicateRecording.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateRecordingSuppressionEnabled(newValue));
        }

        return mSuppressDuplicateRecording;
    }

    private ToggleSwitch getSuppressDuplicateStreaming()
    {
        if(mSuppressDuplicateStreaming == null)
        {
            mSuppressDuplicateStreaming = new ToggleSwitch();
            mSuppressDuplicateStreaming.disableProperty()
                .bind(Bindings.and(getDetectDuplicateTalkgroups().selectedProperty().not(),
                    getDetectDuplicateRadios().selectedProperty().not()));
            mSuppressDuplicateStreaming.setSelected(mPreference.isDuplicateStreamingSuppressionEnabled());
            mSuppressDuplicateStreaming.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateStreamingSuppressionEnabled(newValue));
        }

        return mSuppressDuplicateStreaming;
    }
}
