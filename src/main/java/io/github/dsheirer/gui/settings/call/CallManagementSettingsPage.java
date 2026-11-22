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

package io.github.dsheirer.gui.settings.call;

import io.github.dsheirer.audio.broadcast.PatchGroupStreamingOption;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.duplicate.CallManagementSettings;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for audio call management: duplicate call detection and suppression, and patch group streaming.
 */
public class CallManagementSettingsPage extends SettingsPage
{
    private final static Logger mLog = LoggerFactory.getLogger(CallManagementSettingsPage.class);
    private CallManagementSettings mSettings;
    private ToggleSwitch mDetectDuplicateTalkgroups;
    private ToggleSwitch mDetectDuplicateRadios;
    private ToggleSwitch mSuppressDuplicateListening;
    private ToggleSwitch mSuppressDuplicateRecording;
    private ToggleSwitch mSuppressDuplicateStreaming;
    private ComboBox<PatchGroupStreamingOption> mPatchGroupStreamingOptionComboBox;

    /**
     * Constructs an instance
     */
    public CallManagementSettingsPage(Settings settings)
    {
        super(SettingsPageType.CALL_MANAGEMENT, settings);
        mSettings = settings.getCallManagementSettings();

        SettingsSection detection = addSection("Duplicate Call Detection");
        detection.addHelp("Detect duplicate calls across channels that share a common System name in each channel " +
            "configuration.");
        detection.addRow("By talkgroup", getDetectDuplicateTalkgroups())
            .defaultValue(getDetectDuplicateTalkgroups().selectedProperty(), CallManagementSettings.DEFAULT_DETECT_DUPLICATE_TALKGROUP,
                getDetectDuplicateTalkgroups()::setSelected)
            .help("Detect duplicate calls by matching talkgroup or patchgroup values.");
        detection.addRow("By radio ID", getDetectDuplicateRadios())
            .defaultValue(getDetectDuplicateRadios().selectedProperty(), CallManagementSettings.DEFAULT_DETECT_DUPLICATE_RADIO,
                getDetectDuplicateRadios()::setSelected)
            .help("Detect duplicate calls by matching radio identifiers.  Be careful: this can produce unintended " +
                "side-effects.  For example, if talkgroup 1 is set to record and talkgroup 2 is set to stream, and " +
                "dispatch radio ID 1234 makes a simultaneous call to both talkgroups, there is no way to control " +
                "which call audio gets flagged as the duplicate call, so either the audio for talkgroup 1 doesn't " +
                "record, or the audio for talkgroup 2 doesn't stream.");

        SettingsSection suppression = addSection("Duplicate Call Suppression");
        suppression.addHelp("When duplicate call audio is detected, suppress the audio during:");
        suppression.addRow("Listening", getSuppressDuplicateListening())
            .defaultValue(getSuppressDuplicateListening().selectedProperty(), CallManagementSettings.DEFAULT_SUPPRESS_DUPLICATE_PLAYBACK,
                getSuppressDuplicateListening()::setSelected);
        suppression.addRow("Recording", getSuppressDuplicateRecording())
            .defaultValue(getSuppressDuplicateRecording().selectedProperty(), CallManagementSettings.DEFAULT_SUPPRESS_DUPLICATE_RECORDING,
                getSuppressDuplicateRecording()::setSelected);
        suppression.addRow("Streaming", getSuppressDuplicateStreaming())
            .defaultValue(getSuppressDuplicateStreaming().selectedProperty(), CallManagementSettings.DEFAULT_SUPPRESS_DUPLICATE_STREAMING,
                getSuppressDuplicateStreaming()::setSelected);

        SettingsSection patchGroups = addSection("Patch Groups");
        patchGroups.addRow("Stream a patch group call as", getPatchGroupStreamingOptionComboBox())
            .defaultValue(getPatchGroupStreamingOptionComboBox().valueProperty(), CallManagementSettings.DEFAULT_PATCH_GROUP_STREAMING,
                getPatchGroupStreamingOptionComboBox()::setValue);
    }

    private ToggleSwitch getDetectDuplicateTalkgroups()
    {
        if(mDetectDuplicateTalkgroups == null)
        {
            mDetectDuplicateTalkgroups = new ToggleSwitch();
            mDetectDuplicateTalkgroups.setSelected(mSettings.isDuplicateCallDetectionByTalkgroupEnabled());
            mDetectDuplicateTalkgroups.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mSettings.setDuplicateCallDetectionByTalkgroupEnabled(newValue));
        }

        return mDetectDuplicateTalkgroups;
    }

    private ToggleSwitch getDetectDuplicateRadios()
    {
        if(mDetectDuplicateRadios == null)
        {
            mDetectDuplicateRadios = new ToggleSwitch();
            mDetectDuplicateRadios.setSelected(mSettings.isDuplicateCallDetectionByRadioEnabled());
            mDetectDuplicateRadios.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mSettings.setDuplicateCallDetectionByRadioEnabled(newValue));
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
            mSuppressDuplicateListening.setSelected(mSettings.isDuplicatePlaybackSuppressionEnabled());
            mSuppressDuplicateListening.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mSettings.setDuplicatePlaybackSuppressionEnabled(newValue));
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
            mSuppressDuplicateRecording.setSelected(mSettings.isDuplicateRecordingSuppressionEnabled());
            mSuppressDuplicateRecording.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mSettings.setDuplicateRecordingSuppressionEnabled(newValue));
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
            mSuppressDuplicateStreaming.setSelected(mSettings.isDuplicateStreamingSuppressionEnabled());
            mSuppressDuplicateStreaming.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mSettings.setDuplicateStreamingSuppressionEnabled(newValue));
        }

        return mSuppressDuplicateStreaming;
    }

    /**
     * Combo box for presenting the patch group streaming options.
     * @return combo box.
     */
    private ComboBox<PatchGroupStreamingOption> getPatchGroupStreamingOptionComboBox()
    {
        if(mPatchGroupStreamingOptionComboBox == null)
        {
            ObservableList<PatchGroupStreamingOption> options = FXCollections.observableArrayList();
            options.addAll(PatchGroupStreamingOption.values());
            mPatchGroupStreamingOptionComboBox = new ComboBox<>(options);
            mPatchGroupStreamingOptionComboBox.getSelectionModel().select(mSettings.getPatchGroupStreamingOption());
            mPatchGroupStreamingOptionComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> mSettings.setPatchGroupStreamingOption(newValue));
        }

        return mPatchGroupStreamingOptionComboBox;
    }
}
