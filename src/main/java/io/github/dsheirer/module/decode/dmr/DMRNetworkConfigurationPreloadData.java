/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.controller.channel.event.PreloadDataContent;

/**
 * Preload data container for DMRNetworkConfigurationMonitor.
 *
 * Note: this is used to transfer the monitor during a Capacity plus REST channel rotation operation.  The monitor
 * is passed from the channel that is converting from REST to a TRAFFIC channel and passed to the new STANDARD channel
 * that represents the REST channel.
 */
public class DMRNetworkConfigurationPreloadData extends PreloadDataContent<DMRNetworkConfigurationMonitor>
{
    /**
     * Constructs an instance
     *
     * @param data to preload
     */
    public DMRNetworkConfigurationPreloadData(DMRNetworkConfigurationMonitor data)
    {
        super(data);
    }
}
