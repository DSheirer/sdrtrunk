package controller.channel.map;

import controller.channel.map.ChannelMapEvent.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Broadcaster;
import sample.Listener;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelMapModel extends AbstractListModel<ChannelMap>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMapModel.class);

    private List<ChannelMap> mChannelMaps = new ArrayList<>();
    private Broadcaster<ChannelMapEvent> mEventBroadcaster = new Broadcaster<>();

    public ChannelMapModel()
    {
    }

    /**
     * Returns an unmodifiable list of channel maps currently in the model
     */
    public List<ChannelMap> getChannelMaps()
    {
        return Collections.unmodifiableList(mChannelMaps);
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
