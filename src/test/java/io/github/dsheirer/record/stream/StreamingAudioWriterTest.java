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

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.record.wave.AudioMetadata;
import io.github.dsheirer.record.wave.AudioMetadataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Streaming MP3 and WAVE recordings: audio written in chunks during the call and the final metadata written at close.
 */
public class StreamingAudioWriterTest
{
    private static final int BUFFER_SIZE = 160;

    @TempDir
    Path mTempDir;

    /**
     * 1 kHz tone at 8 kHz in 20 ms buffers
     */
    private static List<float[]> tone(int buffers, float amplitude)
    {
        List<float[]> audio = new ArrayList<>();
        int sample = 0;

        for(int x = 0; x < buffers; x++)
        {
            float[] buffer = new float[BUFFER_SIZE];

            for(int y = 0; y < BUFFER_SIZE; y++)
            {
                buffer[y] = amplitude * (float)Math.sin(2.0 * Math.PI * 1000.0 * sample++ / 8000.0);
            }

            audio.add(buffer);
        }

        return audio;
    }

    private static Map<AudioMetadata,String> metadata(String title)
    {
        Map<AudioMetadata,String> metadata = new EnumMap<>(AudioMetadata.class);
        metadata.put(AudioMetadata.TRACK_TITLE, title);
        metadata.put(AudioMetadata.ARTIST_NAME, "Radio 1234");
        return metadata;
    }

    @Test
    public void mp3StreamIsPlayableDuringAndAfterTheCall() throws Exception
    {
        Path path = mTempDir.resolve("call.mp3");
        StreamingMP3Writer writer = new StreamingMP3Writer(path, InputAudioFormat.SR_16000, MP3Setting.CBR_16, false,
            metadata("Initial"));
        List<float[]> audio = tone(250, 0.5f);

        //5 seconds of audio written in 10 chunks, like the recorder does during a call
        for(int x = 0; x < 10; x++)
        {
            writer.write(audio.subList(x * 25, (x + 1) * 25));

            if(x == 4)
            {
                assertTrue(Files.size(path) > StreamingMP3Writer.RESERVED_TAG_SIZE, "file grows during the call");
                Mp3File partial = new Mp3File(path.toString());
                assertTrue(partial.getFrameCount() > 0, "partial recording has audio frames");
            }
        }

        writer.close(metadata("Final Title"));

        Mp3File mp3 = new Mp3File(path.toString());
        ID3v2 tag = mp3.getId3v2Tag();
        assertNotNull(tag);
        assertEquals("Final Title", tag.getTitle(), "final metadata replaces the initial tag");
        assertEquals("Radio 1234", tag.getArtist());
        assertEquals(StreamingMP3Writer.RESERVED_TAG_SIZE, mp3.getStartOffset(), "audio starts after reserved tag");
        assertEquals(5000, mp3.getLengthInMilliseconds(), 300, "duration");
    }

    @Test
    public void mp3FinalTagLargerThanReservedSpaceRewritesFile() throws Exception
    {
        Path path = mTempDir.resolve("long.mp3");
        StreamingMP3Writer writer = new StreamingMP3Writer(path, InputAudioFormat.SR_16000, MP3Setting.CBR_16, false,
            metadata("Initial"));
        writer.write(tone(100, 0.5f));
        String longTitle = "T".repeat(StreamingMP3Writer.RESERVED_TAG_SIZE * 2);
        writer.close(metadata(longTitle));

        Mp3File mp3 = new Mp3File(path.toString());
        assertEquals(longTitle, mp3.getId3v2Tag().getTitle());
        assertEquals(2000, mp3.getLengthInMilliseconds(), 300);
        assertTrue(!Files.exists(path.resolveSibling("long.mp3.tmp")), "temporary file removed");
    }

    @Test
    public void paddedTagKeepsFrames() throws Exception
    {
        byte[] tag = AudioMetadataUtils.getMP3ID3(metadata("Padded"));
        byte[] padded = StreamingMP3Writer.getPaddedTag(tag, 4096);
        assertNotNull(padded);
        assertEquals(4096, padded.length);
        assertNull(StreamingMP3Writer.getPaddedTag(tag, tag.length - 1), "tag doesn't fit");
    }

    @Test
    public void normalizationGainOnlyDecreases()
    {
        StreamingMP3Writer writer = null;

        try
        {
            writer = new StreamingMP3Writer(mTempDir.resolve("n.mp3"), InputAudioFormat.SR_16000,
                MP3Setting.CBR_16, true, metadata("N"));
            float[] quiet = tone(1, 0.05f).getFirst();
            float[] loud = tone(1, 0.5f).getFirst();
            List<float[]> first = writer.normalize(List.of(quiet));
            List<float[]> second = writer.normalize(List.of(loud));
            List<float[]> third = writer.normalize(List.of(quiet));

            assertEquals(0.95f * 0.05f / 0.1f, peak(first.getFirst()), 0.01f, "quiet audio gain limited to 20 dB");
            assertEquals(0.95f, peak(second.getFirst()), 0.01f, "loud audio normalized to the target");
            assertEquals(0.95f * 0.05f / 0.5f, peak(third.getFirst()), 0.01f, "gain stays at the lower value");
            assertEquals(0.05f, peak(quiet), 0.001f, "source buffers aren't modified");
        }
        catch(Exception e)
        {
            throw new AssertionError(e);
        }
        finally
        {
            if(writer != null)
            {
                writer.abort();
            }
        }
    }

    private static float peak(float[] samples)
    {
        float peak = 0;

        for(float sample: samples)
        {
            peak = Math.max(peak, Math.abs(sample));
        }

        return peak;
    }

    @Test
    public void waveStreamIsValidDuringAndAfterTheCall() throws Exception
    {
        Path path = mTempDir.resolve("call.wav");
        StreamingWaveWriter writer = new StreamingWaveWriter(path);
        List<float[]> audio = tone(100, 0.5f);
        writer.write(audio.subList(0, 50));

        try(AudioInputStream partial = AudioSystem.getAudioInputStream(path.toFile()))
        {
            assertEquals(50 * BUFFER_SIZE, partial.getFrameLength(), "partial file has a valid header");
        }

        writer.write(audio.subList(50, 100));
        writer.close(metadata("Wave"));

        try(AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile()))
        {
            assertEquals(100 * BUFFER_SIZE, stream.getFrameLength());
        }
    }
}
