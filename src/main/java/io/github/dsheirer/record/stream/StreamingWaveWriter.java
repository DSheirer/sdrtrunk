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

import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.record.wave.AudioMetadata;
import io.github.dsheirer.record.wave.AudioMetadataUtils;
import io.github.dsheirer.record.wave.WaveWriter;
import io.github.dsheirer.sample.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Streaming WAVE recording.  The RIFF and data chunk sizes are updated after each write, so the file is a valid WAVE
 * file throughout the call.  The LIST and ID3 metadata chunks are appended when the recording is closed.
 */
public class StreamingWaveWriter implements IStreamingAudioWriter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingWaveWriter.class);
    private final WaveWriter mWaveWriter;
    private final Path mPath;

    /**
     * Creates the recording file.
     * @param path for the new file, which must not exist
     */
    public StreamingWaveWriter(Path path) throws IOException
    {
        mPath = path;
        mWaveWriter = new WaveWriter(AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO, path);
    }

    @Override
    public void write(List<float[]> audioBuffers) throws IOException
    {
        for(float[] audioBuffer: audioBuffers)
        {
            mWaveWriter.writeData(ConversionUtils.convertToSigned16BitSamples(audioBuffer));
        }
    }

    @Override
    public void close(Map<AudioMetadata,String> metadata) throws IOException
    {
        ByteBuffer listChunk = AudioMetadataUtils.getLISTChunk(metadata);
        byte[] id3Bytes = AudioMetadataUtils.getMP3ID3(metadata);
        ByteBuffer id3Chunk = AudioMetadataUtils.getID3Chunk(id3Bytes);
        mWaveWriter.writeMetadata(listChunk, id3Chunk);
        mWaveWriter.close();
    }

    @Override
    public void abort()
    {
        try
        {
            mWaveWriter.close();
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
}
