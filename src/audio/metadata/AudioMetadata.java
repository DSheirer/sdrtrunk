/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package audio.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import alias.id.broadcast.BroadcastChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.Alias;
import alias.id.priority.Priority;

public class AudioMetadata implements Listener<Metadata>
{
    protected static final Logger mLog = LoggerFactory.getLogger(AudioMetadata.class);

    private int mSource;
    private Map<MetadataType, Metadata> mMetadata = new HashMap<>();
    private boolean mUpdated = false;

    /* Channel Selected == highest priority */
    private boolean mSelected = false;

    /* Call Priority */
    private int mPriority = Priority.DEFAULT_PRIORITY;

    /* Indicates if the overall source is recordable */
    private boolean mSourceRecordable = false;

    /* Indicates the current recordable state */
    private boolean mRecordable = false;

    /* Indicates the current streamable state */
    private boolean mStreamable = false;

    /* A unique identifier for this metadata */
    private String mIdentifier;

    public AudioMetadata(int source, boolean recordable)
    {
        mSource = source;

        setIdentifier(mSource);

        mSourceRecordable = recordable;

        setRecordable(mSourceRecordable);
    }

    /**
     * Returns a unique identifier for this audio metadata that contains the
     * source id and optionally the TO metadata string.
     */
    public String getIdentifier()
    {
        return mIdentifier;
    }

    public void setIdentifier(int source)
    {
        setIdentifier(source, null);
    }

    public void setIdentifier(int source, String id)
    {
        mIdentifier = "SRC:" + source + " ID:" + (id == null ? "UNKNOWN" : id);
        mUpdated = true;
    }

    /**
     * Returns a copy of the current audio metadata.
     */
    public AudioMetadata copyOf()
    {
        AudioMetadata copy = new AudioMetadata(mSource, mSourceRecordable);

        copy.mPriority = mPriority;
        copy.mSelected = mSelected;
        copy.mRecordable = mRecordable;
        copy.mStreamable = mStreamable;

        copy.mMetadata.putAll(mMetadata);
        copy.mUpdated = mUpdated;
        copy.mIdentifier = new String(mIdentifier);

        mUpdated = false;

        return copy;
    }

    /**
     * Removes any accumulated temporal metadata and returns to a default set.
     */
    public void reset()
    {
        //Reset recordable state, identifier and audio call priority levels and reprocess the metadata
        mRecordable = mSourceRecordable;
        mStreamable = false;
        setIdentifier(mSource);
        mPriority = Priority.DEFAULT_PRIORITY;

        List<MetadataType> toRemove = new ArrayList<>();

        for (MetadataType type : mMetadata.keySet())
        {
            Metadata metadata = mMetadata.get(type);

            if (metadata != null)
            {
                if (metadata.isTemporal())
                {
                    toRemove.add(type);
                }
                else
                {
                    processMetadata(metadata);
                }
            }
        }

        for (MetadataType type : toRemove)
        {
            mMetadata.remove(type);
        }

        mUpdated = true;
    }

    private void processMetadata(Metadata metadata)
    {
        if (metadata.hasAlias())
        {
            Alias alias = metadata.getAlias();

            if (metadata.getMetadataType() == MetadataType.FROM || metadata.getMetadataType() == MetadataType.TO)
            {
                if (!alias.isRecordable())
                {
                    mRecordable = false;
                }

                if (alias.isStreamable())
                {
                    mStreamable = true;
                }
            }

            if (metadata.getMetadataType() == MetadataType.TO)
            {
                setIdentifier(mSource, metadata.getValue());
            }

            if (alias != null && alias.getCallPriority() < mPriority)
            {
                mPriority = alias.getCallPriority();
            }
        }
    }

    /**
     * Indicates if this audio metadata contains updated information.  The flag
     * will only be set when new data is added and will be reset by a copyOf()
     * method invocation.
     */
    public boolean isUpdated()
    {
        return mUpdated;
    }

    public void setUpdated(boolean updated)
    {
        mUpdated = updated;
    }

    /**
     * Adds the metadata.  Any temporal metadata will be removed when the
     * dispose() method is invoked.
     */
    @Override
    public void receive(Metadata metadata)
    {
        if (metadata.isReset())
        {
            reset();
        }
        else
        {
            Metadata existing = mMetadata.get(metadata.getMetadataType());

            if (existing == null || !existing.equals(metadata))
            {
                mMetadata.put(metadata.getMetadataType(), metadata);
                processMetadata(metadata);
                setUpdated(true);
            }
        }

        mUpdated = true;
    }

    /**
     * Returns the complete set of metadata
     */
    public Map<MetadataType, Metadata> getMetadata()
    {
        return mMetadata;
    }

    /**
     * Returns the metadata object matching the type or null
     */
    public Metadata getMetadata(MetadataType type)
    {
        return mMetadata.get(type);
    }

    /**
     * Source channel ID
     */
    public void setSource(int source)
    {
        mSource = source;

        mUpdated = true;
    }

    public int getSource()
    {
        return mSource;
    }

    /**
     * Sets the priority of this audio packet within the defined min/max priority range
     */
    public void setPriority(int priority)
    {
        mPriority = priority;

        mUpdated = true;
    }

    /**
     * Returns the priority of this audio metadata
     *
     * @return - priority of audio packet where lower numbers are higher priority
     */
    public int getPriority()
    {
        return mPriority;
    }

    public boolean isDoNotMonitor()
    {
        return mPriority == Priority.DO_NOT_MONITOR;
    }

    /**
     * Selected audio channel.  A selected channel will override the priority
     * level so that the audio can be listened to immediately.
     */
    public void setSelected(boolean selected)
    {
        mSelected = selected;

        mUpdated = true;
    }

    public boolean isSelected()
    {
        return mSelected;
    }

    /**
     * Indicates if audio associated with this metadata is recordable.
     */
    public boolean isRecordable()
    {
        return mRecordable;
    }

    public void setRecordable(boolean recordable)
    {
        mRecordable = recordable;
        mUpdated = true;
    }

    /**
     * Indicates if the audio associated with this metadata is streamable
     */
    public boolean isStreamable()
    {
        return mStreamable;
    }

    public Collection<BroadcastChannel> getBroadcastChannels()
    {
        Set<BroadcastChannel> broadcastChannels = new TreeSet<>();

        for(Metadata metadata: mMetadata.values())
        {
            if(metadata.hasAlias() && metadata.getAlias().isStreamable())
            {
                Alias alias = metadata.getAlias();

                broadcastChannels.addAll(alias.getBroadcastChannels());
            }
        }

        return broadcastChannels;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Audio Metadata - source:");
        sb.append(getSource());
        sb.append(" priority:");
        sb.append(getPriority());
        sb.append(" updated:" + isUpdated());
        sb.append(" ");

        for (Entry<MetadataType, Metadata> entry : getMetadata().entrySet())
        {
            sb.append(entry.getValue().getKey());
            sb.append(":");
            sb.append(entry.getValue().getValue());
            sb.append(" ");
        }

        return sb.toString();
    }
}
