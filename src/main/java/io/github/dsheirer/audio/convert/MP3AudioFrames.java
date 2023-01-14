/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.audio.convert;

import java.util.List;

public class MP3AudioFrames extends AudioFrames
{

    /**
     * Create new MP3AudioFrame
     * @param audioDuration total duration in milliseconds
     * @param audioFrames series of audio frames, split on frame boundaries
     */
    public MP3AudioFrames(int audioDuration, List<byte[]> audioFrames)
    {
        super(audioDuration, audioFrames);
    }

    /**
     * Get current frame duration
     * @return frame duration in milliseconds
     */
    public int getCurrentFrameDuration()
    {
        return MP3Header.getFrameDuration(getCurrentFrame(), 0);
    }

}
