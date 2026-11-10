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
package io.github.dsheirer.record.wave;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Provides the file path for a new baseband recording file.  Invoked each time a recorder creates a new file
 * (start, sample rate change, or file size rollover) so that time-stamped and sample-rate-bearing file names are
 * current.
 */
@FunctionalInterface
public interface IRecordingPathProvider
{
    /**
     * @param sampleRate of the recording
     * @return path for the new recording file
     * @throws IOException if a unique path can't be determined
     */
    Path getPath(float sampleRate) throws IOException;
}
