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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.commons.lang3.Validate;

/**
 * Writer for SDRSharp compatible raw I/Q (.iq) recordings: headerless, interleaved, signed 16-bit little-endian
 * I/Q samples.  Recording metadata (start time, tuner, frequency and sample rate) is carried in the file name.
 *
 * @see io.github.dsheirer.source.wave.SDRSharpIQRecording
 */
public class IQWriter implements IBasebandRecordingWriter
{
    private final Path mFile;
    private final FileChannel mFileChannel;

    /**
     * Constructs an instance and creates the (new) file.
     * @param file to create
     * @throws IOException if the file already exists or can't be created
     */
    public IQWriter(Path file) throws IOException
    {
        Validate.notNull(file, "File cannot be null");
        mFile = file;
        mFileChannel = FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }

    public Path getFile()
    {
        return mFile;
    }

    @Override
    public void writeData(ByteBuffer buffer) throws IOException
    {
        buffer.position(0);

        while(buffer.hasRemaining())
        {
            mFileChannel.write(buffer);
        }
    }

    @Override
    public void close() throws IOException
    {
        if(mFileChannel.isOpen())
        {
            mFileChannel.force(true);
            mFileChannel.close();
        }
    }
}
