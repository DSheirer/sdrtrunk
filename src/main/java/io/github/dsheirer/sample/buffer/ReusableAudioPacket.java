/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.sample.buffer;

import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.identifier.IdentifierCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reusable audio packet that carries audio sample data, associated metadata and a type to indicate if this
 * packet contains audio data or is an end-audio packet.
 */
public class ReusableAudioPacket extends AbstractReusableBuffer
{
    private Type mType = Type.AUDIO;
    private float[] mAudioSamples;

    private IdentifierCollection mIdentifierCollection;
    private int mChannelId = 0;
    private int mMonitoringPriority = Priority.DEFAULT_PRIORITY;
    private boolean mRecordable = false;
    private List<BroadcastChannel> mBroadcastChannels = new ArrayList<>();

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
     * Resets the attributes for this packet to default audio priority, non-recordable and non-streamable.
     */
    public void resetAttributes()
    {
        mChannelId = 0;
        mMonitoringPriority = Priority.DEFAULT_PRIORITY;
        mRecordable = false;
        mBroadcastChannels.clear();
    }

    /**
     * Identifier for the channel that produced this audio packet.  This ID is used to identify all packets
     * within a packet stream that come from the same channel.
     *
     * @return channel identifier
     */
    public int getAudioChannelId()
    {
        return mChannelId;
    }

    /**
     * Sets the channel identifier for this audio packet
     *
     * @param channelId for this audio packet.
     */
    public void setAudioChannelId(int channelId)
    {
        mChannelId = channelId;
    }

    /**
     * Monitoring (ie local playback) priority.
     */
    public int getMonitoringPriority()
    {
        return mMonitoringPriority;
    }

    /**
     * Sets the monitoring priority
     *
     * @param priority for monitoring:
     *
     * -1  Do Not Monitor
     * 0  Selected (monitor override)
     * 1  Highest
     * ...
     * 100 Lowest
     */
    public void setMonitoringPriority(int priority)
    {
        mMonitoringPriority = priority;
    }

    /**
     * Indicates if the monitoring priority of this audio packet is set to do not monitor.
     */
    public boolean isDoNotMonitor()
    {
        return mMonitoringPriority == Priority.DO_NOT_MONITOR;
    }

    /**
     * Indicates if this audio packet should be recorded.
     */
    public boolean isRecordable()
    {
        return mRecordable;
    }

    /**
     * Sets the recordable status for this audio packet.
     */
    public void setRecordable(boolean recordable)
    {
        mRecordable = recordable;
    }

    /**
     * List of broadcast/streaming channels for this audio packet.
     */
    public List<BroadcastChannel> getBroadcastChannels()
    {
        return mBroadcastChannels;
    }

    /**
     * Adds the broadcast channels to this metadata
     */
    public void addBroadcastChannels(Collection<BroadcastChannel> channels)
    {
        for(BroadcastChannel channel: channels)
        {
            if(!mBroadcastChannels.contains(channel))
            {
                mBroadcastChannels.add(channel);
            }
        }
    }

    /**
     * Indicates if this audio packet has one or more streaming/broadcast channels assigned.
     */
    public boolean isStreamable()
    {
        return !mBroadcastChannels.isEmpty();
    }

    /**
     * Indicates if this packet has associated identifier collection
     */
    public boolean hasIdentifierCollection()
    {
        return mIdentifierCollection != null;
    }

    /**
     * Associated audio metadata and identifiers
     */
    public IdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Sets the metadata associated with this audio packet.
     */
    public void setIdentifierCollection(IdentifierCollection identifierCollection)
    {
        mIdentifierCollection = identifierCollection;
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
     * @param reusableFloatBuffer to load audio sample data from
     */
    public void loadAudioFrom(ReusableFloatBuffer reusableFloatBuffer)
    {
        if(reusableFloatBuffer.getSamples().length != mAudioSamples.length)
        {
            resize(reusableFloatBuffer.getSamples().length);
        }

        System.arraycopy(reusableFloatBuffer.getSamples(), 0, mAudioSamples, 0, reusableFloatBuffer.getSamples().length);
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
