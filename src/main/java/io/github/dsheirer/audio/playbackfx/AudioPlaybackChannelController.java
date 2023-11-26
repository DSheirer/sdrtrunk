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

import io.github.dsheirer.audio.call.Call;
import io.github.dsheirer.source.mixer.MixerChannel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Controller for a single audio playback channel.
 */
public class AudioPlaybackChannelController implements ChangeListener<MediaPlayer.Status>
{
    private static final String EMPTY = new String("EMPTY");
    private static final String LOADED = new String("LOADED");

    private ObjectProperty<MediaPlayer> mMediaPlayer = new SimpleObjectProperty<>();
    private StringProperty mFrequency = new SimpleStringProperty();
    private StringProperty mFrom = new SimpleStringProperty();
    private StringProperty mFromAlias = new SimpleStringProperty();
    private StringProperty mName = new SimpleStringProperty();
    private StringProperty mSite = new SimpleStringProperty();
    private StringProperty mSystem = new SimpleStringProperty();
    private StringProperty mTo = new SimpleStringProperty();
    private StringProperty mToAlias = new SimpleStringProperty();
    private ObjectProperty<PlaybackMode> mPlaybackMode = new SimpleObjectProperty<>();
    private StringProperty mMediaPlayerStatus = new SimpleStringProperty(EMPTY);
    private Call mCall;
    private double mBalance;
    private List<IAudioPlaybackStatusListener> mAudioPlaybackStatusListeners = new ArrayList<>();
    private EndOfMediaRunnable mEndOfMediaRunnable = new EndOfMediaRunnable();
    private CallUpdateListener mCallUpdateListener = new CallUpdateListener();

    /**
     * Constructs an instance.
     * @param mixerChannel channel for this controller
     * @param balance to effect audio playback balance where -1.0 is full left and 1.0 is full right.
     */
    public AudioPlaybackChannelController(MixerChannel mixerChannel, double balance)
    {
        nameProperty().set(mixerChannel.getLabel());
        playbackModeProperty().set(PlaybackMode.AUTO);
        mBalance = balance;
    }

    /**
     * Status of the media player.
     */
    public StringProperty mediaPlayerStatusProperty()
    {
        return mMediaPlayerStatus;
    }

    /**
     * Indicates if this controller is locked for playback of a specific identifier.
     */
    public boolean isLocked()
    {
        return playbackModeProperty().get().equals(PlaybackMode.LOCKED);
    }

    /**
     * Indicates if this controller is (already) locked to the specified call.
     * @param call to check for lock match.
     * @return true if this controller is locked for the system and identifier of the call.
     */
    public boolean isLockedFor(Call call)
    {
        return playbackModeProperty().get().equals(PlaybackMode.LOCKED) &&
                toProperty().get() != null &&
                (toProperty().get().equals(call.getToId()) || toProperty().get().equals(call.getFromId())) &&
                ((systemProperty().get() == null && call.getSystem() == null) ||
                 (systemProperty().get() != null && systemProperty().get().equals(call.getSystem())));
    }

    /**
     * Indicates if this controller is set for an autoplay mode (AUTO or LOCKED) and currently doesn't have a call assigned.
     * @return true if available and has autoplay mode.
     */
    public boolean isAvailableForAutoPlay()
    {
        return playbackModeProperty().get().isAutoPlayable() && mCall == null;
    }

    /**
     * Registers the listener to be notified of audio playback status changes.
     * @param listener to be registered.
     */
    public void add(IAudioPlaybackStatusListener listener)
    {
        mAudioPlaybackStatusListeners.add(listener);
    }

    /**
     * Broadcasts an audio playback status change to registered listeners.
     */
    private void broadcastPlaybackStatus(MediaPlayer.Status previousStatus, MediaPlayer.Status currentStatus)
    {
        for(IAudioPlaybackStatusListener listener: mAudioPlaybackStatusListeners)
        {
            listener.playbackStatusUpdated(this, previousStatus, currentStatus);
        }
    }

