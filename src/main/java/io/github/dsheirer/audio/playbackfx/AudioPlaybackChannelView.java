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

package io.github.dsheirer.audio.playbackfx;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.elusive.Elusive;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Abstract audio playback channel view
 */
public abstract class AudioPlaybackChannelView extends VBox
{
    protected static final String CSS_STYLE_FONT_LARGE = "-fx-font-size:16;";
    protected static final String CSS_STYLE_FONT_LARGE_BOLD = CSS_STYLE_FONT_LARGE + "-fx-font-weight: bold;";
    protected static final String TOOLTIP_MUTED = "Audio channel muted.  Click to unmute audio channel";
    protected static final String TOOLTIP_UNMUTED = "Audio channel not muted.  Click to mute audio channel";
    protected static final String TOOLTIP_LOCKED = "Audio channel playback locked to talkgroup.  Click to unlock audio channel";
    protected static final String TOOLTIP_UNLOCKED = "Audio channel playback unlocked.  Click to lock audio channel to current talkgroup";
    protected static final Color ICON_COLOR = Color.BLACK;
    protected static final int ICON_SIZE = 20;
    private AudioPlaybackChannelController mController;
    private Label mFromLabel;
    private Label mFromAliasLabel;
    private Label mToLabel;
    private Label mToAliasLabel;
    private Label mPlaybackMode;
    private Label mMediaPlayerStatus;
    private Label mMuteLabel;
    private Label mLockLabel;
    private Label mSystemLabel;

    /**
     * Constructs an instance.
     * @param controller to use for this view.
     */
    public AudioPlaybackChannelView(AudioPlaybackChannelController controller)
    {
        mController = controller;
        getController().playbackModeProperty().addListener((observable, oldValue, newValue) -> updateLockMuteState());
        getToLabel().textProperty().addListener((observable, oldValue, newValue) -> updateLockMuteState());
    }

    /**
     * Perform any actions related to resizing the width of the panel.
     */
    public abstract void resetWidth();

    /**
     * Controller for this view.
     * @return controller
     */
    protected AudioPlaybackChannelController getController()
    {
        return mController;
    }

    /**
     * Updates the state of the lock and mute labels (ie buttons).
     */
    protected void updateLockMuteState()
    {
        switch(getController().playbackModeProperty().get())
        {
            case MUTE:
                setMuted(true);
                setLocked(false, true);
                break;
            case AUTO:
            case REPLAY:
                setMuted(false);
                setLocked(false, getToLabel().textProperty().get() == null);
                break;
            case LOCKED:
                setMuted(false);
                setLocked(true, false);
                break;
        }
    }

    /**
     * Sets the state of the lock label.
     * @param locked true to show locked state, false otherwise
     * @param disabled true to disable the label, false otherwise
     */
    private void setLocked(boolean locked, boolean disabled)
    {
        IconNode iconNode = new IconNode(locked ? Elusive.LOCK : Elusive.UNLOCK);
        iconNode.setIconSize(ICON_SIZE);
        iconNode.setFill(disabled ? Color.DARKGRAY : ICON_COLOR);
        getLockLabel().setGraphic(iconNode);
        getLockLabel().setTooltip(new Tooltip(locked ? TOOLTIP_LOCKED : TOOLTIP_UNLOCKED));
        getLockLabel().setDisable(disabled);
    }

    /**
     * Sets the state of the mute label.
     * @param locked true to show muted state, false otherwise
     */
    private void setMuted(boolean muted)
    {
        IconNode iconNode = new IconNode(muted ? Elusive.VOLUME_OFF : Elusive.VOLUME_UP);
        iconNode.setIconSize(ICON_SIZE);
        iconNode.setFill(muted ? Color.RED : Color.BLACK);
        getMuteLabel().setGraphic(iconNode);
        getMuteLabel().setTooltip(new Tooltip(muted ? TOOLTIP_MUTED : TOOLTIP_UNMUTED));
    }

