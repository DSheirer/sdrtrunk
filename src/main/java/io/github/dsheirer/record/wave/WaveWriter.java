/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.record.wave;

import org.apache.commons.lang3.Validate;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaveWriter implements AutoCloseable
{
    public static final String RIFF_ID = "RIFF";
    public static final int INITIAL_TOTAL_LENGTH = 4;
    public static final String WAVE_ID = "WAVE";

    public static final String FORMAT_CHUNK_ID = "fmt ";
    public static final int FORMAT_CHUNK_LENGTH = 16;
    public static final short FORMAT_UNCOMPRESSED_PCM = 1;

    public static final String DATA_CHUNK_ID = "data";

    private static final Pattern FILENAME_PATTERN = Pattern.compile("(.*_)(\\d+)(\\.tmp)");
    public static final long MAX_WAVE_SIZE = 2l * (long)Integer.MAX_VALUE;

    private AudioFormat mAudioFormat;
    private int mFileRolloverCounter = 1;
    private long mMaxSize;
    private Path mFile;
    private FileChannel mFileChannel;
    private boolean mDataChunkOpen = false;
    private long mDataChunkSizeOffset = 0;
    private int mDataChunkSize = 0;

    /**
     * Constructs a new wave writer that is open with a complete header, ready
     * for writing buffers of PCM sample data.
     *
     * Each time the maximum file size is reached, a new file is created with a
     * series suffix appended to the file name.
     *
     * @param format - audio format (channels, sample size, sample rate)
     * @param file - wave file to write
     * @param maxSize - maximum file size ( range: 1 - 4,294,967,294 bytes )
     * @throws IOException - if there are any IO issues
     */
    public WaveWriter(AudioFormat format, Path file, long maxSize) throws IOException
    {
        Validate.isTrue(format != null);
        Validate.isTrue(file != null);

        mAudioFormat = format;
        mFile = file;

        if(0 < maxSize && maxSize <= MAX_WAVE_SIZE)
        {
            mMaxSize = maxSize;
        }
        else
        {
            mMaxSize = MAX_WAVE_SIZE;
        }

        open();
    }

    /**
     * Constructs a new wave writer that is open with a complete header, ready
     * for writing buffers of PCM sample data.  The maximum file size is limited
     * to the max size specified in the wave file format: max unsigned integer
     *
     * @param format - audio format (channels, sample size, sample rate)
     * @param file - wave file to write
     * @throws IOException - if there are any IO issues
     */
    public WaveWriter(AudioFormat format, Path file) throws IOException
    {
        this(format, file, Integer.MAX_VALUE * 2);
    }

    /**
     * Opens the file and writes a wave header.
     */
    private void open() throws IOException
    {
        int version = 2;

        while(Files.exists(mFile))
        {
            mFile = Paths.get(mFile.toFile().getAbsolutePath().replace(".tmp", "_" + version + ".tmp"));
            version++;
        }

        mFileChannel = (FileChannel.open(mFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));

        ByteBuffer header = getWaveHeader(mAudioFormat);

        while(header.hasRemaining())
        {
            mFileChannel.write(header);
        }
    }

    /**
     * Closes the file
     */
    public void close() throws IOException
    {
        mFileChannel.force(true);
        mFileChannel.close();

        rename();
    }

    /**
     * Renames the file from *.tmp to *.wav after file has been closed.
     * @throws IOException
     */
    private void rename() throws IOException
    {
        if(mFile != null && Files.exists(mFile))
        {
            String renamed = mFile.getFileName().toString();
            renamed = renamed.replace(".tmp", ".wav");
            Path renamedPath = mFile.resolveSibling(renamed);

            int suffix = 1;

            while(Files.exists(renamedPath) && suffix < 4)
            {
                String renamedWithSuffix = mFile.getFileName().toString();
                renamedWithSuffix = renamedWithSuffix.replace(".tmp", "_" + suffix + ".wav");
                Path renamedWithSuffixPath = mFile.resolveSibling(renamedWithSuffix);
                renamedPath = mFile.resolveSibling(renamedWithSuffixPath);
                suffix++;
            }

            Files.move(mFile, renamedPath);
        }
    }

    @Override
    protected void finalize() throws IOException
    {
        mFileChannel.force(true);
        mFileChannel.close();
        mFileChannel = null;
    }

    /**
     * Writes the buffer contents to the file.  Assumes that the buffer is full
     * and the first byte of data is at position 0.
     */
    public void writeData(ByteBuffer buffer) throws IOException
    {
        buffer.position(0);

        openDataChunk();

        /* Write the full buffer if there is room, respecting the max file size */
        if(mFileChannel.size() + buffer.capacity() < mMaxSize)
        {
            while(buffer.hasRemaining())
            {
                mDataChunkSize += mFileChannel.write(buffer);
            }

            updateTotalSize();
            updateDataChunkSize();
        }
        else
        {
            /* Split the buffer to finish filling the current file and then put
             * the leftover into a new file */
            int remaining = (int)(mMaxSize - mFileChannel.size());

            /* Ensure we write full frames to fill up the remaining size */
            remaining -= (int)(remaining % mAudioFormat.getFrameSize());

            byte[] bytes = buffer.array();

            ByteBuffer current = ByteBuffer.wrap(Arrays.copyOf(bytes, remaining));

            ByteBuffer next = ByteBuffer.wrap(Arrays.copyOfRange(bytes,
                remaining, bytes.length));

            while(current.hasRemaining())
            {
                mDataChunkSize += mFileChannel.write(current);
            }

            updateTotalSize();
            updateDataChunkSize();

            rollover();

            openDataChunk();

            while(next.hasRemaining())
            {
                mDataChunkSize += mFileChannel.write(next);
            }

            updateTotalSize();
            updateDataChunkSize();
        }
    }

    /**
     * Closes the current data chunk
     */
    private void closeDataChunk()
    {
        mDataChunkOpen = false;
    }

    /**
     * Opens a new data chunk if a data chunk is not currently open.  This method can be invoked repeatedly as an
     * assurance that the data chunk header has been written.
     *
     * @throws IOException if there is an error writing the data chunk header.
     */
    private void openDataChunk() throws IOException
    {
        if(!mDataChunkOpen)
        {
            if(mFileChannel.size() + 32 >= mMaxSize)
            {
                rollover();
            }

            ByteBuffer formatChunk = getFormatChunk(mAudioFormat);
            formatChunk.position(0);

            while(formatChunk.hasRemaining())
            {
                mFileChannel.write(formatChunk);
            }

            ByteBuffer dataHeader = getDataHeader();
            dataHeader.position(0);

            while(dataHeader.hasRemaining())
            {
                mFileChannel.write(dataHeader);
            }

            mDataChunkSizeOffset = mFileChannel.size() - 4;
            mDataChunkSize = 0;
            mDataChunkOpen = true;

            updateTotalSize();
        }
    }

    /**
     * Writes the metadata to the end of the file if there is sufficient space without exceeding the
     * max file size.
     */
    public void writeMetadata(WaveMetadata metadata) throws IOException
    {
        ByteBuffer listChunk = metadata.getLISTChunk();

        if(mFileChannel.size() + listChunk.capacity() >= mMaxSize)
        {
            throw new IOException("Cannot write LIST metadata chunk - insufficient file space remaining");
        }

        closeDataChunk();

        listChunk.position(0);

        while(listChunk.hasRemaining())
        {
            mFileChannel.write(listChunk);
        }

        updateTotalSize();

        ByteBuffer id3Chunk = metadata.getID3Chunk();

        if(mFileChannel.size() + id3Chunk.capacity() >= mMaxSize)
        {
            throw new IOException("Cannot write ID3 metadata chunk - insufficient file space remaining");
        }

        id3Chunk.position(0);

        while(id3Chunk.hasRemaining())
        {
            mFileChannel.write(id3Chunk);
        }

        updateTotalSize();
    }

    /**
     * Closes out the current file, appends an incremented sequence number to
     * the file name and opens up a new file.
     */
    private void rollover() throws IOException
    {
        closeDataChunk();
        close();

        mFileRolloverCounter++;

        updateFileName();

        open();
    }

    /**
     * Updates the overall and the chunk2 sizes
     */
    private void updateTotalSize() throws IOException
    {
        /* Update overall wave size (total size - 8 bytes) */
        ByteBuffer buffer = getUnsignedIntegerBuffer(mFileChannel.size() - 8);
        mFileChannel.write(buffer, 4);
    }

    /**
     * Updates the data chunk size
     */
    private void updateDataChunkSize() throws IOException
    {
        if(!mDataChunkOpen)
        {
            throw new IOException("Can't update data chunk size - data chunk is not currently open");
        }

        /* Update overall wave size (total size - 8 bytes) */
        ByteBuffer size = getUnsignedIntegerBuffer(mDataChunkSize);
        mFileChannel.write(size, mDataChunkSizeOffset);
    }

    /**
     * Creates a little-endian 4-byte buffer containing an unsigned 32-bit
     * integer value derived from the 4 least significant bytes of the argument.
     *
     * The buffer's position is set to 0 to prepare it for writing to a channel.
     */
    protected static ByteBuffer getUnsignedIntegerBuffer(long size)
    {
        ByteBuffer buffer = ByteBuffer.allocate(4);

        buffer.put((byte)(size & 0xFFl));
        buffer.put((byte)(Long.rotateRight(size & 0xFF00l, 8)));
        buffer.put((byte)(Long.rotateRight(size & 0xFF0000l, 16)));

        /* This side-steps an issue with right shifting a signed long by 32
         * where it produces an error value.  Instead, we right shift in two steps. */
        buffer.put((byte)Long.rotateRight(Long.rotateRight(size & 0xFF000000l, 16), 8));

        buffer.position(0);

        return buffer;
    }

    public static String toString(ByteBuffer buffer)
    {
        StringBuilder sb = new StringBuilder();

        byte[] bytes = buffer.array();

        for(byte b : bytes)
        {
            sb.append(String.format("%02X ", b));
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * Updates the current file name with the rollover counter series suffix
     */
    private void updateFileName()
    {
        String filename = mFile.toString();

        if(mFileRolloverCounter == 2)
        {
            filename = filename.replace(".tmp", "_2.tmp");
        }
        else
        {
            Matcher m = FILENAME_PATTERN.matcher(filename);

            if(m.find())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(m.group(1));
                sb.append(mFileRolloverCounter);
                sb.append(m.group(3));

                filename = sb.toString();
            }
        }

        mFile = Paths.get(filename);
    }

    /**
     * Creates a data chunk header with the chunk size initialized to zero
     */
    public static ByteBuffer getDataHeader()
    {
        ByteBuffer header = ByteBuffer.allocate(8);
        header.put(DATA_CHUNK_ID.getBytes());
        header.position(0);

        return header;
    }

    /**
     * Creates a wave file header with a format descriptor chunk
     */
    public static ByteBuffer getWaveHeader(AudioFormat format)
    {
        ByteBuffer header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);

        //RIFF/WAVE header and size
        header.put(RIFF_ID.getBytes());
        header.putInt(INITIAL_TOTAL_LENGTH);
        header.put(WAVE_ID.getBytes());

        //Reset the buffer pointer to 0
        header.position(0);

        return header;
    }

    /**
     * Creates an audio format chunk
     */
    public static ByteBuffer getFormatChunk(AudioFormat format)
    {
        ByteBuffer header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);

        //Format descriptor
        header.put(FORMAT_CHUNK_ID.getBytes());
        header.putInt(FORMAT_CHUNK_LENGTH);
        header.putShort(FORMAT_UNCOMPRESSED_PCM);
        header.putShort((short)format.getChannels());
        header.putInt((int)format.getSampleRate());

        //Byte Rate = sample rate * channels * bits per sample / 8
        int frameByteRate = format.getChannels() * format.getSampleSizeInBits() / 8;
        int byteRate = (int)(format.getSampleRate() * frameByteRate);
        header.putInt(byteRate);

        //Block Align
        header.putShort((short)frameByteRate);

        //Bits per Sample
        header.putShort((short)format.getSampleSizeInBits());

        //Reset the buffer pointer to 0
        header.position(0);

        return header;
    }
}
