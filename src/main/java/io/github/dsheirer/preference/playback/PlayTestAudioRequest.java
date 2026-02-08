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

package io.github.dsheirer.preference.playback;

/**
 * Request to play a test tone over the currently employed audio playback device.
 *
 * @param audio to play (160 samples in length = 20 milliseconds)
 * @param channel number for the audio channel (0 = mono/left, 1 = right, etc.)
 */
public record PlayTestAudioRequest(float[] audio, int channel)
{
    public static final int ALL_CHANNELS = -1;

    public boolean isAllChannels()
    {
        return channel == ALL_CHANNELS;
    }
};
