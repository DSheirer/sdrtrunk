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

package io.github.dsheirer.audio;

import io.github.dsheirer.audio.playback.AudioChannel;
import io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor;
import io.github.dsheirer.sample.Listener;
import java.util.List;

/**
 * Interface for controlling one or more audio channels
 */
public interface IAudioController
{
	/* Current Mixer and MixerChannel configuration */
	public void setAudioPlaybackDevice(AudioPlaybackDeviceDescriptor device ) throws AudioException;
	public AudioPlaybackDeviceDescriptor getAudioPlaybackDevice() throws AudioException;
	
	/* Audio Channel(s) */
	public List<AudioChannel> getAudioChannels();

	/* Controller Audio Event Listener */
	public void addAudioEventListener(Listener<AudioEvent> listener );
	public void removeAudioEventListener(Listener<AudioEvent> listener );
}
