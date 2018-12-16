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

import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import io.github.dsheirer.sample.buffer.ReusableByteBufferQueue;
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

public class BinaryReader implements Iterator<ReusableByteBuffer>, AutoCloseable
{
    private final static Logger mLog = LoggerFactory.getLogger(BinaryReader.class);
    private ReusableByteBufferQueue mBufferQueue = new ReusableByteBufferQueue("Binary Reader");
    private int mBufferSize;
    private InputStream mInputStream;
    private Path mPath;
    private ReusableByteBuffer mNextBuffer;
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
    public ReusableByteBuffer next()
    {
        ReusableByteBuffer current = mNextBuffer;
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
            mNextBuffer = mBufferQueue.getBuffer(mBufferSize);

            int bytesRead = mInputStream.read(mNextBuffer.getBytes(), 0, mBufferSize);

            mNextBuffer.setTimestamp(mTimestampTracker.getTimestamp());
            mTimestampTracker.updateBytesProcessed(bytesRead);

            if(bytesRead > 0)
            {
                if(bytesRead < mBufferSize)
                {
                    ReusableByteBuffer partialBuffer = mBufferQueue.getBuffer(bytesRead);
                    System.arraycopy(mNextBuffer.getBytes(), 0, partialBuffer.getBytes(), 0, bytesRead);
                    partialBuffer.setTimestamp(mNextBuffer.getTimestamp());
                    mNextBuffer.decrementUserCount();
                    mNextBuffer = partialBuffer;
                }
            }
            else
            {
                mNextBuffer.decrementUserCount();
                mNextBuffer = null;
            }
        }
        catch(IOException e)
        {
            mLog.error("Error reading binary file [" + mPath.toString() + "]", e);
            mNextBuffer.decrementUserCount();
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
