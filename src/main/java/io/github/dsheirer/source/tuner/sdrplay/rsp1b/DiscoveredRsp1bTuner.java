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

package io.github.dsheirer.source.tuner.sdrplay.rsp1b;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceInfo;

/**
 * RSP1B discovered tuner.
 */
public class DiscoveredRsp1bTuner extends DiscoveredRspTuner<IControlRsp1b>
{
    /**
     * Constructs an instance
     * @param deviceInfo for controlling the RSP1A after it's been started
     * @param channelizerType to use for the tuner once started
     */
    public DiscoveredRsp1bTuner(DeviceInfo deviceInfo, ChannelizerType channelizerType)
    {
        super(deviceInfo, channelizerType);
    }
}