    /**
     * Broadcasts an end-of-media event to registered listeners.
     */
    private void broadcastEndOfMedia()
    {
        for(IAudioPlaybackStatusListener listener: mAudioPlaybackStatusListeners)
        {
            listener.endOfMedia(this);
        }

        //If we're in AUTO or LOCKED modes, clear the media player at the end of playback.
        if(playbackModeProperty().get().isAutoPlayable())
        {
            clearMediaPlayerAndCall();
        }
    }

    /**
     * Sets playback mode to LOCKED for the identifier and system, or AUTO if the arguments are null.
     * @param identifier optional identifier for LOCKED playback mode.
     * @param system optional system name for LOCKED playback mode.
     */
    public void lockAutoTo(String identifier, String system)
    {
        Platform.runLater(() -> {
            clearMediaPlayerAndCall();
            playbackModeProperty().set(identifier == null ? PlaybackMode.AUTO : PlaybackMode.LOCKED);
            toProperty().set(identifier);
            //Only set the system value if the identifier is non-null
            systemProperty().set(identifier != null ? system : null);
        });
    }

    /**
     * Sets playback mode to AUTO and clears any currently playing audio.
     */
    public void auto()
    {
        lockAutoTo(null, null);
    }

    /**
     * Mutes this controller.
     */
    public void mute()
    {
        Platform.runLater(() -> {
            clearMediaPlayerAndCall();
            playbackModeProperty().set(PlaybackMode.MUTE);
        });
    }

    /**
     * Plays the specified call and sets playback mode to REPLAY
     * @param call to replay
     */
    public void replay(final Call call)
    {
        Platform.runLater(() -> {
            if(call != null && !call.equals(mCall))
            {
                clearMediaPlayerAndCall();
                mCall = call;
                mCall.playbackChannelProperty().set(nameProperty().get());
                updateCallMetadata();
                playbackModeProperty().set(PlaybackMode.REPLAY);
                loadMediaPlayer();
            }
            else if(call == null)
            {
                clearMediaPlayerAndCall();
            }
        });
    }

    /**
     * Plays the specified call.  If this controller is already playing a previous call, stops that call and proceeds
     * to play this call.  Does not change the replay mode.  At the end of media, closes the audio file and clears
     * the call and call metadata.
     * @param call to play
     */
    public void play(final Call call)
    {
        Platform.runLater(() -> {
            if(call != null && !call.equals(mCall))
            {
                clearMediaPlayerAndCall();
                mCall = call;
                mCall.playbackChannelProperty().set(nameProperty().get());
                loadMediaPlayer();
                updateCallMetadata();
            }
            else if(call == null)
            {
                clearMediaPlayerAndCall();
            }
        });
    }

    private void loadMediaPlayer()
    {
        if(mCall.getFile() != null)
        {
            //Create a new media player
            mMediaPlayer.set(new MediaPlayer(new Media(new File(mCall.getFile()).toURI().toString())));
            //Register status listener
            mMediaPlayer.get().statusProperty().addListener(this);
            //Register runnable for end-of-media event
            mMediaPlayer.get().onEndOfMediaProperty().set(mEndOfMediaRunnable);
            //Set audio channel balance
            mMediaPlayer.get().balanceProperty().set(mBalance);
            //Set the media player to auto-start replay
            mMediaPlayer.get().setAutoPlay(true);

            mMediaPlayerStatus.set(LOADED);

            //Register a listener on the call's last updated property for update notifications.
            mCall.lastUpdatedProperty().addListener(mCallUpdateListener);
        }
    }
    /**
     * Stops playback and clears the media player and nullifies the call.
     */
    private void clearMediaPlayerAndCall()
    {
        if(mCall != null)
        {
            mCall.lastUpdatedProperty().removeListener(mCallUpdateListener);
            mCall.playbackChannelProperty().set(null);
        }

        if(mMediaPlayer.get() != null)
        {
            mMediaPlayer.get().statusProperty().removeListener(this);
            mMediaPlayer.get().onEndOfMediaProperty().set(null);
            mMediaPlayer.get().stop();
            mMediaPlayer.set(null);
        }

        mCall = null;
        updateCallMetadata();
        mediaPlayerStatusProperty().set(EMPTY);
    }

