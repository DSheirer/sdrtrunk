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
package io.github.dsheirer.gui.calls;

import io.github.dsheirer.audio.playback.AudioChannel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.audio.playback.ReplaySource;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.record.RecordingReader;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Replays call recordings in the Calls tab through the sdrtrunk audio channels (the same audio output, channels,
 * mute and level meters as live calls).  The audio playback manager chooses the channel: an idle channel, otherwise
 * the left (or mono) channel, which drops the live call that it is playing.
 */
public class CallPlayer extends HBox
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CallPlayer.class);
    private final AudioPlaybackManager mAudioPlaybackManager;
    private final Button mPlayButton = new Button();
    private final IconNode mPlayIcon = new IconNode(FontAwesome.PLAY);
    private final Button mStopButton = new Button();
    private final Slider mPositionSlider = new Slider(0, 1, 0);
    private final Label mTimeLabel = new Label("0:00 / 0:00");
    private final Label mCallLabel = new Label("Double-click a recorded call to replay it");
    private final CheckBox mAutoPlayCheckBox = new CheckBox("Auto-play next");
    private final ObjectProperty<CallRecord> mCurrentCall = new SimpleObjectProperty<>();
    private final Timeline mProgressTimeline;
    private CallAliasResolver mAliasResolver = new CallAliasResolver(null);
    private Consumer<CallRecord> mFinishedListener;
    private ReplaySource mReplay;
    private long mLoadSequence;

    /**
     * Constructs an instance
     * @param audioPlaybackManager that replays the calls, or null to disable replay
     */
    public CallPlayer(AudioPlaybackManager audioPlaybackManager)
    {
        super(8);
        mAudioPlaybackManager = audioPlaybackManager;
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("calls-player");

        mPlayIcon.setIconSize(14);
        mPlayIcon.fillProperty().bind(mPlayButton.textFillProperty());
        mPlayButton.setGraphic(mPlayIcon);
        mPlayButton.setTooltip(new Tooltip("Pause / resume"));
        mPlayButton.setOnAction(event -> togglePause());

        IconNode stopIcon = new IconNode(FontAwesome.STOP);
        stopIcon.setIconSize(14);
        stopIcon.fillProperty().bind(mStopButton.textFillProperty());
        mStopButton.setGraphic(stopIcon);
        mStopButton.setTooltip(new Tooltip("Stop the replay"));
        mStopButton.setOnAction(event -> stop());

        mPositionSlider.setPrefWidth(260);
        mPositionSlider.setMinWidth(100);
        mPositionSlider.valueChangingProperty().addListener((obs, old, changing) -> {
            if(!changing)
            {
                seek();
            }
        });
        mPositionSlider.setOnMouseReleased(event -> seek());

        mTimeLabel.setMinWidth(Label.USE_PREF_SIZE);
        mCallLabel.setMinWidth(0);
        mAutoPlayCheckBox.setTooltip(new Tooltip("When a replay finishes, replay the next newer recorded call"));
        mAutoPlayCheckBox.setMinWidth(Label.USE_PREF_SIZE);
        HBox.setHgrow(mCallLabel, Priority.ALWAYS);
        mCallLabel.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(mPlayButton, mStopButton, mPositionSlider, mTimeLabel, mCallLabel, mAutoPlayCheckBox);

        mProgressTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> updateProgress()));
        mProgressTimeline.setCycleCount(Timeline.INDEFINITE);
        updateControls();
    }

    /**
     * Sets the resolver for the current TO and FROM aliases shown for the replayed call
     */
    public void setAliasResolver(CallAliasResolver aliasResolver)
    {
        mAliasResolver = aliasResolver;
    }

    /**
     * Registers a listener for when a replay plays to the end (used for auto-play of the next call)
     */
    public void setFinishedListener(Consumer<CallRecord> listener)
    {
        mFinishedListener = listener;
    }

    /**
     * Indicates if auto-play of the next call is enabled
     */
    public boolean isAutoPlay()
    {
        return mAutoPlayCheckBox.isSelected();
    }

    /**
     * Call that is loaded in the player, or null
     */
    public ObjectProperty<CallRecord> currentCallProperty()
    {
        return mCurrentCall;
    }

    /**
     * Replays the recording of the call.  The recording is read on a background thread.  FX thread.
     * @return true if the replay is starting
     */
    public boolean play(CallRecord call)
    {
        if(call == null || !call.isPlayable())
        {
            return false;
        }

        if(mAudioPlaybackManager == null || mAudioPlaybackManager.getAudioChannels().isEmpty())
        {
            mCallLabel.setText("No audio output is available for replay");
            return false;
        }

        Path path = Path.of(call.getFilePath());

        if(!Files.exists(path))
        {
            mCallLabel.setText("Recording file not found: " + path.getFileName());
            return false;
        }

        stopReplay();
        long sequence = ++mLoadSequence;
        String description = describe(call);
        mCurrentCall.set(call);
        mCallLabel.setText("Loading " + description);
        updateControls();

        CompletableFuture.supplyAsync(() -> {
            try
            {
                return RecordingReader.read(path);
            }
            catch(Exception e)
            {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }).whenComplete((samples, error) -> Platform.runLater(() -> {
            if(sequence != mLoadSequence)
            {
                return; //Another call was selected while this one was loading
            }

            if(error != null)
            {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                LOGGER.warn("Unable to replay recording [" + path + "] - " + cause.getMessage());
                mCallLabel.setText("Unable to replay " + path.getFileName() + ": " + cause.getMessage());
                mCurrentCall.set(null);
                updateControls();
                return;
            }

            ReplaySource replay = new ReplaySource(samples, call.getId(), description);
            int channelIndex = mAudioPlaybackManager.replay(replay);

            if(channelIndex < 0)
            {
                mCallLabel.setText("No audio output is available for replay");
                mCurrentCall.set(null);
                updateControls();
                return;
            }

            mReplay = replay;
            mCallLabel.setText(getChannelLabel(channelIndex) + description);
            mProgressTimeline.play();
            updateControls();
        }));

        return true;
    }

    private String getChannelLabel(int channelIndex)
    {
        List<AudioChannel> channels = mAudioPlaybackManager.getAudioChannels();

        if(channels.size() > 1 && channelIndex < channels.size())
        {
            return channels.get(channelIndex).getChannelName() + ":  ";
        }

        return "";
    }

    private String describe(CallRecord call)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(CallsView.formatTime(call.getStartTime()));

        if(call.getToId() != null)
        {
            sb.append("  TO:").append(call.getToId());
            String toAlias = mAliasResolver.getToAliasName(call);

            if(toAlias != null)
            {
                sb.append(" ").append(toAlias);
            }
        }

        if(call.getFromId() != null)
        {
            sb.append("  FROM:").append(call.getFromId());
            String fromAlias = mAliasResolver.getFromAliasName(call);

            if(fromAlias != null)
            {
                sb.append(" ").append(fromAlias);
            }
        }

        return sb.toString();
    }

    private void togglePause()
    {
        if(mReplay != null)
        {
            mReplay.setPaused(!mReplay.isPaused());
            updateControls();
        }
    }

    /**
     * Stops the replay.  FX thread.
     */
    public void stop()
    {
        mLoadSequence++;
        stopReplay();
        mCurrentCall.set(null);
        mCallLabel.setText("Double-click a recorded call to replay it");
        mPositionSlider.setValue(0);
        mTimeLabel.setText("0:00 / 0:00");
        updateControls();
    }

    private void stopReplay()
    {
        mProgressTimeline.stop();

        if(mReplay != null)
        {
            mReplay.stop();
            mReplay = null;
        }
    }

    private void seek()
    {
        if(mReplay != null)
        {
            mReplay.seek(mPositionSlider.getValue());
        }
    }

    /**
     * Updates the position display and detects the end of the replay.  FX thread.
     */
    private void updateProgress()
    {
        ReplaySource replay = mReplay;

        if(replay == null)
        {
            mProgressTimeline.stop();
            return;
        }

        if(!mPositionSlider.isValueChanging())
        {
            mPositionSlider.setValue(replay.getFraction());
        }

        mTimeLabel.setText(format(replay.getPositionMillis()) + " / " + format(replay.getDurationMillis()));

        if(replay.isFinished())
        {
            CallRecord finished = mCurrentCall.get();
            boolean completed = replay.isCompleted();
            stop();

            if(completed && mFinishedListener != null && finished != null)
            {
                mFinishedListener.accept(finished);
            }
        }
    }

    private static String format(long millis)
    {
        long seconds = Math.max(0, millis / 1000);
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private void updateControls()
    {
        boolean loaded = mReplay != null;
        mPlayButton.setDisable(!loaded);
        mStopButton.setDisable(!loaded && mCurrentCall.get() == null);
        mPositionSlider.setDisable(!loaded);
        mPlayIcon.setIconCode(loaded && !mReplay.isPaused() ? FontAwesome.PAUSE : FontAwesome.PLAY);
    }

    /**
     * Stops the replay and releases resources.
     */
    public void dispose()
    {
        stop();
    }
}
