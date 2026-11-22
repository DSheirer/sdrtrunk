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

import io.github.dsheirer.record.BasebandRecordFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;

/**
 * Writer for baseband (complex I/Q) sample recordings.
 */
public interface IBasebandRecordingWriter extends AutoCloseable
{
    /**
     * Writes the buffer contents to the recording.  Assumes the buffer is full, with the first byte at position 0
     * and samples are interleaved, signed 16-bit little-endian I/Q.
     */
    void writeData(ByteBuffer buffer) throws IOException;

    /**
     * Closes the recording.
     */
    @Override
    void close() throws IOException;

    /**
     * Creates a writer for the specified format.
     * @param format of the recording file
     * @param audioFormat describing the sample rate and sample size (2-channel, 16-bit)
     * @param path for the recording file
     * @return opened writer
     * @throws IOException if the file can't be created
     */
    static IBasebandRecordingWriter create(BasebandRecordFormat format, AudioFormat audioFormat, Path path)
        throws IOException
    {
        return switch(format)
        {
            case SDRSHARP_IQ -> new IQWriter(path);
            case WAVE -> new WaveWriter(audioFormat, path);
        };
    }
}
