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

package io.github.dsheirer.audio.playbackfx;

import javafx.scene.media.MediaPlayer;

/**
 * Listener interface for audio playback status events.
 */
public interface IAudioPlaybackStatusListener
{
    /**
     * Notify the listener that the status of the media player has changed.
     * @param controller for the media player
     * @param previousStatus of the media player.
     * @param currentStatus of the media player.
     */
    void playbackStatusUpdated(AudioPlaybackChannelController controller, MediaPlayer.Status previousStatus, MediaPlayer.Status currentStatus);

    /**
     * Notifies the listener that current playback encountered the end of the audio media.
     */
    void endOfMedia(AudioPlaybackChannelController controller);
}
