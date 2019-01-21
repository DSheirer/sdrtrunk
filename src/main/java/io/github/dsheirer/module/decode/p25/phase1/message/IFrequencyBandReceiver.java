/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.phase1.message;


import io.github.dsheirer.channel.IChannelDescriptor;

import java.util.List;

/**
 * Interface to allow messages to be augmented with IdentiferUpdateXXX type
 * messages that provide the channel information necessary to calculate the
 * uplink and downlink frequency for the channel.
 */
public interface IFrequencyBandReceiver
{
    /**
     * List of APCO-25 channels provided by the message.  Channels may be enriched with frequency band
     * details by an external provider.
     *
     * @return list of channels
     */
    List<IChannelDescriptor> getChannels();
}
