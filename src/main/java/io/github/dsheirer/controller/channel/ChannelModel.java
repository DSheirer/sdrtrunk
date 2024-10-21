/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent.Event;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Channel Model
 */
public class ChannelModel implements Listener<ChannelEvent>
{
    private ObservableList<Channel> mChannels = FXCollections.observableArrayList(Channel.extractor());
    private ObservableList<Channel> mTrafficChannels = FXCollections.observableArrayList(Channel.extractor());
    private Broadcaster<ChannelEvent> mChannelEventBroadcaster = new Broadcaster();
    private AliasModel mAliasModel;

    public ChannelModel(AliasModel aliasModel)
    {
        mAliasModel = aliasModel;

        //Register a listener to detect channel changes and broadcast change events to cause playlist save requests
        ChannelListChangeListener changeListener = new ChannelListChangeListener();
        mChannels.addListener(changeListener);
        mTrafficChannels.addListener(changeListener);
    }

    /**
     * Get a deduplicated list of alias list names from across the channel configurations.
     * @return list of alias list names.
     */
    public List<String> getAliasListNames()
    {
        List<String> aliasListNames = new ArrayList<>();

        for(Channel channel : mChannels)
        {
            String aliasListName = channel.getAliasListName();

            if(aliasListName != null && !aliasListName.isEmpty() && !aliasListNames.contains(aliasListName))
            {
                aliasListNames.add(aliasListName);
            }
        }

        return aliasListNames;
    }

    /**
     * Renames the alias list across the set of aliases.
     * @param oldName currently used by the alias
     * @param newName to apply to the alias
     */
    public void renameAliasList(String oldName, String newName)
    {
        if(oldName == null || oldName.isEmpty() || newName == null || newName.isEmpty())
        {
            return;
        }

        mChannels.stream().filter(channel -> channel.getAliasListName().equals(oldName))
                .forEach(channel -> channel.setAliasListName(newName));
    }

    /**
     * Deletes any aliases that have the alias list name
     * @param aliasListName to delete
     */
    public void deleteAliasList(String aliasListName)
    {
        if(aliasListName == null || aliasListName.isEmpty())
        {
            return;
        }

        mChannels.stream().filter(channel -> channel.getAliasListName().equals(aliasListName))
                .forEach(channel -> channel.setAliasListName(null));
    }

    /**
     * Observable list of channel configurations managed by this model
     */
    public ObservableList<Channel> channelList()
    {
        return mChannels;
    }

    /**
     * Observable list of traffic channel configurations managed by this model
     */
    public ObservableList<Channel> trafficChannelList()
    {
        return mTrafficChannels;
    }

    /**
     * Removes all channels and traffic channels and fires remove event for each.
     */
    public void clear()
    {
        List<Channel> trafficChannels = new ArrayList<>(mTrafficChannels);

        for(Channel trafficChannel: trafficChannels)
        {
            removeChannel(trafficChannel);
        }

        List<Channel> channels = new ArrayList<>(mChannels);

        for(Channel channel: channels)
        {
            removeChannel(channel);
        }
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
     * Renames the alias list across all channels.
     * @param existing alias list name
     * @param renamed alias list name.
     */
    public void renameAliaslistForChannels(String existing, String renamed)
    {
        if(existing == null || existing.isEmpty() || renamed == null || renamed.isEmpty())
        {
            return;
        }

        mChannels.forEach(channel -> {
            if(channel.getAliasListName() != null && channel.getAliasListName().equals(existing))
            {
                channel.setAliasListName(renamed);
            }
        });
    }

    /**
     * Removes the alias list name from any channels.
     * @param name to delete
     */
    public void deleteAliasListName(String name)
    {
        if(name == null || name.isEmpty())
        {
            return;
        }

        mChannels.forEach(channel -> {
            if(channel.getAliasListName() != null && channel.getAliasListName().equals(name))
            {
                channel.setAliasListName(null);
            }
        });
    }

    /**
     * Returns a list of unique system values from across the channel set
     */
    public List<String> getSystemNames()
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
                    //rebroadcast this event to any listeners
                    mChannelEventBroadcaster.broadcast(event);
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
    public void addChannel(Channel channel)
    {
        switch(channel.getChannelType())
        {
            case STANDARD:
                mChannels.add(channel);
                //Make sure the alias model has the alias list referred to by the channel
                mAliasModel.addAliasList(channel.getAliasListName());
                break;
            case TRAFFIC:
                mTrafficChannels.add(channel);
                break;
            default:
                break;
        }
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
                    mChannels.remove(channel);
                    break;
                case TRAFFIC:
                    mTrafficChannels.remove(channel);
                    break;
                default:
                    break;
            }
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

        autoStartChannels.sort((channel1, channel2) -> {
            if (channel1.hasAutoStartOrder() && channel2.hasAutoStartOrder()) {
                return Integer.compare(channel1.getAutoStartOrder(), channel2.getAutoStartOrder());
            } else if (channel1.hasAutoStartOrder()) {
                return -1;
            } else if (channel2.hasAutoStartOrder()) {
                return 1;
            }

            return 0;
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

    /**
     * List of site names from all channels in the model
     */
    public List<String> getSiteNames()
    {
        List<String> sites = new ArrayList<>();

        for(Channel channel: mChannels)
        {
            String site = channel.getSite();

            if(site != null && !site.isEmpty() && !sites.contains(site))
            {
                sites.add(site);
            }
        }

        return sites;
    }

    /**
     * Observable list change listener for both channels and traffic channels lists
     */
    public class ChannelListChangeListener implements ListChangeListener<Channel>
    {
        @Override
        public void onChanged(Change<? extends Channel> change)
        {
            while(change.next())
            {
                if(change.wasAdded())
                {
                    for(Channel channel: change.getAddedSubList())
                    {
                        mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_ADD));
                    }
                }
                else if(change.wasRemoved())
                {
                    for(Channel channel: change.getRemoved())
                    {
                        mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_DELETE));
                    }
                }
                else if(change.wasUpdated())
                {
                    for(int x = change.getFrom(); x < change.getTo(); x++)
                    {
                        mChannelEventBroadcaster.broadcast(new ChannelEvent(change.getList().get(x),
                            Event.NOTIFICATION_CONFIGURATION_CHANGE));
                    }
                }
            }
        }
    }
}