    /**
     * Media player property for this controller.
     */
    public ObjectProperty<MediaPlayer> mediaPlayerProperty()
    {
        return mMediaPlayer;
    }

    /**
     * Updates any call metadata that has changed.
     */
    private void updateCallMetadata()
    {
        if(mCall != null)
        {
            //Only change TO and SYSTEM when we're not in locked mode
            if(!playbackModeProperty().get().equals(PlaybackMode.LOCKED))
            {
                updateProperty(toProperty(), mCall.getToId());
                updateProperty(systemProperty(), mCall.getSystem());
            }

            updateProperty(toAliasProperty(), mCall.getToAlias());
            updateProperty(fromProperty(), mCall.getFromId());
            updateProperty(fromAliasProperty(), mCall.getFromAlias());
            updateProperty(frequencyProperty(), String.valueOf(mCall.getFrequency()));
            updateProperty(siteProperty(), mCall.getSite());
        }
        else
        {
            //Only change TO and SYSTEM when we're not in locked mode
            if(!playbackModeProperty().get().equals(PlaybackMode.LOCKED))
            {
                updateProperty(toProperty(), null);
                updateProperty(systemProperty(), null);
            }

            updateProperty(toAliasProperty(), null);
            updateProperty(fromProperty(), null);
            updateProperty(fromAliasProperty(), null);
            updateProperty(frequencyProperty(), null);
            updateProperty(siteProperty(), null);
        }
    }

    /**
     * Updates the specified property if empty or if the value argument is different.
     * @param property to update
     * @param value that is updated.
     */
    private void updateProperty(StringProperty property, String value)
    {
        if(property.get() == null || !property.get().equals(value))
        {
            property.set(value);
        }
    }

    /**
     * From identifier property
     */
    public StringProperty fromProperty()
    {
        return mFrom;
    }

    /**
     * From identifier alias property
     */
    public StringProperty fromAliasProperty()
    {
        return mFromAlias;
    }

    /**
     * To identifier property
     */
    public StringProperty toProperty()
    {
        return mTo;
    }

    /**
     * To identifier alias property
     */
    public StringProperty toAliasProperty()
    {
        return mToAlias;
    }

    /**
     * Channel frequency property
     */
    public StringProperty frequencyProperty()
    {
        return mFrequency;
    }

    /**
     * Site property
     */
    public StringProperty siteProperty()
    {
        return mSite;
    }

    /**
     * System property
     */
    public StringProperty systemProperty()
    {
        return mSystem;
    }

    /**
     * Playback controller name property
     */
    public StringProperty nameProperty()
    {
        return mName;
    }

    /**
     * Playback mode property
     */
    public ObjectProperty<PlaybackMode> playbackModeProperty()
    {
        return mPlaybackMode;
    }

    /**
     * Implements the change listener interface for MediaPlayer status changes.
     */
    @Override
    public void changed(ObservableValue<? extends MediaPlayer.Status> observable, MediaPlayer.Status previousStatus,
                        MediaPlayer.Status currentStatus)
    {
        broadcastPlaybackStatus(previousStatus, currentStatus);
        mMediaPlayerStatus.set(currentStatus.name());
    }

    /**
     * Runnable to register with the media player to detect when the end of the media is detected.
     */
    private class EndOfMediaRunnable implements Runnable
    {
        @Override
        public void run()
        {
            broadcastEndOfMedia();
        }
    }

    /**
     * Monitors the call's last updated property to effect updates to this controller about the call state.
     */
    private class CallUpdateListener implements ChangeListener<Number>
    {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            updateCallMetadata();
        }
    }
}
