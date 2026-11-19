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
package io.github.dsheirer.gui.nowplaying;

import io.github.dsheirer.controller.channel.Channel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ChannelMuteActionTest
{
    private static Channel channel(String name, Channel.ChannelType type)
    {
        Channel channel = new Channel(name, type);
        channel.setSystem("Metro");
        channel.setSite("North");
        return channel;
    }

    @Test
    public void trafficChannelsShareTheirControlChannelsMuteKey()
    {
        Channel control = channel("Control", Channel.ChannelType.STANDARD);
        Channel traffic = channel("T-Control", Channel.ChannelType.TRAFFIC);

        assertEquals(ChannelMuteAction.getChannelMuteKey(control), ChannelMuteAction.getChannelMuteKey(traffic));
    }

    @Test
    public void aStandardChannelNamedWithTheTrafficPrefixKeepsItsOwnKey()
    {
        Channel control = channel("Control", Channel.ChannelType.STANDARD);
        Channel namedLikeTraffic = channel("T-Control", Channel.ChannelType.STANDARD);

        assertNotEquals(ChannelMuteAction.getChannelMuteKey(control),
            ChannelMuteAction.getChannelMuteKey(namedLikeTraffic));
    }

    @Test
    public void keyIsTheSameAcrossRunsAndIgnoresCase()
    {
        assertEquals(ChannelMuteAction.getChannelMuteKey(channel("Fire Dispatch", Channel.ChannelType.STANDARD)),
            ChannelMuteAction.getChannelMuteKey(channel("FIRE DISPATCH", Channel.ChannelType.STANDARD)));
    }
}
