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

package io.github.dsheirer.gui.preference.playback;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.playback.PlaybackPreference;
import io.github.dsheirer.source.mixer.MixerChannelConfiguration;
import io.github.dsheirer.source.mixer.MixerManager;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;


/**
 * Preference settings for audio playback
 */
public class PlaybackPreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaybackPreferenceEditor.class);
    private PlaybackPreference mPlaybackPreference;
    private GridPane mEditorPane;
    private ComboBox<MixerChannelConfiguration> mMixerComboBox;
    private Button mMixerTestButton;
    private ToggleSwitch mUseAudioSegmentStartToneSwitch;
    private Button mTestStartToneButton;
    private ToggleSwitch mUseAudioSegmentPreemptToneSwitch;
    private Button mTestPreemptToneButton;
    private ComboBox<ToneFrequency> mStartToneFrequencyComboBox;
    private ComboBox<ToneVolume> mStartToneVolumeComboBox;
    private ComboBox<ToneFrequency> mPreemptToneFrequencyComboBox;
    private ComboBox<ToneVolume> mPreemptToneVolumeComboBox;

    public PlaybackPreferenceEditor(UserPreferences userPreferences)
    {
        mPlaybackPreference = userPreferences.getPlaybackPreference();

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
            Label outputLabel = new Label("Audio Output Device");
            GridPane.setHalignment(outputLabel, HPos.RIGHT);
            mEditorPane.add(outputLabel, 0, row, 2, 1);
            mEditorPane.add(getMixerComboBox(), 2, row, 3, 1);
            mEditorPane.add(getMixerTestButton(), 5, row);
            mEditorPane.add(new Separator(Orientation.HORIZONTAL), 0, ++row, 6, 1);
            mEditorPane.add(new Label("Audio Playback Insert Tones"), 0, ++row, 2, 1);
            mEditorPane.add(getUseAudioSegmentStartToneSwitch(), 0, ++row);
            mEditorPane.add(new Label("Start Tone"), 1, row, 3, 1);
            Label startFrequencyLabel = new Label("Frequency:");
            GridPane.setHalignment(startFrequencyLabel, HPos.RIGHT);
            mEditorPane.add(startFrequencyLabel, 1, ++row);
            mEditorPane.add(getStartToneFrequencyComboBox(), 2, row);
            Label startVolumeLabel = new Label("Volume:");
            GridPane.setHalignment(startVolumeLabel, HPos.RIGHT);
            mEditorPane.add(startVolumeLabel, 3, row);
            mEditorPane.add(getStartToneVolumeComboBox(), 4, row);
            mEditorPane.add(getTestStartToneButton(), 5, row);
            mEditorPane.add(getUseAudioSegmentPreemptToneSwitch(), 0, ++row);
            mEditorPane.add(new Label("Priority Preempt Tone"), 1, row, 3, 1);
            Label preemptFrequencyLabel = new Label("Frequency:");
            GridPane.setHalignment(preemptFrequencyLabel, HPos.RIGHT);
            mEditorPane.add(preemptFrequencyLabel, 1, ++row);
            mEditorPane.add(getPreemptToneFrequencyComboBox(), 2, row);
            Label preemptVolumeLabel = new Label("Volume:");
            GridPane.setHalignment(preemptVolumeLabel, HPos.RIGHT);
            mEditorPane.add(preemptVolumeLabel, 3, row);
            mEditorPane.add(getPreemptToneVolumeComboBox(), 4, row);
            mEditorPane.add(getTestPreemptToneButton(), 5, row);
        }

        return mEditorPane;
    }

    private ComboBox<MixerChannelConfiguration> getMixerComboBox()
    {
        if(mMixerComboBox == null)
        {
            mMixerComboBox = new ComboBox<>();
            mMixerComboBox.getItems().addAll(MixerManager.getOutputMixers());
            mMixerComboBox.getSelectionModel().select(mPlaybackPreference.getMixerChannelConfiguration());
            mMixerComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setMixerChannelConfiguration(newValue));
        }

        return mMixerComboBox;
    }

    public Button getMixerTestButton()
    {
        if(mMixerTestButton == null)
        {
            mMixerTestButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mMixerTestButton.setGraphic(iconNode);
            mMixerTestButton.setOnAction(event -> play(mPlaybackPreference.getMixerTestTone()));
        }

        return mMixerTestButton;
    }

    private ToggleSwitch getUseAudioSegmentStartToneSwitch()
    {
        if(mUseAudioSegmentStartToneSwitch == null)
        {
            mUseAudioSegmentStartToneSwitch = new ToggleSwitch();
            mUseAudioSegmentStartToneSwitch.setAlignment(Pos.BASELINE_RIGHT);
            mUseAudioSegmentStartToneSwitch.setSelected(mPlaybackPreference.getUseAudioSegmentStartTone());
            mUseAudioSegmentStartToneSwitch.selectedProperty().addListener(new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
                {
                    mPlaybackPreference.setUseAudioSegmentStartTone(newValue);
                }
            });
        }

        return mUseAudioSegmentStartToneSwitch;
    }

    private ToggleSwitch getUseAudioSegmentPreemptToneSwitch()
    {
        if(mUseAudioSegmentPreemptToneSwitch == null)
        {
            mUseAudioSegmentPreemptToneSwitch = new ToggleSwitch();
            mUseAudioSegmentPreemptToneSwitch.setSelected(mPlaybackPreference.getUseAudioSegmentPreemptTone());
            mUseAudioSegmentPreemptToneSwitch.selectedProperty().addListener(new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
                {
                    mPlaybackPreference.setUseAudioSegmentPreemptTone(newValue);
                }
            });
        }

        return mUseAudioSegmentPreemptToneSwitch;
    }

    public Button getTestStartToneButton()
    {
        if(mTestStartToneButton == null)
        {
            mTestStartToneButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mTestStartToneButton.setGraphic(iconNode);
            mTestStartToneButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    play(mPlaybackPreference.getStartTone());
                }
            });

            mTestStartToneButton.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
        }

        return mTestStartToneButton;
    }

    public Button getTestPreemptToneButton()
    {
        if(mTestPreemptToneButton == null)
        {
            mTestPreemptToneButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mTestPreemptToneButton.setGraphic(iconNode);
            mTestPreemptToneButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    play(mPlaybackPreference.getPreemptTone());
                }
            });

            mTestPreemptToneButton.disableProperty().bind(getUseAudioSegmentPreemptToneSwitch().selectedProperty().not());
        }

        return mTestPreemptToneButton;
    }

    public ComboBox<ToneFrequency> getStartToneFrequencyComboBox()
    {
        if(mStartToneFrequencyComboBox == null)
        {
            mStartToneFrequencyComboBox = new ComboBox<>();
            mStartToneFrequencyComboBox.getItems().addAll(ToneFrequency.values());
            mStartToneFrequencyComboBox.getSelectionModel().select(mPlaybackPreference.getStartToneFrequency());
            mStartToneFrequencyComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setStartToneFrequency(newValue));
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
            mStartToneVolumeComboBox.getSelectionModel().select(mPlaybackPreference.getStartToneVolume());
            mStartToneVolumeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setStartToneVolume(newValue));
            mStartToneVolumeComboBox.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
        }

        return mStartToneVolumeComboBox;
    }

    public ComboBox<ToneFrequency> getPreemptToneFrequencyComboBox()
    {
        if(mPreemptToneFrequencyComboBox == null)
        {
            mPreemptToneFrequencyComboBox = new ComboBox<>();
            mPreemptToneFrequencyComboBox.getItems().addAll(ToneFrequency.values());
            mPreemptToneFrequencyComboBox.getSelectionModel().select(mPlaybackPreference.getPreemptToneFrequency());
            mPreemptToneFrequencyComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setPreemptToneFrequency(newValue));
            mPreemptToneFrequencyComboBox.disableProperty().bind(getUseAudioSegmentPreemptToneSwitch().selectedProperty().not());
        }

        return mPreemptToneFrequencyComboBox;
    }

    public ComboBox<ToneVolume> getPreemptToneVolumeComboBox()
    {
        if(mPreemptToneVolumeComboBox == null)
        {
            mPreemptToneVolumeComboBox = new ComboBox<>();
            mPreemptToneVolumeComboBox.getItems().addAll(ToneVolume.values());
            mPreemptToneVolumeComboBox.getSelectionModel().select(mPlaybackPreference.getPreemptToneVolume());
            mPreemptToneVolumeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setPreemptToneVolume(newValue));
            mPreemptToneVolumeComboBox.disableProperty().bind(getUseAudioSegmentPreemptToneSwitch().selectedProperty().not());
        }

        return mPreemptToneVolumeComboBox;
    }

    /**
     * Plays the audio buffer over the default mono playback device
     * @param audioSamples with 8 kHz mono PCM samples
     */
    private void play(float[] audioSamples)
    {
        if(audioSamples != null)
        {
            /* Little-endian byte buffer */
            ByteBuffer buffer = ByteBuffer.allocate(audioSamples.length * 2).order(ByteOrder.LITTLE_ENDIAN);

            ShortBuffer shortBuffer = buffer.asShortBuffer();

            for(float sample : audioSamples)
            {
                shortBuffer.put((short) (sample * Short.MAX_VALUE));
            }

            byte[] bytes = buffer.array();

            DataLine.Info info = new DataLine.Info(Clip.class, AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO);

            if(!AudioSystem.isLineSupported(info))
            {
                mLog.error("Audio clip playback is not supported on this system");
                return;
            }

            try
            {
                Clip clip = (Clip)AudioSystem.getLine(info);
                clip.open(AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO, bytes, 0, bytes.length);
                clip.start();
            }
            catch(Exception e)
            {
                mLog.error("Error attempting to play audio test tone");
            }
        }
    }
}
