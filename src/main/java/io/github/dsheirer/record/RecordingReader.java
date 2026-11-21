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

import io.github.dsheirer.dsp.filter.resample.RealResampler;
import net.sourceforge.lame.lowlevel.LameDecoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads a call audio recording (MP3 or WAVE) as 8 kHz mono PCM samples for replay through the audio channels.
 */
public class RecordingReader
{
    public static final int SAMPLE_RATE = 8000;

    private RecordingReader()
    {
    }

    /**
     * Reads the recording.
     * @param path of an .mp3 or .wav recording
     * @return 8 kHz mono samples in the range -1.0 to 1.0
     * @throws IOException if the file can't be read or decoded
     */
    public static float[] read(Path path) throws IOException
    {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

        if(name.endsWith(".mp3"))
        {
            return readMP3(path);
        }

        if(name.endsWith(".wav"))
        {
            return readWave(path);
        }

        throw new IOException("Unsupported recording format: " + path.getFileName());
    }

    private static float[] readMP3(Path path) throws IOException
    {
        LameDecoder decoder;

        try
        {
            decoder = new LameDecoder(path.toAbsolutePath().toString());
        }
        catch(Exception e)
        {
            throw new IOException("Unable to open MP3 recording " + path.getFileName() + ": " + e.getMessage(), e);
        }

        try
        {
            int channels = Math.max(1, decoder.getChannels());

            //The decoder reports 1152 samples per frame, but MPEG-2/2.5 frames (sample rates below 32 kHz, e.g. the
            //default 16 kHz recordings) contain 576 samples, and the decoder doesn't report how many bytes it wrote.
            int samplesPerFrame = decoder.getSampleRate() >= 32000 ? 1152 : 576;
            int frameBytes = samplesPerFrame * channels * 2;
            ByteBuffer buffer = ByteBuffer.allocate(Math.max(frameBytes, decoder.getFrameSize() * channels * 2));
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();

            while(decoder.decode(buffer))
            {
                pcm.write(buffer.array(), 0, frameBytes);
                buffer.clear();
            }

            float[] samples = toFloat(pcm.toByteArray(), channels, ByteOrder.LITTLE_ENDIAN);
            return resample(samples, decoder.getSampleRate());
        }
        catch(RuntimeException re)
        {
            throw new IOException("Unable to decode MP3 recording " + path.getFileName() + ": " + re.getMessage(), re);
        }
        finally
        {
            decoder.close();
        }
    }

    private static float[] readWave(Path path) throws IOException
    {
        try(AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile()))
        {
            AudioFormat format = source.getFormat();
            AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16,
                format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);

            try(AudioInputStream pcm = AudioSystem.getAudioInputStream(pcmFormat, source))
            {
                float[] samples = toFloat(pcm.readAllBytes(), format.getChannels(), ByteOrder.LITTLE_ENDIAN);
                return resample(samples, Math.round(format.getSampleRate()));
            }
        }
        catch(UnsupportedAudioFileException e)
        {
            throw new IOException("Unsupported WAVE recording " + path.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Converts 16-bit PCM to mono float samples, averaging the channels.
     */
    static float[] toFloat(byte[] pcm, int channels, ByteOrder order)
    {
        ByteBuffer buffer = ByteBuffer.wrap(pcm).order(order);
        int frames = pcm.length / (2 * channels);
        float[] samples = new float[frames];

        for(int x = 0; x < frames; x++)
        {
            float sum = 0;

            for(int c = 0; c < channels; c++)
            {
                sum += buffer.getShort() / 32768.0f;
            }

            samples[x] = sum / channels;
        }

        return samples;
    }

    /**
     * Resamples to 8 kHz.
     */
    static float[] resample(float[] samples, int sampleRate)
    {
        if(sampleRate == SAMPLE_RATE || samples.length == 0)
        {
            return samples;
        }

        RealResampler resampler = new RealResampler(sampleRate, SAMPLE_RATE, 4096, 512);
        List<float[]> output = new ArrayList<>();
        resampler.setListener(output::add);
        int chunk = 4096;

        for(int offset = 0; offset < samples.length; offset += chunk)
        {
            float[] piece = new float[Math.min(chunk, samples.length - offset)];
            System.arraycopy(samples, offset, piece, 0, piece.length);
            resampler.resample(piece);
        }

        resampler.flush();

        int length = 0;

        for(float[] buffer: output)
        {
            length += buffer.length;
        }

        float[] resampled = new float[length];
        int position = 0;

        for(float[] buffer: output)
        {
            System.arraycopy(buffer, 0, resampled, position, buffer.length);
            position += buffer.length;
        }

        return resampled;
    }
}
