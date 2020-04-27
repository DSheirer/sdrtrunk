/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.record.binary;

import io.github.dsheirer.sample.buffer.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinaryReader implements Iterator<ByteBuffer>, AutoCloseable
{
    private final static Logger mLog = LoggerFactory.getLogger(BinaryReader.class);
    private Object mBufferQueue = new Object() {
        /**
         * Disposes of any reclaimed buffers to prepare this queue for disposal.
         */
        public void dispose()
        {
        }
    };
    private int mBufferSize;
    private InputStream mInputStream;
    private Path mPath;
    private ByteBuffer mNextBuffer;
    private TimestampTracker mTimestampTracker = new TimestampTracker();

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
        mTimestampTracker.processFileName(path.toString());
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
     * Loads the next buffer
     */
    private void getNext()
    {
        try
        {
            ByteBuffer buffer1 = new ByteBuffer(new byte[mBufferSize]);
            mNextBuffer = buffer1;

            int bytesRead = mInputStream.read(mNextBuffer.getBytes(), 0, mBufferSize);

            mNextBuffer.setTimestamp(mTimestampTracker.getTimestamp());
            mTimestampTracker.updateBytesProcessed(bytesRead);

            if(bytesRead > 0)
            {
                if(bytesRead < mBufferSize)
                {
                    ByteBuffer buffer = new ByteBuffer(new byte[bytesRead]);
                    ByteBuffer partialBuffer = buffer;
                    System.arraycopy(mNextBuffer.getBytes(), 0, partialBuffer.getBytes(), 0, bytesRead);
                    partialBuffer.setTimestamp(mNextBuffer.getTimestamp());

                    mNextBuffer = partialBuffer;
                }
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

    public class TimestampTracker
    {
        private final Pattern TIMESTAMP_BITRATE_PATTERN = Pattern.compile(".*(\\d{8}_\\d{6})_(\\d{4,8})BPS_.*.bits");
        private final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
        private Double mBitRate = null;
        private long mTimestamp;

        public TimestampTracker()
        {
        }

        /**
         * Processes the file name to attempt to extract the file starting timestamp and bit rate.
         * @param filename
         */
        public void processFileName(String filename)
        {
            try
            {
                Matcher m = TIMESTAMP_BITRATE_PATTERN.matcher(filename);

                if(m.find())
                {
                    long timestamp = SDF.parse(m.group(1)).getTime();
                    int bitRate = Integer.parseInt(m.group(2));

                    setTimestamp(timestamp);
                    setBitRate(bitRate);
                    return;
                }
            }
            catch(Exception e)
            {
                mLog.error("Couldn't parse timestamp and bit rate from filename [" + filename + "]");
            }
        }

        /**
         * Sets the expected bit rate (ie bits per second) for the tracker
         */
        public void setBitRate(int bitRate)
        {
            mBitRate = (double)bitRate;
        }

        /**
         * Sets the timestamp for the tracker, otherwise current system time is assumed.
         */
        public void setTimestamp(long timestamp)
        {
            mTimestamp = timestamp;
        }

        /**
         * Gets the currently tracked timestamp when bits per second has been previously specified, otherwise
         * returns current system time.
         *
         * @return current tracked timestamp or current system time.
         */
        public long getTimestamp()
        {
            if(mBitRate != null)
            {
                return mTimestamp;
            }

            return System.currentTimeMillis();
        }

        /**
         * Updates the timestamp based on the number of bytes processed and the specified bit rate.
         *
         * @param bytesProcessed thus far
         */
        public void updateBytesProcessed(int bytesProcessed)
        {
            if(mBitRate != null)
            {
                mTimestamp += (long)(((double)bytesProcessed * 8.0) / mBitRate * 1000.0);
            }
        }
    }
}
