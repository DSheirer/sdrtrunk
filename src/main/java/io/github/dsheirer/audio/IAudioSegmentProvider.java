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

package io.github.dsheirer.audio;

import io.github.dsheirer.sample.Listener;

/**
 * Interface for audio segment provider
 */
public interface IAudioSegmentProvider
{
	/**
	 * Registers the audio segment listener
	 * @param listener of audio segments
	 */
	void setAudioSegmentListener(Listener<AudioSegment> listener);

	/**
	 * De-registers the audio segment listener
	 */
	void removeAudioSegmentListener();
}
