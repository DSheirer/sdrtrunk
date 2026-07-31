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

package io.github.dsheirer.module.decode.nxdn.layer3.broadcast;

import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import java.util.Map;

/**
 * Interface for messages that require a channel access information structure when calculating channel frequency values.
 *
 * Note: the channel access information object is from the SiteInformation broadcast message.
 */
public interface IChannelInformationReceiver
{
    /**
     * Provides the latest channel access information and any optional channel:frequency mapping entries.
     * @param channelAccessInformation from the control channel
     * @param channelFrequencyMap provided by the user that maps each channel number to a frequency.
     */
    void receive(ChannelAccessInformation channelAccessInformation, Map<Integer, ChannelFrequency> channelFrequencyMap);
}