    protected Label getFromLabel()
    {
        if(mFromLabel == null)
        {
            mFromLabel = new Label();
            mFromLabel.setStyle(CSS_STYLE_FONT_LARGE_BOLD);
            mFromLabel.textProperty().bind(getController().fromProperty());
        }

        return mFromLabel;
    }

    protected Label getFromAliasLabel()
    {
        if(mFromAliasLabel == null)
        {
            mFromAliasLabel = new Label();
            mFromAliasLabel.setStyle(CSS_STYLE_FONT_LARGE);
            mFromAliasLabel.textProperty().bind(getController().fromAliasProperty());
        }

        return mFromAliasLabel;
    }

    protected Label getSystemLabel()
    {
        if(mSystemLabel == null)
        {
            mSystemLabel = new Label();
            mSystemLabel.textProperty().bind(getController().systemProperty());
        }

        return mSystemLabel;
    }

    protected Label getToLabel()
    {
        if(mToLabel == null)
        {
            mToLabel = new Label();
            mToLabel.setStyle(CSS_STYLE_FONT_LARGE_BOLD);
            mToLabel.textProperty().bind(getController().toProperty());
        }

        return mToLabel;
    }

    protected Label getToAliasLabel()
    {
        if(mToAliasLabel == null)
        {
            mToAliasLabel = new Label();
            mToAliasLabel.setStyle(CSS_STYLE_FONT_LARGE);
            mToAliasLabel.textProperty().bind(getController().toAliasProperty());
        }

        return mToAliasLabel;
    }

    protected Label getMuteLabel()
    {
        if(mMuteLabel == null)
        {
            mMuteLabel = new Label();
            mMuteLabel.setTooltip(new Tooltip(TOOLTIP_UNMUTED));
            mMuteLabel.setPadding(new Insets(1));
            IconNode iconNode = new IconNode(Elusive.VOLUME_UP);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(ICON_COLOR);
            mMuteLabel.setGraphic(iconNode);
            mMuteLabel.setOnMouseClicked(event -> {
                if(getController().playbackModeProperty().get().equals(PlaybackMode.MUTE))
                {
                    getController().auto();
                }
                else
                {
                    getController().mute();
                }
            });
        }

        return mMuteLabel;
    }

    protected Label getLockLabel()
    {
        if(mLockLabel == null)
        {
            mLockLabel = new Label();
            mLockLabel.setPadding(new Insets(1));
            IconNode iconNode = new IconNode(FontAwesome.UNLOCK);
            iconNode.setIconSize(ICON_SIZE);
            iconNode.setFill(Color.DARKGRAY);
            mLockLabel.setGraphic(iconNode);
            mLockLabel.setOnMouseClicked(event -> {
                if(getController().playbackModeProperty().get().equals(PlaybackMode.LOCKED))
                {
                    getController().auto();
                }
                else
                {
                    String to = getToLabel().textProperty().get();
                    String system = getSystemLabel().textProperty().get();
                    getController().lockAutoTo(to, system);
                }
            });
        }

        return mLockLabel;
    }

    protected Label getPlaybackMode()
    {
        if(mPlaybackMode == null)
        {
            mPlaybackMode = new Label();
            mPlaybackMode.textProperty().bind(getController().playbackModeProperty().map(playbackMode -> switch(playbackMode)
            {
                case AUTO -> "AUTO";
                case LOCKED -> "AUTO-LOCKED";
                case REPLAY -> "REPLAY";
                case MUTE -> "MUTED";
            }));
        }

        return mPlaybackMode;
    }

    protected Label getMediaPlayerStatus()
    {
        if(mMediaPlayerStatus == null)
        {
            mMediaPlayerStatus = new Label();
            mMediaPlayerStatus.setDisable(true);
            mMediaPlayerStatus.textProperty().bind(getController().mediaPlayerStatusProperty());
        }

        return mMediaPlayerStatus;
    }
}
