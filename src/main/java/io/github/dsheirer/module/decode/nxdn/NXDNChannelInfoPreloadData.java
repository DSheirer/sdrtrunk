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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.controller.channel.event.PreloadDataContent;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import java.util.List;

/**
 * Channel access info and optional channel frequencies to preload into a traffic channel.
 */
public class NXDNChannelInfoPreloadData extends PreloadDataContent<ChannelInformation>
{
    /**
     * Constructs an instance
     *
     * @param info from the channel to preload
     * @param frequencies optional provided by the user in the config
     */
    public NXDNChannelInfoPreloadData(ChannelAccessInformation info, List<ChannelFrequency> frequencies)
    {
        super(new ChannelInformation(info, frequencies));
    }
}
