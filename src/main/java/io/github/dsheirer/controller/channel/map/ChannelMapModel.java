/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.controller.channel.map;

import io.github.dsheirer.controller.channel.map.ChannelMapEvent.Event;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractListModel;
import java.util.ArrayList;
import java.util.List;

public class ChannelMapModel extends AbstractListModel<ChannelMap>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMapModel.class);

    private ObservableList<ChannelMap> mChannelMaps = FXCollections.observableArrayList(ChannelMap.extractor());
    private Broadcaster<ChannelMapEvent> mEventBroadcaster = new Broadcaster<>();

    public ChannelMapModel()
    {
    }

    /**
     * Removes all channel maps from this model and broadcasts a remove/delete event for each.
     */
    public void clear()
    {
        List<ChannelMap> channelMaps = new ArrayList<>(mChannelMaps);

        for(ChannelMap channelMap: channelMaps)
        {
            removeChannelMap(channelMap);
        }
    }

    /**
     * Returns an unmodifiable list of channel maps currently in the model
     */
    public ObservableList<ChannelMap> getChannelMaps()
    {
        return mChannelMaps;
    }

    /**
     * Returns the channel map with a matching name or null
     */
    public ChannelMap getChannelMap(String name)
    {
        for(ChannelMap channelMap : mChannelMaps)
        {
            if(channelMap.getName().equalsIgnoreCase(name))
            {
                return channelMap;
            }
        }

        return null;
    }

    /**
     * Adds a listener to receive notifications when channel map updates occur
     */
    public void addListener(Listener<ChannelMapEvent> listener)
    {
        mEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the channel map update notification listener
     */
    public void removeListener(Listener<ChannelMapEvent> listener)
    {
        mEventBroadcaster.removeListener(listener);
    }

    /**
     * Broadcasts a channel map change.
     *
     * Note: use the add/remove methods to add or remove channel maps from this
     * model.  When using those methods, an add or delete event will automatically
     * be generated.
     */
    public void broadcast(ChannelMapEvent event)
    {
        if(event.getEvent() == ChannelMapEvent.Event.CHANGE ||
            event.getEvent() == ChannelMapEvent.Event.RENAME)
        {
            int index = mChannelMaps.indexOf(event.getChannelMap());

            if(index >= 0)
            {
                fireContentsChanged(this, index, index);
            }
        }

        mEventBroadcaster.broadcast(event);
    }

    /**
     * Adds a list of channel maps to this model
     */
    public void addChannelMaps(List<ChannelMap> channelMaps)
    {
        for(ChannelMap channelMap : channelMaps)
        {
            addChannelMap(channelMap);
        }
    }

    /**
     * Adds the channel map to this model
     */
    public void addChannelMap(ChannelMap channelMap)
    {
        if(!mChannelMaps.contains(channelMap))
        {
            mChannelMaps.add(channelMap);

            int index = mChannelMaps.indexOf(channelMap);

            fireIntervalAdded(this, index, index);

            broadcast(new ChannelMapEvent(channelMap, Event.ADD));
        }
    }

    /**
     * Removes the channel map from this model
     */
    public void removeChannelMap(ChannelMap channelMap)
    {
        if(mChannelMaps.contains(channelMap))
        {
            int index = mChannelMaps.indexOf(channelMap);

            mChannelMaps.remove(channelMap);

            fireIntervalRemoved(this, index, index);

            broadcast(new ChannelMapEvent(channelMap, Event.DELETE));
        }
    }

    /**
     * Size/Number of channel maps in this model
     */
    @Override
    public int getSize()
    {
        return mChannelMaps.size();
    }

    /**
     * Returns the channel map at the specified index
     */
    @Override
    public ChannelMap getElementAt(int index)
    {
        if(index <= mChannelMaps.size())
        {
            return mChannelMaps.get(index);
        }

        return null;
    }
}
