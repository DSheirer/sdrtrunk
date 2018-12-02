/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent.Event;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Channel Model
 */
public class ChannelModel extends AbstractTableModel implements Listener<ChannelEvent>
{
    private static final long serialVersionUID = 1L;

    public static final int COLUMN_ENABLED = 0;
    public static final int COLUMN_SYSTEM = 1;
    public static final int COLUMN_SITE = 2;
    public static final int COLUMN_NAME = 3;
    public static final int COLUMN_ALIAS_LIST = 4;
    public static final int COLUMN_SOURCE = 5;
    public static final int COLUMN_DECODER = 6;
    public static final int COLUMN_AUTO_START = 7;

    private static final String[] COLUMN_NAMES = new String[] {"Playing", "System", "Site", "Name", "Alias List",
        "Source", "Decoder", "Auto-Start"};
    private static final String VALUE_YES = "Yes";

    private List<Channel> mChannels = new CopyOnWriteArrayList<>();
    private List<Channel> mTrafficChannels = new CopyOnWriteArrayList<>();
    private Broadcaster<ChannelEvent> mChannelEventBroadcaster = new Broadcaster();

    public ChannelModel()
    {
    }

    /**
     * Unmodifiable list of non-traffic channels currently in the model
     */
    public List<Channel> getChannels()
    {
        return Collections.unmodifiableList(mChannels);
    }

    public Channel getChannelAtIndex(int row)
    {
        if(row < mChannels.size())
        {
            return mChannels.get(row);
        }

        return null;
    }

    /**
     * Returns a list of unique system values from across the channel set
     */
    public List<String> getSystems()
    {
        List<String> systems = new ArrayList<>();

        for(Channel channel : mChannels)
        {
            if(channel.hasSystem() && !systems.contains(channel.getSystem()))
            {
                systems.add(channel.getSystem());
            }
        }

        Collections.sort(systems);

        return systems;
    }

    /**
     * Returns a list of unique site values for all channels that have a matching
     * system value
     */
    public List<String> getSites(String system)
    {
        List<String> sites = new ArrayList<>();

        if(system != null)
        {
            for(Channel channel : mChannels)
            {
                if(channel.hasSystem() &&
                    system.equals(channel.getSystem()) &&
                    channel.hasSite() &&
                    !sites.contains(channel.getSite()))
                {
                    sites.add(channel.getSite());
                }
            }
        }

        Collections.sort(sites);

        return sites;
    }

    /**
     * Broadcasts the channel event to all registered listeners
     */
    public void receive(ChannelEvent event)
    {
        if(event.getChannel().getChannelType() == ChannelType.STANDARD)
        {
            int index;
            switch(event.getEvent())
            {
                case NOTIFICATION_CONFIGURATION_CHANGE:
                case NOTIFICATION_SELECTION_CHANGE:
                    index = mChannels.indexOf(event.getChannel());
                    if(index > 0)
                    {
                        fireTableRowsUpdated(index, index);
                    }
                    //rebroadcast this event to any listeners
                    mChannelEventBroadcaster.broadcast(event);
                    break;
                case NOTIFICATION_PROCESSING_START:
                case NOTIFICATION_PROCESSING_STOP:
                    index = mChannels.indexOf(event.getChannel());
                    if(index > 0)
                    {
                        fireTableRowsUpdated(index, index);
                    }
                    break;
                default:
                    break;
            }
        }

        if(event.getEvent() == Event.REQUEST_DELETE)
        {
            removeChannel(event.getChannel());
        }
    }

    /**
     * Bulk loading of channel list.  Each channel is added and a channel add
     * event is broadcast.
     */
    public void addChannels(List<Channel> channels)
    {
        for(Channel channel : channels)
        {
            addChannel(channel);
        }
    }

