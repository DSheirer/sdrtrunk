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

package io.github.dsheirer.source.tuner.sdrplay.rsp2;

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.api.device.DeviceInfo;

/**
 * RSP2 discovered tuner.
 */
public class DiscoveredRsp2Tuner extends DiscoveredRspTuner<IControlRsp2>
{
    /**
     * Constructs an instance
     * @param deviceInfo for the tuner
     * @param channelizerType to use for the tuner once started
     */
    public DiscoveredRsp2Tuner(DeviceInfo deviceInfo, ChannelizerType channelizerType)
    {
        super(deviceInfo, channelizerType);
    }
}
