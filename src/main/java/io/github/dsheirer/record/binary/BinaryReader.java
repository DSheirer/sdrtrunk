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
package io.github.dsheirer.record.binary;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryReader implements Iterator<ByteBuffer>, AutoCloseable
{
    private final static Logger mLog = LoggerFactory.getLogger(BinaryReader.class);
    private int mBufferSize;
    private InputStream mInputStream;
    private Path mPath;
    private ByteBuffer mNextBuffer;
    private long mByteCounter;

    /**
     * Constructs a binary reader
     * @param path to the binary file
     * @param bufferSize for each buffer read from the file
     * @throws IOException
     */
    public BinaryReader(Path path, int bufferSize) throws IOException
    {
        mBufferSize = bufferSize;
        mPath = path;
        mInputStream = new BufferedInputStream(new FileInputStream(path.toFile()));
        getNext();
    }

    @Override
    public void close() throws Exception
    {
        if(mInputStream != null)
        {
            mInputStream.close();
            mInputStream = null;
            mPath = null;
        }
    }

    @Override
    public boolean hasNext()
    {
        return mNextBuffer != null;
    }

    /**
     * Returns a full reusable byte buffer, or at the end of the file this method may return a
     * reusable byte buffer that is less than the requested buffer size, containing the remaining
     * bytes from the file.
     *
     * @return
     */
    @Override
    public ByteBuffer next()
    {
        ByteBuffer current = mNextBuffer;
        getNext();
        return current;
    }

    /**
     * Total number of bytes read from file
     */
    public long getByteCounter()
    {
        return mByteCounter;
    }

    /**
     * Loads the next buffer
     */
    private void getNext()
    {
        try
        {
            byte[] readBytes = new byte[mBufferSize];
            int bytesRead = mInputStream.read(readBytes);
            mByteCounter += bytesRead;

            if(bytesRead < readBytes.length && bytesRead > 0)
            {
                readBytes = Arrays.copyOf(readBytes, bytesRead);
            }

            if(bytesRead > 0)
            {
                mNextBuffer = ByteBuffer.wrap(readBytes);
            }
            else
            {
                mNextBuffer = null;
            }
        }
        catch(IOException e)
        {
            mLog.error("Error reading binary file [" + mPath.toString() + "]", e);
            mNextBuffer = null;
        }
    }
}
