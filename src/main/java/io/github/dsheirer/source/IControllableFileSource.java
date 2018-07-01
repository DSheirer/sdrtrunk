/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Defines a controllable source that can be manually controlled for stepping
 * through the file.
 */
public interface IControllableFileSource
{
    /**
     * Opens the file source
     */
    public void open() throws IOException, UnsupportedAudioFileException;

    public void close() throws IOException;

    public File getFile();

    public void next(int frames) throws IOException;

    public void next(int frames, boolean broadcast) throws IOException;

    public long getFrameCount() throws IOException;

    public double getSampleRate();

    public void setListener(IFrameLocationListener listener);

    public void removeListener(IFrameLocationListener listener);
}
