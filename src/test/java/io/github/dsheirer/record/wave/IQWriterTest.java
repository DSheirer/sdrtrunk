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

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.ConversionUtils;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.source.wave.ComplexWaveSource;
import io.github.dsheirer.source.wave.SDRSharpIQRecording;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IQWriterTest
{
    @TempDir
    Path mTempDir;

    @Test
    void writesHeaderlessSamplesReadableByComplexWaveSource() throws Exception
    {
        float[] samples = new float[]{0.5f, -0.25f, 0.125f, -1.0f, 0.0f, 0.75f};
        String name = SDRSharpIQRecording.createFileName(LocalDateTime.now(), "test", "AIRSPY", 851_000_000L, 25_000);
        Path path = mTempDir.resolve(name);

        try(IQWriter writer = new IQWriter(path))
        {
            writer.writeData(ConversionUtils.convertToSigned16BitSamples(new InterleavedComplexSamples(samples, 0)));
        }

        //Headerless: 3 complex samples x 4 bytes
        assertEquals(12, Files.size(path));

        assertTrue(ComplexWaveSource.supports(path.toFile()));

        List<float[]> received = new ArrayList<>();

        try(ComplexWaveSource source = new ComplexWaveSource(path.toFile()))
        {
            source.setListener((INativeBuffer buffer) -> {
                Iterator<InterleavedComplexSamples> it = buffer.iteratorInterleaved();
                while(it.hasNext())
                {
                    received.add(it.next().samples());
                }
            });
            source.open();
            assertEquals(25_000d, source.getSampleRate());
            source.next(3);
        }

        float[] readBack = received.get(0);
        assertEquals(samples.length, readBack.length);
        assertArrayEquals(samples, readBack, 0.0001f);
    }

    @Test
    void refusesToOverwriteExistingFile() throws Exception
    {
        Path path = mTempDir.resolve("existing.iq");
        Files.write(path, new byte[]{1, 2, 3, 4});
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> new IQWriter(path));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, Files.readAllBytes(path));
    }

    @Test
    void writesFullBuffer() throws Exception
    {
        Path path = mTempDir.resolve("full.iq");
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        buffer.position(8); //Writer must rewind

        try(IQWriter writer = new IQWriter(path))
        {
            writer.writeData(buffer);
        }

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, Files.readAllBytes(path));
    }
}
