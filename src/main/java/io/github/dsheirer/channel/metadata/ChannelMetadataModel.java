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
package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.controller.channel.Channel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Model of the channel metadata for the currently processing channels, with a map from each channel metadata to the
 * channel that it belongs to.  The model is thread-safe and isn't tied to a user interface toolkit: listeners are
 * notified on the thread that changes the model and pass the changes to their user interface thread.
 */
public class ChannelMetadataModel implements IChannelMetadataUpdateListener
{
    private final List<ChannelMetadata> mChannelMetadata = new ArrayList<>();
    private final Map<ChannelMetadata,Channel> mMetadataChannelMap = new HashMap<>();
    private final List<Listener> mListeners = new CopyOnWriteArrayList<>();

    /**
     * Listener for changes to the channel metadata model.  Notifications arrive on the thread that changed the
     * model, sometimes while the model is locked, so implementations must not block and must not call back into
     * the model from the notification thread.
     */
    public interface Listener
    {
        /**
         * Channel metadata(s) for a channel were added to the model.
         */
        void added(ChannelAndMetadata channelAndMetadata);

        /**
         * Channel metadata was removed from the model.
         */
        void removed(ChannelMetadata channelMetadata);

        /**
         * A field of a channel metadata in the model was updated.
         */
        void updated(ChannelMetadata channelMetadata, ChannelMetadataField channelMetadataField);
    }

    /**
     * Constructs an instance
     */
    public ChannelMetadataModel()
    {
    }

    /**
     * Registers the listener to receive model change notifications.
     * @param listener to register
     * @return the channel metadata in the model when the listener is registered, so that the listener starts from
     * the current state and receives the changes that follow.
     */
    public synchronized List<ChannelMetadata> addListener(Listener listener)
    {
        mListeners.add(listener);
        return new ArrayList<>(mChannelMetadata);
    }

    /**
     * Removes the listener from receiving model change notifications.
     */
    public void removeListener(Listener listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Creates a diagnostic description of the channel metadata and metadata-channel map.
     * @return diagnostic description.
     */
    public String getDiagnosticInformation()
    {
        Map<ChannelMetadata,Channel> mapCopy;
        List<ChannelMetadata> metadataCopy;

        synchronized(this)
        {
            mapCopy = new HashMap<>(mMetadataChannelMap);
            metadataCopy = new ArrayList<>(mChannelMetadata);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Channel Metadata Model Diagnostics\n");

        for(ChannelMetadata channelMetadata: metadataCopy)
        {
            sb.append("\n--------------- CHANNEL METADATA ITEM --------------------\n\n");
            if(mapCopy.containsKey(channelMetadata))
            {
                Channel channel = mapCopy.get(channelMetadata);
                sb.append("\tMapped Channel:").append(channel).append("\n");
                sb.append("\t\tSource Configuration: ").append(channel.getSourceConfiguration()).append("\n");
            }
            else
            {
                sb.append("\tMapped Channel: **Channel Metadata Is Not In the Metadata:Channel Map**\n");
            }

            sb.append(channelMetadata.getDescription());
        }

        //Check for orphaned map entries that don't match the current channel metadata list.
        for(Map.Entry<ChannelMetadata,Channel> entry: mapCopy.entrySet())
        {
            if(!metadataCopy.contains(entry.getKey()))
            {
                sb.append("\n\tWARNING: orphaned Metadata Channel map entry - channel: " + entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Prepares the model for disposal.
     */
    public void dispose()
    {
        mListeners.clear();
    }

    /**
     * Adds the channel metadata(s) for a channel to the model.
     */
    public synchronized void add(ChannelAndMetadata channelAndMetadata)
    {
        for(ChannelMetadata channelMetadata: channelAndMetadata.getChannelMetadata())
        {
            if(!mChannelMetadata.contains(channelMetadata))
            {
                mChannelMetadata.add(channelMetadata);
            }

            mMetadataChannelMap.put(channelMetadata, channelAndMetadata.getChannel());
            channelMetadata.setUpdateEventListener(this);
        }

        for(Listener listener: mListeners)
        {
            listener.added(channelAndMetadata);
        }
    }

    /**
     * Updates the mapping of channel metadata to the channel used to create the channel metadatas (and processing chain)
     * @param channelMetadatas
     * @param channel
     */
    public synchronized void updateChannelMetadataToChannelMap(Collection<ChannelMetadata> channelMetadatas, Channel channel)
    {
        for(ChannelMetadata channelMetadata: channelMetadatas)
        {
            mMetadataChannelMap.put(channelMetadata, channel);
        }
    }

    /**
     * Removes the channel metadata from the model.
     */
    public synchronized void remove(ChannelMetadata channelMetadata)
    {
        channelMetadata.removeUpdateEventListener();
        mMetadataChannelMap.remove(channelMetadata);

        if(mChannelMetadata.remove(channelMetadata))
        {
            for(Listener listener: mListeners)
            {
                listener.removed(channelMetadata);
            }
        }
    }

    /**
     * Current channel metadata in the model.
     * @return copy of the channel metadata list.
     */
    public synchronized List<ChannelMetadata> getChannelMetadata()
    {
        return new ArrayList<>(mChannelMetadata);
    }

    /**
     * Returns the channel that matches the metadata or null
     */
    public synchronized Channel getChannelFromMetadata(ChannelMetadata channelMetadata)
    {
        return mMetadataChannelMap.get(channelMetadata);
    }

    @Override
    public void updated(ChannelMetadata channelMetadata, ChannelMetadataField channelMetadataField)
    {
        for(Listener listener: mListeners)
        {
            listener.updated(channelMetadata, channelMetadataField);
        }
    }
}