    /**
     * Adds the channel to the model and broadcasts a channel add event
     */
    public int addChannel(Channel channel)
    {
        int index = -1;

        switch(channel.getChannelType())
        {
            case STANDARD:
                mChannels.add(channel);

                index = mChannels.size() - 1;

                fireTableRowsInserted(index, index);
                break;
            case TRAFFIC:
                mTrafficChannels.add(channel);
                index = mChannels.size() - 1;
                break;
            default:
                break;
        }

        mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_ADD));

        return index;
    }

    /**
     * Removes the channel from the model and broadcasts a channel remove event
     */
    public void removeChannel(Channel channel)
    {
        if(channel != null)
        {
            switch(channel.getChannelType())
            {
                case STANDARD:
                    int index = mChannels.indexOf(channel);

                    mChannels.remove(channel);

                    if(index >= 0)
                    {
                        fireTableRowsDeleted(index, index);
                    }
                    break;
                case TRAFFIC:
                    mTrafficChannels.remove(channel);
                    break;
                default:
                    break;
            }

            mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_DELETE));
        }
    }

    /**
     * List of channels that have the auto-start flag set.
     *
     * @return auto-start channels sorted by the channel order value.
     */
    public List<Channel> getAutoStartChannels()
    {
        List<Channel> autoStartChannels = new ArrayList<>();

        for(Channel channel: getChannels())
        {
            if(channel.isAutoStart())
            {
                autoStartChannels.add(channel);
            }
        }

        Collections.sort(autoStartChannels, new Comparator<Channel>()
        {
            @Override
            public int compare(Channel channel1, Channel channel2)
            {
                if(channel1.hasAutoStartOrder() && channel2.hasAutoStartOrder())
                {
                    return Integer.compare(channel1.getAutoStartOrder(), channel2.getAutoStartOrder());
                }
                else if(channel1.hasAutoStartOrder())
                {
                    return -1;
                }
                else if(channel2.hasAutoStartOrder())
                {
                    return 1;
                }

                return 0;
            }
        });

        return autoStartChannels;
    }

    /**
     * Returns a list of channels that fall within the frequency range
     *
     * @param start frequency of the range
     * @param stop frequency of the range
     * @return list of channels or an empty list if none fall within the range
     */
    public List<Channel> getChannelsInFrequencyRange(long start, long stop)
    {
        List<Channel> channels = new ArrayList<>();

        for(Channel channel : mChannels)
        {
            if(channel.isWithin(start, stop))
            {
                channels.add(channel);
            }
        }

        for(Channel channel : mTrafficChannels)
        {
            if(channel.isWithin(start, stop))
            {
                channels.add(channel);
            }
        }

        return channels;
    }

    /**
     * Adds a listener to receive channel events
     */
    public void addListener(Listener<ChannelEvent> listener)
    {
        mChannelEventBroadcaster.addListener(listener);
    }

    /**
     * Removes listener from receiving channel events
     */
    public void removeListener(Listener<ChannelEvent> listener)
    {
        mChannelEventBroadcaster.removeListener(listener);
    }

    //Table Model Interface Methods - standard channels only
    @Override
    public int getRowCount()
    {
        return mChannels.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        if(columnIndex < COLUMN_NAMES.length)
        {
            return COLUMN_NAMES[columnIndex];
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        Channel channel = mChannels.get(rowIndex);

        switch(columnIndex)
        {
            case COLUMN_ENABLED:
                return channel.isProcessing() ? VALUE_YES : null;
            case COLUMN_SYSTEM:
                return channel.getSystem();
            case COLUMN_SITE:
                return channel.getSite();
            case COLUMN_NAME:
                return channel.getName();
            case COLUMN_ALIAS_LIST:
                return channel.getAliasListName();
            case COLUMN_SOURCE:
                return channel.getSourceConfiguration().getDescription();
            case COLUMN_DECODER:
                return channel.getDecodeConfiguration().getDecoderType().getShortDisplayString();
            case COLUMN_AUTO_START:
                if(channel.isAutoStart())
                {
                    if(channel.hasAutoStartOrder())
                    {
                        return channel.getAutoStartOrder();
                    }
                    else
                    {
                        return VALUE_YES;
                    }
                }
                break;
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        throw new IllegalArgumentException("Not yet implemented");
    }

    public void createChannel(DecoderType decoderType, long frequency)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
