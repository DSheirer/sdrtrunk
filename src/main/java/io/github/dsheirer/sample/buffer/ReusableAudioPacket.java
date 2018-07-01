/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.sample.buffer;

import io.github.dsheirer.channel.metadata.Metadata;

/**
 * Reusable audio packet that carries audio sample data, associated metadata and a type to indicate if this
 * packet contains audio data or is an end-audio packet.
 */
public class ReusableAudioPacket extends AbstractReusableBuffer
{
    private Type mType = Type.AUDIO;
    private Metadata mMetadata;
    private float[] mAudioSamples;

    /**
     * Constructs a reusable audio packet.  This constructor is package private and should only be used by
     * a reusable audio packet queue.
     *
     * @param bufferDisposedListener to be notified when all users have released the packet
     * @param length of the sample buffer
     */
    ReusableAudioPacket(IReusableBufferDisposedListener bufferDisposedListener, int length)
    {
        super(bufferDisposedListener);
        resize(length);
    }

    /**
     * Indicates if this packet has associated metadata
     */
    public boolean hasMetadata()
    {
        return mMetadata != null;
    }

    /**
     * Associated audio metadata
     */
    public Metadata getMetadata()
    {
        return mMetadata;
    }

    /**
     * Sets the metadata associated with this audio packet.
     */
    public void setMetadata(Metadata metadata)
    {
        mMetadata = metadata;
    }

    /**
     * Indicates the type of audio packet
     */
    public Type getType()
    {
        return mType;
    }

    /**
     * Sets the type of audio packet.
     *
     * This method is package private and is used by the reusable audio packet queue.
     */
    void setType(Type type)
    {
        mType = type;
    }

    /**
     * PCM 8 kHz audio samples
     */
    public float[] getAudioSamples()
    {
        return mAudioSamples;
    }

    /**
     * Indicates if this audio packet contains audio samples.
     */
    public boolean hasAudioSamples()
    {
        return mType != null && mType == Type.AUDIO && mAudioSamples != null;
    }

    /**
     * Resizes the internal audio sample buffer.
     */
    public void resize(int length)
    {
        if(mAudioSamples == null || mAudioSamples.length != length)
        {
            mAudioSamples = new float[length];
        }
    }

    /**
     * Loads a copy of the float sample data from the reusable buffer into this audio packet, resizing the
     * internal audio sample buffer as necessary.
     *
     * @param reusableBuffer to load audio sample data from
     */
    public void loadAudioFrom(ReusableBuffer reusableBuffer)
    {
        if(reusableBuffer.getSamples().length != mAudioSamples.length)
        {
            resize(reusableBuffer.getSamples().length);
        }

        System.arraycopy(reusableBuffer.getSamples(), 0, mAudioSamples, 0, reusableBuffer.getSamples().length);
    }

    /**
     * Loads the audio sample array into this buffer.
     *
     * Note: this method is used for compatibility with legacy audio converters that have not been updated to
     * use reusable audio packets and is therefore deprecated and will be removed once converters (ie JMBE) have
     * been updated.
     *
     * @param audio to load
     */
    @Deprecated
    public void loadAudioFrom(float[] audio)
    {
        mAudioSamples = audio;
    }

    /**
     * Audio Packet Type
     */
    public enum Type
    {
        AUDIO,
        END;
    }
}
