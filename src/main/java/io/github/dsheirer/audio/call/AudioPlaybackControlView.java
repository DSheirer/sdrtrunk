/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.audio.call;

import io.github.dsheirer.audio.playbackfx.AudioPlaybackChannelController;
import io.github.dsheirer.audio.playbackfx.IAudioPlaybackStatusListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Audio playback controller.
 */
public class AudioPlaybackControlView extends HBox implements IAudioPlaybackStatusListener
{
    private static final int ICON_SIZE = 16;
    private ObjectProperty<MediaPlayer> mMediaPlayer = new SimpleObjectProperty<>();
    private ToggleButton mAutoRepeatButton;
    private ToggleGroup mPlaybackGroup;
    private ToggleButton mPlayButton;
    private ToggleButton mPauseButton;
    private ToggleButton mStopButton;
    private boolean mUpdating = false;

    /**
     * Constructs an instance
     */
    public AudioPlaybackControlView()
    {
        setSpacing(2);
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setPadding(new Insets(0, 5, 0 , 7));
        getChildren().addAll(getStopButton(), getPlayButton(), getPauseButton(), getAutoRepeatButton());
        mediaPlayerProperty().addListener((observable, oldValue, newValue) -> updateControls());
    }

    /**
     * Implements the IAudioPLaybackStatusListener method to click the stop button when end of media is detected.
     * @param controller where this occurred.
     */
    @Override
    public void endOfMedia(AudioPlaybackChannelController controller)
    {
        getStopButton().setSelected(true);
    }

    @Override
    public void playbackStatusUpdated(AudioPlaybackChannelController controller, MediaPlayer.Status previousStatus,
                                      MediaPlayer.Status currentStatus)
    {
        if(currentStatus != null)
        {
            switch(currentStatus)
            {
                case UNKNOWN:
                    break;
                case READY:
                    break;
                case PAUSED:
                    mUpdating = true;
                    getPauseButton().setSelected(true);
                    mUpdating = false;
                    break;
                case PLAYING:
                    mUpdating = true;
                    getPlayButton().setSelected(true);
                    mUpdating = false;
                    break;
                case STOPPED:
                    mUpdating = true;
                    getStopButton().setSelected(true);
                    mUpdating = false;
                    if(getAutoRepeatButton().isSelected())
                    {
                        getPlayButton().setSelected(true);
                    }
                    break;
                case STALLED:
                case HALTED:
                case DISPOSED:
                    System.out.println("Playback is " + currentStatus);
                    break;
            }
        }
    }

    /**
     * Update the controls when the media player changes
     */
    private void updateControls()
    {
        boolean disable = mediaPlayerProperty().get() == null;
        getStopButton().setDisable(disable);
        getPlayButton().setDisable(disable);
        getPauseButton().setDisable(disable);
        getAutoRepeatButton().setDisable(disable);
    }

    /**
     * Media player property that can be bound to a media player controller.
     */
    public ObjectProperty<MediaPlayer> mediaPlayerProperty()
    {
        return mMediaPlayer;
    }

    /**
     * Playback toggle buttons group (play, stop, and pause)
     */
    private ToggleGroup getPlaybackGroup()
    {
        if(mPlaybackGroup == null)
        {
            mPlaybackGroup = new ToggleGroup();
            mPlaybackGroup.selectedToggleProperty().addListener((observable, oldValue, selectedToggle) -> {
                MediaPlayer mediaPlayer = mediaPlayerProperty().get();

                if(!mUpdating && mediaPlayer != null)
                {
                    if(getPlayButton().equals(selectedToggle))
                    {
                        mediaPlayer.play();
                    }
                    else if(getPauseButton().equals(selectedToggle))
                    {
                        mediaPlayer.pause();
                    }
                    else if(getStopButton().equals(selectedToggle))
                    {
                        mediaPlayer.stop();
                    }
                }
            });
        }

        return mPlaybackGroup;
    }

    /**
     * Play button
     */
    private ToggleButton getPlayButton()
    {
        if(mPlayButton == null)
        {
            mPlayButton = new ToggleButton();
            mPlayButton.setToggleGroup(getPlaybackGroup());
            mPlayButton.setTooltip(new Tooltip("Play Audio"));
            mPlayButton.setDisable(true);
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mPlayButton.setGraphic(iconNode);
        }

        return mPlayButton;
    }

    /**
     * Pause button
     */
    private ToggleButton getPauseButton()
    {
        if(mPauseButton == null)
        {
            mPauseButton = new ToggleButton();
            mPauseButton.setToggleGroup(getPlaybackGroup());
            mPauseButton.setTooltip(new Tooltip("Pause Audio Playback"));
            mPauseButton.setDisable(true);
            IconNode iconNode = new IconNode(FontAwesome.PAUSE);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mPauseButton.setGraphic(iconNode);
        }

        return mPauseButton;
    }

    /**
     * Stop button
     */
    private ToggleButton getStopButton()
    {
        if(mStopButton == null)
        {
            mStopButton = new ToggleButton();
            mStopButton.setToggleGroup(getPlaybackGroup());
            mStopButton.setTooltip(new Tooltip("Stop Audio Playback"));
            mStopButton.setDisable(true);
            IconNode iconNode = new IconNode(FontAwesome.STOP);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mStopButton.setGraphic(iconNode);
        }

        return mStopButton;
    }

    private ToggleButton getAutoRepeatButton()
    {
        if(mAutoRepeatButton == null)
        {
            mAutoRepeatButton = new ToggleButton();
            mAutoRepeatButton.setTooltip(new Tooltip("Auto-repeat audio playback"));
            mAutoRepeatButton.setDisable(true);
            IconNode iconNode = new IconNode(FontAwesome.REPEAT);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.BLACK);
            mAutoRepeatButton.setGraphic(iconNode);
        }

        return mAutoRepeatButton;
    }
}
