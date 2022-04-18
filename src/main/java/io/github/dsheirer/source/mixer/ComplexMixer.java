/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.source.mixer;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.adapter.ISampleAdapter;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

public class ComplexMixer
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexMixer.class);
    private MixerReader<INativeBuffer> mMixerReader;
    private String mName;

    /**
     * Complex Mixer - constructs a reader on the mixer/sound card target data line using the specified audio
     * format (sample size, sample rate ) and broadcasts complex (I/Q) sample buffers to the registered listener.
     *
     * @param targetDataLine - mixer or sound card to be used
     * @param format - audio format
     * @param name - token name to use for this mixer
     * @param sampleAdapter - adapter to convert byte array data read from the mixer into ReusableComplexBuffer.
     */
    public ComplexMixer(TargetDataLine targetDataLine, AudioFormat format, String name, ISampleAdapter sampleAdapter)
    {
        mMixerReader = new MixerReader<INativeBuffer>(format, targetDataLine, sampleAdapter);
        mName = name;
    }

    /**
     * Audio format used by the underlying mixer reader
     */
    public AudioFormat getAudioFormat()
    {
        return mMixerReader.getAudioFormat();
    }

    /**
     * Sets the buffer size in bytes per buffer for each read interval.
     * @param bufferSize in bytes
     */
    public void setBufferSampleCount(int bufferSize)
    {
        mMixerReader.setBufferSampleSize(bufferSize);
    }

    public void start()
    {
        mMixerReader.start();
    }

    public void stop()
    {
        mMixerReader.stop();
    }

    public void setBufferListener(Listener<INativeBuffer> listener)
    {
        mMixerReader.setBufferListener(listener);
    }

    public void removeBufferListener()
    {
        mMixerReader.removeBufferListener();
    }

    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mMixerReader.setSourceEventListener(listener);
    }

    public void removeSourceEventListener()
    {
        mMixerReader.removeSourceEventListener();
    }

    @Override
    public String toString()
    {
        return mName;
    }

    public void dispose()
    {
        mMixerReader.dispose();
    }
}
