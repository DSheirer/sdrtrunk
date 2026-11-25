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

package io.github.dsheirer.gui.status;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.Setting;

/**
 * Decoding channels: the channels that are processing, with the temporary traffic channels counted separately in
 * the tooltip.
 */
public class ChannelStatusItem extends ServiceStatusItem
{
    private final ChannelProcessingManager mChannelProcessingManager;
    private final Listener<ChannelEvent> mChannelEventListener = this::receive;
    private String mActionHint = "";

    /**
     * Constructs an instance.  JavaFX thread only.
     */
    public ChannelStatusItem(ChannelProcessingManager channelProcessingManager)
    {
        super("Channels", Setting.MAIN_WINDOW_STATUS_ITEM_CHANNELS);
        mChannelProcessingManager = channelProcessingManager;
        mChannelProcessingManager.addChannelEventListener(mChannelEventListener);
        update();
    }

    /**
     * Sets the click action and the hint that the tooltip shows for it.
     */
    public void setAction(Runnable action, String hint)
    {
        mActionHint = hint != null ? hint : "";
        setAction(action);
        update();
    }

    private void receive(ChannelEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_PROCESSING_START, NOTIFICATION_PROCESSING_STOP -> scheduleUpdate();
            default -> {}
        }
    }

    @Override
    protected void update()
    {
        int channels = 0;
        int traffic = 0;

        for(Channel channel: mChannelProcessingManager.getProcessingChannels())
        {
            if(channel.isTrafficChannel())
            {
                traffic++;
            }
            else
            {
                channels++;
            }
        }

        int total = channels + traffic;
        String summary;

        if(total == 0)
        {
            summary = "No channels are decoding.  Start channels in the playlist editor.";
        }
        else
        {
            summary = total + (total == 1 ? " channel" : " channels") + " decoding: " + channels +
                (channels == 1 ? " channel" : " channels") + " and " + traffic +
                (traffic == 1 ? " traffic channel." : " traffic channels.");
        }

        show(total > 0 ? StatusSeverity.OK : StatusSeverity.NEUTRAL, "Channels " + total,
            summary + (mActionHint.isEmpty() ? "" : "\n\n" + mActionHint));
    }

    @Override
    public void dispose()
    {
        mChannelProcessingManager.removeChannelEventListener(mChannelEventListener);
    }
}
