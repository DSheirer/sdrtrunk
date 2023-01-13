/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.sample.Listener;

public class ChannelSelectionManager implements Listener<ChannelEvent>
{
    private Listener<ChannelEvent> mChannelEventListener;
    private Channel mSelectedChannel = null;

    /**
     * Manages channel selection state to ensure that only one channel is ever
     * in a selected state.
     */
    public ChannelSelectionManager(ChannelModel channelModel)
    {
        mChannelEventListener = channelModel;
        channelModel.addListener(ChannelSelectionManager.this);
    }

    @Override
    public void receive(ChannelEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_SELECT:
                if(mSelectedChannel != null)
                {
                    mSelectedChannel.setSelected(false);
                    mChannelEventListener.receive(new ChannelEvent(mSelectedChannel, ChannelEvent.Event.NOTIFICATION_SELECTION_CHANGE));
                    mSelectedChannel = null;
                }

                mSelectedChannel = event.getChannel();
                mSelectedChannel.setSelected(true);
                mChannelEventListener.receive(new ChannelEvent(event.getChannel(), ChannelEvent.Event.NOTIFICATION_SELECTION_CHANGE));
                break;
            case NOTIFICATION_PROCESSING_STOP:
            case REQUEST_DESELECT:
                if(mSelectedChannel != null &&
                    mSelectedChannel == event.getChannel())
                {
                    mSelectedChannel.setSelected(false);
                    mSelectedChannel = null;
                    mChannelEventListener.receive(new ChannelEvent(event.getChannel(), ChannelEvent.Event.NOTIFICATION_SELECTION_CHANGE));
                }
                break;
            default:
                break;
        }
    }
}
