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
package io.github.dsheirer.record.stream;

import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3AudioConverter;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.record.wave.AudioMetadata;
import io.github.dsheirer.record.wave.AudioMetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Streaming MP3 recording.
 *
 * The ID3v2 tag has to be at the start of the file, but the final call metadata is only known when the call ends.  The
 * writer reserves space for the tag by writing the initial tag padded to a fixed size, and overwrites it in place with
 * the final tag when the recording closes.  If the final tag doesn't fit (very rare), the file is rewritten.
 *
 * Audio normalization: the whole-call normalization of the non-streaming recorder can't be used while the call is in
 * progress.  Instead the gain is derived from the peak level seen so far in the call, so the gain can only decrease
 * during the call and the audio never clips.  The same 0.95 target and -20 dBFS gain limit are used.
 */
public class StreamingMP3Writer implements IStreamingAudioWriter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingMP3Writer.class);
    static final int RESERVED_TAG_SIZE = 4096;
    private static final int ID3_HEADER_SIZE = 10;
    private static final float NORMALIZE_TARGET = 0.95f;
    private static final float NORMALIZE_MINIMUM_PEAK = 0.1f;

    private final Path mPath;
    private final FileChannel mFileChannel;
    private final MP3AudioConverter mConverter;
    private final boolean mNormalize;
    private float mPeak = NORMALIZE_MINIMUM_PEAK;

    /**
     * Creates the recording file and writes the initial (padded) ID3 tag.
     * @param path for the new file, which must not exist
     * @param inputAudioFormat for the encoder
     * @param mp3Setting for the encoder
     * @param normalize to apply peak-tracking audio gain normalization
     * @param initialMetadata known at the start of the call
     */
    public StreamingMP3Writer(Path path, InputAudioFormat inputAudioFormat, MP3Setting mp3Setting, boolean normalize,
                              Map<AudioMetadata,String> initialMetadata) throws IOException
    {
        mPath = path;
        mNormalize = normalize;
        mConverter = new MP3AudioConverter(inputAudioFormat, mp3Setting, false);
        mFileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);

        byte[] tag = getPaddedTag(AudioMetadataUtils.getMP3ID3(initialMetadata), RESERVED_TAG_SIZE);

        if(tag == null)
        {
            //Initial tag is too large to pad - reserve an empty tag and rely on the rewrite at close
            tag = getPaddedTag(AudioMetadataUtils.getMP3ID3(Map.of()), RESERVED_TAG_SIZE);
        }

        writeFully(ByteBuffer.wrap(tag));
    }

    @Override
    public void write(List<float[]> audioBuffers) throws IOException
    {
        List<float[]> buffers = mNormalize ? normalize(audioBuffers) : audioBuffers;
        writeFrames(mConverter.encodeStream(buffers));
    }

    /**
     * Applies the peak-tracking normalization gain to copies of the buffers.
     */
    List<float[]> normalize(List<float[]> audioBuffers)
    {
        List<float[]> normalized = new ArrayList<>(audioBuffers.size());

        for(float[] audioBuffer: audioBuffers)
        {
            for(float sample: audioBuffer)
            {
                float magnitude = Math.abs(sample);

                if(magnitude > mPeak && !Float.isNaN(magnitude))
                {
                    mPeak = magnitude;
                }
            }

            float gain = NORMALIZE_TARGET / mPeak;
            float[] copy = Arrays.copyOf(audioBuffer, audioBuffer.length);

            for(int x = 0; x < copy.length; x++)
            {
                copy[x] *= gain;
            }

            normalized.add(copy);
        }

        return normalized;
    }

    private void writeFrames(List<byte[]> frames) throws IOException
    {
        for(byte[] frame: frames)
        {
            writeFully(ByteBuffer.wrap(frame));
        }
    }

    private void writeFully(ByteBuffer buffer) throws IOException
    {
        while(buffer.hasRemaining())
        {
            mFileChannel.write(buffer);
        }
    }

    @Override
    public void close(Map<AudioMetadata,String> metadata) throws IOException
    {
        try
        {
            writeFrames(mConverter.finishStream());

            byte[] tag = getPaddedTag(AudioMetadataUtils.getMP3ID3(metadata), RESERVED_TAG_SIZE);

            if(tag != null)
            {
                mFileChannel.write(ByteBuffer.wrap(tag), 0);
                mFileChannel.force(true);
                mFileChannel.close();
            }
            else
            {
                mFileChannel.force(true);
                mFileChannel.close();
                rewriteWithTag(AudioMetadataUtils.getMP3ID3(metadata));
            }
        }
        finally
        {
            if(mFileChannel.isOpen())
            {
                mFileChannel.close();
            }
        }
    }

    /**
     * Rewrites the file with the (unpadded) tag replacing the reserved tag space.
     */
    private void rewriteWithTag(byte[] tag) throws IOException
    {
        Path temp = mPath.resolveSibling(mPath.getFileName() + ".tmp");

        try(FileChannel source = FileChannel.open(mPath, StandardOpenOption.READ);
            FileChannel target = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING))
        {
            ByteBuffer buffer = ByteBuffer.wrap(tag);

            while(buffer.hasRemaining())
            {
                target.write(buffer);
            }

            long position = RESERVED_TAG_SIZE;
            long size = source.size();

            while(position < size)
            {
                position += source.transferTo(position, size - position, target);
            }
        }

        Files.move(temp, mPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void abort()
    {
        try
        {
            mFileChannel.close();
        }
        catch(IOException ioe)
        {
            LOGGER.warn("Error closing aborted recording [" + mPath + "] - " + ioe.getMessage());
        }
    }

    @Override
    public Path getPath()
    {
        return mPath;
    }

    /**
     * Pads an ID3v2 tag with zero bytes to the requested total size and updates the tag size in the header.  ID3v2
     * readers treat the zero bytes after the last frame as padding.
     * @param tag complete ID3v2 tag (header and frames, no footer)
     * @param size total size for the padded tag
     * @return padded tag, or null if the tag is larger than the requested size or isn't a valid ID3v2 tag.
     */
    static byte[] getPaddedTag(byte[] tag, int size)
    {
        if(tag == null || tag.length < ID3_HEADER_SIZE || tag.length > size || tag[0] != 'I' || tag[1] != 'D' ||
           tag[2] != '3' || (tag[5] & 0x10) != 0)
        {
            return null;
        }

        byte[] padded = Arrays.copyOf(tag, size);
        int tagSize = size - ID3_HEADER_SIZE;

        //Synchsafe integer: 4 bytes of 7 bits each
        padded[6] = (byte)((tagSize >> 21) & 0x7F);
        padded[7] = (byte)((tagSize >> 14) & 0x7F);
        padded[8] = (byte)((tagSize >> 7) & 0x7F);
        padded[9] = (byte)(tagSize & 0x7F);
        return padded;
    }
}
