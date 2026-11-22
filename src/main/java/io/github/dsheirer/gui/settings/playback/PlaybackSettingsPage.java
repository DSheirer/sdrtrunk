/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui.settings.playback;

import io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor;
import io.github.dsheirer.audio.playback.AudioPlaybackDeviceManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.playback.PlayTestAudioRequest;
import io.github.dsheirer.settings.playback.PlaybackSettings;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for audio playback: the output device and the tones inserted into call audio
 */
public class PlaybackSettingsPage extends SettingsPage
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaybackSettingsPage.class);
    private final PlaybackSettings mPlaybackSettings;
    private ComboBox<AudioPlaybackDeviceDescriptor> mAudioPlaybackDevicesCombo;
    private Button mPlaybackDeviceTestButton;
    private ToggleSwitch mUseAudioSegmentStartToneSwitch;
    private Button mTestStartToneButton;
    private ToggleSwitch mUseAudioSegmentDropToneSwitch;
    private Button mTestDropToneButton;
    private ComboBox<ToneFrequency> mStartToneFrequencyComboBox;
    private ComboBox<ToneVolume> mStartToneVolumeComboBox;
    private ComboBox<ToneFrequency> mDropToneFrequencyComboBox;
    private ComboBox<ToneVolume> mDropToneVolumeComboBox;

    public PlaybackSettingsPage(Settings settings)
    {
        super(SettingsPageType.PLAYBACK, settings);
        mPlaybackSettings = settings.getPlaybackSettings();

        SettingsSection output = addSection("Output");
        output.addRow("Audio playback device", getAudioPlaybackDevicesCombo(), getPlaybackDeviceTestButton());

        SettingsSection startTone = addSection("Start Tone");
        startTone.addRow("Play a tone at the start of each call", getUseAudioSegmentStartToneSwitch())
            .defaultValue(getUseAudioSegmentStartToneSwitch().selectedProperty(), PlaybackSettings.DEFAULT_USE_START_TONE,
                getUseAudioSegmentStartToneSwitch()::setSelected);
        startTone.addRow("Frequency and volume", getStartToneFrequencyComboBox(), getStartToneVolumeComboBox(),
            getTestStartToneButton())
            .defaultValue(getStartToneFrequencyComboBox().valueProperty(), PlaybackSettings.DEFAULT_START_TONE_FREQUENCY,
                getStartToneFrequencyComboBox()::setValue)
            .defaultValue(getStartToneVolumeComboBox().valueProperty(), PlaybackSettings.DEFAULT_START_TONE_VOLUME,
                getStartToneVolumeComboBox()::setValue);

        SettingsSection dropTone = addSection("Drop Tone");
        dropTone.addRow("Play a tone when a call is dropped", getUseAudioSegmentDropToneSwitch())
            .defaultValue(getUseAudioSegmentDropToneSwitch().selectedProperty(), PlaybackSettings.DEFAULT_USE_DROP_TONE,
                getUseAudioSegmentDropToneSwitch()::setSelected)
            .help("Plays when sdrtrunk stops playing a call because the call became Do Not Monitor or a duplicate.");
        dropTone.addRow("Frequency and volume", getDropToneFrequencyComboBox(), getDropToneVolumeComboBox(),
            getTestDropToneButton())
            .defaultValue(getDropToneFrequencyComboBox().valueProperty(), PlaybackSettings.DEFAULT_DROP_TONE_FREQUENCY,
                getDropToneFrequencyComboBox()::setValue)
            .defaultValue(getDropToneVolumeComboBox().valueProperty(), PlaybackSettings.DEFAULT_DROP_TONE_VOLUME,
                getDropToneVolumeComboBox()::setValue);
    }

    private ComboBox<AudioPlaybackDeviceDescriptor> getAudioPlaybackDevicesCombo()
    {
        if(mAudioPlaybackDevicesCombo == null)
        {
            mAudioPlaybackDevicesCombo = new ComboBox<>();
            mAudioPlaybackDevicesCombo.getItems().addAll(AudioPlaybackDeviceManager.getAudioPlaybackDevices());
            mAudioPlaybackDevicesCombo.getSelectionModel().select(mPlaybackSettings.getAudioPlaybackDevice());
            mAudioPlaybackDevicesCombo.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue,
                              newValue) -> mPlaybackSettings.setAudioPlaybackDevice(newValue));
        }

        return mAudioPlaybackDevicesCombo;
    }

    public Button getPlaybackDeviceTestButton()
    {
        if(mPlaybackDeviceTestButton == null)
        {
            mPlaybackDeviceTestButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mPlaybackDeviceTestButton.setGraphic(iconNode);
            mPlaybackDeviceTestButton.setOnAction(event ->
                        play(mPlaybackSettings.getAudioPlaybackTestTone(), PlayTestAudioRequest.ALL_CHANNELS));
        }

        return mPlaybackDeviceTestButton;
    }

    private ToggleSwitch getUseAudioSegmentStartToneSwitch()
    {
        if(mUseAudioSegmentStartToneSwitch == null)
        {
            mUseAudioSegmentStartToneSwitch = new ToggleSwitch();
            mUseAudioSegmentStartToneSwitch.setSelected(mPlaybackSettings.getUseAudioSegmentStartTone());
            mUseAudioSegmentStartToneSwitch.selectedProperty().addListener((observable, oldValue, newValue) ->
                    mPlaybackSettings.setUseAudioSegmentStartTone(newValue));
        }

        return mUseAudioSegmentStartToneSwitch;
    }

    private ToggleSwitch getUseAudioSegmentDropToneSwitch()
    {
        if(mUseAudioSegmentDropToneSwitch == null)
        {
            mUseAudioSegmentDropToneSwitch = new ToggleSwitch();
            mUseAudioSegmentDropToneSwitch.setSelected(mPlaybackSettings.getUseAudioSegmentDropTone());
            mUseAudioSegmentDropToneSwitch.selectedProperty().addListener((observable, oldValue, newValue) ->
                    mPlaybackSettings.setUseAudioSegmentDropTone(newValue));
        }

        return mUseAudioSegmentDropToneSwitch;
    }

    public Button getTestStartToneButton()
    {
        if(mTestStartToneButton == null)
        {
            mTestStartToneButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mTestStartToneButton.setGraphic(iconNode);
            mTestStartToneButton.setOnAction(_ ->
                play(mPlaybackSettings.getStartTone(PlaybackSettings.TONE_LENGTH_SAMPLES * 3),
                        PlayTestAudioRequest.ALL_CHANNELS));
            mTestStartToneButton.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
        }

        return mTestStartToneButton;
    }

    public Button getTestDropToneButton()
    {
        if(mTestDropToneButton == null)
        {
            mTestDropToneButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mTestDropToneButton.setGraphic(iconNode);
            mTestDropToneButton.setOnAction(_ ->
                    play(mPlaybackSettings.getDropTone(PlaybackSettings.TONE_LENGTH_SAMPLES * 3),
                            PlayTestAudioRequest.ALL_CHANNELS));
            mTestDropToneButton.disableProperty().bind(getUseAudioSegmentDropToneSwitch().selectedProperty().not());
        }

        return mTestDropToneButton;
    }

    public ComboBox<ToneFrequency> getDropToneFrequencyComboBox()
    {
        if(mDropToneFrequencyComboBox == null)
        {
            mDropToneFrequencyComboBox = new ComboBox<>();
            mDropToneFrequencyComboBox.getItems().addAll(ToneFrequency.values());
            mDropToneFrequencyComboBox.getSelectionModel().select(mPlaybackSettings.getDropToneFrequency());
            mDropToneFrequencyComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackSettings.setDropToneFrequency(newValue));
            mDropToneFrequencyComboBox.disableProperty().bind(getUseAudioSegmentDropToneSwitch().selectedProperty().not());
        }

        return mDropToneFrequencyComboBox;
    }

    public ComboBox<ToneVolume> getDropToneVolumeComboBox()
    {
        if(mDropToneVolumeComboBox == null)
        {
            mDropToneVolumeComboBox = new ComboBox<>();
            mDropToneVolumeComboBox.getItems().addAll(ToneVolume.values());
            mDropToneVolumeComboBox.getSelectionModel().select(mPlaybackSettings.getDropToneVolume());
            mDropToneVolumeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackSettings.setDropToneVolume(newValue));
            mDropToneVolumeComboBox.disableProperty().bind(getUseAudioSegmentDropToneSwitch().selectedProperty().not());
        }

        return mDropToneVolumeComboBox;
    }

    public ComboBox<ToneFrequency> getStartToneFrequencyComboBox()
    {
        if(mStartToneFrequencyComboBox == null)
        {
            mStartToneFrequencyComboBox = new ComboBox<>();
            mStartToneFrequencyComboBox.getItems().addAll(ToneFrequency.values());
            mStartToneFrequencyComboBox.getSelectionModel().select(mPlaybackSettings.getStartToneFrequency());
            mStartToneFrequencyComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackSettings.setStartToneFrequency(newValue));
            mStartToneFrequencyComboBox.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
        }

        return mStartToneFrequencyComboBox;
    }

    public ComboBox<ToneVolume> getStartToneVolumeComboBox()
    {
        if(mStartToneVolumeComboBox == null)
        {
            mStartToneVolumeComboBox = new ComboBox<>();
            mStartToneVolumeComboBox.getItems().addAll(ToneVolume.values());
            mStartToneVolumeComboBox.getSelectionModel().select(mPlaybackSettings.getStartToneVolume());
            mStartToneVolumeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackSettings.setStartToneVolume(newValue));
            mStartToneVolumeComboBox.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
        }

        return mStartToneVolumeComboBox;
    }

    /**
     * Sends a request to play the audio samples over the specified audio playback channel
     * @param audioSamples with 8 kHz mono PCM samples
     * @param channel number (0=mono/left, 1=right, etc.)
     */
    private void play(float[] audioSamples, int channel)
    {
        MyEventBus.getGlobalEventBus().post(new PlayTestAudioRequest(audioSamples, channel));
    }
}
