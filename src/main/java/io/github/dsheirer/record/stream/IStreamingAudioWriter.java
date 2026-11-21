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

import io.github.dsheirer.record.wave.AudioMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Writes a call audio recording incrementally while the call is in progress, so that the recording file exists from
 * the start of the call.  The metadata (e.g. ID3 tags) is written when the recording is opened and rewritten with the
 * final call metadata when the recording is closed.
 */
public interface IStreamingAudioWriter
{
    /**
     * Writes the next chunk of 8 kHz PCM audio.  The buffers are not modified.
     */
    void write(List<float[]> audioBuffers) throws IOException;

    /**
     * Finishes the recording with the final call metadata and closes the file.
     */
    void close(Map<AudioMetadata,String> metadata) throws IOException;

    /**
     * Closes the file without finishing it, for example when the recording will be discarded.
     */
    void abort();

    /**
     * Recording file
     */
    Path getPath();
}
