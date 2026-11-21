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
package io.github.dsheirer.record;

import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.record.stream.StreamingMP3Writer;
import io.github.dsheirer.record.stream.StreamingWaveWriter;
import io.github.dsheirer.record.wave.AudioMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Recordings written by the streaming writers read back as 8 kHz audio for replay.
 */
public class RecordingReaderTest
{
    @TempDir
    Path mTempDir;

    private static List<float[]> tone(int buffers)
    {
        List<float[]> audio = new ArrayList<>();
        int sample = 0;

        for(int x = 0; x < buffers; x++)
        {
            float[] buffer = new float[160];

            for(int y = 0; y < 160; y++)
            {
                buffer[y] = 0.5f * (float)Math.sin(2.0 * Math.PI * 1000.0 * sample++ / 8000.0);
            }

            audio.add(buffer);
        }

        return audio;
    }

    /**
     * RMS of the middle of the samples and the count of zero crossings, to verify level and frequency.
     */
    private static void assertTone(float[] samples, int expectedSamples, String label)
    {
        assertEquals(expectedSamples, samples.length, expectedSamples * 0.08, label + " length");
        int start = samples.length / 4;
        int end = samples.length * 3 / 4;
        double sum = 0;
        int crossings = 0;

        for(int x = start; x < end; x++)
        {
            sum += samples[x] * samples[x];

            if(x > start && (samples[x - 1] < 0) != (samples[x] < 0))
            {
                crossings++;
            }
        }

        double rms = Math.sqrt(sum / (end - start));
        double frequency = crossings / 2.0 / ((end - start) / 8000.0);
        assertEquals(0.5 / Math.sqrt(2), rms, 0.07, label + " level");
        assertEquals(1000, frequency, 30, label + " frequency");
    }

    @Test
    public void readsStreamingMP3() throws Exception
    {
        for(InputAudioFormat format: new InputAudioFormat[]{InputAudioFormat.SR_16000, InputAudioFormat.SR_8000,
            InputAudioFormat.SR_44100})
        {
            Path path = mTempDir.resolve("call_" + format.name() + ".mp3");
            StreamingMP3Writer writer = new StreamingMP3Writer(path, format, MP3Setting.CBR_16, false, Map.of());
            writer.write(tone(100));
            writer.close(Map.of(AudioMetadata.TRACK_TITLE, "Test"));

            float[] samples = RecordingReader.read(path);
            assertTone(samples, 16000, "MP3 " + format);
        }
    }

    @Test
    public void readsStreamingWave() throws Exception
    {
        Path path = mTempDir.resolve("call.wav");
        StreamingWaveWriter writer = new StreamingWaveWriter(path);
        writer.write(tone(100));
        writer.close(Map.of(AudioMetadata.TRACK_TITLE, "Test"));

        float[] samples = RecordingReader.read(path);
        assertEquals(16000, samples.length, "exact length");
        assertTone(samples, 16000, "WAVE");
        assertTrue(samples.length > 0);
    }
}
