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

package io.github.dsheirer.gui.audio;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.audio.playback.AudioChannel;
import io.github.dsheirer.audio.playback.AudioOutput;
import io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javax.sound.sampled.FloatControl;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconFontFX;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio playback display.  A narrow master column with the output mute, audio device settings and (when the output
 * supports it) a vertical volume slider, followed by one
 * display per audio channel: a single wide display for a mono output or side by side left and right displays for a
 * stereo output.  Rebuilds the channel displays when the audio playback device configuration changes.
 */
public class AudioView extends HBox
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioView.class);
    static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    static final PseudoClass MUTED = PseudoClass.getPseudoClass("muted");
    private static final double UPDATES_PER_SECOND = 25.0;

    private final AudioPlaybackManager mAudioPlaybackManager;
    private final AliasModel mAliasModel;
    private final IconModel mIconModel;
    private final UserPreferences mUserPreferences;
    private final Listener<AudioEvent> mAudioEventListener = this::receive;
    private final List<AudioChannelView> mChannelViews = new ArrayList<>();
    private final HBox mChannelsBox = new HBox(8);
    private final ToggleButton mMuteButton = new ToggleButton();
    private final IconNode mMuteIcon = new IconNode(FontAwesome.VOLUME_UP);
    private final Button mSettingsButton = new Button();
    private final VBox mVolumeBox;
    private final Slider mVolumeSlider = new Slider(0, 100, 50);
    private final Timeline mTimeline;
    private FloatControl mGainControl;

    static
    {
        IconFontFX.register(FontAwesome.getIconFont());
    }

    /**
     * Constructs an instance
     * @param audioPlaybackManager for the audio output and channels
     * @param aliasModel for alias lookup
     * @param iconModel for alias icons
     * @param userPreferences for identifier formatting
     */
    public AudioView(AudioPlaybackManager audioPlaybackManager, AliasModel aliasModel, IconModel iconModel,
                     UserPreferences userPreferences)
    {
        mAudioPlaybackManager = audioPlaybackManager;
        mAliasModel = aliasModel;
        mIconModel = iconModel;
        mUserPreferences = userPreferences;

        getStyleClass().add("audio-view");
        URL stylesheet = AudioView.class.getResource("/audio_panel.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        setSpacing(8);
        setFillHeight(true);

        //Master controls
        Label title = new Label("AUDIO");
        title.getStyleClass().add("audio-master-title");

        mMuteIcon.setIconSize(22);
        mMuteIcon.getStyleClass().add("audio-button-icon");
        mMuteButton.setGraphic(mMuteIcon);
        mMuteButton.getStyleClass().addAll("audio-icon-button", "audio-master-mute");
        mMuteButton.setMaxWidth(Double.MAX_VALUE);
        mMuteButton.setOnAction(event -> setOutputMuted(mMuteButton.isSelected()));

        IconNode settingsIcon = new IconNode(FontAwesome.COG);
        settingsIcon.setIconSize(16);
        settingsIcon.getStyleClass().add("audio-button-icon");
        mSettingsButton.setGraphic(settingsIcon);
        mSettingsButton.getStyleClass().add("audio-icon-button");
        mSettingsButton.setMaxWidth(Double.MAX_VALUE);
        mSettingsButton.setAccessibleText("Audio settings");
        mSettingsButton.setOnAction(event -> showAudioPreferences());

        IconNode volumeIcon = new IconNode(FontAwesome.VOLUME_DOWN);
        volumeIcon.setIconSize(12);
        volumeIcon.getStyleClass().add("audio-detail-icon");
        mVolumeSlider.getStyleClass().add("audio-volume");
        mVolumeSlider.setOrientation(Orientation.VERTICAL);
        mVolumeSlider.setMinHeight(40);
        mVolumeSlider.setPrefHeight(60);
        VBox.setVgrow(mVolumeSlider, Priority.ALWAYS);
        mVolumeSlider.setTooltip(new Tooltip("Volume (double-click to reset)"));
        mVolumeSlider.setAccessibleText("Volume");
        mVolumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> applyVolume(newValue.intValue()));
        mVolumeSlider.setOnMouseClicked(event -> {
            if(event.getClickCount() == 2)
            {
                mVolumeSlider.setValue(50);
            }
        });
        mVolumeBox = new VBox(4, mVolumeSlider, volumeIcon);
        mVolumeBox.setAlignment(Pos.TOP_CENTER);

        VBox controls = new VBox(6, title, mMuteButton, mSettingsButton);
        controls.setAlignment(Pos.TOP_CENTER);
        controls.setFillWidth(true);

        HBox master = new HBox(4, controls, mVolumeBox);
        master.getStyleClass().add("audio-master");
        master.setMinWidth(HBox.USE_PREF_SIZE);

        HBox.setHgrow(mChannelsBox, Priority.ALWAYS);
        mChannelsBox.setFillHeight(true);
        mChannelsBox.setMinWidth(0);
        getChildren().addAll(master, mChannelsBox);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem preferences = new MenuItem("Audio Playback Device ...");
        IconNode cog = new IconNode(FontAwesome.COG);
        cog.setIconSize(14);
        preferences.setGraphic(cog);
        preferences.setOnAction(event -> showAudioPreferences());
        contextMenu.getItems().add(preferences);
        setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.SECONDARY)
            {
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
            }
            else
            {
                contextMenu.hide();
            }
        });

        mTimeline = new Timeline(new KeyFrame(Duration.millis(1000.0 / UPDATES_PER_SECOND), event -> tick()));
        mTimeline.setCycleCount(Animation.INDEFINITE);

        rebuild();

        mAudioPlaybackManager.addAudioEventListener(mAudioEventListener);
        MyEventBus.getGlobalEventBus().register(this);
        mTimeline.play();
    }

    /**
     * Stops updates and deregisters from the audio playback manager.
     */
    public void dispose()
    {
        mTimeline.stop();
        mAudioPlaybackManager.removeAudioEventListener(mAudioEventListener);
        MyEventBus.getGlobalEventBus().unregister(this);
        disposeChannelViews();
    }

    /**
     * Updates the identifier display when the identifier format preference changes.
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            Platform.runLater(() -> mChannelViews.forEach(AudioChannelView::refresh));
        }
    }

    /**
     * Audio playback manager events, received on the calling thread.
     */
    private void receive(AudioEvent event)
    {
        if(event.getType() == AudioEvent.Type.AUDIO_CONFIGURATION_CHANGE_COMPLETE)
        {
            Platform.runLater(this::rebuild);
        }
    }

    private void tick()
    {
        for(AudioChannelView channelView: mChannelViews)
        {
            channelView.tick();
        }
    }

    private void disposeChannelViews()
    {
        mChannelViews.forEach(AudioChannelView::dispose);
        mChannelViews.clear();
        mChannelsBox.getChildren().clear();
    }

    /**
     * Rebuilds the channel displays and master controls for the current audio output.  FX thread only.
     */
    private void rebuild()
    {
        disposeChannelViews();

        List<AudioChannel> channels = mAudioPlaybackManager.getAudioChannels();

        for(AudioChannel channel: channels)
        {
            AudioChannelView view = new AudioChannelView(channel, mAliasModel, mIconModel,
                mUserPreferences.getTalkgroupFormatPreference(), UPDATES_PER_SECOND, this::updateMuted);
            mChannelViews.add(view);
            mChannelsBox.getChildren().add(view);
        }

        AudioOutput output = mAudioPlaybackManager.getAudioOutput();
        mGainControl = output != null && output.hasGainControl() ? output.getGainControl() : null;
        mVolumeBox.setVisible(mGainControl != null);
        mVolumeBox.setManaged(mGainControl != null);

        if(mGainControl != null)
        {
            mVolumeSlider.setValue(toSliderValue(mGainControl.getValue()));
        }

        AudioPlaybackDeviceDescriptor device = mAudioPlaybackManager.getAudioPlaybackDevice();
        String deviceName = device != null ? device.toString() : "No audio device";
        String layout = switch(channels.size())
        {
            case 0 -> "";
            case 1 -> "Mono";
            case 2 -> "Stereo";
            default -> channels.size() + " channels";
        };
        String deviceText = layout.isEmpty() ? deviceName : layout + " · " + deviceName;
        mSettingsButton.setTooltip(new Tooltip("Audio playback device and preferences\n" + deviceText));

        if(channels.isEmpty())
        {
            Label none = new Label("No audio playback device available");
            none.getStyleClass().add("audio-detail");
            mChannelsBox.getChildren().add(none);
        }

        mMuteButton.setDisable(output == null);
        updateMuted();
    }

    /**
     * Updates the master mute button.  The output is muted when every channel is muted.
     */
    private void updateMuted()
    {
        boolean muted = !mChannelViews.isEmpty();

        for(AudioChannelView view: mChannelViews)
        {
            muted &= view.getAudioChannel().isMuted();
        }

        mMuteButton.setSelected(muted);
        mMuteIcon.setIconCode(muted ? FontAwesome.VOLUME_OFF : FontAwesome.VOLUME_UP);
        mMuteButton.setTooltip(new Tooltip(muted ? "Unmute all audio" : "Mute all audio"));
        mMuteButton.setAccessibleText(muted ? "Unmute all audio" : "Mute all audio");
    }

    private void setOutputMuted(boolean muted)
    {
        AudioOutput output = mAudioPlaybackManager.getAudioOutput();

        if(output != null)
        {
            output.setMuted(muted);
        }

        mChannelViews.forEach(AudioChannelView::updateMuted);
        updateMuted();
    }

    private void showAudioPreferences()
    {
        MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.AUDIO_OUTPUT));
    }

    private void applyVolume(int value)
    {
        FloatControl control = mGainControl;

        if(control != null)
        {
            try
            {
                control.shift(control.getValue(), toGain(control, value), 1000);
            }
            catch(Exception e)
            {
                mLog.warn("Unable to set audio output gain", e);
            }
        }
    }

    /**
     * Converts a gain control value in dB to the slider value, where 50 is the 0 dB (no gain) mid-point.
     */
    private int toSliderValue(float gain)
    {
        FloatControl control = mGainControl;

        if(control == null || gain == 0.0f)
        {
            return 50;
        }
        else if(gain < 0.0f)
        {
            return 50 - (int)(gain / control.getMinimum() * 50.0f);
        }
        else
        {
            return 50 + (int)(gain / control.getMaximum() * 50.0f);
        }
    }

    /**
     * Converts the slider value to a gain control value in dB, where 50 is the 0 dB (no gain) mid-point.
     */
    static float toGain(FloatControl control, int value)
    {
        if(value == 50)
        {
            return 0.0f;
        }
        else if(value < 50)
        {
            return (float)(50 - value) / 50.0f * control.getMinimum();
        }
        else
        {
            return (float)(value - 50) / 50.0f * control.getMaximum();
        }
    }
}
